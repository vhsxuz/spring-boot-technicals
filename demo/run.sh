#!/bin/bash
# Load environment variables from .env file and run the Spring Boot application

set -a  # automatically export all variables
source .env
set +a

# Run the application
./mvnw spring-boot:run
