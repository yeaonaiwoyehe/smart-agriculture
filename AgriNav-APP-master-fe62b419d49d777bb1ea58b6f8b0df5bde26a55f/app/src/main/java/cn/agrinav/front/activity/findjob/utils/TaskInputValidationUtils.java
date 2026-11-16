package cn.agrinav.front.activity.findjob.utils;

import android.content.Context;
import android.widget.EditText;
import android.widget.TextView;

import cn.agrinav.R;
import cn.agrinav.front.activity.findfarm.utils.InputValidationUtils;
import cn.agrinav.front.entity.TagSelector;

public class TaskInputValidationUtils {
    private static final int MAX_TASK_NAME_LENGTH = 30;

    public static boolean validateTaskInputs(Context context,
                                           EditText taskName,
                                           TagSelector machineTypeSelector,
                                           EditText edtMachineType,
                                           TagSelector workableCropsSelector,
                                           EditText edtWorkableCrops,
                                           TagSelector workAreaSelector,
                                           EditText edtWorkArea,
                                           TagSelector grainTransportSelector,
                                           EditText edtGrainTransport,
                                           EditText phone,
                                           TextView validityPeriod,
                                           boolean hasUserSelectedImage) {
        boolean isValid = true;

        // 验证任务名（可选）
        String taskNameText = taskName.getText().toString().trim();
        if (!taskNameText.isEmpty() && taskNameText.length() > MAX_TASK_NAME_LENGTH) {
            taskName.setError(context.getString(R.string.find_job_task_name_limit));
            isValid = false;
        }

        // 验证农机型号（必选）
        if (machineTypeSelector.getSelectedTags().isEmpty()) {
            edtMachineType.setError(context.getString(R.string.find_job_machine_type_choose));
            isValid = false;
        }

        // 验证可作业作物（必选）
        if (workableCropsSelector.getSelectedTags().isEmpty()) {
            edtWorkableCrops.setError(context.getString(R.string.find_job_work_crop_choose));
            isValid = false;
        }

        // 验证作业区域（必选）
        if (workAreaSelector.getSelectedTags().isEmpty()) {
            edtWorkArea.setError(context.getString(R.string.find_job_work_area_choose));
            isValid = false;
        }

        // 验证配备运粮车（必选）
        if (grainTransportSelector.getSelectedTags().isEmpty()) {
            edtGrainTransport.setError(context.getString(R.string.find_job_have_crop_transport));
            isValid = false;
        }

        // 验证手机号
        String phoneNumber = phone.getText().toString().trim();
        if (phoneNumber.isEmpty()) {
            phone.setError(context.getString(R.string.find_job_phone_must));
            isValid = false;
        } else if (!InputValidationUtils.isValidPhoneNumber(phoneNumber)) {
            phone.setError(context.getString(R.string.find_job_phone_valid));
            isValid = false;
        }

        // 验证任务有效期
        if (validityPeriod.getText().toString().trim().isEmpty()) {
            validityPeriod.setError(context.getString(R.string.find_job_valid_period_choose));
            isValid = false;
        }

        // 验证农机照片
        if (!hasUserSelectedImage) {
            JobUIUtils.showToast(context, context.getString(R.string.find_job_machine_image_upload));
            isValid = false;
        }

        return isValid;
    }
} 