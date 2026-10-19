package com.example.revolution.persistence;

import com.example.revolution.number.LargeNumbers;
import com.example.revolution.resource.repository.ResourceRepository;
import com.mongodb.client.model.UpdateOptions;
import java.math.BigDecimal;
import java.util.UUID;
import org.bson.Document;
import static com.example.revolution.resource.definition.ResourceDefinitions.*;

public final class MongoResourceRepository implements ResourceRepository {
    private final MongoConnection connection;

    public MongoResourceRepository(MongoConnection connection) { this.connection = connection; }

    @Override
    public void purgeDeprecatedLightnings() {

        connection.collection("players").updateMany(new Document(RETIRED_LIGHTNINGS, new Document("$exists", true)),
                new Document("$unset", new Document(RETIRED_LIGHTNINGS, "")));
        connection.collection("custom_mobs").updateMany(new Document("resource", RETIRED_LIGHTNINGS),
                new Document("$unset", new Document("resource", "").append("amount", "")));
        connection.database().getCollection("custom_resources").deleteMany(new Document("name", RETIRED_LIGHTNINGS));
    }

    @Override
    public void purgeDeprecatedLightnings(UUID uuid) {

        connection.collection("players").updateOne(new Document("_id", uuid.toString()),
                new Document("$unset", new Document(RETIRED_LIGHTNINGS, "")));
    }

    @Override
    public BigDecimal getPlayerResourceValue(UUID uuid, String resource) {

        String canonical = findCanonicalName(resource);
        if (canonical == null) return BigDecimal.ZERO;
        Document doc = connection.collection("players").find(new Document("_id", uuid.toString())).first();
        if (doc == null) return BigDecimal.ZERO;
        String normalized = normalize(canonical);
        if (normalized.equals("coins")) return getDecimal(doc, "balance");
        if (normalized.equals("crystals")) return getDecimal(doc, "crystals");
        Document custom = doc.get("resources", Document.class);
        return custom == null ? BigDecimal.ZERO : getDecimal(custom, normalized);
    }

    @Override
    public void writePlayerResource(UUID uuid, String canonical, BigDecimal value) {

        String field = playerResourceField(canonical);
        connection.collection("players").updateOne(new Document("_id", uuid.toString()),
                new Document("$set", new Document(field, LargeNumbers.canonical(value.max(BigDecimal.ZERO)))),
                new UpdateOptions().upsert(true));
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
    private String playerResourceField(String canonical) {
        String normalized = normalize(canonical);
        if (normalized.equals("coins")) return "balance";
        if (normalized.equals("crystals")) return "crystals";
        return "resources." + normalized;
    }
}
