FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --chown=10001:10001 target/*.jar app.jar
RUN mkdir -p /var/log/pawandhamdarshanandholidays_log \
    && chown -R 10001:10001 /var/log/pawandhamdarshanandholidays_log
VOLUME ["/var/log/pawandhamdarshanandholidays_log"]
ENV PAWANDHAM_LOG_ROOT=/var/log/pawandhamdarshanandholidays_log
ENV KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx512m -XX:+ExitOnOutOfMemoryError"
USER 10001

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
