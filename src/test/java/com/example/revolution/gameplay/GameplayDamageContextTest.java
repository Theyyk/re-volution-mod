package com.example.revolution.gameplay;

public final class GameplayDamageContextTest {
    public static void main(String[] args) {
        check(GameplayDamageContext.current() == null, "context must start empty");

        GameplayDamageContext.run(GameplayDamageSource.BURNING, () -> {
            check(GameplayDamageContext.current() == GameplayDamageSource.BURNING, "outer source missing");
            GameplayDamageContext.run(GameplayDamageSource.CURSE, () ->
                    check(GameplayDamageContext.current() == GameplayDamageSource.CURSE, "nested source missing"));
            check(GameplayDamageContext.current() == GameplayDamageSource.BURNING, "outer source not restored");
        });

        String result = GameplayDamageContext.call(GameplayDamageSource.DECAY, () -> {
            check(GameplayDamageContext.current() == GameplayDamageSource.DECAY, "call source missing");
            return "ok";
        });
        check("ok".equals(result), "call result changed");
        check(GameplayDamageContext.current() == null, "context leaked after application");

        System.out.println("GameplayDamageContextTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
