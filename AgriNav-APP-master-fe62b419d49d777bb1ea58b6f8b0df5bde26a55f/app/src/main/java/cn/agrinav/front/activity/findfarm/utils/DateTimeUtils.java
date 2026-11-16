package cn.agrinav.front.activity.findfarm.utils;

import android.app.DatePickerDialog;
import android.content.Context;
import android.util.Log;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Locale;

public class DateTimeUtils {
    private static final String DATE_FORMAT = "yyyy/MM/dd";
    private static final String DATE_TIME_FORMAT = "yyyy/MM/dd HH:mm:ss";

    public interface DateSelectCallback {
        void onDateSelected(String date);
    }

    /**
     * 格式化日期时间为 yyyy/MM/dd 格式
     */
    public static String formatDate(String dateTimeStr) {
        try {
            DateTimeFormatter inputFormatter = DateTimeFormatter.ISO_DATE_TIME;
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, inputFormatter);
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
            return dateTime.format(outputFormatter);
        } catch (Exception e) {
            Log.e("DateTimeUtils", "时间格式化失败: " + e.getMessage());
            return dateTimeStr;
        }
    }

    /**
     * 格式化日期时间为 yyyy/MM/dd HH:mm:ss 格式
     */
    public static String formatDateTimeDetail(String dateTimeStr) {
        try {
            DateTimeFormatter inputFormatter = DateTimeFormatter.ISO_DATE_TIME;
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, inputFormatter);
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
            return dateTime.format(outputFormatter);
        } catch (Exception e) {
            Log.e("DateTimeUtils", "时间格式化失败: " + e.getMessage());
            return dateTimeStr;
        }
    }

    /**
     * 将日期字符串转换为LocalDateTime
     */
    public static LocalDateTime parseDateTime(String dateStr) {
        String dateTimeStr = dateStr + " 00:00:00";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
        return LocalDateTime.parse(dateTimeStr, formatter);
    }

    /**
     * 显示日期选择器
     */
    public static void showDatePicker(Context context, EditText targetEditText) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                context,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
                    targetEditText.setText(sdf.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }
    /**
     * 显示日期选择器（无日期限制）
     */
    public static void showDatePickerWithoutLimit(Context context, EditText targetEditText, DateSelectCallback callback) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                context,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
                    String selectedDate = sdf.format(calendar.getTime());
                    targetEditText.setText(selectedDate);
                    if (callback != null) {
                        callback.onDateSelected(selectedDate);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
    /**
     * 显示日期选择器（有最小日期限制）
     */
    public static void showDatePickerWithLimit(Context context, EditText targetEditText, String minDateStr) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                context,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
                    targetEditText.setText(sdf.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // 设置最小日期
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
            Calendar minDate = Calendar.getInstance();
            minDate.setTime(sdf.parse(minDateStr));
            datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());
        } catch (Exception e) {
            Log.e("DateTimeUtils", "解析日期失败: " + e.getMessage());
        }

        datePickerDialog.show();
    }
    /**
     * 比较两个日期字符串
     * @return 如果date1早于date2返回-1，相等返回0，晚于返回1
     */
    public static int compareDate(String date1, String date2) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
            return sdf.parse(date1).compareTo(sdf.parse(date2));
        } catch (Exception e) {
            Log.e("DateTimeUtils", "比较日期失败: " + e.getMessage());
            return 0;
        }
    }
}