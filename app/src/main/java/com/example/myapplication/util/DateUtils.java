package com.example.myapplication.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * 日期工具类：提供保质期相关的日期计算和格式化方法。
 */
public final class DateUtils {

    /** 应用统一使用的日期格式 */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    private static final SimpleDateFormat SDF = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());

    static {
        SDF.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private DateUtils() { /* 工具类禁止实例化 */ }

    /**
     * 计算距离过期还剩多少天。
     *
     * @param expireDate 过期日期字符串（yyyy-MM-dd）
     * @return 剩余天数，负数表示已过期
     */
    public static int getDaysRemaining(String expireDate) {
        if (expireDate == null || expireDate.isEmpty()) return Integer.MAX_VALUE;
        try {
            Date expire = SDF.parse(expireDate);
            if (expire == null) return Integer.MAX_VALUE;
            Date today = getTodayUtc();
            long diff = expire.getTime() - today.getTime();
            return (int) TimeUnit.MILLISECONDS.toDays(diff);
        } catch (ParseException e) {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * 根据过期日期计算商品状态。
     */
    public static String getStatus(String expireDate) {
        int days = getDaysRemaining(expireDate);
        if (days < 0) return Constants.STATUS_EXPIRED;
        if (days <= 3) return Constants.STATUS_EXPIRING;
        return Constants.STATUS_NORMAL;
    }

    /**
     * 格式化日期字符串为 yyyy-MM-dd。
     */
    public static String formatDate(int year, int month, int dayOfMonth) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        return SDF.format(cal.getTime());
    }

    /**
     * 获取当前 UTC 日期字符串。
     */
    public static String getCurrentDateStr() {
        return SDF.format(getTodayUtc());
    }

    /**
     * 验证日期范围是否合法（购买日期不能晚于过期日期）。
     */
    public static boolean isValidDateRange(String buyDate, String expireDate) {
        if (buyDate == null || expireDate == null) return false;
        try {
            Date buy = SDF.parse(buyDate);
            Date expire = SDF.parse(expireDate);
            return buy != null && expire != null && !buy.after(expire);
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * 根据剩余天数生成通知消息。
     */
    public static String calculateNotificationMessage(String itemName, int daysRemaining) {
        if (daysRemaining < 0) {
            return "\"" + itemName + "\" 已过期 " + Math.abs(daysRemaining) + " 天，请及时处理！";
        } else if (daysRemaining == 0) {
            return "\"" + itemName + "\" 今天到期！";
        } else {
            return "\"" + itemName + "\" 将在 " + daysRemaining + " 天后过期。";
        }
    }

    /**
     * 将 UTC 毫秒时间戳转换为日期字符串。
     */
    public static String utcMillisToDateString(long utcMillis) {
        return SDF.format(new Date(utcMillis));
    }

    // ---- 内部辅助 ----

    private static Date getTodayUtc() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
