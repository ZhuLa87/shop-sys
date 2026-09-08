# 正式 / staging 用:multi-stage,產出精簡的 JRE 執行環境
# build context 是專案根目錄

# ── build ────────────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# 先只複製 pom.xml 下載相依,讓這層在原始碼變動時仍能命中 layer cache
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ── runtime ──────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# curl 供 compose healthcheck 使用
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# 不用 root 執行
RUN groupadd --system --gid 1001 app \
    && useradd --system --uid 1001 --gid app app

COPY --from=build --chown=app:app /build/target/*.jar /app/app.jar

USER app
EXPOSE 8088

# MaxRAMPercentage 讓 JVM 依容器記憶體上限自動調整 heap,不要寫死 -Xmx
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]
