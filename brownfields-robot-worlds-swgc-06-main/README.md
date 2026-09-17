
# Robot World Game

A feature-rich, multi-transport client-server robot world game where multiple clients can control robots concurrently, navigate dynamic environments, engage in combat, and persist game state using an Object-Relational Mapper (ORM) and a dual-transport backend.

---

## Architecture & Iteration Overview

* **Iteration 1 & 2 (Core Domain & Sockets):** Establishes the coordinate grid (`[0,0]` center), movement rules, and raw TCP socket communication governed by a strict JSON protocol.
* **Iteration 3 (Multiplayer, Combat & Persistence):** Introduces specialized robot types (Sniper, Juggernaut, Intruder), environmental hazards (mountains, lakes, bottomless pits, and mines), active/passive attacks, server admin commands, and SQLite persistence.
* **Iteration 4 (Separation of Concerns, ORM & Web API):** Decouples the domain layer from networking, introducing an embedded **Javalin Web API** running alongside the TCP Socket Server, and replacing raw JDBC with **EoDSQL** Data Access Interfaces and Objects.

---

## Technologies Used
* **Java 21** (Temurin JDK / Alpine containers)
* **Maven** (Build and dependency management)
* **Javalin 6** (Embedded HTTP Web API framework)
* **EoDSQL 2.2** (Lightweight Object-Relational Bridge for SQLite)
* **SQLite JDBC** (Relational database persistence)
* **Docker** (Containerized deployment)

---

## Getting Started & Build Commands

Build the executable fat JAR and run tests using the provided `Makefile`:

```bash
# Full development build (compiles, runs full test suite, packages fat JAR)
make all

# Run unit tests only
make unit-test

# Run iteration acceptance tests against your server
make test

# Run the complete test suite (unit + acceptance)
make test-all

```

---

## Running the Server

The unified server entry point (`Server.java`) boots up the application consoles and binds both transport layers to share a single, unified `World` state in memory:

* **TCP Socket Server:** Listens on port `5000` (or configured port via `-p` / environment variables).
* **Javalin Web API:** Listens on port `7000`.

To run the server locally from the packaged fat JAR:

```bash
make run-server
# Equivalent to: java -jar target/robot-world-0.1.1-SNAPSHOT-jar-with-dependencies.jar

```

---

## Running the Client

To launch a command-line client and connect to the local TCP server:

```bash
make run-client

```

When prompted, you must first launch your robot using the launch contract:

```text
launch <type> <name>

```

### Supported Robot Types

* **Juggernaut:** High shields (5), heavy ammo (5), low movement range (1).
* **Tank:** Balanced shields (4), ammo (4), movement range (2).
* **Soldier:** Standard stats — shields (3), ammo (3), movement range (3).
* **Marksman:** Low shields (2), ammo (2), high range (4).
* **Sniper:** Fragile (1 shield, 1 shot), maximum range (5).
* **Intruder:** Passive attack variant — no gun (0 shots), shields (3), can place mines.

### Supported Client Commands (after launching)

* `forward <steps>` — Move forward in the direction faced
* `back <steps>` — Move backward
* `turn <left|right>` — Rotate direction (`NORTH`, `EAST`, `SOUTH`, `WEST`)
* `look` — Scan surroundings within visibility range
* `repair` — Fix damaged shields (takes configured `repairTime`)
* `fire` — Shoot another robot in the line of sight
* `reload` — Refill ammunition (takes configured `reloadTime`)
* `mine` — Place a mine at the current coordinate (Intruder only)
* `state` — View current robot status (`NORMAL`, `REPAIR`, `SETMINE`, `DEAD`)
* `help` — Show command list
* `off` — Disconnect and quit client

---

## Server Admin Console Commands

While the server is running (`make run-server`), you can issue administrative commands directly into the server terminal:

* `robots` — Lists every active robot and its current status.
* `dump` — Renders an ASCII map grid showing robots (`@`), obstacles (`#`), and pits (`*`).
* `purge <robot_name>` — Forcefully kills and removes a robot from the world.
* `save <world_name>` — Persists the current world dimensions, obstacles, pits, and mines to SQLite using EoDSQL.
* `restore <world_name>` — Loads a previously saved world state from the database into active memory.
* `quit` — Gracefully disconnects all clients and shuts down the server.

---

## Web API Layer (HTTP)

The Javalin Web API provides a RESTful HTTP interface running alongside the socket server, allowing web and mobile clients to interact with the exact same game world.

### Endpoints

* **`GET /world`**
  Returns the dimensions, obstacles, pits, mines, and active robots of the current active world as formatted JSON.
* **`GET /world/{name}`**
  Restores the named world from the database into active memory and returns its objects.
* **`POST /robot/{name}`**
  Executes a robot command (such as `launch` or `look`) using the standard JSON protocol envelope shared with the TCP socket server.

### Example Web API Request (`curl`)

Launch a robot via HTTP:

```bash
curl -X POST http://localhost:7000/robot/web_bot \
     -H "Content-Type: application/json" \
     -d '{"robot": "web_bot", "command": "launch", "arguments": ["sniper"]}'

```

Inspect world state via HTTP (pretty-printed with `jq`):

```bash
curl -s http://localhost:7000/world | jq

```

---

## Docker Deployment

The application can be packaged into a lightweight Alpine container (`eclipse-temurin:21-jre-alpine`) for isolated deployment and automated pipeline testing.

### Building and Running Locally

Build the executable fat JAR, construct the Docker image, and run a container mapping host port `5050`:

```bash
make docker-image
make docker-run

```

### Running Acceptance Tests Against Docker

To verify the containerized server against the acceptance test suite:

```bash
make test-docker

```

---

## Publishing to the GitLab Container Registry

Team members with registry permissions can push and pull build artifacts via the Makefile automation:

```bash
make docker-login   # Authenticate with GitLab Personal Access Token (PAT)
make docker-push    # Tag and push image to project registry

```

```

```