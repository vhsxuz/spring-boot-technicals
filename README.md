# Spring Boot Technicals
Sample Local Setup Guide

```
export DB_URL=jdbc:postgresql://localhost:5432/itsec_testing
export DB_USER=postgres
export DB_PASS=
export JWT_SECRET=02fcc75f2c0d43a6a602350e0b15840cbaa961fdfcac4488547d4d1a8f59b9e6
export EMAIL_PASSWORD=mwyenicowhbkirzs
```

Clean Run
```
./mvnw clean package
./mvnw spring-boot:run
```

Sample Docker Setup Guide
```
docker build -t demo-app .         

docker run -p 8080:8080 \
  -e JWT_SECRET=02fcc75f2c0d43a6a602350e0b15840cbaa961fdfcac4488547d4d1a8f59b9e6 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/itsec_testing \
  -e DB_USER=postgres \
  -e DB_PASS= \
  -e EMAIL_PASSWORD=mwyenicowhbkirzs \
  demo-app
```

check on 
```
http://localhost:8080/api/v1/health

```

The result should be 
```
{
  "code": 200,
  "status": "OK",
  "message": "Service is healthy",
  "data": {
    "service": "Spring Boot Technical Demo API",
    "status": "UP",
    "version": "1.0.0",
    "timestamp": "2025-11-12T01:13:07.090683920Z"
  }
}
```

Swagger Endpoint
```
http://localhost:8080/api/v1/swagger-ui/index.html
```