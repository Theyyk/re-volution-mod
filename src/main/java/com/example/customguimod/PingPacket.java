package com.example.customguimod;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PingPacket implements IMessage {

    private String action;

    public PingPacket() {}

    public PingPacket(String action) {
        this.action = action;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, action);
    }

    public String getAction() {
        return action;
    }

    public static class Handler implements IMessageHandler<PingPacket, IMessage> {
        private static final int LEGACY_CARD_COST = 100;
        private static final Random RANDOM = new Random();

        @Override
        @SideOnly(Side.SERVER)
        public IMessage onMessage(PingPacket message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                String action = message.getAction();
                CustomGuiMod.logger.info("Packet from " + player.getName() + ": " + action);

                List<String> deckNames = MongoManager.getDeckNames(player.getUniqueID());
                int deckIndex = MongoManager.getActiveDeck(player.getUniqueID());

                if (!deckNames.isEmpty() && (deckIndex < 0 || deckIndex >= deckNames.size())) {
                    deckIndex = 0;
                    MongoManager.setActiveDeck(player.getUniqueID(), deckIndex);
                }

                if (action.equals("buy_card")) {
                    buyCards(player, deckIndex, 1, false);
                } else if (action.startsWith("buy_cards:")) {
                    String rawAmount = action.substring("buy_cards:".length());

                    if (rawAmount.equalsIgnoreCase("all")) {
                        buyCards(player, deckIndex, Integer.MAX_VALUE, true);
                        return;
                    }

                    try {
                        int requestedAmount = Integer.parseInt(rawAmount);
                        if (requestedAmount != 1 && requestedAmount != 5
                                && requestedAmount != 10 && requestedAmount != 100) {
                            player.sendMessage(new TextComponentString("§cНекорректное количество рун."));
                            return;
                        }
                        buyCards(player, deckIndex, requestedAmount, false);
                    } catch (NumberFormatException e) {
                        player.sendMessage(new TextComponentString("§cНекорректное количество рун."));
                    }
                } else if (action.startsWith("upgrade_rune:")) {
                    String rawIndex = action.substring("upgrade_rune:".length());
                    try {
                        awakenRune(player, deckIndex, Integer.parseInt(rawIndex));
                    } catch (NumberFormatException e) {
                        player.sendMessage(new TextComponentString("§cНекорректная руна."));
                    }
                } else if (action.equals("get_player_stats")) {
                    PlayerStatsService.sendTo(player);
                } else if (action.equals("get_balance")) {
                    int balance = MongoManager.getBalance(player.getUniqueID());
                    player.sendMessage(new TextComponentString(
                            "§6Твой баланс: " + balance + " монет"));
                    NetworkHandler.INSTANCE.sendTo(new PongPacket(balance), player);
                } else if (action.equals("load_cards")) {
                    sendCards(player, deckIndex);
                } else if (action.equals("load_decks")) {
                    sendDeckState(player);
                } else if (action.startsWith("switch_deck:")) {
                    String rawIndex = action.substring("switch_deck:".length());
                    try {
                        int targetDeck = Integer.parseInt(rawIndex);
                        List<String> decks = MongoManager.getDeckNames(player.getUniqueID());

                        if (targetDeck < 0 || targetDeck >= decks.size()) {
                            player.sendMessage(new TextComponentString("§cТакой колоды не существует."));
                            sendDeckState(player);
                            return;
                        }

                        MongoManager.setActiveDeck(player.getUniqueID(), targetDeck);
                        sendDeckState(player);
                        sendCards(player, targetDeck);
                        player.sendMessage(new TextComponentString(
                                "§aАктивная колода: §e" + decks.get(targetDeck)));
                    } catch (NumberFormatException e) {
                        player.sendMessage(new TextComponentString("§cНекорректный номер колоды."));
                    }
                }
            });
            return null;
        }

        private void buyCards(EntityPlayerMP player, int deckIndex, int requestedAmount, boolean buyAll) {
            List<String> decks = MongoManager.getDeckNames(player.getUniqueID());
            if (decks.isEmpty() || deckIndex < 0 || deckIndex >= decks.size()) {
                player.sendMessage(new TextComponentString("§cУ тебя нет доступных колод."));
                return;
            }

            if (deckIndex == 0) {
                buyEarthRunes(player, deckIndex, requestedAmount, buyAll);
                return;
            }

            int balance = MongoManager.getBalance(player.getUniqueID());
            List<Integer> freeSlots = MongoManager.getFreeSlots(player.getUniqueID(), deckIndex);

            if (freeSlots.isEmpty()) {
                player.sendMessage(new TextComponentString("§eВ этой legacy-коллекции больше нет свободных слотов."));
                NetworkHandler.INSTANCE.sendTo(new PongPacket(balance), player);
                return;
            }

            int affordable = balance / LEGACY_CARD_COST;
            if (affordable <= 0) {
                player.sendMessage(new TextComponentString(
                        "§cНедостаточно монет! Нужно минимум " + LEGACY_CARD_COST + ", у тебя " + balance));
                NetworkHandler.INSTANCE.sendTo(new PongPacket(balance), player);
                return;
            }

            int maxPossible = Math.min(affordable, freeSlots.size());
            int amountToBuy = buyAll ? maxPossible : Math.min(requestedAmount, maxPossible);

            if (amountToBuy <= 0) {
                NetworkHandler.INSTANCE.sendTo(new PongPacket(balance), player);
                return;
            }

            Collections.shuffle(freeSlots);

            for (int i = 0; i < amountToBuy; i++) {
                int slot = freeSlots.get(i);
                int cardIndex = RANDOM.nextInt(10);
                int layer = RuneInventory.NORMAL_RANK;
                int level = 1;

                MongoManager.addCardToDeck(
                        player.getUniqueID(), deckIndex, slot, cardIndex, layer, level);
                NetworkHandler.INSTANCE.sendTo(
                        new CardPacket(slot, cardIndex, layer, true), player);
            }

            finishLegacyPurchase(player, balance, requestedAmount, buyAll, amountToBuy, amountToBuy, 0);
        }

        private void buyEarthRunes(EntityPlayerMP player, int deckIndex, int requestedAmount, boolean buyAll) {
            int statusId = RuneEconomyConfig.STATUS_POISON;
            String purchaseResource = canonicalPurchaseResource(statusId);
            String purchaseDisplayName = ResourceManager.getDisplayName(purchaseResource);
            int unitCost = RuneEconomyConfig.purchaseUnitCost(statusId);
            int balance = ResourceManager.getPlayerResource(player.getUniqueID(), purchaseResource);
            int affordable = balance / unitCost;

            if (affordable <= 0) {
                player.sendMessage(new TextComponentString(
                        "§cНедостаточно ресурса «" + purchaseDisplayName + "»! Нужно минимум "
                                + unitCost + ", у тебя " + balance + "."));
                syncPurchaseBalance(player, purchaseResource, balance);
                return;
            }

            List<MongoManager.SavedCard> cards = MongoManager.getCardsInDeck(player.getUniqueID(), deckIndex);
            boolean[] owned = new boolean[RuneInventory.BOOST_TYPE_COUNT];
            int[] bestRanks = new int[RuneInventory.BOOST_TYPE_COUNT];
            int ownedCount = 0;

            for (MongoManager.SavedCard card : cards) {
                if (card.cardIndex < 0 || card.cardIndex >= RuneInventory.BOOST_TYPE_COUNT) continue;
                if (!owned[card.cardIndex]) {
                    owned[card.cardIndex] = true;
                    ownedCount++;
                }
                bestRanks[card.cardIndex] = Math.max(bestRanks[card.cardIndex], clampRank(card.layer));
            }

            if (ownedCount >= RuneInventory.BOOST_TYPE_COUNT) {
                player.sendMessage(new TextComponentString(
                        allAtLeast(bestRanks, RuneInventory.ADVANCED_RANK)
                                ? "§6Все 26 рун уже продвинутые. Достигнут максимальный ранг."
                                : "§eВсе 26 слотов заполнены. §6Пробудите подходящую руну."));
                syncPurchaseBalance(player, purchaseResource, balance);
                return;
            }

            int targetAttempts = buyAll ? affordable : Math.min(requestedAmount, affordable);
            if (targetAttempts <= 0) {
                syncPurchaseBalance(player, purchaseResource, balance);
                return;
            }

            int attempts = 0;
            int newRunes = 0;
            int duplicates = 0;

            for (int i = 0; i < targetAttempts && ownedCount < RuneInventory.BOOST_TYPE_COUNT; i++) {
                attempts++;
                int runeIndex = RANDOM.nextInt(RuneInventory.BOOST_TYPE_COUNT);
                if (owned[runeIndex]) {
                    duplicates++;
                    continue;
                }

                owned[runeIndex] = true;
                bestRanks[runeIndex] = RuneInventory.NORMAL_RANK;
                ownedCount++;
                newRunes++;
                MongoManager.addCardToDeck(
                        player.getUniqueID(), deckIndex,
                        runeIndex, runeIndex, RuneInventory.NORMAL_RANK, 1);
                NetworkHandler.INSTANCE.sendTo(
                        new CardPacket(runeIndex, runeIndex, RuneInventory.NORMAL_RANK, true), player);
            }

            String duplicateResource = RuneEconomyConfig.resourceForStatus(statusId);
            int duplicateCompensation = RuneEconomyConfig.duplicateCompensation(
                    statusId,
                    RuneInventory.NORMAL_RANK,
                    duplicates
            );
            if (duplicateCompensation > 0 && !duplicateResource.isEmpty()) {
                ResourceManager.addPlayerResource(
                        player.getUniqueID(), duplicateResource, duplicateCompensation);
            }

            finishRunePurchase(
                    player,
                    purchaseResource,
                    unitCost,
                    requestedAmount,
                    buyAll,
                    attempts,
                    newRunes,
                    duplicates
            );

            if (duplicateCompensation > 0) {
                player.sendMessage(new TextComponentString(
                        "§aКомпенсация за дубликаты: §e+" + duplicateCompensation + " §f"
                                + ResourceManager.getDisplayName(duplicateResource) + "§a."));
            }

            if (ownedCount >= RuneInventory.BOOST_TYPE_COUNT) {
                player.sendMessage(new TextComponentString(
                        allAtLeast(bestRanks, RuneInventory.ADVANCED_RANK)
                                ? "§6Все 26 рун уже продвинутые. Максимальный ранг достигнут."
                                : "§6Все 26 слотов заполнены. Пробудите подходящую руну."));
            }
        }

        private void finishLegacyPurchase(EntityPlayerMP player, int balance, int requestedAmount, boolean buyAll,
                                          int amountToBuy, int newRunes, int duplicates) {
            int spent = amountToBuy * LEGACY_CARD_COST;
            int newBalance = balance - spent;
            MongoManager.setBalance(player.getUniqueID(), newBalance);
            PlayerStatsService.sendTo(player);

            String duplicateText = duplicates > 0
                    ? " §7Дубликаты: §e" + duplicates + "§7."
                    : "";

            if (!buyAll && amountToBuy < requestedAmount) {
                player.sendMessage(new TextComponentString(
                        "§eКуплено " + amountToBuy + " из " + requestedAmount
                                + " попыток. Новых рун: " + newRunes
                                + ". Потрачено: " + spent + ". Остаток: " + newBalance + "." + duplicateText));
            } else {
                player.sendMessage(new TextComponentString(
                        "§aПокупок: " + amountToBuy + ". Новых рун: " + newRunes
                                + ". Потрачено: " + spent + ". Остаток: " + newBalance + "." + duplicateText));
            }
        }

        private void finishRunePurchase(EntityPlayerMP player, String purchaseResource, int unitCost,
                                        int requestedAmount, boolean buyAll,
                                        int amountToBuy, int newRunes, int duplicates) {
            int spent = safeMultiply(unitCost, amountToBuy);
            int newBalance = ResourceManager.addPlayerResource(
                    player.getUniqueID(), purchaseResource, -spent);
            PlayerStatsService.sendTo(player);
            syncPurchaseBalance(player, purchaseResource, newBalance);

            String resourceName = ResourceManager.getDisplayName(purchaseResource);
            String duplicateText = duplicates > 0
                    ? " §7Дубликаты: §e" + duplicates + "§7."
                    : "";

            if (!buyAll && amountToBuy < requestedAmount) {
                player.sendMessage(new TextComponentString(
                        "§eКуплено " + amountToBuy + " из " + requestedAmount
                                + " попыток. Новых рун: " + newRunes
                                + ". Потрачено: " + spent + " " + resourceName
                                + ". Остаток: " + newBalance + "." + duplicateText));
            } else {
                player.sendMessage(new TextComponentString(
                        "§aПокупок: " + amountToBuy + ". Новых рун: " + newRunes
                                + ". Потрачено: " + spent + " " + resourceName
                                + ". Остаток: " + newBalance + "." + duplicateText));
            }
        }

        private String canonicalPurchaseResource(int statusId) {
            String configured = RuneEconomyConfig.purchaseResourceForStatus(statusId);
            String canonical = ResourceManager.findCanonicalName(configured);
            return canonical == null ? "coins" : canonical;
        }

        private void syncPurchaseBalance(EntityPlayerMP player, String resource, int balance) {
            PlayerStatsService.sendTo(player);
            if ("coins".equals(ResourceManager.normalize(resource))) {
                NetworkHandler.INSTANCE.sendTo(new PongPacket(balance), player);
            }
        }

        private int safeMultiply(int left, int right) {
            long total = (long) left * right;
            return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
        }

        private void awakenRune(EntityPlayerMP player, int deckIndex, int runeIndex) {
            if (deckIndex != 0) {
                player.sendMessage(new TextComponentString("§cПробуждение пока доступно только первой стихии."));
                return;
            }
            if (runeIndex < 0 || runeIndex >= RuneInventory.BOOST_TYPE_COUNT) {
                player.sendMessage(new TextComponentString("§cНекорректная руна."));
                return;
            }

            List<MongoManager.SavedCard> cards = MongoManager.getCardsInDeck(player.getUniqueID(), deckIndex);
            boolean[] owned = new boolean[RuneInventory.BOOST_TYPE_COUNT];
            int[] bestRanks = new int[RuneInventory.BOOST_TYPE_COUNT];
            int ownedCount = 0;

            for (MongoManager.SavedCard card : cards) {
                if (card.cardIndex < 0 || card.cardIndex >= RuneInventory.BOOST_TYPE_COUNT) continue;
                if (!owned[card.cardIndex]) {
                    owned[card.cardIndex] = true;
                    ownedCount++;
                }
                bestRanks[card.cardIndex] = Math.max(bestRanks[card.cardIndex], clampRank(card.layer));
            }

            if (ownedCount < RuneInventory.BOOST_TYPE_COUNT) {
                player.sendMessage(new TextComponentString(
                        "§eСначала заполни все 26 слотов. Сейчас: " + ownedCount + " / 26."));
                return;
            }
            if (!owned[runeIndex]) {
                player.sendMessage(new TextComponentString("§cЭта руна ещё не получена."));
                return;
            }

            int currentRank = bestRanks[runeIndex];
            if (currentRank >= RuneInventory.ADVANCED_RANK) {
                player.sendMessage(new TextComponentString("§6Эта руна уже продвинутая."));
                return;
            }

            if (!allAtLeast(bestRanks, currentRank)) {
                player.sendMessage(new TextComponentString(
                        "§eЧтобы пробудить эту руну дальше, сначала доведите все 26 рун минимум до ранга «"
                                + rankName(currentRank) + "»."));
                return;
            }

            int newRank = currentRank + 1;

            MongoManager.clearDeck(player.getUniqueID(), deckIndex);

            for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
                if (rune == runeIndex) continue;
                if (bestRanks[rune] <= currentRank) continue;

                MongoManager.addCardToDeck(
                        player.getUniqueID(), deckIndex,
                        rune, rune, bestRanks[rune], 1);
            }

            MongoManager.addCardToDeck(
                    player.getUniqueID(), deckIndex,
                    runeIndex, runeIndex, newRank, 1);

            NetworkHandler.INSTANCE.sendTo(new CardPacket(-1, 0, 0, false), player);
            for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
                if (rune == runeIndex) continue;
                if (bestRanks[rune] <= currentRank) continue;
                NetworkHandler.INSTANCE.sendTo(
                        new CardPacket(rune, rune, bestRanks[rune], false), player);
            }
            NetworkHandler.INSTANCE.sendTo(new CardPacket(runeIndex, runeIndex, newRank, false), player);
            PlayerStatsService.sendTo(player);

            player.sendMessage(new TextComponentString(
                    "§aРуна пробуждена: §f" + rankName(currentRank) + " §7→ §f" + rankName(newRank)
                            + "§a. Руны ранга «" + rankName(currentRank)
                            + "» сброшены, более высокие ранги сохранены."));
        }

        private boolean allAtLeast(int[] bestRanks, int rank) {
            for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
                if (bestRanks[rune] < rank) return false;
            }
            return true;
        }

        private int clampRank(int rank) {
            if (rank <= RuneInventory.NORMAL_RANK) return RuneInventory.NORMAL_RANK;
            if (rank >= RuneInventory.ADVANCED_RANK) return RuneInventory.ADVANCED_RANK;
            return rank;
        }

        private String rankName(int rank) {
            if (rank == RuneInventory.IMPROVED_RANK) return "Улучшенная";
            if (rank == RuneInventory.ADVANCED_RANK) return "Продвинутая";
            return "Обычная";
        }

        private void sendDeckState(EntityPlayerMP player) {
            List<String> decks = MongoManager.getDeckNames(player.getUniqueID());
            int activeDeck = MongoManager.getActiveDeck(player.getUniqueID());
            NetworkHandler.INSTANCE.sendTo(new DeckPacket(decks, activeDeck), player);
        }

        private void sendCards(EntityPlayerMP player, int deckIndex) {
            NetworkHandler.INSTANCE.sendTo(new CardPacket(-1, 0, 0, false), player);

            List<MongoManager.SavedCard> cards = MongoManager.getCardsInDeck(player.getUniqueID(), deckIndex);
            CustomGuiMod.logger.info("Sending cards to client from deck " + deckIndex + ": " + cards.size());

            for (MongoManager.SavedCard card : cards) {
                NetworkHandler.INSTANCE.sendTo(
                        new CardPacket(card.slot, card.cardIndex, card.layer, false), player);
            }
        }
    }
}
