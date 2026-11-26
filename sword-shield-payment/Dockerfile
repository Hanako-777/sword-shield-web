# syntax=docker/dockerfile:1

FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /workspace

COPY pom.xml ./
COPY sword-shield-common/pom.xml sword-shield-common/pom.xml
COPY sword-shield-api/pom.xml sword-shield-api/pom.xml
COPY sword-shield-gateway/pom.xml sword-shield-gateway/pom.xml
COPY sword-shield-user/pom.xml sword-shield-user/pom.xml
COPY sword-shield-activity/pom.xml sword-shield-activity/pom.xml
COPY sword-shield-content/pom.xml sword-shield-content/pom.xml
COPY sword-shield-interaction/pom.xml sword-shield-interaction/pom.xml
COPY sword-shield-file/pom.xml sword-shield-file/pom.xml
COPY sword-shield-admin/pom.xml sword-shield-admin/pom.xml
COPY sword-shield-ai/pom.xml sword-shield-ai/pom.xml
COPY sword-shield-payment/pom.xml sword-shield-payment/pom.xml
RUN mvn -B -DskipTests -pl sword-shield-payment -am dependency:go-offline

COPY . .
RUN mvn -B -DskipTests -pl sword-shield-payment -am package && \
    mvn -B -DskipTests -f sword-shield-payment/pom.xml spring-boot:repackage

FROM eclipse-temurin:17-jre
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    JAVA_AGENT_OPTS=""
WORKDIR /app
COPY --from=builder /workspace/sword-shield-payment/target/sword-shield-payment-*.jar app.jar
COPY otel/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
EXPOSE 8108
ENTRYPOINT ["sh","-c","java ${JAVA_OPTS} ${JAVA_AGENT_OPTS} -jar /app/app.jar"]
