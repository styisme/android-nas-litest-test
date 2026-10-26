package com.styisme.nasmin;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import java.io.IOException;

public class HttpServerService extends Service {
    public static volatile boolean running = false;
    public static volatile String lastError = "";
    private static final int PORT = 8080;
    private static final int NOTIFY_ID = 1001;

    private NasHttpServer server;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;

    @Override public void onCreate() {
        super.onCreate();
        goForeground();
        AuthManager.init(this);
        try {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "nasmin::cpu");
            wakeLock.acquire();
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "nasmin::wifi");
            wifiLock.acquire();
        } catch (Exception ignored) {}
        try {
            server = new NasHttpServer(PORT, this);
            server.start();
            running = true;
        } catch (IOException e) {
            running = false;
            lastError = e.getMessage();
        }
    }

    private void goForeground() {
        try {
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), flags);
            Notification n;
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                android.app.NotificationChannel ch = new android.app.NotificationChannel(
                        "nasmin", "NAS Min", NotificationManager.IMPORTANCE_LOW);
                if (nm != null) nm.createNotificationChannel(ch);
                n = new Notification.Builder(this, "nasmin")
                        .setContentTitle("NAS Min")
                        .setContentText("文件服务运行中 :8080")
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .setContentIntent(pi).setOngoing(true).build();
            } else if (Build.VERSION.SDK_INT >= 16) {
                n = new Notification.Builder(this)
                        .setContentTitle("NAS Min")
                        .setContentText("文件服务运行中 :8080")
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .setContentIntent(pi).setOngoing(true).build();
            } else {
                n = new Notification(android.R.drawable.stat_sys_download_done, "NAS Min", System.currentTimeMillis());
                try {
                    java.lang.reflect.Method m = n.getClass().getMethod("setLatestEventInfo",
                            Context.class, CharSequence.class, CharSequence.class, PendingIntent.class);
                    m.invoke(n, this, "NAS Min", "运行中", pi);
                } catch (Exception ignored) {}
                n.flags |= Notification.FLAG_ONGOING_EVENT;
            }
            startForeground(NOTIFY_ID, n);
        } catch (Throwable ignored) {}
    }

    @Override public int onStartCommand(Intent i, int f, int s) { return START_STICKY; }

    @Override public void onDestroy() {
        if (server != null) server.stop();
        try { if (wakeLock != null && wakeLock.isHeld()) wakeLock.release(); } catch (Exception ignored) {}
        try { if (wifiLock != null && wifiLock.isHeld()) wifiLock.release(); } catch (Exception ignored) {}
        running = false;
        try { stopForeground(true); } catch (Throwable ignored) {}
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent i) { return null; }
}
