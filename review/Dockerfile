# ---- Build Stage ----
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY common/pom.xml common/
COPY gateway/pom.xml gateway/
COPY auth/pom.xml auth/
COPY merchant/pom.xml merchant/
COPY role/pom.xml role/
COPY user/pom.xml user/
COPY email-service/pom.xml email-service/
COPY banner/pom.xml banner/
COPY order/pom.xml order/
COPY order_item/pom.xml order_item/
COPY transaction/pom.xml transaction/
COPY category/pom.xml category/
COPY merchant_award/pom.xml merchant_award/
COPY merchant_business/pom.xml merchant_business/
COPY merchant_detail/pom.xml merchant_detail/
COPY merchant_policy/pom.xml merchant_policy/
COPY product/pom.xml product/
COPY cart/pom.xml cart/
COPY review/pom.xml review/
COPY review_detail/pom.xml review_detail/
COPY shipping_address/pom.xml shipping_address/
COPY slider/pom.xml slider/
COPY chaos.yaml chaos.yaml

COPY common/src common/src
COPY review/src review/src

RUN mvn clean package -pl review -am -DskipTests -q

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/review/target/quarkus-app/ ./
EXPOSE 9017 8097
USER 1001
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
