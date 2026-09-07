package app.dmonly;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pattern lists that decide whether a screen is allowed.
 *
 * Syntax, one entry per line:
 * <pre>
 *   clips_tab              plain entry: case-insensitive substring of a view id (package prefix removed)
 *   clips_tab [selected]   the same id, but only while that node is selected (the active bottom tab)
 *   desc:reels             matches content descriptions instead of ids
 *   clips_viewer -> back   optional action override: inbox, back or home
 *   # comment
 * </pre>
 */
final class Rules {
    static final class Pattern {
        final boolean onDescription;
        final String needle;
        /** Action override for this pattern, or null to use the global setting. */
        final String action;
        final String source;

        Pattern(boolean onDescription, String needle, String action, String source) {
            this.onDescription = onDescription;
            this.needle = needle;
            this.action = action;
            this.source = source;
        }
    }

    /** Whether a screen is allowed, which rule decided it and, for blocked screens, an optional action override. */
    static final class Verdict {
        final boolean blocked;
        final String reason;
        final String action;

        Verdict(boolean blocked, String reason, String action) {
            this.blocked = blocked;
            this.reason = reason;
            this.action = action;
        }
    }

    final List<Pattern> block;
    final List<Pattern> allow;
    final boolean strict;

    Rules(String blockText, String allowText, boolean strict) {
        this.block = parse(blockText);
        this.allow = parse(allowText);
        this.strict = strict;
    }

    static List<Pattern> parse(String text) {
        List<Pattern> out = new ArrayList<Pattern>();
        if (text == null) {
            return out;
        }
        for (String rawLine : text.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String action = null;
            int arrow = line.indexOf("->");
            if (arrow >= 0) {
                String a = line.substring(arrow + 2).trim().toLowerCase(Locale.ROOT);
                if (Prefs.ACTION_INBOX.equals(a) || Prefs.ACTION_BACK.equals(a) || Prefs.ACTION_HOME.equals(a)) {
                    action = a;
                }
                line = line.substring(0, arrow).trim();
            }
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.startsWith("desc:")) {
                String needle = lower.substring(5).trim();
                if (!needle.isEmpty()) {
                    out.add(new Pattern(true, needle, action, rawLine.trim()));
                }
            } else if (!lower.isEmpty()) {
                out.add(new Pattern(false, lower, action, rawLine.trim()));
            }
        }
        return out;
    }

    /** Returns the first matching pattern, or null. */
    static Pattern firstMatch(List<Pattern> patterns, Snapshot s) {
        for (Pattern p : patterns) {
            List<String> haystack = p.onDescription ? s.descs : s.ids;
            for (String h : haystack) {
                if (h.toLowerCase(Locale.ROOT).contains(p.needle)) {
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * Allow rules win over block rules, so a profile opened from the feed stack stays reachable even though
     * the feed itself is blocked. Screens matching neither list are allowed unless strict mode is on.
     */
    Verdict evaluate(Snapshot s) {
        Pattern ok = firstMatch(allow, s);
        if (ok != null) {
            return new Verdict(false, "allowed by: " + ok.source, null);
        }
        Pattern hit = firstMatch(block, s);
        if (hit != null) {
            return new Verdict(true, "blocked by: " + hit.source, hit.action);
        }
        if (strict) {
            return new Verdict(true, "strict mode: no allow rule matched", null);
        }
        return new Verdict(false, "no rule matched", null);
    }
}
