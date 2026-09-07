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
     * View-id fragments of the screens we keep the user away from: the home feed, Reels, Explore and stories.
     * Instagram renames ids from time to time; the in-app screen inspector exists so these can be fixed
     * without rebuilding the app.
     */
    static final String DEFAULT_BLOCK = ""
            + "# Home feed: the post rows of the timeline\n"
            + "row_feed_\n"
            + "feed_timeline\n"
            + "# Reels tab and the Reels player (a reel opened from a chat goes back to the chat)\n"
            + "clips_tab [selected]\n"
            + "clips_viewer -> back\n"
            + "# Explore grid on the Search tab\n"
            + "explore_grid\n"
            + "explore_\n"
            + "# Stories player\n"
            + "reel_viewer -> back\n";

    /** Screens that must stay reachable. These win over the block list, and strict mode allows only these. */
    static final String DEFAULT_ALLOW = ""
            + "# Messages: inbox, chats, message requests\n"
            + "direct_\n"
            + "row_thread_composer\n"
            + "# Profiles (yours and other people's)\n"
            + "profile_header\n"
            + "# Follow requests, followers / following lists, notifications\n"
            + "follow_request\n"
            + "follow_list\n"
            + "row_newsfeed\n"
            + "activity_feed\n"
            + "# People search results\n"
            + "row_search_user\n";

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
