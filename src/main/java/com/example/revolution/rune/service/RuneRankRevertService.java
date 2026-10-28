package com.example.revolution.rune.service;

import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.player.service.PlayerStatsSyncService;
import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.rules.RuneProgression;
import com.example.revolution.rune.rules.RuneRankRevertDecision;
import com.example.revolution.rune.state.RuneInventory;
import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;

/** Reverts exactly one selected rune by one rank without touching resources or enhancements. */
public final class RuneRankRevertService {
    private final PlayerRepository players;
    private final PlayerStatsSyncService stats;
    private final RuneSyncService sync;

    public RuneRankRevertService(PlayerRepository players, PlayerStatsSyncService stats, RuneSyncService sync) {
        this.players = players;
        this.stats = stats;
        this.sync = sync;
    }

    public void revertRune(EntityPlayerMP player, int collectionIndex, int runeIndex) {
        int statusId = RuneStatusConfig.statusForCollection(collectionIndex);
        if (!RuneStatusConfig.isRuneStatus(statusId)) {
            player.sendMessage(new TextComponentString("§cОткат ранга недоступен для этой legacy-коллекции."));
            return;
        }
        if (runeIndex < 0 || runeIndex >= RuneInventory.BOOST_TYPE_COUNT) {
            player.sendMessage(new TextComponentString("§cНекорректная руна."));
            return;
        }

        List<RuneRecord> runes = players.getRunesInCollection(player.getUniqueID(), collectionIndex);
        int[] bestRanks = new int[RuneInventory.BOOST_TYPE_COUNT];
        for (RuneRecord rune : runes) {
            if (rune.runeIndex < 0 || rune.runeIndex >= RuneInventory.BOOST_TYPE_COUNT) continue;
            bestRanks[rune.runeIndex] = Math.max(bestRanks[rune.runeIndex], rune.layer);
        }

        RuneRankRevertDecision decision = RuneRankRevertDecision.evaluate(bestRanks, runeIndex);
        if (!decision.isAllowed()) {
            if (decision.reason == RuneRankRevertDecision.Reason.MISSING_RUNE) {
                player.sendMessage(new TextComponentString("§cЭта руна ещё не получена."));
            } else if (decision.reason == RuneRankRevertDecision.Reason.BELOW_ALLOWED_RANK) {
                player.sendMessage(new TextComponentString("§eОткат доступен начиная с ранга «"
                        + RuneProgression.rankName(RuneProgression.ADVANCED_RANK) + "»."));
            } else {
                player.sendMessage(new TextComponentString("§cЭту руну сейчас нельзя откатить."));
            }
            return;
        }

        if (!players.setRuneRank(player.getUniqueID(), collectionIndex, runeIndex, decision.targetRank)) {
            player.sendMessage(new TextComponentString("§cНе удалось сохранить откат ранга руны."));
            return;
        }

        sync.sendRunes(player, collectionIndex);
        stats.sendTo(player);
        player.sendMessage(new TextComponentString("§aРанг руны откатан: §f"
                + RuneProgression.rankName(decision.currentRank) + " §7→ §f"
                + RuneProgression.rankName(decision.targetRank)
                + "§a. Усиление сохранено. Ресурсы не возвращаются."));
    }
}
