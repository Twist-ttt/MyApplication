package com.example.myapplication.util;

/**
 * 全局常量定义类。
 */
public final class Constants {

    private Constants() { /* 禁止实例化 */ }

    // ---- 商品类别 ----
    public static final String CATEGORY_FOOD = "食品";
    public static final String CATEGORY_DRINK = "饮品";
    public static final String CATEGORY_DAILY = "日用品";
    public static final String CATEGORY_MEDICINE = "药品";
    public static final String CATEGORY_COSMETIC = "化妆品";
    public static final String CATEGORY_OTHER = "其他";

    public static final String[] CATEGORIES = {
            CATEGORY_FOOD,
            CATEGORY_DRINK,
            CATEGORY_DAILY,
            CATEGORY_MEDICINE,
            CATEGORY_COSMETIC,
            CATEGORY_OTHER
    };

    // ---- 商品状态 ----
    public static final String STATUS_NORMAL = "正常";
    public static final String STATUS_EXPIRING = "即将过期";
    public static final String STATUS_EXPIRED = "已过期";
    public static final String STATUS_USED = "已使用";

    // ---- 通知 ----
    public static final String CHANNEL_ID = "expiry_reminder_channel";
    public static final String CHANNEL_NAME = "保质期提醒";
    public static final String CHANNEL_DESCRIPTION = "商品过期提醒通知";

    // ---- SharedPreferences ----
    public static final String PREFS_NAME = "expiry_tracker_prefs";
    public static final String PREF_KEY_REMIND_DAYS = "default_remind_days";
    public static final int DEFAULT_REMIND_DAYS = 7;

    // ---- WorkManager ----
    public static final String WORK_NAME_EXPIRY_CHECK = "expiry_check_work";
    public static final long CHECK_INTERVAL_HOURS = 6L;   // 每 6 小时检查一次

    // ---- Intent Extra Keys ----
    public static final String EXTRA_ITEM_ID = "extra_item_id";
    public static final String EXTRA_IS_EDIT = "extra_is_edit";

    // ---- 通知 ID ----
    public static final int NOTIFICATION_ID_BASE = 1000;
}
