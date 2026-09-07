package app.dmonly;

import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** What the accessibility tree of the current Instagram screen looks like: visible view ids and descriptions. */
final class Snapshot {
    final long time;
    final List<String> ids;
    final List<String> descs;
    final int nodeCount;
    final boolean truncated;

    private Snapshot(long time, List<String> ids, List<String> descs, int nodeCount, boolean truncated) {
        this.time = time;
        this.ids = ids;
        this.descs = descs;
        this.nodeCount = nodeCount;
        this.truncated = truncated;
    }

    /** Breadth-first walk over at most {@code maxNodes} nodes, collecting visible ids and content descriptions. */
    static Snapshot capture(AccessibilityNodeInfo root, int maxNodes) {
        LinkedHashSet<String> ids = new LinkedHashSet<String>();
        LinkedHashSet<String> descs = new LinkedHashSet<String>();
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<AccessibilityNodeInfo>();
        queue.add(root);
        int count = 0;
        boolean truncated = false;
        while (!queue.isEmpty()) {
            if (count >= maxNodes) {
                truncated = true;
                break;
            }
            AccessibilityNodeInfo node = queue.poll();
            if (node == null) {
                continue;
            }
            count++;
            if (node.isVisibleToUser()) {
                String id = node.getViewIdResourceName();
                if (id != null) {
                    int slash = id.indexOf('/');
                    String name = slash >= 0 ? id.substring(slash + 1) : id;
                    ids.add(name);
                    if (node.isSelected()) {
                        // Lets a rule target the active bottom tab, e.g. "clips_tab [selected]".
                        ids.add(name + " [selected]");
                    }
                }
                CharSequence desc = node.getContentDescription();
                if (desc != null) {
                    String d = desc.toString().trim();
                    if (!d.isEmpty()) {
                        descs.add(d.length() > 80 ? d.substring(0, 80) : d);
                    }
                }
            }
            int n = node.getChildCount();
            for (int i = 0; i < n; i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    queue.add(child);
                }
            }
        }
        return new Snapshot(System.currentTimeMillis(), new ArrayList<String>(ids),
                new ArrayList<String>(descs), count, truncated);
    }
}
