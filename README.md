# Spring Boot Technicals
Setup Guide

```
export SPRING_DATASOURCE_URL="jdbc:postgresql://aws-1-ap-southeast-2.pooler.supabase.com:6543/postgres?sslmode=require"
export SPRING_DATASOURCE_USERNAME="postgres.vczthgeupjrizbckxurh"
export SPRING_DATASOURCE_PASSWORD="itsectesting10112025"
./mvnw clean package
./mvnw spring-boot:run
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
    "message": "Successfully fetched data",
    "data": {
        "system_status": "ALIVE",
        "status_message": "System is healthy"
    }
}
```
