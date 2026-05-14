package com.example.myapplication.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.myapplication.R;

/**
 * 通知工具类：创建通知渠道、发送过期提醒通知。
 */
public final class NotificationHelper {

    private NotificationHelper() { /* 工具类 */ }

    /**
     * 创建通知渠道（Android 8.0+ 必需）。
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    Constants.CHANNEL_ID,
                    Constants.CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(Constants.CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * 显示一条过期提醒通知。
     *
     * @param context   上下文
     * @param title     通知标题
     * @param message   通知内容
     * @param notifyId  通知 ID（用于区分不同商品）
     */
    public static void showExpiryNotification(Context context, String title, String message, int notifyId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        try {
            NotificationManagerCompat.from(context).notify(notifyId, builder.build());
        } catch (SecurityException e) {
            // Android 13 未授予通知权限时忽略
        }
    }
}
