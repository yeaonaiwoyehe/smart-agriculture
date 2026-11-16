package com.agri.cms.common.service;

import java.io.OutputStream;

/**
 * @author simon
 * @since 2024/10/22
 */
public interface QRCodeService {
    void generateQRCode(String content, OutputStream os) throws Exception;
}
