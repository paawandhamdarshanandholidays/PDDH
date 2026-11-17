FROM eclipse-temurin:25-jre

WORKDIR /app
COPY --chown=10001:10001 target/pddh-backend-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /var/log/pawandhamdarshanandholidays_log \
    && chown -R 10001:10001 /var/log/pawandhamdarshanandholidays_log
VOLUME ["/var/log/pawandhamdarshanandholidays_log"]
ENV PAWANDHAM_LOG_ROOT=/var/log/pawandhamdarshanandholidays_log
ENV JAVA_TOOL_OPTIONS="-XX:+UseG1GC -Xms256m -Xmx2g -XX:MaxMetaspaceSize=512m -XX:+ExitOnOutOfMemoryError"
USER 10001

EXPOSE 9091
ENTRYPOINT ["java", "-jar", "app.jar"]
