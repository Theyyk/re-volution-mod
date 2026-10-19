package com.example.revolution.persistence;

import com.example.revolution.RevolutionMod;
import com.example.revolution.mob.definition.MobDefinition;
import com.example.revolution.mob.repository.MobRepository;
import com.example.revolution.number.LargeNumbers;
import com.mongodb.client.MongoCollection;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import static com.example.revolution.resource.definition.ResourceDefinitions.*;

public final class MongoMobRepository implements MobRepository {
    private final MongoConnection connection;
    private final boolean enabled;

    public MongoMobRepository(MongoConnection connection) { this(connection, true); }

    /** The old core repositories were available only after physical-server preInit. */
    public MongoMobRepository(MongoConnection connection, boolean enabled) {
        this.connection = connection;
        this.enabled = enabled;
    }

    private MongoCollection<Document> getMobsCollection() { return connection.database().getCollection("custom_mobs"); }

    @Override
    public void createMob(String name, String type, BigDecimal hp, int money) {
        if (!enabled || !connection.isConnected()) return;
        getMobsCollection().insertOne(new Document("name", name).append("type", type)
                .append("hp", LargeNumbers.canonical(hp)).append("money", money)
                .append("x", 0.0).append("y", 0.0).append("z", 0.0).append("enabled", false));
        RevolutionMod.logger.info("Mob created: " + name);
    }

    @Override
    public void setMobSpawn(String name, double x, double y, double z) {
        if (!enabled || !connection.isConnected()) return;
        getMobsCollection().updateOne(new Document("name", name),
                new Document("$set", new Document("x", x).append("y", y).append("z", z).append("enabled", true)));
    }

    @Override
    public void unspawnMob(String name) {
        if (!enabled || !connection.isConnected()) return;
        getMobsCollection().updateOne(new Document("name", name), new Document("$set", new Document("enabled", false)));
    }

    @Override
    public void deleteMob(String name) {
        if (!enabled || !connection.isConnected()) return;
        getMobsCollection().deleteMany(new Document("name", name));
    }

    @Override
    public List<MobDefinition> getAllMobs() {
        List<MobDefinition> result = new ArrayList<>();
        if (!enabled || !connection.isConnected()) return result;
        for (Document doc : getMobsCollection().find()) {
            result.add(new MobDefinition(doc.getString("name"), doc.getString("type"), getBigDecimal(doc, "hp", BigDecimal.TEN),
                    doc.getInteger("money", 50), doc.getDouble("x"), doc.getDouble("y"), doc.getDouble("z"),
                    doc.getBoolean("enabled", false)));
        }
        return result;
    }

    @Override
    public MobDefinition getMob(String name) {
        if (!enabled || !connection.isConnected()) return null;
        Document doc = getMobsCollection().find(new Document("name", name)).first();
        if (doc == null) return null;
        return new MobDefinition(doc.getString("name"), doc.getString("type"), getBigDecimal(doc, "hp", BigDecimal.TEN),
                doc.getInteger("money", 50), doc.getDouble("x"), doc.getDouble("y"), doc.getDouble("z"),
                doc.getBoolean("enabled", false));
    }

    private BigDecimal getBigDecimal(Document doc, String field, BigDecimal fallback) {
        if (doc == null) return fallback;
        Object raw = doc.get(field);
        if (raw == null) return fallback;
        try {
            if (raw instanceof Number) return LargeNumbers.normalize(new BigDecimal(raw.toString()));
            return LargeNumbers.parseExact(String.valueOf(raw));
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
    @Override
    public void setMobReward(String mobName, String resource, BigDecimal amount) {

        String canonical = findCanonicalName(resource);
        if (canonical == null || amount == null || amount.signum() <= 0) return;
        connection.collection("custom_mobs").updateOne(new Document("name", mobName),
                new Document("$set", new Document("resource", canonical)
                        .append("amount", LargeNumbers.canonical(amount))));
    }
    @Override
    public void setMobReward(String mobName, String resource, int amount) {
        setMobReward(mobName, resource, BigDecimal.valueOf(amount));
    }
    @Override
    public String getMobResource(String mobName) {

        Document doc = connection.collection("custom_mobs").find(new Document("name", mobName)).first();
        if (doc == null) return "coins";
        String resource = doc.getString("resource");
        if (resource == null || resource.isEmpty()) return "coins";
        String canonical = findCanonicalName(resource);
        return canonical == null ? "coins" : canonical;
    }
    @Override
    public BigDecimal getMobAmountValue(String mobName) {

        Document doc = connection.collection("custom_mobs").find(new Document("name", mobName)).first();
        if (doc == null) return BigDecimal.ZERO;
        BigDecimal amount = getDecimal(doc, "amount");
        if (amount.signum() > 0) return amount;
        return getDecimal(doc, "money");
    }
    @Override
    public int getMobAmount(String mobName) {
        BigDecimal value = getMobAmountValue(mobName);
        if (value.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) >= 0) return Integer.MAX_VALUE;
        return Math.max(0, value.intValue());
    }
    private BigDecimal getDecimal(Document doc, String field) {
        if (doc == null) return BigDecimal.ZERO;
        Object raw = doc.get(field);
        if (raw == null) return BigDecimal.ZERO;
        try {
            if (raw instanceof Number) return LargeNumbers.normalize(new BigDecimal(raw.toString()));
            return LargeNumbers.parseExact(String.valueOf(raw));
        } catch (RuntimeException ignored) {
            return BigDecimal.ZERO;
        }
    }
}
