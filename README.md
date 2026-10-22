# Distrimarket Auth

Usuarios, roles, login y emisión/validación de tokens JWT. Depende de
`com.distrimarket:distrimarket-common:1.0.1-SNAPSHOT` para entidades, DTOs y
migraciones Liquibase.

## Run

Build the shared library first, then this service:

```sh
cd ../distrimarket-common
python3 scripts/build_stack.py --classes
cd ../distrimarket-ms-auth
mvn spring-boot:run
```

Or run the packaged JAR on port 8084:

```sh
java -jar target/distrimarket-auth-0.0.1.jar
```

Configuration is read from `../distrimarket-common/.env.local` when using
`run_stack.py`, or from the environment. The important variables are `DB_*`,
`REDIS_*`, `JWT_SECRET`, and `AUTH_ADMIN_*`.

Redis is optional for local development: set `CACHE_ENABLED=false` to use the
database for every read. The default development admin is `admin` / `Admin123!`
and is created on startup only if no active ADMIN exists.

Swagger: <http://localhost:8084/auth/swagger-ui.html>
Login: `POST http://localhost:8084/auth/login`
