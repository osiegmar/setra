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
