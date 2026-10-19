package com.example.revolution.admin.api;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** v1.3.2 deliberately permits only the IPv4 loopback endpoint. Remote administration is not implemented. */
public final class AdminApiBindPolicy {
    private AdminApiBindPolicy() {}

    public static InetAddress loopback() {
        try {
            return InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            throw new IllegalStateException("IPv4 loopback is unavailable", e);
        }
    }

    public static boolean isAllowed(InetAddress address) {
        return address != null && "127.0.0.1".equals(address.getHostAddress()) && address.isLoopbackAddress();
    }

    public static void requireAllowed(InetAddress address) {
        if (!isAllowed(address)) throw new IllegalArgumentException("Admin API requires IPv4 loopback binding");
    }
}
