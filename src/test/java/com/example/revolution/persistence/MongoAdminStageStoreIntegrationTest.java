package com.example.revolution.persistence;

import com.example.revolution.admin.definition.StagePatch;
import com.example.revolution.location.definition.MobZoneIds;
import java.util.Collections;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="REVOLUTION_ADMIN_TEST_MONGO_URI", matches="mongodb://127\\.0\\.0\\.1:[0-9]+/.*")
class MongoAdminStageStoreIntegrationTest {
    @Test void standaloneMongoCommitsWholeBatchAndRecoversPersistedBeforeImageAfterRestart() {
        String originalUri = System.getProperty("revolution.mongo.uri");
        String originalDatabase = System.getProperty("revolution.mongo.database");
        String database = "admin140_test_" + UUID.randomUUID().toString().replace("-", "");
        System.setProperty("revolution.mongo.uri", System.getenv("REVOLUTION_ADMIN_TEST_MONGO_URI"));
        System.setProperty("revolution.mongo.database", database);
        try (MongoConnection connection = new MongoConnection(LogManager.getLogger("AdminStageIntegrationTest"))) {
            connection.connect();
            int id = MobZoneIds.encode(1,1);
            connection.collection("custom_locations").insertOne(new Document("locationId",id).append("kind","BOSS_ZONE"));
            connection.collection("custom_mobs").insertOne(new Document("name","Mob1_1").append("hp","100").append("resource","coins").append("amount","10").append("money",50));
            connection.collection("custom_mob_spawns").insertOne(new Document("locationId",id).append("spawnId",1).append("mobName","Mob1_1").append("x",4.0).append("y",64.0).append("z",8.0).append("enabled",true).append("spawnMode","FIXED_POINT"));
            MongoAdminStageStore store = new MongoAdminStageStore(connection,true);
            String before = store.revision();
            store.commit(before, Collections.singletonList(new StagePatch(1,1,"1E18",5,null,null)));
            assertEquals("1E18",connection.collection("custom_mobs").find().first().getString("hp"));
            assertEquals("10",connection.collection("custom_mobs").find().first().getString("amount"));
            assertEquals(5,connection.collection("custom_mob_spawns").countDocuments());
            assertEquals("BOSS_ZONE",connection.collection("custom_locations").find().first().getString("kind"));
            assertThrows(IllegalArgumentException.class, () -> store.commit(before,Collections.singletonList(new StagePatch(1,1,"999",null,null,null))));
            // Simulated crash: a durable PENDING journal and a partially materialized after-image.
            connection.collection("admin_stage_commit").updateOne(new Document("_id","active"),new Document("$set",new Document("state","PENDING")));
            connection.collection("custom_mobs").updateOne(new Document("name","Mob1_1"),new Document("$set",new Document("hp","666")));
            connection.collection("custom_mob_spawns").deleteOne(new Document("spawnId",2));
            new MongoAdminStageStore(connection,true).recover();
            assertEquals("100",connection.collection("custom_mobs").find().first().getString("hp"));
            assertEquals(1,connection.collection("custom_mob_spawns").countDocuments());
            assertEquals(before,store.revision());
            assertEquals("REVERTED",connection.collection("admin_stage_commit").find().first().getString("state"));
            assertTrue(database.startsWith("admin140_test_"));
            connection.database().drop();
        } finally {
            restore("revolution.mongo.uri",originalUri); restore("revolution.mongo.database",originalDatabase);
        }
    }
    private static void restore(String key,String value) { if(value==null)System.clearProperty(key);else System.setProperty(key,value); }
}
