# Web

The goal of this project is to create a RESTful application with modern virtual threads, database support and as small
memory footprint as possible.

The fat-jar is around 6MB at this point.

## Features

1. REST service using [java-http](https://github.com/FusionAuth/java-http)
2. JSON serialization using [dsl-json](https://github.com/ngs-doo/dsl-json)
3. JWT authorization and authentication using [java-jwt](https://github.com/auth0/java-jwt)
4. PostgreSQL JDBC
5. UUID v7 support using [java-uuid-generator](https://github.com/cowtowncoder/java-uuid-generator)
6. Request validation using javax.validation
   and [Hibernate Validator](https://docs.hibernate.org/stable/validator/reference/en-US/html_single/)

## Getting Started

Start postgres with docker compose using:

```bash
docker-compose up -d
```

## Requests

Requests inside the `requests` folder are examples on how to communicate with the server. 
