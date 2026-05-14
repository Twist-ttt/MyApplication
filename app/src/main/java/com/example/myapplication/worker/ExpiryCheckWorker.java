package com.example.myapplication.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.myapplication.data.ItemRepository;
import com.example.myapplication.data.entity.Item;
import com.example.myapplication.util.Constants;
import com.example.myapplication.util.DateUtils;
import com.example.myapplication.util.NotificationHelper;

import java.util.List;

/**
 * 后台定期检查商品过期状态并发送通知的 Worker。
 */
public class ExpiryCheckWorker extends Worker {

    public ExpiryCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            ItemRepository repository = ItemRepository.getInstance(
                    (android.app.Application) getApplicationContext());

            String todayStr = DateUtils.getCurrentDateStr();
            List<Item> expiringItems = repository.getExpiringItems(todayStr);

            if (expiringItems != null) {
                for (Item item : expiringItems) {
                    int daysRemaining = DateUtils.getDaysRemaining(item.getExpireDate());
                    String message = DateUtils.calculateNotificationMessage(
                            item.getName(), daysRemaining);

                    // 更新状态
                    String newStatus = DateUtils.getStatus(item.getExpireDate());
                    repository.updateStatus(item.getId(), newStatus);

                    // 发送通知
                    NotificationHelper.showExpiryNotification(
                            getApplicationContext(),
                            "保质期提醒",
                            message,
                            Constants.NOTIFICATION_ID_BASE + item.getId()
                    );
                }
            }
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }
    }
}
