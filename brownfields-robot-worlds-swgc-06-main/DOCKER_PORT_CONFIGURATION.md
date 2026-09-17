# Docker Port Configuration

## Overview

The Robot Worlds Server supports flexible port configuration through both environment variables and command-line arguments. This allows the server to run on different ports in various deployment scenarios (local development, Docker containers, CI/CD pipelines).

## Configuration Methods

### 1. Environment Variable (Default)

The server reads the `ROBOT_WORLD_PORT` environment variable with a fallback to port `5000`:

```bash
# Set via environment variable
export ROBOT_WORLD_PORT=5050
java -jar robot-world-0.1.1-SNAPSHOT-jar-with-dependencies.jar

# Default if not set
java -jar robot-world-0.1.1-SNAPSHOT-jar-with-dependencies.jar
# Listens on port 5000
```

### 2. Command-Line Argument (CLI Override)

The server accepts a `-p` flag to specify the port. This takes precedence over the environment variable:

```bash
java -jar robot-world-0.1.1-SNAPSHOT-jar-with-dependencies.jar -p 5050
# Listens on port 5050
```

### 3. Docker Container

The Dockerfile pins the port in the entrypoint and boots the container
with the same world configuration the acceptance tests use:

```dockerfile
WORKDIR /srv/app
COPY target/robot-world-*-jar-with-dependencies.jar /srv/app/robot-server.jar
ENTRYPOINT ["java", "-Dwidth=3", "-Dheight=3", "-Dobstacles=0,1", "-jar", "robot-server.jar", "-p", "5050"]
```

When the container starts:
```bash
docker run -p 5050:5050 robot-worlds-server:0.1.1-SNAPSHOT
# Passes -p 5050 to the Java application
# Server listens on port 5050 inside the container
```

## Priority Order

Port configuration is resolved in this order (first match wins):

1. **Command-line argument** (`-p <port>`) — Highest priority
2. **Environment variable** (`ROBOT_WORLD_PORT`) — Medium priority
3. **Default** (`5000`) — Lowest priority (fallback)

### Example Scenarios

**Scenario 1: Docker with default CMD**
```bash
docker run -p 5050:5050 robot-worlds-server:0.1.1-SNAPSHOT
# Server uses: -p 5050 (from CMD)
# Result: Listens on port 5050
```

**Scenario 2: Docker with environment variable override**
```bash
docker run -p 5050:5050 -e ROBOT_WORLD_PORT=5050 robot-worlds-server:0.1.1-SNAPSHOT
# Server uses: -p 5050 (from CMD takes precedence)
# Result: Listens on port 5050
```

**Scenario 3: Local development with environment variable**
```bash
export ROBOT_WORLD_PORT=9000
java -jar target/robot-world-0.1.1-SNAPSHOT-jar-with-dependencies.jar
# Server uses: ROBOT_WORLD_PORT=9000 (from environment)
# Result: Listens on port 9000
```

**Scenario 4: Local development with CLI override**
```bash
java -jar target/robot-world-0.1.1-SNAPSHOT-jar-with-dependencies.jar -p 8080
# Server uses: -p 8080 (from CLI argument)
# Result: Listens on port 8080
```

## Implementation Details

### Server.java Main Method

```java
public static void main(String[] args) throws IOException {
    // Initialize the database and tables before starting the server
    DatabaseManager.initializeDatabase();

    // Read port from environment variable with default, allow CLI override
    int port = Integer.parseInt(System.getenv().getOrDefault("ROBOT_WORLD_PORT", "5000"));
    
    // Check for -p argument in CLI args (CLI args override env var)
    for (int i = 0; i < args.length - 1; i++) {
        if ("-p".equals(args[i])) {
            port = Integer.parseInt(args[i + 1]);
            break;
        }
    }

    ServerSocket serverSocket = new ServerSocket();
    serverSocket.bind(new InetSocketAddress("0.0.0.0", port));
    Server server = new Server(serverSocket, new World());

    server.listenForCommand();
    server.startServer();
}
```

## Usage in Makefile

The Makefile targets automatically use the correct port configuration:

```bash
# Local development (uses default port 5000)
make package
java -jar target/robot-world-*-jar-with-dependencies.jar

# Docker container (uses port 5050)
make docker-image
make docker-run              # Maps host 5050 → container 5050
make test-docker            # Runs tests against container on port 5050
```

## Testing Against Docker

Run acceptance tests against a Docker container:

```bash
make test-docker
# Automatically:
# 1. Builds the Docker image
# 2. Starts container on port 5050
# 3. Waits for server to accept connections
# 4. Runs acceptance tests with -Dtest.port=5050
# 5. Stops and removes the container
```

## Backward Compatibility

This implementation maintains full backward compatibility:

- **Existing scripts** using only `ROBOT_WORLD_PORT` environment variable continue to work
- **Local JAR execution** defaults to port 5000 as before
- **Docker containers** use port 5050 as specified in requirements
- **No changes required** to existing application code or business logic

## Summary

| Method | Priority | Example | Use Case |
|--------|----------|---------|----------|
| CLI Argument | 1 (Highest) | `java -jar app.jar -p 8080` | Override for specific runs |
| Environment Variable | 2 (Medium) | `ROBOT_WORLD_PORT=5050` | CI/CD, Docker, deployment |
| Default Fallback | 3 (Lowest) | (port 5000) | Local development |

This flexible approach ensures the server adapts to any deployment scenario without code changes.
