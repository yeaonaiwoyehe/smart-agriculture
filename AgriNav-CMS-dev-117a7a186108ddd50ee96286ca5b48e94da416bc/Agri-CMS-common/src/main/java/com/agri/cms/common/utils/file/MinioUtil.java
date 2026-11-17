package com.agri.cms.common.utils.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;
import com.agri.cms.common.config.GlobalConfig;
import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.utils.ServletUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 文件存储工具类（方案 B：去 MinIO，改为本地磁盘存储，通过 /profile 静态资源访问）
 */
public class MinioUtil {

    /**
     * 保存文件到本地磁盘并返回可访问 URL
     *
     * @param bucketName 兼容保留参数（本地方案不再使用独立桶）
     * @param fileName   相对存储根目录的文件路径（含日期目录）
     */
    public static String uploadFile(String bucketName, String fileName, MultipartFile multipartFile) throws IOException {
        try {
            File dest = new File(GlobalConfig.getProfile() + File.separator + fileName);
            if (!dest.getParentFile().exists() && !dest.getParentFile().mkdirs()) {
                throw new IOException("创建目录失败: " + dest.getParent());
            }
            try (InputStream in = multipartFile.getInputStream(); FileOutputStream out = new FileOutputStream(dest)) {
                in.transferTo(out);
            }
            return getDomain() + Constants.RESOURCE_PREFIX + "/" + fileName;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * 基于当前请求获取服务根地址（协议+域名+端口+上下文）
     */
    private static String getDomain() {
        HttpServletRequest request = ServletUtils.getRequest();
        StringBuffer url = request.getRequestURL();
        String contextPath = request.getServletContext().getContextPath();
        return url.delete(url.length() - request.getRequestURI().length(), url.length()).append(contextPath).toString();
    }
}
