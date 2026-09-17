#!/usr/bin/env bash
# =============================================================
# Robot Worlds release pipeline (called by the Makefile)
#
#   scripts/release.sh build     Release build: strip -SNAPSHOT from
#                                pom.xml, run the acceptance tests
#                                (Launch/State/Look stories) against
#                                both the reference server and our own
#                                server, package, restore the dev pom.
#   scripts/release.sh release   The same, then tag release-x.y.z on
#                                git - the tag is only reached when
#                                every acceptance test has passed.
#   scripts/release.sh tag       Tag the current version only
#                                (refuses -SNAPSHOT versions; no test
#                                gate - use `make release` instead).
# =============================================================
set -euo pipefail

POM="pom.xml"
REF_SERVER_JAR="reference-server-0.1.0.jar"
MODE="${1:-build}"

# The acceptance tests for this iteration: the test classes for the
# three stories (Launch / State / Look). The Makefile passes its
# ACCEPTANCE_TESTS variable in the environment; the default here
# must stay in sync with the Makefile.
ACCEPTANCE_TESTS="${ACCEPTANCE_TESTS:-TestLaunch,TestState,TestLook}"

current_version() {
    # The first <version> element in the pom is the project version.
    sed -n 's:.*<version>\([^<]*\)</version>.*:\1:p' "$POM" | head -1 | tr -d '[:space:]'
}

VERSION="$(current_version)"
RELEASE_VERSION="${VERSION%-SNAPSHOT}"
TAG="release-$RELEASE_VERSION"

# The flow library required by the pom logs every run to
#   .lms/.flow/<branch>_<user>.flot
# and does not create the folder for branch names containing '/'.
# A failed flow write makes the recorder call System.exit(), which
# kills/hangs the test JVM - so make sure the folder exists first.
FLOW_DIR=".lms/.flow/$(git rev-parse --abbrev-ref HEAD 2>/dev/null | sed -n 's|^\(.*/\)[^/]*$|\1|p')"
mkdir -p "$FLOW_DIR"

restore_pom() {
    if [[ -f "$POM.release-bak" ]]; then
        mv "$POM.release-bak" "$POM"
        echo ">> Restored development version in $POM"
    fi
}

make_tag() {
    if git rev-parse -q --verify "refs/tags/$TAG" >/dev/null 2>&1; then
        echo ">> Tag $TAG already exists - skipping"
        return 0
    fi
    git tag -a "$TAG" -m "Release $RELEASE_VERSION - passed all acceptance tests against the reference server and our own server"
    echo ">> Created tag $TAG"
    if git remote | grep -q .; then
        # Best effort: in environments without push credentials the tag
        # still exists locally and marks the release.
        if git push origin "$TAG" 2>/dev/null; then
            echo ">> Pushed $TAG to origin"
        else
            echo ">> NOTE: could not push $TAG to origin (no credentials?) - the tag exists locally"
        fi
    else
        echo ">> No git remote configured - tag created locally only"
    fi
}

run_tests_and_package() {
    echo ">> mvn clean"
    mvn -q clean

    echo ">> Acceptance tests ($ACCEPTANCE_TESTS) against OUR OWN server"
    mvn test -Dtest="$ACCEPTANCE_TESTS"

    echo ">> Acceptance tests ($ACCEPTANCE_TESTS) against the REFERENCE server ($REF_SERVER_JAR)"
    mvn test -Dtest="$ACCEPTANCE_TESTS" -DuseRefServer=true

    echo ">> Packaging $RELEASE_VERSION"
    mvn -q package -DskipTests
    echo ">> Build artefact: target/robot-world-$RELEASE_VERSION-jar-with-dependencies.jar"
}

case "$MODE" in
    build|release)
        if [[ "$VERSION" == "$RELEASE_VERSION" ]]; then
            echo "!! WARNING: pom.xml version '$VERSION' has no -SNAPSHOT suffix - nothing to strip."
        else
            cp "$POM" "$POM.release-bak"
            trap restore_pom EXIT
            sed -i "s|<version>$VERSION</version>|<version>$RELEASE_VERSION</version>|" "$POM"
            echo ">> Stripped -SNAPSHOT: building release $RELEASE_VERSION"
        fi

        run_tests_and_package

        if [[ "$MODE" == "release" ]]; then
            make_tag
        fi
        ;;

    tag)
        if [[ "$VERSION" == *-SNAPSHOT ]]; then
            echo "!! Refusing to tag a SNAPSHOT version ($VERSION). Run 'make release' for the tested path."
            exit 1
        fi
        make_tag
        ;;

    *)
        echo "usage: $0 {build|release|tag}" >&2
        exit 1
        ;;
esac