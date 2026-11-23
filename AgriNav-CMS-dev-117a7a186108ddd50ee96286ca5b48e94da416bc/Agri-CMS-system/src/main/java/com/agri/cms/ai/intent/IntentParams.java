package com.agri.cms.ai.intent;

/**
 * 意图参数名常量（A-04）。
 *
 * 统一管理意图识别与后续工具调用中使用的参数键，
 * 避免在代码中散落字符串字面量，保证参数命名一致。
 *
 * @author agrinav
 */
public final class IntentParams {

    /** 任务编号 */
    public static final String TASK_ID = "taskId";

    /** 作业单号 */
    public static final String WORK_ORDER_NO = "workOrderNo";

    /** 地块编号 */
    public static final String LAND_PLOT_ID = "landPlotId";

    /** 日期，格式 yyyy-MM-dd */
    public static final String DATE = "date";

    /** 月份，格式 yyyy-MM */
    public static final String MONTH = "month";

    /** 异常 / 复核的文字描述 */
    public static final String DESCRIPTION = "description";

    /** 农机编号 */
    public static final String MACHINERY_ID = "machineryId";

    private IntentParams() {
    }
}
