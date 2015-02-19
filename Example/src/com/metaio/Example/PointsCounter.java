package com.metaio.Example;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;

public class PointsCounter {

    public static final long SLEEPTIME = 10;
    boolean running;
    Thread refreshThread;
    double time;

    TextView timeView;
    Activity a;

    public PointsCounter(Activity a, TextView timeView) {
        running = false;
        time = 0;
        this.a = a;
        this.timeView = timeView;
    }

    public void stopThread() {
        running = false;
    }

    public void initThread() {
        if (running) return;

        time = 0;
        running = true;

        refreshThread = new Thread(new Runnable() {
            public void run() {
                while (running) {
                    time = time + 0.01;
                    try {
                        Thread.sleep(SLEEPTIME);
                    } catch (InterruptedException ex) {
                        //
                        Log.e("SLEEP", "Not succesfull!");
                    }
                    a.runOnUiThread(new Runnable() {
                        public void run() {
                            // Time: %1$s

                            timeView.setText(String.format("Time: %1$s ", String.format("%.2f", time)));

                        }
                    });

                }
            }
        });
        refreshThread.start();
    }

}
