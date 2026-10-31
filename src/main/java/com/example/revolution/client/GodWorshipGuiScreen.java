package com.example.revolution.client;

import com.example.revolution.client.network.GodWorshipClientState;
import com.example.revolution.network.GodWorshipActionPacket;
import com.example.revolution.network.GodWorshipSnapshotPacket;
import com.example.revolution.network.NetworkHandler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/** Functional v2.8.0 God Worship UI. Visual polish is intentionally deferred. */
public final class GodWorshipGuiScreen extends GuiScreen {
    private static long nextNonce = Math.max(1L, System.currentTimeMillis());
    private final List<GodWorshipSnapshotPacket.Entry> entries;
    private final String offerings;
    private final long permanentDurationLevel;
    private final long permanentDurationBonusSeconds;
    private final String durationUpgradeCost;
    private final boolean autoActivationEnabled;
    private final boolean durationUpgradeEnabled;
    private final boolean secondaryBuffsEnabled;
    private boolean suppressCloseSignal;

    public GodWorshipGuiScreen(List<GodWorshipSnapshotPacket.Entry> entries, String offerings,
                               long permanentDurationLevel, long permanentDurationBonusSeconds,
                               String durationUpgradeCost, boolean autoActivationEnabled,
                               boolean durationUpgradeEnabled, boolean secondaryBuffsEnabled) {
        this.entries = entries == null ? new ArrayList<GodWorshipSnapshotPacket.Entry>() : new ArrayList<>(entries);
        this.offerings = offerings == null ? "0" : offerings;
        this.permanentDurationLevel = Math.max(0L, permanentDurationLevel);
        this.permanentDurationBonusSeconds = Math.max(0L, permanentDurationBonusSeconds);
        this.durationUpgradeCost = durationUpgradeCost == null ? "0" : durationUpgradeCost;
        this.autoActivationEnabled = autoActivationEnabled;
        this.durationUpgradeEnabled = durationUpgradeEnabled;
        this.secondaryBuffsEnabled = secondaryBuffsEnabled;
    }

    public void suppressCloseSignal() { suppressCloseSignal = true; }

    @Override public void initGui() {
        buttonList.clear();
        int left = width / 2 - 215;
        int top = height / 2 - 142;
        for (int i = 0; i < entries.size() && i < 5; i++) {
            GodWorshipSnapshotPacket.Entry e = entries.get(i);
            int y = top + 38 + i * 44;
            int base = 100 + i * 10;
            GuiButton action = new GuiButton(base, left + 220, y, 62, 18,
                    e.unlocked ? ("BUFF_ACTIVE".equals(e.phase) ? "Active" : "Worship") : "Unlock");
            action.enabled = !"BUFF_ACTIVE".equals(e.phase);
            buttonList.add(action);
            GuiButton upgrade = new GuiButton(base + 1, left + 285, y, 62, 18, "Upgrade");
            upgrade.enabled = e.unlocked;
            buttonList.add(upgrade);
            GuiButton auto = new GuiButton(base + 2, left + 350, y, 78, 18, e.autoWorshipEnabled ? "Auto: ON" : "Auto: OFF");
            auto.enabled = e.unlocked;
            buttonList.add(auto);
            GuiButton mode = new GuiButton(base + 3, left + 220, y + 20, 128, 18, "Mode: " + shortMode(e.activationMode));
            mode.enabled = e.unlocked && autoActivationEnabled;
            buttonList.add(mode);
            GuiButton link = new GuiButton(base + 4, left + 350, y + 20, 78, 18,
                    "LINKED_TO_GOD".equals(e.activationMode) ? shortGod(e.linkedGodId) : "Link: -");
            link.enabled = e.unlocked && autoActivationEnabled && "LINKED_TO_GOD".equals(e.activationMode);
            buttonList.add(link);
        }
        GuiButton duration = new GuiButton(10, left, top + 266, 220, 20,
                durationUpgradeEnabled
                        ? "Duration +1 level (" + durationUpgradeCost + " Offerings)"
                        : "Permanent Duration: LOCKED");
        duration.enabled = durationUpgradeEnabled;
        buttonList.add(duration);
        buttonList.add(new GuiButton(11, left + 224, top + 266, 100, 20, "Refresh"));
        buttonList.add(new GuiButton(12, left + 328, top + 266, 100, 20, "Close"));
    }

    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 10) { send("duration_upgrade", "", "", ""); return; }
        if (button.id == 11) { NetworkHandler.INSTANCE.sendToServer(new GodWorshipActionPacket("sync", "", "", "", 0L)); return; }
        if (button.id == 12) { mc.displayGuiScreen(null); return; }
        if (button.id < 100) return;
        int index = (button.id - 100) / 10;
        int action = (button.id - 100) % 10;
        if (index < 0 || index >= entries.size()) return;
        GodWorshipSnapshotPacket.Entry e = entries.get(index);
        if (action == 0) send(e.unlocked ? "click" : "unlock", e.godId, "", "");
        else if (action == 1) send("upgrade", e.godId, "", "");
        else if (action == 2) send("auto_worship", e.godId, Boolean.toString(!e.autoWorshipEnabled), "");
        else if (action == 3) {
            String next = nextMode(e.activationMode);
            String linked = "LINKED_TO_GOD".equals(next) ? defaultLinked(e.godId) : "";
            send("activation_mode", e.godId, next, linked);
        } else if (action == 4 && "LINKED_TO_GOD".equals(e.activationMode)) {
            send("activation_mode", e.godId, e.activationMode, nextLinked(e.godId, e.linkedGodId));
        }
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int left = width / 2 - 215;
        int top = height / 2 - 142;
        drawCenteredString(fontRenderer, "God Worship", width / 2, top, 0xFFFFFF);
        drawString(fontRenderer, "Offerings: " + offerings + "   Duration Lv: " + permanentDurationLevel
                + " ( +" + permanentDurationBonusSeconds + "s )", left, top + 14, 0xAAAAAA);
        drawString(fontRenderer, "Auto Activation: " + (autoActivationEnabled ? "UNLOCKED" : "LOCKED")
                + "   Secondary Buffs: " + (secondaryBuffsEnabled ? "UNLOCKED" : "LOCKED"), left, top + 25, 0x777777);
        long now = System.currentTimeMillis();
        for (int i = 0; i < entries.size() && i < 5; i++) {
            GodWorshipSnapshotPacket.Entry e = entries.get(i);
            int y = top + 38 + i * 44;
            drawRect(left, y - 2, left + 428, y + 40, 0x88000000);
            drawString(fontRenderer, shortGod(e.godId) + "  Lv " + e.level + "  [" + e.phase + "]  x" + e.currentMultiplier,
                    left + 6, y + 3, 0xFFFFFF);
            String channels = shortChannel(e.primaryChannel);
            if (!e.secondaryChannel.isEmpty()) channels += " + " + shortChannel(e.secondaryChannel)
                    + (secondaryBuffsEnabled ? "" : " (locked)");
            drawString(fontRenderer, "Channels: " + channels, left + 6, y + 13, 0x55FFFF);
            if (!e.unlocked) {
                drawString(fontRenderer, "Unlock: " + e.unlockCost + " " + e.resource, left + 6, y + 25, 0xFFFF55);
            } else {
                String progress = "Charge: " + e.chargeProgress + "/" + e.chargeRequired;
                if ("BUFF_ACTIVE".equals(e.phase)) progress = "Buff left: " + secondsLeft(e.buffActiveUntil, now) + "s";
                else if ("COOLDOWN".equals(e.phase)) progress = "Cooldown: " + secondsLeft(e.cooldownUntil, now) + "s";
                String linked = "LINKED_TO_GOD".equals(e.activationMode) ? "   -> " + shortGod(e.linkedGodId) : "";
                drawString(fontRenderer, progress + "   Upgrade: " + e.upgradeCost + " " + e.resource + linked,
                        left + 6, y + 25, 0xAAAAAA);
            }
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override public void onGuiClosed() {
        if (!suppressCloseSignal) {
            GodWorshipClientState.requestClose();
            if (mc != null && mc.getConnection() != null) {
                NetworkHandler.INSTANCE.sendToServer(new GodWorshipActionPacket("menu_closed", "", "", "", 0L));
            }
        }
        super.onGuiClosed();
    }

    @Override public boolean doesGuiPauseGame() { return false; }

    private void send(String action, String godId, String value, String linked) {
        NetworkHandler.INSTANCE.sendToServer(new GodWorshipActionPacket(action, godId, value, linked, nonce()));
    }

    private static synchronized long nonce() {
        if (nextNonce == Long.MAX_VALUE) nextNonce = 1L;
        return ++nextNonce;
    }

    private static String nextMode(String mode) {
        if ("MANUAL".equals(mode)) return "AUTO_WHEN_READY";
        if ("AUTO_WHEN_READY".equals(mode)) return "LINKED_TO_GOD";
        return "MANUAL";
    }

    private static String defaultLinked(String owner) { return nextLinked(owner, ""); }

    private static String nextLinked(String owner, String current) {
        String[] gods = {"GOD_1", "GOD_2", "GOD_3", "GOD_4", "GOD_5"};
        int start = -1;
        for (int i = 0; i < gods.length; i++) if (gods[i].equals(current)) start = i;
        for (int step = 1; step <= gods.length; step++) {
            String candidate = gods[(start + step + gods.length) % gods.length];
            if (!candidate.equals(owner)) return candidate;
        }
        return "";
    }

    private static String shortMode(String mode) {
        if ("AUTO_WHEN_READY".equals(mode)) return "AUTO";
        if ("LINKED_TO_GOD".equals(mode)) return "LINK";
        return "MANUAL";
    }

    private static String shortGod(String godId) {
        if (godId == null || godId.isEmpty()) return "-";
        return godId.replace("GOD_", "God ");
    }

    private static String shortChannel(String channel) {
        if (channel == null || channel.isEmpty()) return "-";
        return channel.replace("_DAMAGE_MULTIPLIER", "").replace("_MULTIPLIER", "");
    }

    private static long secondsLeft(long until, long now) {
        return until <= now ? 0L : (until - now + 999L) / 1000L;
    }
}
