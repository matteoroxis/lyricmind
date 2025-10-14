# LyricMind

A Spring Boot application for managing and analyzing song lyrics.

## Description

LyricMind is a project developed for an InfoQ article that demonstrates the integration of modern technologies for managing music lyrics.

## Technologies Used

- **Java** - Primary programming language
- **Spring Boot** - Framework for enterprise application development
- **Maven** - Dependency management and build automation system

## Prerequisites

- JDK 17 or higher
- Maven 3.6 or higher

## Installation

```bash
# Clone the repository
git clone https://github.com/matteoroxis/lyricmind.git

# Navigate to the project directory
cd lyricmind

# Build the project
mvn clean install
```

## Running the Application

```bash
# Start the application
mvn spring-boot:run
```

The application will be available at `http://localhost:8080`

## Build

```bash
# Create the executable JAR file
mvn clean package

# Run the JAR
java -jar target/lyricmind-0.0.1-SNAPSHOT.jar
```

## Project Structure

```
lyricmind/
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
├── pom.xml
└── README.md
```

## Configuration

Application settings can be modified in the `src/main/resources/application.properties` file

## Testing

```bash
# Run tests
mvn test
```

## Contributing

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project was created for the InfoQ article on LyricMind.

## Author

GitHub: matteoroxis

---

For more information, please refer to the InfoQ article.