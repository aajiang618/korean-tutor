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

    /** Why a screen was blocked and, if the rule says so, what to do about it. */
    static final class Verdict {
        final String reason;
        final String action;

        Verdict(String reason, String action) {
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

    /** Returns null when the screen is allowed, otherwise why it is blocked. */
    Verdict evaluate(Snapshot s) {
        Pattern hit = firstMatch(block, s);
        if (hit != null) {
            return new Verdict(hit.source, hit.action);
        }
        if (strict && firstMatch(allow, s) == null) {
            return new Verdict("strict mode: no messages pattern found", null);
        }
        return null;
    }
}
