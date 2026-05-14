package com.example.myapplication;

import android.app.Application;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.myapplication.util.Constants;
import com.example.myapplication.util.NotificationHelper;
import com.example.myapplication.worker.ExpiryCheckWorker;

import java.util.concurrent.TimeUnit;

/**
 * Application 子类：初始化通知渠道、调度后台过期检查 Worker。
 */
public class ExpiryTrackerApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 创建通知渠道
        NotificationHelper.createNotificationChannel(this);

        // 调度周期性过期检查（每 6 小时执行一次）
        PeriodicWorkRequest expiryCheckRequest =
                new PeriodicWorkRequest.Builder(
                        ExpiryCheckWorker.class,
                        Constants.CHECK_INTERVAL_HOURS,
                        TimeUnit.HOURS
                )
                        .setInitialDelay(1, TimeUnit.MINUTES)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                Constants.WORK_NAME_EXPIRY_CHECK,
                ExistingPeriodicWorkPolicy.KEEP,
                expiryCheckRequest
        );
    }
}
