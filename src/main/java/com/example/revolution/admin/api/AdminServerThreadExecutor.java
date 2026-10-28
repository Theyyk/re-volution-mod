package com.example.revolution.admin.api;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.minecraft.server.MinecraftServer;

/** Hands Admin API reads/mutations onto the authoritative Minecraft server thread. */
final class AdminServerThreadExecutor {
    private static final long TIMEOUT_SECONDS = 5L;
    private final MinecraftServer server;

    AdminServerThreadExecutor(MinecraftServer server) {
        if (server == null) throw new IllegalArgumentException("MinecraftServer is required");
        this.server = server;
    }

    <T> T call(Callable<T> callable) throws Exception {
        return call(callable, TIMEOUT_SECONDS);
    }

    <T> T call(Callable<T> callable, long timeoutSeconds) throws Exception {
        if (timeoutSeconds < 1 || timeoutSeconds > 30) throw new IllegalArgumentException("Invalid server-thread timeout");
        if (server.isCallingFromMinecraftThread()) return callable.call();
        FutureTask<T> task = new FutureTask<>(callable);
        server.addScheduledTask(task);
        try {
            return task.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            task.cancel(false);
            throw e;
        } catch (TimeoutException e) {
            task.cancel(false);
            throw e;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new IllegalStateException(cause);
        }
    }
}
