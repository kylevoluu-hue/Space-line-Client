package com.spaceline.launcher.account;

/** How an account authenticates. */
public enum AuthType {
    /** Authenticated through Microsoft/Xbox Live; can join online servers. */
    MICROSOFT,
    /** A local, offline profile; restricted to offline/LAN play. */
    OFFLINE
}
