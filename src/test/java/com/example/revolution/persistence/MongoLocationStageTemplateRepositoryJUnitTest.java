package com.example.revolution.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.revolution.location.definition.LocationStageTemplate;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.bson.Document;
import org.junit.jupiter.api.Test;

public final class MongoLocationStageTemplateRepositoryJUnitTest {
    @Test
    void mapsPersistentStageTemplateFields() throws Exception {
        MongoConnection connection = new MongoConnection(LogManager.getLogger("stage-template-test"));
        AtomicReference<String> collectionName = new AtomicReference<>();
        AtomicReference<Document> stored = new AtomicReference<>();
        AtomicReference<Document> current = new AtomicReference<>();

        Field database = MongoConnection.class.getDeclaredField("database");
        database.setAccessible(true);
        database.set(connection, proxy(MongoDatabase.class, (object, method, values) -> {
            if (!method.getName().equals("getCollection")) throw new AssertionError(method);
            collectionName.set((String) values[0]);
            return proxy(MongoCollection.class, (target, operation, arguments) -> {
                if (operation.getName().equals("updateOne")) {
                    stored.set(((Document) arguments[1]).get("$set", Document.class));
                    return null;
                }
                if (operation.getName().equals("find")) {
                    return proxy(FindIterable.class, (iterator, query, ignored) -> {
                        if (query.getName().equals("first")) return current.get();
                        throw new AssertionError(query);
                    });
                }
                throw new AssertionError(operation);
            });
        }));

        MongoLocationStageTemplateRepository repository = new MongoLocationStageTemplateRepository(connection, true);
        repository.upsert(new LocationStageTemplate(4, 15.0D, 5.0D, 6.0D));
        assertEquals("location_stage_templates", collectionName.get());
        assertEquals(4, stored.get().getInteger("worldLocationId"));
        assertEquals(15.0D, ((Number) stored.get().get("mobRadius")).doubleValue(), 0.0D);
        assertEquals(5.0D, ((Number) stored.get().get("triggerOffset")).doubleValue(), 0.0D);
        assertEquals(6.0D, ((Number) stored.get().get("triggerRadius")).doubleValue(), 0.0D);

        current.set(new Document("worldLocationId", 4)
                .append("mobRadius", 21.0D)
                .append("triggerOffset", 2.0D)
                .append("triggerRadius", 3.0D));
        LocationStageTemplate mapped = repository.get(4);
        assertNotNull(mapped);
        assertEquals(21.0D, mapped.getMobRadius(), 0.0D);
        assertEquals(2.0D, mapped.getTriggerOffset(), 0.0D);
        assertEquals(3.0D, mapped.getTriggerRadius(), 0.0D);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
