package com.agri.cms.framework.security.filter;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.ReadListener;

import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

/**
 * 请求包装器
 *
 * @author mfr
 * @since 2024/11/6
 */
public class RequestWrapper extends HttpServletRequestWrapper {
    private final String body;
    private final byte[] bodyBytes;

    public RequestWrapper(HttpServletRequest request, String body) {
        super(request);
        this.body = body;
        this.bodyBytes = body.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bodyBytes);

        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // 不需要实现
            }

            @Override
            public int read() throws IOException {
                return byteArrayInputStream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new StringReader(body));
    }

    @Override
    public int getContentLength() {
        return bodyBytes.length;
    }

    @Override
    public long getContentLengthLong() {
        return bodyBytes.length;
    }
} 