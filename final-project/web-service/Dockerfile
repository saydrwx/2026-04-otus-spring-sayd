FROM maven:3.9.11-eclipse-temurin-25-alpine AS builder
ENV APP_DIR=/opt/app
RUN mkdir -p $APP_DIR
WORKDIR $APP_DIR
COPY ./pom.xml $APP_DIR
RUN mvn dependency:go-offline -Dmaven.resolver.transport=wagon -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true
COPY ./src $APP_DIR/src
RUN mvn clean package -DskipTests -Dmaven.resolver.transport=wagon -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true

FROM eclipse-temurin:25-alpine
ENV APP_DIR=/opt/app
RUN mkdir -p $APP_DIR
WORKDIR $APP_DIR
COPY --from=builder $APP_DIR/target/web-service.jar $APP_DIR/
EXPOSE 8080
CMD ["java", "-jar", "/opt/app/web-service.jar"]
