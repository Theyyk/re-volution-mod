package com.example.revolution.gameplay;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * Narrow synchronous context for server-authoritative damage applications.
 * It labels an already-calculated hit without owning combat math or persistence.
 */
public final class GameplayDamageContext {
    private static final ThreadLocal<Deque<GameplayDamageSource>> SOURCES =
            ThreadLocal.withInitial(ArrayDeque::new);

    private GameplayDamageContext() {}

    public static GameplayDamageSource current() {
        Deque<GameplayDamageSource> stack = SOURCES.get();
        return stack.isEmpty() ? null : stack.peek();
    }

    public static void run(GameplayDamageSource source, Runnable action) {
        if (action == null) return;
        if (source == null) {
            action.run();
            return;
        }
        Deque<GameplayDamageSource> stack = SOURCES.get();
        stack.push(source);
        try {
            action.run();
        } finally {
            stack.pop();
            if (stack.isEmpty()) SOURCES.remove();
        }
    }

    public static <T> T call(GameplayDamageSource source, Supplier<T> action) {
        if (action == null) return null;
        if (source == null) return action.get();
        Deque<GameplayDamageSource> stack = SOURCES.get();
        stack.push(source);
        try {
            return action.get();
        } finally {
            stack.pop();
            if (stack.isEmpty()) SOURCES.remove();
        }
    }
}
