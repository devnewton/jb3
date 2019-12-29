# jb3

jb3 is a KISS tribune powered by java, spring boot and mongodb.

![](https://github.com/devnewton/jb3/workflows/Java%20CI/badge.svg)
## Features

- archives
- fortunes
- bots
- last id
- XPost
- X-Post-Id
- rooms

# Demo

A live demo is accessible [here](https://jb3.devnewton.fr).

## Coincoin configuration

You can also configure your favorite coincoin using these parameters:

- backend URL: https://jb3.devnewton.fr/legacy/xml
- post URL: https://jb3.devnewton.fr/legacy/post
- post data: message=%m
- tags: encoded

## Advanced coincoin configuration

- backend using last id: https://jb3.devnewton.fr/legacy/xml?last=%last
- post returns backend: post request to https://jb3.devnewton.fr/legacy/post include backend data (same as GET https://jb3.devnewton.fr/legacy/xml response).
- post message using last id: message=%m&last=%last
- XPOST: post reply body contains last messages
- X-Post-Id: post reply returns posted message id in X-Post-Id header

## Rooms

Rooms are like IRC channel. From a coincoin, they can be seen as independent tribune.

- room backend : https://jb3.devnewton.fr/legacy/xml?room=%room
- post message in a room: message=%m&room=%room

## Gateway rooms

Gateway are used to receive and post on external tribunes or other chat systems.

Implemented gateways:

- dlfp
- euromussels
- gabuzomeu
- moules
- ototu
- sveetch

# Run demo using [docker](https://www.docker.com/)
A docker image exists, see [Package](https://github.com/devnewton/jb3/packages).

Using provided docker-compose file, simply run the following command:

```bash
docker-compose up -d
```
It uses github docker registry. You may need to authenticate before, in order to pull image.
```bash
docker login -u USERNAME -p TOKEN docker.pkg.github.com
```

See [Github help](https://help.github.com/en/github/managing-packages-with-github-packages/configuring-docker-for-use-with-github-packages#authenticating-to-github-packages) for more details


Then access to the jb3 application using a web browser on http://localhost:8080
This will runs two docker container, one for jb3 and one for mongodb. Mongodb data will be stored locally in *data* folder, allowing to keep it between runs.

# How to use

## Skill check

Please note that a thorough knowledge of Java web application development and hosting is required.

## Requirements

- OpenJDK 11+
- Maven 3+
- mongodb 2+

## Build

    mvn package

## Run locally

Ensure that mongodb is running and listening on 127.0.0.1 then run:

    java -jar target/jb3-*.jar

The frontend is now accessible on [locahost:8080](http://localhost:8080).

## Deploy and hosting on a production server

There is several options to deploy and host jb3. Here is one that requires:

- a domain name (example: mydomain.me).
- a web server with http proxy capabilities (example [nginx](http://nginx.org/)).

### Application configuration

Please change the jb3.defaults.room in application.properties file.

### Deployment

1. Build and launch jb3 on the server. If your OS use systemd, check the [sample configuration](docs/jb3.service).
2. Configure your web server to act as reverse proxy on [http://localhost:8080](http://localhost:8080). If you are using nginx, check the [sample configuration](docs/nginx-sample.conf).
