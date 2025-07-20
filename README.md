## Getting Started

### Prerequisites

- **Java 21** or higher
- **Maven 3.6+** (or use the included Maven wrapper)
- **Git** (for cloning the repository)

### Running the Application

#### Option 1: Using Maven Wrapper (Recommended)

```bash
# Clone the repository
git clone <your-repo-url>
cd eaglebank

# Run the application using Maven wrapper
./mvnw spring-boot:run
```

#### Option 2: Using Local Maven

```bash
# Clone the repository
git clone <your-repo-url>
cd eaglebank

# Clean and compile the project
mvn clean compile

# Run the application
mvn spring-boot:run
```

#### Option 3: Using JAR file

```bash
# Build the JAR file
./mvnw clean package

# Run the JAR file
java -jar target/eaglebank-0.0.1-SNAPSHOT.jar
```

### Application Access

Once the application is running, you can access:

#### Swagger UI (API Documentation)
- **URL**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Description**: Interactive API documentation where you can test all endpoints
- **Features**: 
  - Try out API endpoints directly from the browser
  - View request/response schemas
  - Authentication support for testing protected endpoints

#### H2 Database Console (Development)
- **URL**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
- **JDBC URL**: `jdbc:h2:mem:eaglebank`
- **Username**: `sa`
- **Password**: `password`
- **Description**: Web-based database console for viewing and querying data during development

#### OpenAPI Specification
- **URL**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **Description**: Raw OpenAPI 3.0 specification in JSON format

### Authentication

The API uses JWT (JSON Web Token) authentication. To access protected endpoints:

1. Create a user account using `POST /v1/users`
2. Authenticate using the authentication endpoint to get a JWT token
3. Include the token in the `Authorization` header as `Bearer <token>`

### Testing

#### Run Unit Tests
```bash
./mvnw test
```

#### Run Integration Tests
```bash
./mvnw test -Dtest=EagleBankIntegrationTest
```

#### Run All Tests
```bash
./mvnw clean test
```

### Development Notes

- The application uses an **H2 in-memory database**, so data is reset on each restart
- **Debug logging** is enabled for development
- **SQL queries** are logged to the console for debugging
- The application runs on **port 8080** by default
- **Hot reload** is supported during development when using Spring Boot DevTools

### Troubleshooting

#### Common Issues

1. **Port 8080 already in use**
   ```bash
   # Change the port in application.properties or use:
   ./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
   ```

2. **Java version issues**
   ```bash
   # Check your Java version
   java -version
   # Should be Java 21 or higher
   ```

3. **Maven wrapper permissions (Linux/Mac)**
   ```bash
   chmod +x mvnw
   ```

### API Testing with curl

#### Create a User
```bash
curl -X POST http://localhost:8080/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "password": "securePassword123"
  }'
```

#### Get User (requires authentication)
```bash
curl -X GET http://localhost:8080/v1/users/{userId} \
  -H "Authorization: Bearer <your-jwt-token>"
```

For more detailed API examples, please refer to the Swagger UI documentation at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).
