FROM java:8u111-jre
COPY securetransfer.sh /
COPY build/unzipped/META-INF /app/META-INF
COPY build/unzipped/org /app/org
COPY build/unzipped/BOOT-INF/lib /app/BOOT-INF/lib
COPY build/unzipped/BOOT-INF/classes /app/BOOT-INF/classes
VOLUME /securetransfer
ENV SECURETRANSFER_BASE_DIR=/securetransfer
EXPOSE 8080
CMD ["/securetransfer.sh"]
