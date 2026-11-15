package com.example.revolution.client;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Source-level regression guard; visual shader appearance still needs Minecraft QA. */
public final class QuestAuraBlurLifecycleContractTest {
    private static String source(String screen) throws Exception {
        return new String(Files.readAllBytes(Paths.get(
                "src/main/java/com/example/revolution/client/" + screen + ".java")),
                StandardCharsets.UTF_8);
    }

    @Test void allQuestAuraScreensOwnAndReleaseBlur() throws Exception {
        for (String screen : new String[] {"QuestGuiScreen", "AuraGuiScreen", "AuraCraftGuiScreen"}) {
            String source = source(screen);
            assertTrue(source.contains("new GuiBackgroundBlur()"), screen);
            assertTrue(source.contains("backgroundBlur.enable(mc);"), screen);
            assertTrue(source.contains("void onGuiClosed()"), screen);
            assertTrue(source.contains("backgroundBlur.disable(mc);"), screen);
            assertTrue(source.contains("super.onGuiClosed();"), screen);
            assertTrue(source.indexOf("backgroundBlur.enable(mc);") <
                       source.indexOf("void onGuiClosed()"), screen);
        }
    }

    @Test void parentCraftTransitionsRetainExistingNavigation() throws Exception {
        String aura = source("AuraGuiScreen");
        String craft = source("AuraCraftGuiScreen");
        assertTrue(aura.contains("mc.displayGuiScreen(new AuraCraftGuiScreen(this))"));
        assertTrue(craft.contains("mc.displayGuiScreen(parent)"));
        assertTrue(craft.contains("parent.drawScreen(-1000, -1000, partialTicks)"));
    }
}
