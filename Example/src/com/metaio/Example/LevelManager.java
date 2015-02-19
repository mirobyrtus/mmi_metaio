package com.metaio.Example;

public class LevelManager {

    private static long timestamp = 0;
    private static int level = 1;
    private static final long levelDuration = 10000;

    public void initLevelManager() {
        timestamp = 0;
        level = 1;
    }

    public void reset() {
        timestamp = 0;
        level = 1;
    }

    public int getLevel() {
        long currentTimeMillis = System.currentTimeMillis();

        if (this.timestamp == 0) {
            this.timestamp = currentTimeMillis;
        } else {

            if (currentTimeMillis - this.timestamp >= levelDuration) {
                this.timestamp = currentTimeMillis;
                level++;
            }
        }
        return level;
    }

}
