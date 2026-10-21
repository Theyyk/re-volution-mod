package com.example.revolution.player.service;

import com.example.revolution.RevolutionMod;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.resource.service.ResourceService;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;

public final class PlayerSessionService {
    private final PlayerRepository players;
    private final ResourceService resources;
    private final PlayerStatsSyncService stats;

    public PlayerSessionService(PlayerRepository players, ResourceService resources, PlayerStatsSyncService stats) {
        this.players = players;
        this.resources = resources;
        this.stats = stats;
    }

    public void joined(EntityPlayerMP player) {
        java.util.UUID uuid = player.getUniqueID();

        // Создаём основную коллекцию, если её нет
        if (players.getCollectionNames(uuid).isEmpty()) {
            players.addCollection(uuid, "Основная");
            RevolutionMod.logger.info("Created default collection for " + player.getName());
        }

        // v1.2.2: полностью удаляем устаревшее поле lightnings,
        // в том числе у новых документов, созданных старой схемой players.
        resources.purgeDeprecatedLightnings(uuid);

        stats.sendTo(player);
        RevolutionMod.logger.info("Player joined: " + player.getName());
        }
}
