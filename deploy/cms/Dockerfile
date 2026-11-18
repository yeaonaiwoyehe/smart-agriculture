# AgriNav CMS 后端镜像（Spring Boot，JDK21 运行时）
FROM eclipse-temurin:21-jre

# healthcheck 需要 curl
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 由 build-all.ps1 从 Agri-CMS-admin/target 复制为 app.jar
COPY app.jar /app/app.jar

EXPOSE 8088

# 容器内限制 JVM 堆，避免与中间件争抢内存
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
