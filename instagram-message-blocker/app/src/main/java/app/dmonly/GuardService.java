package app.dmonly;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Watches Instagram's window. Whenever the visible screen matches a block rule (feed, Reels, Explore, stories,
 * profile...) it sends the user back to the messages inbox instead. Runs only while Instagram is in front.
 */
public class GuardService extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int MAX_NODES = 500;
    /** Wait for the screen to settle before scanning it. */
    private static final long DEBOUNCE_MS = 200;
    /** Ignore events for a moment after we acted, so the transition to the inbox is not scanned mid-way. */
    private static final long GRACE_AFTER_ACTION_MS = 1500;
    /** A messages screen older than this no longer counts as "the user just backed out of messages". */
    private static final long BACKOUT_MAX_AGE_MS = 10 * 60 * 1000L;
    /** Loop guard: this many actions inside the window means we cannot recognise the inbox. */
    private static final int LOOP_MAX_ACTIONS = 6;
    private static final long LOOP_WINDOW_MS = 12000;
    private static final long LOOP_PAUSE_MS = 30000;
    private static final int HISTORY_SIZE = 20;

    static final String ACTION_NONE = "none";
    static final String ACTION_LOOP_PAUSE = "loop guard: paused";

    private static volatile boolean running;
    private static volatile Decision lastDecision;
    private static final ArrayDeque<Decision> HISTORY = new ArrayDeque<Decision>();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable checkRunnable = new Runnable() {
        @Override
        public void run() {
            check();
        }
    };
    private final ArrayDeque<Long> actionTimes = new ArrayDeque<Long>();

    private SharedPreferences prefs;
    private Rules rules;
    private boolean enabled;
    private String action;
    private String redirectUrl;
    private boolean homeOnBackout;
    private long pausedUntil;

    private long graceUntil;
    /** True when the previous scan of Instagram (in this foreground session) showed a messages screen. */
    private boolean prevAllowed;
    private long prevAllowedAt;
    private String imePackage;

    static boolean isRunning() {
        return running;
    }

    static Decision lastDecision() {
        return lastDecision;
    }

    static List<Decision> history() {
        synchronized (HISTORY) {
            return new ArrayList<Decision>(HISTORY);
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        prefs = Prefs.get(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        loadPrefs();
        running = true;
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        running = false;
        handler.removeCallbacks(checkRunnable);
        if (prefs != null) {
            prefs.unregisterOnSharedPreferenceChangeListener(this);
        }
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        running = false;
        handler.removeCallbacks(checkRunnable);
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        loadPrefs();
    }

    private void loadPrefs() {
        enabled = Prefs.enabled(prefs);
        action = Prefs.action(prefs);
        redirectUrl = Prefs.redirectUrl(prefs);
        homeOnBackout = Prefs.homeOnBackout(prefs);
        pausedUntil = Prefs.pausedUntil(prefs);
        rules = new Rules(Prefs.blockPatterns(prefs), Prefs.allowPatterns(prefs), Prefs.strict(prefs));
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!enabled || event == null) {
            return;
        }
        CharSequence pkg = event.getPackageName();
        if (pkg == null) {
            return;
        }
        if (!Prefs.INSTAGRAM_PACKAGE.contentEquals(pkg)) {
            // Another app's window came to the front: the user left Instagram, so forget what we saw there.
            // Keyboards and the system bar draw over Instagram without the user leaving it, so they don't count.
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && !isOverlayPackage(pkg)) {
                prevAllowed = false;
                handler.removeCallbacks(checkRunnable);
            }
            return;
        }
        handler.removeCallbacks(checkRunnable);
        handler.postDelayed(checkRunnable, DEBOUNCE_MS);
    }

    private boolean isOverlayPackage(CharSequence pkg) {
        if ("com.android.systemui".contentEquals(pkg) || "android".contentEquals(pkg)) {
            return true;
        }
        if (imePackage == null || !imePackage.contentEquals(pkg)) {
            String ime = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
            if (ime != null) {
                int slash = ime.indexOf('/');
                imePackage = slash > 0 ? ime.substring(0, slash) : ime;
            }
        }
        return imePackage != null && imePackage.contentEquals(pkg);
    }

    @Override
    public void onInterrupt() {
        // Nothing to interrupt: we never speak or vibrate.
    }

    private void check() {
        long now = System.currentTimeMillis();
        if (!enabled || now < pausedUntil || now < graceUntil) {
            return;
        }
        AccessibilityNodeInfo root;
        try {
            root = getRootInActiveWindow();
        } catch (RuntimeException e) {
            return;
        }
        if (root == null) {
            return;
        }
        CharSequence pkg = root.getPackageName();
        if (pkg == null || !Prefs.INSTAGRAM_PACKAGE.contentEquals(pkg)) {
            return;
        }
        Snapshot snapshot = Snapshot.capture(root, MAX_NODES);
        Rules.Verdict verdict = rules.evaluate(snapshot);
        if (!verdict.blocked) {
            prevAllowed = true;
            prevAllowedAt = now;
            record(new Decision(now, false, verdict.reason, ACTION_NONE, snapshot), false);
            return;
        }
        String chosen = chooseAction(verdict, now);
        record(new Decision(now, true, verdict.reason, chosen, snapshot), true);
        perform(chosen, now);
    }

    private String chooseAction(Rules.Verdict verdict, long now) {
        while (!actionTimes.isEmpty() && now - actionTimes.peekFirst() > LOOP_WINDOW_MS) {
            actionTimes.pollFirst();
        }
        if (actionTimes.size() >= LOOP_MAX_ACTIONS) {
            return ACTION_LOOP_PAUSE;
        }
        if (verdict.action != null) {
            return verdict.action;
        }
        // The last thing we saw was a messages screen and now a blocked one replaced it: the user backed out
        // of messages onto the feed. Leaving Instagram beats bouncing them straight back into the inbox.
        if (homeOnBackout && prevAllowed && now - prevAllowedAt < BACKOUT_MAX_AGE_MS) {
            return Prefs.ACTION_HOME;
        }
        return action;
    }

    private void perform(String chosen, long now) {
        if (ACTION_LOOP_PAUSE.equals(chosen)) {
            actionTimes.clear();
            pausedUntil = now + LOOP_PAUSE_MS;
            prefs.edit().putLong(Prefs.KEY_PAUSED_UNTIL, pausedUntil).apply();
            prevAllowed = false;
            performGlobalAction(GLOBAL_ACTION_HOME);
            Toast.makeText(this, R.string.toast_loop, Toast.LENGTH_LONG).show();
            return;
        }
        actionTimes.addLast(now);
        graceUntil = now + GRACE_AFTER_ACTION_MS;
        prevAllowed = false;
        if (Prefs.ACTION_INBOX.equals(chosen)) {
            if (!Prefs.openInbox(this, redirectUrl)) {
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
        } else if (Prefs.ACTION_BACK.equals(chosen)) {
            performGlobalAction(GLOBAL_ACTION_BACK);
        } else {
            performGlobalAction(GLOBAL_ACTION_HOME);
        }
    }

    /** Keeps the latest verdict for the inspector; allowed screens only enter the history after a block. */
    private static void record(Decision d, boolean force) {
        Decision previous = lastDecision;
        lastDecision = d;
        if (!force && previous != null && !previous.blocked) {
            return;
        }
        synchronized (HISTORY) {
            HISTORY.addFirst(d);
            while (HISTORY.size() > HISTORY_SIZE) {
                HISTORY.pollLast();
            }
        }
    }
}
