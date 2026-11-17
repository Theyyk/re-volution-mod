"""Negative tests prove the dependency checks reject accidental boundary violations."""
import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('architecture', Path(__file__).with_name('check-architecture.py'))
architecture = importlib.util.module_from_spec(spec)
spec.loader.exec_module(architecture)


class ArchitectureGuardTest(unittest.TestCase):
    def test_invalid_dependencies_are_rejected(self):
        cases = [
            ('client/Screen.java', 'com.example.revolution.persistence.MongoPlayerRepository'),
            ('rune/definition/Definition.java', 'com.example.revolution.rune.state.RuneInventory'),
            ('rune/rules/Rules.java', 'net.minecraft.world.World'),
            ('player/repository/PlayerRepository.java', 'org.bson.Document'),
            ('player/service/PlayerService.java', 'com.example.revolution.lifecycle.ServerLifecycle'),
            ('mob/service/MobService.java', 'net.minecraftforge.event.entity.living.LivingHurtEvent'),
            ('rune/state/RuneState.java', 'com.example.revolution.player.repository.PlayerRepository'),
            ('network/ActionPacket.java', 'com.example.revolution.rune.service.RuneActionService'),
            ('commands/AdminCommand.java', 'com.example.revolution.player.repository.PlayerRepository'),
            ('player/PlayerJoinHandler.java', 'net.minecraft.client.Minecraft'),
        ]
        for path, dependency in cases:
            with self.subTest(path=path, dependency=dependency):
                self.assertTrue(architecture.violations(path, f'import {dependency};\nclass Example {{}}'))

    def test_permitted_boundaries(self):
        self.assertFalse(architecture.violations('rune/service/RuneService.java',
                         'import com.example.revolution.player.repository.PlayerRepository;\nclass Example {}'))
        self.assertFalse(architecture.violations('rune/state/RuneState.java',
                         'import com.example.revolution.rune.rules.RuneProgression;\nclass Example {}'))

    def test_mutable_static_state_and_raw_sends_are_rejected(self):
        self.assertTrue(architecture.violations('rune/service/RuneService.java',
                        'class Example {\n    private static Map<String, Long> cache = new HashMap<>();\n}'))
        self.assertTrue(architecture.violations('player/service/SyncService.java',
                        'class Example { void send() { NetworkHandler.INSTANCE.sendTo(packet, player); } }'))

    def test_root_is_bootstrap_only(self):
        self.assertTrue(architecture.violations('RandomManager.java', 'class RandomManager {}'))
        self.assertFalse(architecture.violations('RevolutionMod.java', 'class RevolutionMod {}'))

    def test_mastery_refresh_does_not_move_exchange_header(self):
        # Regression: a former buttonList[-1] lookup selected the Exchange
        # button instead of Refresh and moved Exchange into the footer.
        root = Path(__file__).resolve().parents[2]
        screen = (root / 'src/main/java/com/example/revolution/client/MasteryGuiScreen.java').read_text(
            encoding='utf-8')
        layout = screen.split('public void refreshButtons()', 1)[1].split(
            'private static boolean contains(', 1)[0]
        self.assertIn('GuiButton refresh = new GuiButton(20,', layout)
        self.assertIn('buttonList.add(refresh);', layout)
        self.assertIn('GuiButton exchange = new GuiControls.PanelButton(30,', layout)
        self.assertIn('buttonList.add(exchange);', layout)
        self.assertLess(layout.index('buttonList.add(refresh);'),
                        layout.index('GuiButton exchange ='))
        self.assertNotIn('buttonList.get(buttonList.size() - 1)', layout)

    def test_resource_text_is_portable_and_binary_stays_exact(self):
        self.assertEqual(architecture.resource_digest('moss_turtle.palette.txt', b'palette\r\n'),
                         architecture.resource_digest('moss_turtle.palette.txt', b'palette\n'))
        self.assertEqual(architecture.resource_digest('pack.mcmeta', b'{}\r\n'),
                         architecture.resource_digest('pack.mcmeta', b'{}\n'))
        self.assertNotEqual(architecture.resource_digest('font.otf', b'font\r\n'),
                            architecture.resource_digest('font.otf', b'font\n'))


if __name__ == '__main__':
    unittest.main()
