# Secure Transfer

[![Build Status](https://api.travis-ci.org/osiegmar/setra.svg)](https://travis-ci.org/osiegmar/setra)

Web application to transfer text messages and files securely.


## Prerequisites

This application needs strong cryptography. Either install
[Java Cryptography Extension (JCE)](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html)
when using Oracle JDK or use [OpenJDK](http://openjdk.java.net).


## Quick start

Use prebuilt docker image [osiegmar/setra](https://hub.docker.com/r/osiegmar/setra/) or create
your own via:


```sh
./gradlew build
docker build -t secure-transfer .
docker run -d --name secure-transfer secure-transfer
```

Then call http://[your docker host]:8080


## More advanced configuration

```sh
docker run \
    --detach \
    --publish 127.0.0.1:8500:8080 \
    --name setra \
    --env SERVER_USE_FORWARD_HEADERS=true \
    osiegmar/setra
```

And a NGINX proxy configuration:

```
location / {
    proxy_pass http://localhost:8500/;

    # Keep this value in sync with SPRING_HTTP_MULTIPART_MAX_REQUEST_SIZE
    client_max_body_size 2048M;

    proxy_buffering off;
    proxy_request_buffering off;

    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

### Configuration properties

Secure Transfer uses [Spring Boot](https://projects.spring.io/spring-boot/) and thus offers a
wide range of configuration properties. The following is just an overview of the most important
options.


* **SECURETRANSFER_BASE_DIR**:
  The base directory where Secure Transfer will create its own directory structure.
  Default: /securetransfer within the docker build, ${java.io.tmpdir}/securetransfer otherwise.

* **SECURETRANSFER_MAX_FILE_SIZE**:
  Max file size. Values can use the suffixed "MB" or "KB" to indicate a Megabyte or Kilobyte size.
  Default: 2147483648 (2 GB)

* **SECURETRANSFER_MAX_REQUEST_SIZE**:
  Max request size. Values can use the suffixed "MB" or "KB" to indicate a Megabyte or Kilobyte
  size.
  Default: 2147483648 (2 GB)

* **SERVER_USE_FORWARD_HEADERS**:
  If X-Forwarded-* headers should be applied to the HttpRequest.
  Default: false

* **SPRING_MVC_ASYNC_REQUEST_TIMEOUT**:
  The maximum duration (in milliseconds) for a file download.
  Default: 3600000 ms (1 hour)
