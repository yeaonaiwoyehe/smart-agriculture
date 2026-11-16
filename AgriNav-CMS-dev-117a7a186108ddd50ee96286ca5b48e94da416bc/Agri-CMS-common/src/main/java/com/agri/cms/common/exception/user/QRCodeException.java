package com.agri.cms.common.exception.user;

/**
 * @author simon
 * @since 2024/10/21
 */
public class QRCodeException extends UserException {
    private static final long serialVersionUID = 1L;

    public QRCodeException() {
        super("user.qrcode.blank", null);
    }
}
