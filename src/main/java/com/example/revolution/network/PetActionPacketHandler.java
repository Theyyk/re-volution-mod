package com.example.revolution.network;

import com.example.revolution.lifecycle.ServerLifecycle;
import com.example.revolution.lifecycle.ServerServices;
import com.example.revolution.pet.definition.PetDefinition;
import com.example.revolution.pet.definition.PetEffectDefinition;
import com.example.revolution.pet.service.PetService;
import com.example.revolution.pet.state.PetPlayerState;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server adapter for Pet UI intents. */
public final class PetActionPacketHandler implements IMessageHandler<PetActionPacket, IMessage> {
    @Override public IMessage onMessage(PetActionPacket message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        if (player == null || message == null) return null;
        player.getServerWorld().addScheduledTask(() -> handle(player, message));
        return null;
    }

    private static void handle(EntityPlayerMP player, PetActionPacket request) {
        ServerServices services = ServerLifecycle.services();
        PetService.Result result = null;
        String action = request.getAction();
        if ("sync".equals(action)) { sendSnapshot(player, services); return; }
        if ("purchase_pet".equals(action)) result = services.pets.purchasePet(player.getUniqueID(), request.getPetId());
        else if ("select_pet".equals(action)) result = services.pets.selectPet(player.getUniqueID(), request.getPetId());
        else if ("awaken_pet".equals(action)) result = services.pets.awaken(player.getUniqueID(), request.getPetId());
        else if ("revert_pet".equals(action)) result = services.pets.revertRank(player.getUniqueID(), request.getPetId());
        else if ("purchase_normal_slot".equals(action)) result = services.pets.purchaseNormalSlot(player.getUniqueID());
        else if ("purchase_premium_slot".equals(action)) result = services.pets.purchasePremiumSlot(player.getUniqueID());
        if (result != null && result != PetService.Result.APPLIED && result != PetService.Result.ALREADY_OWNED) {
            player.sendMessage(new TextComponentString("Pets: " + result.name()));
        }
        sendSnapshot(player, services);
    }

    public static void sendSnapshot(EntityPlayerMP player, ServerServices services) {
        UUID id = player.getUniqueID();
        PetPlayerState state = services.pets.snapshot(id);
        PetPlayerState.RunState run = state.run;
        List<PetSnapshotPacket.Entry> entries = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (PetDefinition pet : services.pets.catalog()) {
            PetPlayerState.PetProgress progress = state.pets.get(pet.petId);
            PetPlayerState.SelectedPet selected = null;
            if (run != null) for (PetPlayerState.SelectedPet candidate : run.selectedPets) if (pet.petId.equals(candidate.petId)) { selected = candidate; break; }
            boolean persistent = progress != null && progress.rank.isPersistentAcrossPrestige();
            boolean owned = persistent || (run != null && run.purchasedPetIds.contains(pet.petId));
            long cooldown = run == null || !run.cooldownUntil.containsKey(pet.petId) ? 0L : run.cooldownUntil.get(pet.petId);
            if (cooldown <= now) cooldown = 0L;
            StringBuilder effects = new StringBuilder();
            for (PetEffectDefinition effect : pet.effects) {
                if (effects.length() > 0) effects.append("; ");
                effects.append(effect.type.name());
                if (!effect.channel.isEmpty()) effects.append('[').append(effect.channel).append(']');
                effects.append('=').append(services.balance.getActiveValue(effect.balanceValueId));
            }
            entries.add(new PetSnapshotPacket.Entry(
                    pet.petId,
                    progress == null ? "ORDINARY" : progress.rank.name(),
                    progress == null ? "1" : progress.level.toString(),
                    progress == null ? "0" : progress.xp.stripTrailingZeros().toPlainString(),
                    owned, selected != null, selected != null && selected.premiumSlot, cooldown,
                    services.balance.getActiveValue(pet.purchaseCostValueId), effects.toString()));
        }
        int normal = run == null ? 0 : run.normalSlotsUnlockedThisRun;
        String nextCost = services.pets.nextNormalSlotCost(id).stripTrailingZeros().toPlainString();
        NetworkHandler.sendToClient(new PetSnapshotPacket(normal, state.premiumPetSlotsUnlocked, nextCost, entries), player);
    }
}
