package io.taucoin.torrent.publishing.core.log;

public enum TauLogLevel {
    EMERG(0),           // system is unusable
    ALERT(1),           // action must be taken immediately
    CRIT(2),            // critical conditions
    ERROR(3),           // error conditions
    WARN(4),            // warning conditions
    NOTICE(5),          // normal but significant condition
    INFO(6),            // informational
    DEBUG(7);           // debug-level messages


    private int level;

    TauLogLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
