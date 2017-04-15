# Secure Transfer

[![Build Status](https://api.travis-ci.org/osiegmar/setra.svg)](https://travis-ci.org/osiegmar/setra)

Web application to transfer text messages and files securely.

## Quick start

```sh
./gradlew build
docker build -t secure-transfer .
docker run -d --name secure-transfer secure-transfer
```

Then call http://[your docker host]:8080
