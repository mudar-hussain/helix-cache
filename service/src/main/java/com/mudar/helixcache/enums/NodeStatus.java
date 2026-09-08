package com.mudar.helixcache.enums;

public enum NodeStatus {
    UP, SUSPECT, DOWN
}

// UP -> Green -> responds to heartbeat
// SUSPECT -> Yellow -> missed 1-2 heartbeat: it can be slow or unreachable but not confirmed
// DOWN -> Red -> missed threshold heartbeats to be declared dead