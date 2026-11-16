package com.agri.cms.common.config;

import com.agri.cms.common.exception.user.QRCodeException;
import com.agri.cms.common.service.QRCodeService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * @author simon
 * @since 2024/10/21
 */
@Configuration
public class QRCodeConfig {
    /**
     * CODE_WIDTH：二维码宽度，单位像素
     * CODE_HEIGHT：二维码高度，单位像素
     * FRONT_COLOR：二维码前景色，0x000000 表示黑色
     * BACKGROUND_COLOR：二维码背景色，0xFFFFFF 表示白色
     * 演示用 16 进制表示，和前端页面 CSS 的取色是一样的，注意前后景颜色应该对比明显，如常见的黑白
     */
    private static final int CODE_WIDTH = 400;
    private static final int CODE_HEIGHT = 400;
    private static final int FRONT_COLOR = 0x000000;
    private static final int BACKGROUND_COLOR = 0xFFFFFF;
    private static Map<EncodeHintType, Object> hints;

    @PostConstruct
    void initGenerator() {
        hints = new HashMap<>(3);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);
    }

    /**
     * 生成二维码服务
     *
     * @return QRCodeService
     * @author simon
     * @since 2024/10/21
     */
    @Bean
    public QRCodeService QRCodeService() {
        return (content, os) -> {
            if (content == null || content.trim().isEmpty()) {
                throw new QRCodeException();
            }
            content = content.trim();
            MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
            BitMatrix bitMatrix = multiFormatWriter.encode(content, BarcodeFormat.QR_CODE, CODE_WIDTH, CODE_HEIGHT, hints);
            BufferedImage bufferedImage = new BufferedImage(CODE_WIDTH, CODE_HEIGHT, BufferedImage.TYPE_INT_BGR);
            for (int x = 0; x < CODE_WIDTH; x++) {
                for (int y = 0; y < CODE_HEIGHT; y++) {
                    bufferedImage.setRGB(x, y, bitMatrix.get(x, y) ? FRONT_COLOR : BACKGROUND_COLOR);
                }
            }
            ImageIO.write(bufferedImage, "png", os);
        };
    }


}
