# AgriNav GPS / JT808 接入服务镜像（Netty，JDK21 运行时）
FROM eclipse-temurin:21-jre

# healthcheck 需要 curl
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 由 build-all.ps1 从 jtt808-server/target 复制为 app.jar
COPY app.jar /app/app.jar

EXPOSE 8100 7100 7200

# GPS 服务内存略小于 CMS
ENV JAVA_OPTS="-Xms256m -Xmx384m -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
