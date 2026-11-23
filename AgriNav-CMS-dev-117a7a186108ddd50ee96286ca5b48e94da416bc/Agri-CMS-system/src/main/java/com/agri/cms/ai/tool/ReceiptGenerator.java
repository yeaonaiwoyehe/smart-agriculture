package com.agri.cms.ai.tool;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 回执编号生成器（A-05）。
 *
 * 写操作成功后生成可核对的回执编号，格式：RC-yyyyMMdd-8位随机串，
 * 例如 RC-20261123-A1B2C3D4。
 *
 * @author agrinav
 */
@Component
public class ReceiptGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    public String next() {
        String date = LocalDate.now().format(DATE_FORMATTER);
        String random = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase();
        return "RC-" + date + "-" + random;
    }
}
