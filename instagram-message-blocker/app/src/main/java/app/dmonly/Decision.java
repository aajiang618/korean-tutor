package app.dmonly;

/** The verdict the service reached for one screen, kept for the inspector. */
final class Decision {
    final long time;
    final boolean blocked;
    /** Pattern that matched, or a short explanation when nothing matched. */
    final String reason;
    /** What the service did about it. */
    final String action;
    final Snapshot snapshot;

    Decision(long time, boolean blocked, String reason, String action, Snapshot snapshot) {
        this.time = time;
        this.blocked = blocked;
        this.reason = reason;
        this.action = action;
        this.snapshot = snapshot;
    }
}
