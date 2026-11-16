package cn.agrinav.front.activity.findjob.utils;

import android.app.DatePickerDialog;
import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import cn.agrinav.R;

public class JobDateTimeUtils {
    private static final String DATE_FORMAT = "yyyy/MM/dd";
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    
    /**
     * 显示日期选择器（仅允许选择今天及以后的日期）
     */
    public static void showDatePicker(Context context, TextView targetTextView) {
        // 获取当前日期
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            context,
            (view, selectedYear, selectedMonth, selectedDay) -> {
                // 检查选择的日期是否在今天之后
                Calendar selectedDate = Calendar.getInstance();
                selectedDate.set(selectedYear, selectedMonth, selectedDay);
                
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);
                
                if (selectedDate.before(today)) {
                    JobUIUtils.showToast(context, context.getString(R.string.find_job_valid_period_future));
                    return;
                }
                
                // 格式化并显示选择的日期
                String formattedDate = formatDate(selectedDate.getTime());
                targetTextView.setText(formattedDate);
                targetTextView.setError(null); // 清除错误提示
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );

        // 设置最小日期为今天
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    /**
     * 格式化日期为yyyy/MM/dd格式
     */
    public static String formatDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        return dateFormat.format(date);
    }

    /**
     * 解析日期字符串为Date对象
     */
    public static Date parseDate(String dateStr) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
            return dateFormat.parse(dateStr);
        } catch (ParseException e) {
            Log.e("JobDateTimeUtils", "Date parse failed", e);
            return null;
        }
    }

    /**
     * 格式化日期为后端所需的格式 (yyyy-MM-ddTHH:mm:ss)
     */
    public static String formatDateForServer(Date date) {
        if (date == null) return null;
        
        // 设置日期为当天的23:59:59
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        
        Instant instant = calendar.getTime().toInstant();
        LocalDateTime time = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
        return time.format(formatter);
    }

    /**
     * 解析服务器返回的时间字符串
     * @param dateTimeStr 服务器返回的时间字符串
     * @return 解析后的Date对象，解析失败时返回当前时间
     */
    public static Date parseServerDateTime(String dateTimeStr) {
        try {
            // 移除毫秒部分
            if (dateTimeStr.contains(".")) {
                dateTimeStr = dateTimeStr.substring(0, dateTimeStr.indexOf("."));
            }
            SimpleDateFormat formatter = new SimpleDateFormat(DATE_TIME_FORMAT);
            return formatter.parse(dateTimeStr);
        } catch (Exception e) {
            Log.e("JobDateTimeUtils", "Date parsing error for: " + dateTimeStr, e);
            return new Date(); // 返回当前时间作为默认值
        }
    }
} 