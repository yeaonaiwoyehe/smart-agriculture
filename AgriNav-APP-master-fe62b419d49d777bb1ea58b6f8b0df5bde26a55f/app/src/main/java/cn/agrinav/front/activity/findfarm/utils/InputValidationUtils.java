package cn.agrinav.front.activity.findfarm.utils;

import android.content.Context;

import java.util.List;

public class InputValidationUtils {
    private static final int MAX_TASK_NAME_LENGTH = 50;
    private static final float MAX_AREA_SIZE = 10000000f;

    /**
     * 验证手机号格式
     */
    public static boolean isValidPhoneNumber(String phone) {
        String phoneRegex = "^1[3-9]\\d{9}$";
        return phone.matches(phoneRegex);
    }

    /**
     * 验证任务表单输入
     */
    public static boolean validateFarmTaskInputs(Context context, String taskName, String area,
                                                 String phone, String operationDate, List<String> selectedOperations) {

        if (taskName.isEmpty()) {
            UIUtils.showToast(context, "请输入任务名称");
            return false;
        }

        if (taskName.length() > MAX_TASK_NAME_LENGTH) {
            UIUtils.showToast(context, "任务名称不能超过50个字符");
            return false;
        }

        if (area.isEmpty()) {
            UIUtils.showToast(context, "请输入地块亩数");
            return false;
        }

        try {
            float areaValue = Float.parseFloat(area);
            if (areaValue <= 0) {
                UIUtils.showToast(context, "地块亩数必须大于0");
                return false;
            }
            if (areaValue >= MAX_AREA_SIZE) {
                UIUtils.showToast(context, "地块亩数不能超过1000万亩");
                return false;
            }
        } catch (NumberFormatException e) {
            UIUtils.showToast(context, "请输入有效的地块亩数");
            return false;
        }

        if (selectedOperations.isEmpty()) {
            UIUtils.showToast(context, "请选择作业类型");
            return false;
        }


        if (phone.isEmpty()) {
            UIUtils.showToast(context, "请输入联系方式");
            return false;
        }

        if (!isValidPhoneNumber(phone)) {
            UIUtils.showToast(context, "请输入正确的手机号格式");
            return false;
        }

        if (operationDate.isEmpty()) {
            UIUtils.showToast(context, "请选择任务有效期");
            return false;
        }

        return true;
    }

}