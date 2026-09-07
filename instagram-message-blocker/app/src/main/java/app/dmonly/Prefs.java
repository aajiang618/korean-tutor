package app.dmonly;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

/** Thin wrapper around SharedPreferences plus a few small helpers shared by the UI and the service. */
final class Prefs {
    static final String FILE = "dmonly";

    static final String KEY_ENABLED = "enabled";
    static final String KEY_STRICT = "strict";
    static final String KEY_HOME_ON_BACKOUT = "home_on_backout";
    static final String KEY_ACTION = "action";
    static final String KEY_REDIRECT_URL = "redirect_url";
    static final String KEY_BLOCK = "block_patterns";
    static final String KEY_ALLOW = "allow_patterns";
    static final String KEY_PAUSED_UNTIL = "paused_until";

    static final String ACTION_INBOX = "inbox";
    static final String ACTION_BACK = "back";
    static final String ACTION_HOME = "home";

    static final String INSTAGRAM_PACKAGE = "com.instagram.android";
    static final String DEFAULT_REDIRECT_URL = "https://www.instagram.com/direct/inbox/";

    /**
     * View-id fragments that only appear on screens we want to keep the user away from.
     * Instagram renames ids from time to time; the in-app screen inspector exists so these can be fixed
     * without rebuilding the app.
     */
    static final String DEFAULT_BLOCK = ""
            + "# Bottom navigation bar: any screen that shows it is not a messages screen\n"
            + "feed_tab\n"
            + "search_tab\n"
            + "clips_tab\n"
            + "profile_tab\n"
            + "creation_tab\n"
            + "# Reels player: a reel opened from a chat goes back to the chat\n"
            + "clips_viewer -> back\n"
            + "# Stories player\n"
            + "reel_viewer -> back\n"
            + "# Explore / search results\n"
            + "explore_\n"
            + "# Main feed list\n"
            + "feed_timeline\n";

    /** Fragments that identify the inbox or a chat thread. Only consulted in strict mode. */
    static final String DEFAULT_ALLOW = ""
            + "# Messages inbox and chat threads\n"
            + "direct_inbox\n"
            + "inbox_\n"
            + "direct_thread\n"
            + "thread_\n"
            + "row_thread_composer\n"
            + "message_\n";

    private Prefs() {
    }

    static SharedPreferences get(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static boolean enabled(SharedPreferences p) {
        return p.getBoolean(KEY_ENABLED, true);
    }

    static boolean strict(SharedPreferences p) {
        return p.getBoolean(KEY_STRICT, false);
    }

    static boolean homeOnBackout(SharedPreferences p) {
        return p.getBoolean(KEY_HOME_ON_BACKOUT, true);
    }

    static String action(SharedPreferences p) {
        return p.getString(KEY_ACTION, ACTION_INBOX);
    }

    static String redirectUrl(SharedPreferences p) {
        String s = p.getString(KEY_REDIRECT_URL, DEFAULT_REDIRECT_URL);
        return s == null || s.trim().isEmpty() ? DEFAULT_REDIRECT_URL : s.trim();
    }

    static String blockPatterns(SharedPreferences p) {
        return p.getString(KEY_BLOCK, DEFAULT_BLOCK);
    }

    static String allowPatterns(SharedPreferences p) {
        return p.getString(KEY_ALLOW, DEFAULT_ALLOW);
    }

    static long pausedUntil(SharedPreferences p) {
        return p.getLong(KEY_PAUSED_UNTIL, 0L);
    }

    static boolean isPaused(SharedPreferences p) {
        return pausedUntil(p) > System.currentTimeMillis();
    }

    /** True when the user has switched our accessibility service on in system settings. */
    static boolean isServiceEnabled(Context c) {
        AccessibilityManager am = (AccessibilityManager) c.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) {
            return false;
        }
        List<AccessibilityServiceInfo> list =
                am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        if (list == null) {
            return false;
        }
        for (AccessibilityServiceInfo info : list) {
            if (info.getResolveInfo() != null
                    && info.getResolveInfo().serviceInfo != null
                    && c.getPackageName().equals(info.getResolveInfo().serviceInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    /** Opens Instagram directly on the messages inbox. Returns false if Instagram refused the link. */
    static boolean openInbox(Context c, String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            i.setPackage(INSTAGRAM_PACKAGE);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
