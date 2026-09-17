#Robot Worlds - build pipeline
#
# Development build (default):  make
#                               make build
# Release build (no git tag):   make build RELEASE=1
# Full release + git tag:       make release
#
# The version lives in pom.xml (single source of truth). A
# development build keeps the -SNAPSHOT suffix; a release build
# strips it before compiling/testing and restores the pom
# afterwards (the pom surgery and tagging live in
# scripts/release.sh).
#
# The git tag release-x.y.z is only created after ALL acceptance
# tests have passed against BOTH the reference server and our own
# server.

# ACCEPTANCE TEST SCOPE: the acceptance tests for this iteration are
# the ones covering the three stories - Launch a robot, Get the
# state of a robot, and Look (see src/test/.../TestLaunch.java,
# TestState.java, TestLook.java). These are the ONLY acceptance
# tests the Makefile runs in its test stages and in the release
# gate. The remaining test classes are unit tests and stay out of
# the pipeline ('make test-all' runs the entire suite).

# NOTE: the pipeline is serial (.NOTPARALLEL) because
# every acceptance test binds to port 5000 - do not run the stages
# concurrently.

# PORT HYGIENE: every test stage binds port 5000, and a run that was
# interrupted before its teardown completed leaves an ORPHANED server
# process holding the port - the next run then dies with
# "Address already in use". Every test stage therefore runs
# scripts/free-port.sh first (target free-port below), which waits for
# the port and, as a last resort, kills the java process holding it.
# The test harness (PortGuard) applies the same guard per test, so
# plain `mvn test` started outside make is protected too.


# --- Version handling ------------------------------------------
# Read straight from pom.xml (the first <version> element is the
# project version); never hardcode it here.
VERSION         := $(shell sed -n 's:.*<version>\([^<]*\)</version>.*:\1:p' pom.xml | head -1)
RELEASE_VERSION := $(VERSION:%-SNAPSHOT=%)
# In a release build (RELEASE=1) the JAR carries the stripped version.
BUILD_VERSION    = $(if $(RELEASE),$(RELEASE_VERSION),$(VERSION))

ARTIFACT       := robot-world
JAR_FILE       := target/$(ARTIFACT)-$(BUILD_VERSION)-jar-with-dependencies.jar
SERVER_CLASS   := za.co.wethinkcode.robots.server.Server
CLIENT_CLASS   := za.co.wethinkcode.robots.client.Client
RELEASE_SCRIPT := scripts/release.sh
FREE_PORT_SCRIPT := scripts/free-port.sh
TEST_PORT      := 5000
DOCKER_PORT    := 5050
DOCKER_IMAGE   := robot-worlds-server:$(BUILD_VERSION)
# Registry path for publishing, derived from the origin remote so every
# team member pushes to the same project registry without hardcoding any
# personal namespace. Both remote styles map to the same registry path:
#   git@gitlab.wethinkco.de:<group>/<project>.git
#   https://gitlab.wethinkco.de/<group>/<project>.git
#   -> gitlab.wethinkco.de:5050/<group>/<project>
REGISTRY_HOST  := gitlab.wethinkco.de:5050
REGISTRY_PATH  := $(shell git remote get-url origin 2>/dev/null | sed -n 's|.*[:/]\([^/]*/[^/]*\)\.git$$|\1|p')
REGISTRY_IMAGE ?= $(if $(REGISTRY_PATH),$(REGISTRY_HOST)/$(REGISTRY_PATH),)

# --- Acceptance test scope --------------------------------------
# The acceptance tests for this iteration: the test classes for the
# three stories (Launch / State / Look). Passed to surefire with
# -Dtest=... and handed to scripts/release.sh so the Makefile and
# the release script always gate on exactly the same tests.
ACCEPTANCE_TESTS := TestLaunch,TestState,TestLook

# --- Unit test scope ---------------------------------------------
# The unit-test suite = everything EXCEPT the acceptance classes in
# ACCEPTANCE_TESTS above. Derived from that variable (never maintain
# two lists): each acceptance class is prefixed with '!', surefire's
# exclusion marker, producing e.g. -Dtest='!TestLaunch,!TestState,!TestLook'
comma := ,
empty :=
space := $(empty) $(empty)
UNIT_EXCLUDES := $(subst $(space),$(comma),$(foreach t,$(subst $(comma), ,$(ACCEPTANCE_TESTS)),!$(t)))

# --- flow (LMS monitoring) log directory -----------------------
# The flow library required by the pom writes its run log to
#   .lms/.flow/<branch>_<user>.flot
# A branch name containing '/' therefore needs a matching
# sub-directory inside .lms/.flow/ - flow does not create it
# itself, and a failed write makes the recorder call
# System.exit(), which kills/hangs the test JVM. `make flow-dir`
# creates the directory before any maven task that loads our
# classes.
BRANCH       := $(shell git rev-parse --abbrev-ref HEAD 2>/dev/null)
FLOW_LOG_DIR := .lms/.flow/$(shell echo "$(BRANCH)" | sed -n 's|^\(.*/\)[^/]*$$|\1|p')

.PHONY: all build compile verify test test-ref test-docker unit-test test-all package docker-image docker-run docker-stop docker-logs docker-client docker-login docker-tag docker-push run-server run-client \
                build-release release tag version clean flow-dir free-port help

# Every stage binds port 5000 - keep them strictly sequential.
.NOTPARALLEL:

# Default target: full development build (pom.xml left untouched).
# Fails (and therefore never packages) if either test suite fails.
all: clean compile test-all free-port test-ref package

# `make build` - release flavour via variable: make build RELEASE=1
ifeq ($(RELEASE),1)
build: build-release
else
build: all
endif

clean:
	mvn clean

compile:
	mvn compile

# Check that all dependencies on other libraries are satisfied and
# that the full build lifecycle completes (tests skipped - they
# have their own dedicated targets below).
verify:
	mvn verify -DskipTests

# Acceptance tests against OUR OWN server: the three stories of this
# iteration (Launch / State / Look - see ACCEPTANCE_TESTS above).
# The scenarios need a FRESH world per test, so AbstractAcceptanceTest
# boots our server around every test; the build script selects which
# server stage runs (this target vs test-ref below).
# free-port clears any orphaned server left by an earlier interrupted
# run before the tests try to bind.
test: free-port
	mvn test -Dtest='$(ACCEPTANCE_TESTS)'

# Unit tests ONLY (no acceptance stories, no port binding): the
# fastest feedback stage, used by the CI unit-test job.
unit-test:
	mvn test '-Dtest=$(UNIT_EXCLUDES)'

# Acceptance tests against the REFERENCE server
# (reference-server-0.2.3.jar, booted by the test harness when
# -DuseRefServer=true; select another jar with -DrefServerJar=...).
# Same three stories, same fresh-world-per-test arrangement.
test-ref: free-port
	mvn test -Dtest='$(ACCEPTANCE_TESTS)' -DuseRefServer=true

# Acceptance tests against the server running inside Docker.
test-docker: docker-run
	trap '$(MAKE) docker-stop' EXIT; mvn test -Dtest='$(ACCEPTANCE_TESTS)' -DdockerServer=true -Dtest.port=$(DOCKER_PORT)

# The complete suite: every acceptance test above plus the unit
# tests (TestWorld, TestCommands, ...), all against our own server.
test-all: free-port
	mvn test

# Free the shared test port (TEST_PORT) before a stage binds it:
# waits for it, then kills the java process holding it - see
# scripts/free-port.sh.
free-port:
	bash $(FREE_PORT_SCRIPT) $(TEST_PORT)

# Developer package (fat JAR) used by run-server / run-client.
package:
	mvn package -DskipTests

# Build and run the deployment image. The image listens on container port 5050.
# clean first: the Dockerfile COPYs the fat JAR via a wildcard, so a stale
# jar from a build with a different version (e.g. after RELEASE=1) left in
# target/ would make COPY match two files and fail.
docker-image:
	mvn clean package -DskipTests
	docker build -t $(DOCKER_IMAGE) .

docker-run: docker-image
	- docker rm -f robot-worlds-test
	docker run -d --name robot-worlds-test -p $(DOCKER_PORT):$(DOCKER_PORT) $(DOCKER_IMAGE)

docker-logs:
	docker logs robot-worlds-test

# Play the game against the containerized server (run make docker-run
# first). 127.0.0.1 on purpose: localhost can resolve to ::1 first and
# Docker Desktop's IPv6 forwarding is not always reliable.
docker-client: package
	java -cp $(JAR_FILE) $(CLIENT_CLASS) 127.0.0.1 $(DOCKER_PORT)

docker-stop:
	- docker stop robot-worlds-test
	- docker rm robot-worlds-test

# Interactive login to the GitLab container registry. The password it
# prompts for is a personal access token with read_registry and
# write_registry scopes (GitLab > User settings > Access tokens).
docker-login:
	@docker login $(REGISTRY_HOST)

# Tag the built image for the project registry. REGISTRY_IMAGE defaults
# to the path derived from the origin remote; override it to publish
# elsewhere, e.g. make docker-push REGISTRY_IMAGE=registry.example/team/robot-worlds
docker-tag: docker-image
	@if [ -z "$(REGISTRY_IMAGE)" ]; then echo "Could not derive REGISTRY_IMAGE from 'git remote get-url origin' - set it explicitly, e.g. make docker-push REGISTRY_IMAGE=registry.example/team/robot-worlds:$(BUILD_VERSION)"; exit 1; fi
	docker tag $(DOCKER_IMAGE) $(REGISTRY_IMAGE)

docker-push: docker-tag
	docker push $(REGISTRY_IMAGE)

# Release build WITHOUT a git tag: strips -SNAPSHOT from pom.xml,
# runs the acceptance tests against both servers, packages, then
# restores the development pom. ACCEPTANCE_TESTS is handed to the
# script so both gate on exactly the same tests.
build-release:
	ACCEPTANCE_TESTS='$(ACCEPTANCE_TESTS)' bash $(RELEASE_SCRIPT) build

# Full release pipeline: release build + git tag release-x.y.z.
# The tag is only created if every acceptance test passed against
# both servers.
release:
	ACCEPTANCE_TESTS='$(ACCEPTANCE_TESTS)' bash $(RELEASE_SCRIPT) release

# Convenience: tag the CURRENT version without rebuilding. Refuses
# to tag a -SNAPSHOT version. Prefer `make release` - this bypasses
# the test gate.
tag:
	bash $(RELEASE_SCRIPT) tag

version:
	@echo "pom.xml version: $(VERSION)"
	@echo "release version: $(RELEASE_VERSION)"
	@echo "JAR to build:    $(JAR_FILE)"

# Run the server/client from the packaged fat JAR. The manifest
# main class is the server, so the client is launched with -cp.
run-server: package
	java -jar $(JAR_FILE)

run-client: package
	java -cp $(JAR_FILE) $(CLIENT_CLASS)

help:
	@echo "make                 - development build (clean, compile, acceptance tests vs reference server, package)"
	@echo "make build           - same as above"
	@echo "make build RELEASE=1 - release build: strips -SNAPSHOT, acceptance tests, packages"
	@echo "make release         - release build + git tag release-x.y.z (only if all acceptance tests pass)"
	@echo "make compile         - compile only"
	@echo "make verify          - check dependencies & complete build lifecycle (tests skipped)"
	@echo "make test            - acceptance tests (Launch/State/Look stories) against our own server"
	@echo "make unit-test      - unit tests only (excludes the acceptance story classes)"
	@echo "make test-ref        - acceptance tests (Launch/State/Look stories) against the reference server"
	@echo "make test-docker     - acceptance tests against a running Docker server"
	@echo "make test-all        - the complete suite: acceptance + unit tests against our own server"
	@echo "make free-port       - free port 5000: kills an orphaned java server left by an earlier run"
	@echo "make package         - package the fat JAR (skips tests)"
	@echo "make docker-image    - build the Docker image (container port 5050)"
	@echo "make docker-run      - run the Docker image on host port 5050"
	@echo "make docker-logs     - show the container server's console output"
	@echo "make docker-client   - play the game against the container (needs docker-run)"
	@echo "make docker-login    - log in to the GitLab container registry (PAT)"
	@echo "make docker-push     - tag and publish the image to the project registry (override with REGISTRY_IMAGE=...)"
	@echo "make run-server      - run the server from the fat JAR"
	@echo "make run-client      - run the client from the fat JAR"
	@echo "make version         - show the version currently in pom.xml"
	@echo "make clean           - remove build artefacts"