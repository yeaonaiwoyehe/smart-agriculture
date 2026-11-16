package com.agri.cms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 启动程序
 *
 * @author ruoyi
 */

@EnableScheduling
@EnableTransactionManagement
@EnableAspectJAutoProxy(exposeProxy = true, proxyTargetClass = true)
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class AgriCMSApplication {
    public static void main(String[] args) {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(AgriCMSApplication.class, args);
        System.out.println("""
                     田界智航启动成功
                     ██       ████████  ███████   ██         ██████  ████     ████  ████████
                    ████     ██░░░░░░██░██░░░░██ ░██        ██░░░░██░██░██   ██░██ ██░░░░░░\s
                   ██░░██   ██      ░░ ░██   ░██ ░██       ██    ░░ ░██░░██ ██ ░██░██      \s
                  ██  ░░██ ░██         ░███████  ░██ █████░██       ░██ ░░███  ░██░█████████
                 ██████████░██    █████░██░░░██  ░██░░░░░ ░██       ░██  ░░█   ░██░░░░░░░░██
                ░██░░░░░░██░░██  ░░░░██░██  ░░██ ░██      ░░██    ██░██   ░    ░██       ░██
                ░██     ░██ ░░████████ ░██   ░░██░██       ░░██████ ░██        ░██ ████████\s
                ░░      ░░   ░░░░░░░░  ░░     ░░ ░░         ░░░░░░  ░░         ░░ ░░░░░░░░ \s
                                
                """);
    }
}
