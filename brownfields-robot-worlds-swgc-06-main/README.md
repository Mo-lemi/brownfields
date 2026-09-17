# Robot World Game

A simple client-server based robot world game where multiple clients can control robots and interact with a dynamic game world.

## Features

- Launch and control robots from client terminals.
- Dynamic obstacles like lakes, mountains, and pits.
- Multiple robot types: Juggernaut, Tank, Soldier, Marksman, Sniper.

## Technologies Used

- Java
- Socket programming
- JSON for communication

# Getting Started

## Server

To start the server:

```bash
Run class:
za.co.wethinkcode.robots.server.Server
```

## Connecting a client
To start the client:

```bash
Run class:
za.co.wethinkcode.robots.client.Client

```

## Docker Deployment

The image listens on port **5050** inside the container. The Dockerfile copies the fat JAR that `make` builds with Maven, so the JAR must exist in `target/` before `docker build` runs.

### Building and Running Locally

Build the executable JAR and the Docker image:

```bash
make docker-image
```

This packages the fat JAR (`mvn clean package -DskipTests`) and builds the image `robot-worlds-server:<version>` from the Dockerfile.

Run the server container with port `5050` published on your host:

```bash
make docker-run
# equivalent to:
docker run -d --name robot-worlds-test -p 5050:5050 robot-worlds-server:0.1.1-SNAPSHOT
```

Verify it is up - the server answers any TCP line with a JSON response:

```bash
curl -s telnet://localhost:5050   # then type a JSON request and press Enter
```

Stop and remove the running container:

```bash
make docker-stop
```

### Testing Against Docker

Run the acceptance tests against the running Docker container:

```bash
make test-docker
```

This target will automatically:
1. Build the Docker image
2. Start a container on host port 5050
3. Restart the container before each test (fresh world per scenario) and wait until the server actually answers
4. Run the acceptance test suite
5. Clean up the container afterward

### Publishing to the GitLab Container Registry

The project registry lives on the WTC GitLab server at
`gitlab.wethinkco.de:5050/<namespace>/<project>` (the same path is derived automatically from the `origin` remote).

1. Create a personal access token with the `read_registry` and `write_registry` scopes (GitLab > User settings > Access tokens).
2. Log in - the password prompt expects the token:

```bash
make docker-login
# equivalent to:
docker login gitlab.wethinkco.de:5050
```

3. Tag and push (uses the derived registry path; override with `REGISTRY_IMAGE=...`):

```bash
make docker-push
# equivalent to:
docker tag robot-worlds-server:0.1.1-SNAPSHOT gitlab.wethinkco.de:5050/nhmiyswgc025/brownfields-robot-worlds-swgc-06
docker push gitlab.wethinkco.de:5050/nhmiyswgc025/brownfields-robot-worlds-swgc-06
```

### Pull and Run Published Images

Every teammate with registry access can pull the exact same image and run it:

```bash
docker pull gitlab.wethinkco.de:5050/nhmiyswgc025/brownfields-robot-worlds-swgc-06:latest
docker run -p 5050:5050 gitlab.wethinkco.de:5050/nhmiyswgc025/brownfields-robot-worlds-swgc-06:latest
```

Any free host port works as the left-hand side of the mapping, e.g. `-p 9002:5050` publishes the container's 5050 on host port 9002.

### Automatic Publishing (CI)

The pipeline (`.gitlab-ci.yml`, runs on every commit to `main`) publishes the image after all test stages pass: the `publish-image` job builds the image and pushes two tags to the project's container registry using the job's temporary token - `latest` and the commit's short SHA.

### Understanding Image Tags

- **`latest`** - A mutable convenience tag pointing to the most recent build. Use this for development and quick iteration.
- **`<commit-sha>`** - An immutable tag identifying an exact build (pushed by CI). Use this for reproducible deployments.

Both tags are pushed to the GitLab Container Registry during the CI/CD pipeline on the main branch.

### Launch Your Robot
When prompted, use the launch command to create your robot in the game world. 
For example: `launch soldier alpha`

### Supported Commands (after launching)
- `forward <steps>` – Move forward
- `back <steps>` – Move backward
- `turn <left|right>` – Rotate direction
- `look` – Scan surroundings 
- `repair` – Fix damage 
- `fire` – Shoot another robot 
- `reload` – Refill shots 
- `mine` – Place a mine (Intruder only) 
- `state` – View current robot status 
- `help` – Show command list

### Mines and the Intruder

The **Intruder** is the passive-attack kind: it has no gun (`fire` is not
available to it), but it can lay mines with the `mine` command.

- The mine is placed at the Intruder's current coordinate.
- Setting a mine takes `setMineTime` seconds (configured in
  `config/world_config.properties`). During that time the robot is in
  SetMine mode: it **cannot move at all** and its **shields are disabled** -
  any hit while mining destroys it. It's a sitting duck.
- Once the mine is set, the Intruder automatically moves one step forward.
  If that step is blocked by an obstacle, it stays standing on its own mine
  and takes the damage.
- Any robot that steps on a mine - own or someone else's - loses **3
  shields**, and the mine detonates (it only hurts once).
- Mines are hard to spot: a robot detects them only within a **quarter of
  the visibility range, rounded down** (visibility 10 → mines 2 steps away),
  in any direction. They show up in `look` results as type `MINE`.

### Obstacles and Pits

Obstacles and bottomless pits are placed in the world by their **x, y
coordinates**.

Configure them through system properties (or the equivalent entries in
`config/world_config.properties`):

```
-Dobstacles=0,1            obstacle at (0,1)
-Dobstacles=0,1;2,2        several obstacles, separated by ';'
-Dpits=2,2                 bottomless pit at (2,2)
-Dpits=none                no pits
```

- **Obstacles** block the path of robots - a robot cannot enter them.
  Obstacles **cannot overlap** each other; a placement on an already
  occupied coordinate is rejected. Mountains also block the line of
  sight, lakes are see-through.
- **Pits** kill: a robot that enters a bottomless pit dies. Pits **may
  overlap** each other. Pits are see-through for `look`.
- The `look` command reports what a robot sees as `OBSTACLE`, `PIT`,
  `MINE`, `ROBOT` or `EDGE`, each with its direction and distance in steps.
- A world that is created without any configured obstacles or pits gets
  one of each automatically (placed in far corners). Explicitly configured
  worlds are taken exactly as configured.


## Iteration 4: Web API, ORM, & Shared World Demo

This project features a unified "split-brain" architecture. A Javalin Web API (HTTP) and a legacy Socket Server (TCP) share the exact same live game world and database context, completely decoupled from the transport layers.

### 1. Run the CI/CD Pipeline & Docker Tests
Verify that all automated tests pass and the Docker container builds and binds correctly:
```bash
make test-docker

```

### 2. Boot the Unified Server

Start the server locally. It will bind the Socket Server to port `5000` and the Web API to port `7000`:

```bash
make run-server

```

### 3. Launch a Robot via Web API (HTTP)

In a new terminal, use `curl` to send a POST request to the Javalin Web API:

```bash
curl -X POST http://localhost:7000/robot/web_bot \
     -H "Content-Type: application/json" \
     -d '{"robot": "web_bot", "command": "launch", "arguments": ["sniper"]}'

```

View the current world state. We pipe it into `jq` (or python) to pretty-print the JSON:

```bash
curl -s http://localhost:7000/world | jq
# (If jq is not installed, use: curl -s http://localhost:7000/world | python3 -m json.tool)

```

### 4. Verify the Shared World via Sockets (TCP)

In a third terminal, connect a TCP client to the legacy Socket Server:

```bash
make run-client

```

Launch a second robot and use the `look` command to prove it sees `web_bot` standing in the exact same world:

```text
> launch soldier socket_bot
> look

```

### 5. Save and Restore via EoDSQL ORM (Database)

In the terminal running the server, type the following into the admin console to persist the shared world state to SQLite via the EoDSQL Object-Relational Mapper:

```text
save demo_world

```

Prove the Web API can query the database directly using the new Data Access Interfaces (DAI) to retrieve the saved state:

```bash
curl -s http://localhost:7000/world/demo_world | jq

```

```

This perfectly outlines the exact script your team can follow during the demo to prove you hit every single requirement!

```
