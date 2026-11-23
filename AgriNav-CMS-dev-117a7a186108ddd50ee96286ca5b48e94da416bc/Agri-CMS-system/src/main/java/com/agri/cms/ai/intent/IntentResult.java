package com.agri.cms.ai.intent;

import java.util.List;
import java.util.Map;

/**
 * 意图识别的结构化结果（A-04）。
 *
 * @param intent        识别出的意图
 * @param confidence    置信度，0~1
 * @param parameters    从用户输入中抽取到的参数
 * @param missingParams 执行该意图尚缺失的必填参数，非空时需要先追问
 * @param rawText       用户输入原文，便于审计与回溯
 * @author agrinav
 */
public record IntentResult(
        IntentType intent,
        double confidence,
        Map<String, Object> parameters,
        List<String> missingParams,
        String rawText) {

    /**
     * 主构造：对集合参数做空值兜底，避免后续 NPE。
     */
    public IntentResult {
        parameters = parameters == null ? Map.of() : parameters;
        missingParams = missingParams == null ? List.of() : missingParams;
    }

    /**
     * 静态工厂。
     */
    public static IntentResult of(IntentType intent, double confidence,
                                  Map<String, Object> parameters,
                                  List<String> missingParams, String rawText) {
        return new IntentResult(intent, confidence, parameters, missingParams, rawText);
    }

    /**
     * 构造一个无法识别的兜底结果。
     */
    public static IntentResult unknown(String rawText) {
        return new IntentResult(IntentType.UNKNOWN, 0.0, Map.of(), List.of(), rawText);
    }

    /**
     * 必填参数是否齐全；齐全才允许继续执行。
     */
    public boolean hasAllRequiredParams() {
        return missingParams.isEmpty();
    }

    /**
     * 是否需要向用户追问；缺参时返回 true。
     */
    public boolean needFollowUp() {
        return !missingParams.isEmpty();
    }
}
