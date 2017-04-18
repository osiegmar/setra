# Secure Transfer

[![Build Status](https://api.travis-ci.org/osiegmar/setra.svg)](https://travis-ci.org/osiegmar/setra)

Web application to transfer text messages and files securely.

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
    proxy_redirect off;
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

* **SERVER_USE_FORWARD_HEADERS**:
  If X-Forwarded-* headers should be applied to the HttpRequest.
  Default: false

* **SPRING_HTTP_MULTIPART_MAX_FILE_SIZE**:
  Max file size. Values can use the suffixed "MB" or "KB" to indicate a Megabyte or Kilobyte size.
  Default: 2048MB

* **SPRING_HTTP_MULTIPART_MAX_REQUEST_SIZE**:
  Max request size. Values can use the suffixed "MB" or "KB" to indicate a Megabyte or Kilobyte
  size.
  Default: 2048MB

* **SPRING_HTTP_MULTIPART_FILE_SIZE_THRESHOLD**:
  Threshold after which files will be written to disk. Values can use the suffixed "MB" or "KB" to
  indicate a Megabyte or Kilobyte size.
  Default: 5MB
