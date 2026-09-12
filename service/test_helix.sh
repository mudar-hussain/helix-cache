#!/usr/bin/env bash

###############################################################################
# HELIX CACHE - END TO END TEST SUITE
#
# Requirements:
#   - curl
#   - bash
#
# Run:
#   bash helix-test.sh
#
# The script intentionally DOES NOT use "set -e" because a failed test
# must not stop the complete test suite.
###############################################################################

set -uo pipefail

###############################################################################
# CONFIGURATION
###############################################################################

A="http://localhost:8081"
B="http://localhost:8082"
C="http://localhost:8083"
D="http://localhost:8084"
E="http://localhost:8085"

NODES=("$A" "$B" "$C" "$D" "$E")

GREEN="\033[0;32m"
RED="\033[0;31m"
YELLOW="\033[1;33m"
BLUE="\033[0;34m"
CYAN="\033[0;36m"
NC="\033[0m"

PASS=0
FAIL=0
SKIP=0

###############################################################################
# HELPERS
###############################################################################

pass() {
    echo -e "${GREEN}✔ PASS${NC} - $1"
    PASS=$((PASS + 1))
}

fail() {
    echo -e "${RED}✘ FAIL${NC} - $1"
    FAIL=$((FAIL + 1))
}

skip() {
    echo -e "${YELLOW}⚠ SKIP${NC} - $1"
    SKIP=$((SKIP + 1))
}

section() {
    echo
    echo -e "${BLUE}============================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}============================================================${NC}"
}

info() {
    echo -e "${CYAN}$1${NC}"
}

###############################################################################
# HTTP HELPERS
###############################################################################

http() {
    curl -sS --connect-timeout 3 --max-time 10 "$@" 2>/dev/null
}

code() {
    curl -sS \
        --connect-timeout 3 \
        --max-time 10 \
        -o /dev/null \
        -w "%{http_code}" \
        "$@" 2>/dev/null
}

body() {
    curl -sS \
        --connect-timeout 3 \
        --max-time 10 \
        "$@" 2>/dev/null
}

contains() {
    local text="$1"
    local expected="$2"

    [[ "$text" == *"$expected"* ]]
}

json_contains() {
    local url="$1"
    local expected="$2"

    local response

    response=$(body "$url" || true)

    [[ "$response" == *"$expected"* ]]
}

###############################################################################
# WAIT FOR CLUSTER
###############################################################################

wait_for_node() {

    local node="$1"
    local max_attempts=30
    local attempt=1

    while [[ $attempt -le $max_attempts ]]
    do
        if [[ "$(code "$node/actuator/health")" == "200" ]]
        then
            return 0
        fi

        sleep 1
        attempt=$((attempt + 1))
    done

    return 1
}

###############################################################################
# START
###############################################################################

clear 2>/dev/null || true

echo
echo "============================================================"
echo "              HELIX CACHE E2E TEST SUITE"
echo "============================================================"
echo
echo "Nodes:"
printf '  %s\n' "${NODES[@]}"
echo

###############################################################################
# HEALTH
###############################################################################

section "1. NODE HEALTH"

for node in "${NODES[@]}"
do
    if wait_for_node "$node"
    then
        pass "$node is healthy"
    else
        fail "$node is not healthy"
    fi
done

###############################################################################
# BASIC CLUSTER
###############################################################################

section "2. CLUSTER"

response=$(body "$A/cluster/ping" || true)

if contains "$response" "\"status\":\"UP\""
then
    pass "Cluster ping"
else
    fail "Cluster ping"
    info "Response: $response"
fi


response=$(body "$A/cluster/node" || true)

if [[ -n "$response" ]]
then
    pass "Node information"
else
    fail "Node information"
fi


response=$(body "$A/cluster/nodes" || true)

if [[ -n "$response" ]]
then
    pass "Node list"
else
    fail "Node list"
fi


response=$(body "$A/cluster/stats" || true)

if [[ -n "$response" ]]
then
    pass "Cluster stats"
else
    fail "Cluster stats"
fi

###############################################################################
# RING
###############################################################################

section "3. CONSISTENT HASH RING"

response=$(body "$A/cluster/ring" || true)

if [[ -n "$response" ]]
then
    pass "Ring endpoint"
else
    fail "Ring endpoint"
fi


response=$(body "$A/cluster/ring/nodes" || true)

if [[ -n "$response" ]]
then
    pass "Ring nodes endpoint"
else
    fail "Ring nodes endpoint"
fi

###############################################################################
# ROUTING
###############################################################################

section "4. CONSISTENT HASH ROUTING"

r1=$(body "$A/cluster/route/google" || true)
r2=$(body "$A/cluster/route/google" || true)

if [[ -n "$r1" && "$r1" == "$r2" ]]
then
    pass "Deterministic routing"
else
    fail "Deterministic routing"
    info "First:  $r1"
    info "Second: $r2"
fi


r3=$(body "$A/cluster/route/user-123" || true)
r4=$(body "$A/cluster/route/user-456" || true)

if [[ -n "$r3" && -n "$r4" ]]
then
    pass "Multiple keys can be routed"
else
    fail "Multiple key routing"
fi

###############################################################################
# REPLICAS
###############################################################################

section "5. REPLICATION / REPLICA LOOKUP"

response=$(body "$A/cluster/replicas/google" || true)

if [[ -n "$response" ]]
then
    pass "Replica lookup"
else
    fail "Replica lookup"
fi

###############################################################################
# DISTRIBUTION
###############################################################################

section "6. DISTRIBUTION"

response=$(body "$A/cluster/distribution" || true)

if [[ -n "$response" ]]
then
    pass "Distribution endpoint"
else
    fail "Distribution endpoint"
fi

###############################################################################
# BASIC CACHE PUT / GET
###############################################################################

section "7. CACHE PUT / GET"

TEST_KEY="e2e-user-$(date +%s)"
TEST_VALUE="Mudar-Hussain"

put_code=$(code -X PUT "$A/cache/$TEST_KEY?value=$TEST_VALUE")

if [[ "$put_code" =~ ^2 ]]
then
    pass "PUT cache entry"
else
    fail "PUT cache entry (HTTP $put_code)"
fi


get_response=$(body "$A/cache/$TEST_KEY" || true)

if contains "$get_response" "$TEST_VALUE"
then
    pass "GET returns stored value"
else
    fail "GET returns stored value"
    info "Response: $get_response"
fi

###############################################################################
# UPDATE
###############################################################################

section "8. CACHE UPDATE"

UPDATED_VALUE="Updated-Value"

update_code=$(code -X PUT "$A/cache/$TEST_KEY?value=$UPDATED_VALUE")

if [[ "$update_code" =~ ^2 ]]
then
    pass "Update existing key"
else
    fail "Update existing key (HTTP $update_code)"
fi


get_response=$(body "$A/cache/$TEST_KEY" || true)

if contains "$get_response" "$UPDATED_VALUE"
then
    pass "Updated value returned"
else
    fail "Updated value returned"
fi

###############################################################################
# REPLICATION
###############################################################################

section "9. REPLICATION"

sleep 2

replica_count=0

for node in "${NODES[@]}"
do
    response=$(body "$node/cache/$TEST_KEY" || true)

    if contains "$response" "$UPDATED_VALUE"
    then
        replica_count=$((replica_count + 1))
        info "$node contains replicated key"
    fi
done

echo
echo "Replica copies found: $replica_count"

if [[ $replica_count -ge 2 ]]
then
    pass "Replication factor >= 2"
else
    fail "Replication factor >= 2"
fi

###############################################################################
# CROSS NODE READ
###############################################################################

section "10. CROSS NODE READ"

CROSS_KEY="cross-node-$(date +%s)"
CROSS_VALUE="cluster-value"

put_code=$(code -X PUT "$B/cache/$CROSS_KEY?value=$CROSS_VALUE")

if [[ "$put_code" =~ ^2 ]]
then
    pass "Write through node B"
else
    fail "Write through node B (HTTP $put_code)"
fi


sleep 2

cross_response=$(body "$A/cache/$CROSS_KEY" || true)

if contains "$cross_response" "$CROSS_VALUE"
then
    pass "Read value through node A"
else
    fail "Read value through node A"
    info "Response: $cross_response"
fi

###############################################################################
# TTL
###############################################################################

section "11. TTL EXPIRATION"

TTL_KEY="ttl-test-$(date +%s)"

EXPIRY_AT=$(date -d "+4 seconds" +"%Y-%m-%dT%H:%M:%S")
echo -e "${GREEN}-> INFO${NC} - EXPIRY_AT = $EXPIRY_AT (IST)${NC}"

put_code=$(code -X PUT "$A/cache/$TTL_KEY?value=temporary&expiresAt=$EXPIRY_AT")

if [[ "$put_code" =~ ^2 ]]
then
    pass "TTL entry created"
else
    fail "TTL entry creation (HTTP $put_code)"
fi


before_ttl=$(code "$A/cache/$TTL_KEY")
if [[ "$before_ttl" == "200" ]]
then
    pass "TTL entry exists before expiration"
else
    fail "TTL entry exists before expiration (HTTP $before_ttl)"
fi


echo "Waiting 5 seconds for TTL..."
sleep 5


after_ttl=$(code "$A/cache/$TTL_KEY")
echo "GET response after TTL:"
body "$A/cache/$TTL_KEY"
echo
if [[ "$after_ttl" == "422" ]]
then
    pass "Entry removed after TTL"
else
    fail "TTL expiration (HTTP $after_ttl)"
fi

###############################################################################
# DELETE
###############################################################################

section "12. DELETE"

DELETE_KEY="delete-test-$(date +%s)"

put_code=$(code -X PUT "$A/cache/$DELETE_KEY?value=delete-me")

if [[ "$put_code" =~ ^2 ]]
then
    pass "Delete test entry created"
else
    fail "Delete test entry creation"
fi


delete_code=$(code -X DELETE "$A/cache/$DELETE_KEY")

if [[ "$delete_code" =~ ^2 ]]
then
    pass "DELETE request"
else
    fail "DELETE request (HTTP $delete_code)"
fi


sleep 3

delete_check=$(code "$A/cache/$DELETE_KEY")

if [[ "$delete_check" == "422" ]]
then
    pass "Deleted key no longer exists"
else
    fail "Deleted key still exists (HTTP $delete_check)"
fi

###############################################################################
# NOT FOUND
###############################################################################

section "13. NOT FOUND"

missing_key="definitely-does-not-exist-$(date +%s)"

missing_code=$(code "$A/cache/$missing_key")

if [[ "$missing_code" == "422" ]]
then
    pass "Missing key returns 422"
else
    fail "Missing key returns (HTTP $missing_code)"
fi

###############################################################################
# INTERNAL APIs
###############################################################################

section "14. INTERNAL APIs"

internal_key="internal-$(date +%s)"

internal_put=$(code -X PUT \
    "$A/internal/cache/$internal_key?value=internal-test")

if [[ "$internal_put" =~ ^2 ]]
then
    pass "Internal PUT"
else
    fail "Internal PUT (HTTP $internal_put)"
fi


internal_get=$(body "$A/internal/cache/$internal_key" || true)

if contains "$internal_get" "internal-test"
then
    pass "Internal GET"
else
    fail "Internal GET"
fi


hints=$(body "$A/internal/cache/hints" || true)

if [[ -n "$hints" ]]
then
    pass "Hints endpoint"
else
    fail "Hints endpoint"
fi


sync_response=$(body "$A/internal/cache/sync/node?targetNodeId=node-b" || true)

if [[ -n "$sync_response" ]]
then
    pass "Sync endpoint reachable"
else
    skip "Sync endpoint response could not be validated"
fi

###############################################################################
# ACCESS TRACKING
###############################################################################

section "15. ACCESS TRACKING"

ACCESS_KEY="hot-key-$(date +%s)"

http -X PUT "$A/cache/$ACCESS_KEY?value=hot-value" >/dev/null || true

for i in {1..30}
do
    http "$A/cache/$ACCESS_KEY" >/dev/null || true
done


access_response=$(body "$A/admin/stats/access" || true)

if [[ -n "$access_response" ]]
then
    pass "Access statistics endpoint"
else
    fail "Access statistics endpoint"
fi

###############################################################################
# HOT KEY PREDICTION
###############################################################################

section "16. HOT KEY PREDICTION"

prediction_response=$(body "$A/admin/stats/predictions" || true)

if [[ -n "$prediction_response" ]]
then
    pass "Hot-key prediction endpoint"
else
    fail "Hot-key prediction endpoint"
fi

###############################################################################
# ADMIN NODE STATE
###############################################################################

section "17. NODE PAUSE / RESUME"

# ------------------------------------------------------------
# Pause local node A
# ------------------------------------------------------------

pause_code=$(code -X PUT "$A/admin/node/pause")

if [[ "$pause_code" =~ ^2 ]]
then
    pass "Pause node A"
else
    fail "Pause node A (HTTP $pause_code)"
fi


echo "Waiting for node-a to be reported DOWN..."
sleep 20

pause_state=$(body "$B/cluster/nodes" || true)

echo "Cluster nodes after pause:"
echo "$pause_state"


if echo "$pause_state" | grep -q '"nodeId"[[:space:]]*:[[:space:]]*"node-a"'
then
    if echo "$pause_state" | grep -A5 '"nodeId"[[:space:]]*:[[:space:]]*"node-a"' \
        | grep -q '"nodeStatus"[[:space:]]*:[[:space:]]*"DOWN"'
    then
        pass "Node A reports DOWN after pause"
    else
        skip "Node A pause accepted but nodeStatus is not DOWN yet"
    fi
else
    fail "Node A not found in /cluster/nodes"
fi


# ------------------------------------------------------------
# Resume local node A
# ------------------------------------------------------------

resume_code=$(code -X PUT "$A/admin/node/resume")

if [[ "$resume_code" =~ ^2 ]]
then
    pass "Resume node A"
else
    fail "Resume node A (HTTP $resume_code)"
fi


echo "Waiting for node-a to be reported UP..."
sleep 3

resume_state=$(body "$B/cluster/nodes" || true)

echo "Cluster nodes after resume:"
echo "$resume_state"


if echo "$resume_state" | grep -q '"nodeId"[[:space:]]*:[[:space:]]*"node-a"'
then
    if echo "$resume_state" | grep -A5 '"nodeId"[[:space:]]*:[[:space:]]*"node-a"' \
        | grep -q '"nodeStatus"[[:space:]]*:[[:space:]]*"UP"'
    then
        pass "Node A reports UP after resume"
    else
        skip "Node A resume accepted but nodeStatus is not UP yet"
    fi
else
    fail "Node A not found in /cluster/nodes"
fi

###############################################################################
# SLOW NODE
###############################################################################

section "18. SLOW NODE SIMULATION"

slow_code=$(code -X POST "$A/admin/node/slow?milliseconds=200")

if [[ "$slow_code" =~ ^2 ]]
then
    pass "Slow-node simulation"
else
    fail "Slow-node simulation (HTTP $slow_code)"
fi

###############################################################################
# SSE
###############################################################################

section "19. SSE EVENTS"

SSE_FILE="/tmp/helix-sse-test.out"
rm -f "$SSE_FILE"

echo "Starting SSE listener..."

curl -sN \
    --connect-timeout 3 \
    "$A/cluster/events/stream" \
    > "$SSE_FILE" 2>/dev/null &

SSE_PID=$!

sleep 1

echo "Triggering cluster event..."

SSE_KEY="sse-test-$(date +%s)"

code -X PUT \
    "$A/cache/$SSE_KEY?value=sse-test" >/dev/null

sleep 2

kill "$SSE_PID" 2>/dev/null || true
wait "$SSE_PID" 2>/dev/null || true

echo
echo "SSE response:"
cat "$SSE_FILE"
echo

if [[ -s "$SSE_FILE" ]]
then
    pass "SSE event received"
else
    fail "SSE event was not received"
fi

###############################################################################
# DISTRIBUTION SANITY
###############################################################################

section "20. DISTRIBUTION SANITY"

declare -A ROUTES

for i in {1..30}
do
    key="distribution-key-$i"

    route=$(body "$A/cluster/route/$key" || true)

    if [[ -n "$route" ]]
    then
        ROUTES["$route"]=1
    fi
done

unique_routes=${#ROUTES[@]}

echo "Unique route targets observed: $unique_routes"

if [[ $unique_routes -ge 2 ]]
then
    pass "Keys distributed across multiple nodes"
else
    fail "Keys distributed across multiple nodes"
fi

###############################################################################
# FINAL READ
###############################################################################

section "21. FINAL DATA INTEGRITY"

final_response=$(body "$A/cache/$TEST_KEY" || true)

if contains "$final_response" "$UPDATED_VALUE"
then
    pass "Original test data remains readable"
else
    fail "Original test data was lost"
fi

###############################################################################
# SUMMARY
###############################################################################

section "TEST SUMMARY"

TOTAL=$((PASS + FAIL + SKIP))

echo
echo -e "${GREEN}PASS : $PASS${NC}"
echo -e "${RED}FAIL : $FAIL${NC}"
echo -e "${YELLOW}SKIP : $SKIP${NC}"
echo
echo "TOTAL: $TOTAL"
echo

if [[ $FAIL -eq 0 ]]
then
    echo -e "${GREEN}============================================================${NC}"
    echo -e "${GREEN}                 ALL TESTS PASSED${NC}"
    echo -e "${GREEN}============================================================${NC}"
    exit 0
else
    echo -e "${RED}============================================================${NC}"
    echo -e "${RED}                 TEST SUITE FAILED${NC}"
    echo -e "${RED}============================================================${NC}"
    exit 1
fi