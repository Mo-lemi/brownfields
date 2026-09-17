#!/usr/bin/env bash
# =============================================================
# Free the shared acceptance-test port before a test stage
# (called by the Makefile: make free-port / make test / ...)
#
# Every stage of the pipeline - acceptance tests against the
# reference server and against our own server - binds port 5000
# (override with: scripts/free-port.sh <port>).
#
# A run that was interrupted before its teardown completed
# (Ctrl+C, a killed surefire fork, a teardown exception) leaves
# an ORPHANED server process holding the port, and every later
# run fails with "Address already in use". This script clears
# the port before the next stage boots.
#
# Safety: only JAVA processes are ever killed. If the port is
# held by any other application the script fails loudly instead.
# =============================================================
set -uo pipefail

PORT="${1:-5000}"

windows_pid_of_listeners() {
    # netstat -ano lines:  TCP  0.0.0.0:5000  0.0.0.0:0  LISTENING  18528
    netstat -ano -p tcp | awk -v port="$PORT" '
        $1 == "TCP" && toupper($4) == "LISTENING" {
            n = split($2, parts, ":")
            if (parts[n] == port) print $NF
        }' | sort -u
}

unix_pid_of_listeners() {
    lsof -t -iTCP:"$PORT" -sTCP:LISTEN 2>/dev/null | sort -u
}

pid_of_listeners() {
    case "$(uname -s)" in
        MINGW*|MSYS*|CYGWIN*|Windows_NT) windows_pid_of_listeners ;;
        *)                               unix_pid_of_listeners ;;
    esac
}

# Image/process name of a pid, or empty when it cannot be determined.
name_of_pid() {
    local pid="$1"
    case "$(uname -s)" in
        MINGW*|MSYS*|CYGWIN*|Windows_NT)
            # double slashes + ARG_CONV_EXCL stop MSYS mangling the switches
            MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL='*' \
                tasklist /FO CSV /NH /FI "PID eq $pid" 2>/dev/null |
                head -1 | cut -d'"' -f2
            ;;
        *)
            ps -p "$pid" -o comm= 2>/dev/null | xargs -r basename
            ;;
    esac
}

kill_pid() {
    local pid="$1"
    case "$(uname -s)" in
        MINGW*|MSYS*|CYGWIN*|Windows_NT)
            MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL='*' \
                taskkill /F /T /PID "$pid" >/dev/null 2>&1
            ;;
        *)
            kill -9 "$pid" >/dev/null 2>&1
            ;;
    esac
}

PIDS="$(pid_of_listeners)"
if [[ -z "$PIDS" ]]; then
    echo ">> Port $PORT is free"
    exit 0
fi

echo ">> Port $PORT is held by pid(s): $(echo "$PIDS" | tr '\n' ' ')"

KILLED=""
for pid in $PIDS; do
    name="$(name_of_pid "$pid")"
    if [[ "$name" =~ ^java ]]; then
        echo ">> Killing orphaned java server pid $pid ($name)"
        kill_pid "$pid"
        KILLED="$KILLED $pid"
    else
        echo "!! Port $PORT is held by NON-java pid $pid (${name:-unknown}) -" \
             "not killing it. Free the port manually, then re-run."
        exit 1
    fi
done

# Wait briefly for the OS to release the sockets of the killed process(es).
for _ in 1 2 3 4 5 6 7 8 9 10; do
    [[ -z "$(pid_of_listeners)" ]] && break
    sleep 0.5
done

if [[ -n "$(pid_of_listeners)" ]]; then
    echo "!! Port $PORT is still not free after killing:$KILLED"
    exit 1
fi

echo ">> Port $PORT freed (killed:$KILLED)"
exit 0
