# v1.3.0 Project Cleanup & Terminology Migration — first structural PR

## Remote baseline (2026-10-18)

- main: `696777932b075bfc20cbf17ccfbd570770939a9f` (published v1.2.11).
- develop: `efa84b3a11c0e0a8a56c44a6b2a2a4b252de387d`; one merge commit behind main, identical file tree.
- tmp-noop-ignore: `8073d7ae36822b31b896888fa1e859507eba394f`; ancestor of develop, 106 commits behind, zero unique commits.
- 90 main Java files, 16 test Java files. No application JARs in Java source directories; the Gradle wrapper JAR is required.
- PR targets develop and preserves its ancestry. No history rewriting, tag changes or release deletion.

## Scope and package boundaries

Apply in order: terminology, independent package extraction, documentation/archive, verification.
CardPacket describes one physical rune slot; DeckPacket describes collection names and the selected collection, not a status definition.
PrimaryRuneCatalog is the first built-in collection, including click and poison effects.

This first PR extracts independent admin access/transport, commands, generic network transport, numeric presentation/parsing, resource persistence, Mongo storage, managed mobs and HUD rendering.
The coupled rune rules, rune screens, shared GUI helpers and integration handlers remain together in the root package for this PR. They are deliberately NOT all made public. A later rune/client split needs a designed view/action API; this is a staged package migration, not the final package layout.
AdminPanelStateClientHandler stays beside AdminPanelScreen.applyState (package-private).
SettingsScreen/SettingsKeyHandler stay with the number feature because they edit its private mode/settings model.
RuneSlotPacket's client handler stays with the private rune client state; its transport DTO and no-op server handler can move independently.

Three small delegation APIs form the extracted boundaries: `number.LargeNumbers` (format/parse/normalization/settings init), root `RuneStorageRules` (existing rank limits/free-slot policy), root `RuneMobEffects` (existing poison resolution/reward policy). Existing package-private classes retain visibility. One existing read-only query, MobManager.isApplyingPoisonDamage, becomes public for DamageBonusHandler across the mob boundary; its state stays private. No formulas are copied into new APIs.
Tests move with the implementation they inspect; all 16 remain. Three test-only bridges preserve cross-package assertions without production visibility changes. A new RuneTransportContractTest pins slot and collection bytes. RuneProgressionTest and RuneAwakeningInputTest are invoked from RuneInventoryTest and GuiLayoutTest respectively.

## Compatibility contracts (retained verbatim)

- Mongo database selection, collections, keys including `decks`, `cards`, `cardIndex`, `activeDeck`, `runeEnhancements`, stored names and old slot capacity.
- Channel `customgui:main`; discriminator IDs 0–7, sides, message registration order, byte order, field types, command payload strings and admin tab-separated records.
- Namespace/mod id `customguimod`, all PNG/font/shader resource paths, configuration keys including `customguimod.runes.deck.<index>.status` and existing system property names.
- GUI strings, fallback catalog contents, geometry, input, timings, combat, economy, reward and progression behavior.
- Existing version metadata remains baseline v1.2.11; this PR does not publish v1.3.0. The existing CustomGuiMod.VERSION=1.2.10 discrepancy is recorded and left out of the structural change.

Internal Java identifiers using Card/Cards/Deck are renamed by role (rune, collection). String literals are preserved. Historical releases/reports keep historical terminology. Old screenshots are moved using their original blob SHA to docs/archive/screenshots; no fresh runtime screenshots are invented.

## Exact class map

`main/` and `test/` below refer to the original flat Java package. An unchanged root destination is intentional for this first PR.

| Current class | Responsibility | First PR destination |
| --- | --- | --- |
| `main/AdminOnlyCommand` | All player-issued mod commands require the dedicated Admin Panel whitelist. | `com.example.customguimod.commands.AdminOnlyCommand` |
| `main/AdminPanelAccess` | Separate whitelist for the in-game admin/test panel and mod commands. | `com.example.customguimod.admin.AdminPanelAccess` |
| `main/AdminPanelCommand` | Server command: AdminPanel | `com.example.customguimod.commands.AdminPanelCommand` |
| `main/AdminPanelKeyHandler` | AdminPanelKeyHandler | `com.example.customguimod.admin.AdminPanelKeyHandler` |
| `main/AdminPanelRequestClientHandler` | Client-side registration placeholder: AdminPanelRequestPacket only travels to the server. | `com.example.customguimod.admin.AdminPanelRequestClientHandler` |
| `main/AdminPanelRequestPacket` | Transport / handler: AdminPanelRequestPacket | `com.example.customguimod.AdminPanelRequestPacket` |
| `main/AdminPanelScreen` | Client screen: AdminPanelScreen | `com.example.customguimod.AdminPanelScreen` |
| `main/AdminPanelStateClientHandler` | Apply admin snapshot through package-private screen refresh | `com.example.customguimod.AdminPanelStateClientHandler` |
| `main/AdminPanelStatePacket` | Transport / handler: AdminPanelStatePacket | `com.example.customguimod.admin.AdminPanelStatePacket` |
| `main/AdminPanelStateServerHandler` | Server-side registration placeholder: AdminPanelStatePacket only travels to the client. | `com.example.customguimod.admin.AdminPanelStateServerHandler` |
| `main/BalancePacket` | Transport / handler: BalancePacket | `com.example.customguimod.network.BalancePacket` |
| `main/CardPacket` | One rune slot: index/rank and purchase-feedback flag | `com.example.customguimod.network.RuneSlotPacket` |
| `main/CardPacketHandler` | Client slot update, private preload/rank state and purchase pulse | `com.example.customguimod.RuneSlotPacketHandler` |
| `main/CardPacketServerHandler` | Dedicated-server no-op for clientbound slot updates | `com.example.customguimod.network.RuneSlotPacketServerHandler` |
| `main/CardsGuiScreen` | Rune screen lifecycle, layout, search, input and collection display | `com.example.customguimod.RunesGuiScreen` |
| `main/CardSyncState` | Client collection synchronization settling state | `com.example.customguimod.RuneCollectionSyncState` |
| `main/ClientPacketHandler` | Compatibility custom-channel balance listener | `com.example.customguimod.ClientPacketHandler` |
| `main/ClientPlayerStats` | Client copy of server-authored balances, costs and status | `com.example.customguimod.ClientPlayerStats` |
| `main/ClientRuneEnhancements` | ClientRuneEnhancements | `com.example.customguimod.ClientRuneEnhancements` |
| `main/ClientRuneRanks` | ClientRuneRanks | `com.example.customguimod.ClientRuneRanks` |
| `main/CustomGuiMod` | Forge bootstrap, lifecycle, event and command registration | `com.example.customguimod.CustomGuiMod` |
| `main/CustomMobCommand` | Server command: CustomMob | `com.example.customguimod.commands.CustomMobCommand` |
| `main/CustomResourceCommand` | Server command: CustomResource | `com.example.customguimod.commands.CustomResourceCommand` |
| `main/CustomRunesCommand` | Server command: CustomRunes | `com.example.customguimod.CustomRunesCommand` |
| `main/CustomStatusCommand` | Server command: CustomStatus | `com.example.customguimod.CustomStatusCommand` |
| `main/DamageBonusHandler` | Server damage listener; prevents click effects on poison damage | `com.example.customguimod.DamageBonusHandler` |
| `main/DeckPacket` | Collection names and active collection index | `com.example.customguimod.RuneCollectionsPacket` |
| `main/DeckPacketHandler` | Apply collection list to preloader and screen | `com.example.customguimod.RuneCollectionsPacketHandler` |
| `main/DeckPacketServerHandler` | Dedicated-server no-op for clientbound collection lists | `com.example.customguimod.RuneCollectionsPacketServerHandler` |
| `main/EarthRuneCatalog` | Fixed first collection: 26 click/poison/reward rune definitions | `com.example.customguimod.PrimaryRuneCatalog` |
| `main/FlyingCard` | Unreachable old flight path; both current callers pass false. Purchase/enhancement feedback is a slot pulse. | Removed with its unused screen path; RuneUxUiHandler pulse unchanged. |
| `main/GuiControls` | GUI button/label controls using private rendering helpers | `com.example.customguimod.GuiControls` |
| `main/GuiDrawing` | GUI surfaces and borders | `com.example.customguimod.GuiDrawing` |
| `main/GuiFontRenderer` | Local GUI font rendering and cached font loading | `com.example.customguimod.GuiFontRenderer` |
| `main/GuiLayout` | GUI canvas geometry, slot layout and coordinate mapping | `com.example.customguimod.GuiLayout` |
| `main/GuiTheme` | Shared GUI colors and presentation constants | `com.example.customguimod.GuiTheme` |
| `main/HealthBarRenderer` | Client managed-mob health/reward display | `com.example.customguimod.client.hud.HealthBarRenderer` |
| `main/KeyInputHandler` | Open preloaded rune screen from the client key binding | `com.example.customguimod.KeyInputHandler` |
| `main/LargeNumberDisplayMode` | Client-selectable presentation mode for large numeric values. | `com.example.customguimod.number.LargeNumberDisplayMode` |
| `main/LargeNumberDisplaySettings` | Local client preference for large-number presentation. | `com.example.customguimod.number.LargeNumberDisplaySettings` |
| `main/LargeNumberFormatter` | Shared presentation-only formatter for large numeric values. | `com.example.customguimod.number.LargeNumberFormatter` |
| `main/LargeNumberValue` | Shared arbitrary-precision value helpers for compact large-number input. | `com.example.customguimod.number.LargeNumberValue` |
| `main/MobDeathHandler` | MobDeathHandler | `com.example.customguimod.mob.MobDeathHandler` |
| `main/MobManager` | Server managed-mob storage snapshot, spawn/chunk lifecycle, virtual HP and rewards | `com.example.customguimod.mob.MobManager` |
| `main/MobRewardBonus` | Pure mob-reward policy for rune reward stats. Only custom-mob drops use this policy; compensation/admin grants do not. | `com.example.customguimod.MobRewardBonus` |
| `main/MongoManager` | Mongo player collections and managed-mob storage adapter | `com.example.customguimod.persistence.MongoManager` |
| `main/NameHider` | Client suppression of default managed-mob name rendering | `com.example.customguimod.client.hud.NameHider` |
| `main/NetworkHandler` | Stable channel, discriminator/side registration and client capability guard | `com.example.customguimod.network.NetworkHandler` |
| `main/PingPacket` | Existing client action command transport and server dispatch | `com.example.customguimod.PingPacket` |
| `main/PingPacketClientHandler` | Transport / handler: PingPacketClientHandler | `com.example.customguimod.network.PingPacketClientHandler` |
| `main/PlayerJoinHandler` | Initialize player collection, retired-resource cleanup and stats sync | `com.example.customguimod.PlayerJoinHandler` |
| `main/PlayerStatsHudRenderer` | Client balance/damage HUD | `com.example.customguimod.client.hud.PlayerStatsHudRenderer` |
| `main/PlayerStatsPacket` | Transport / handler: PlayerStatsPacket | `com.example.customguimod.network.PlayerStatsPacket` |
| `main/PlayerStatsPacketHandler` | Transport / handler: PlayerStatsPacketHandler | `com.example.customguimod.PlayerStatsPacketHandler` |
| `main/PlayerStatsPacketServerHandler` | Transport / handler: PlayerStatsPacketServerHandler | `com.example.customguimod.network.PlayerStatsPacketServerHandler` |
| `main/PlayerStatsService` | Server rune-derived stats and resource/cost snapshot delivery | `com.example.customguimod.PlayerStatsService` |
| `main/PongPacket` | Transport / handler: PongPacket | `com.example.customguimod.network.PongPacket` |
| `main/PongPacketHandler` | Transport / handler: PongPacketHandler | `com.example.customguimod.network.PongPacketHandler` |
| `main/PongPacketServerHandler` | Transport / handler: PongPacketServerHandler | `com.example.customguimod.network.PongPacketServerHandler` |
| `main/ResourceManager` | Fixed resource catalog, exact balances and payment/grant persistence | `com.example.customguimod.resource.ResourceManager` |
| `main/RuneAwakeningDecision` | Server/client-neutral result of checking whether a rune may be awakened. | `com.example.customguimod.RuneAwakeningDecision` |
| `main/RuneAwakeningInput` | Resolves awakening input and rune selection using the rendered layout. | `com.example.customguimod.RuneAwakeningInput` |
| `main/RuneAwakeningPaymentService` | RuneAwakeningPaymentService | `com.example.customguimod.RuneAwakeningPaymentService` |
| `main/RuneCombatMath` | Pure combat calculation for the v1.2.10 rune damage model. | `com.example.customguimod.RuneCombatMath` |
| `main/RuneCombatService` | Server-authoritative resolver for rune damage, proc and temporary rage effects. | `com.example.customguimod.RuneCombatService` |
| `main/RuneCompositeEffect` | Structured rune effects that cannot be represented by a single scalar stat. Tooltip lines are intentionally compact and may span multiple rows. | `com.example.customguimod.RuneCompositeEffect` |
| `main/RuneEconomyConfig` | Central economy rules for rune statuses. Gameplay code should read values here instead of hard-coding them. | `com.example.customguimod.RuneEconomyConfig` |
| `main/RuneEnhancementManager` | Persistent v1.2.9 slot enhancement storage. Enhancement belongs to a fixed rune slot inside a status/deck and intentionally survives card resets. | `com.example.customguimod.RuneEnhancementManager` |
| `main/RuneEnhancementPacket` | Transport / handler: RuneEnhancementPacket | `com.example.customguimod.RuneEnhancementPacket` |
| `main/RuneEnhancementPacketHandler` | Transport / handler: RuneEnhancementPacketHandler | `com.example.customguimod.RuneEnhancementPacketHandler` |
| `main/RuneEnhancementPacketServerHandler` | Transport / handler: RuneEnhancementPacketServerHandler | `com.example.customguimod.RuneEnhancementPacketServerHandler` |
| `main/RuneGuiPreloader` | Client preloaded collection/slot/stat snapshot | `com.example.customguimod.RuneGuiPreloader` |
| `main/RuneInventory` | Keeps server records intact and groups them into the visible rune catalog. | `com.example.customguimod.RuneInventory` |
| `main/RuneProgression` | Central rank model for rune progression and awakening rules. | `com.example.customguimod.RuneProgression` |
| `main/RunePurchaseUiHandler` | RunePurchaseUiHandler | `com.example.customguimod.RunePurchaseUiHandler` |
| `main/RuneResourcesCardsGuiScreen` | Resource balances view sharing the rune screen layout | `com.example.customguimod.RuneResourcesGuiScreen` |
| `main/RuneRewardBonusService` | Resolves server-authoritative rune reward stats for the player's active rune deck. | `com.example.customguimod.RuneRewardBonusService` |
| `main/RuneStat` | Data-driven rune stat definition plus a calculation breakdown prepared for future detailed tooltips. v1.2.9 renders only finalValue. | `com.example.customguimod.RuneStat` |
| `main/RuneStatDisplay` | RuneStatDisplay | `com.example.customguimod.RuneStatDisplay` |
| `main/RuneStatType` | RuneStatType | `com.example.customguimod.RuneStatType` |
| `main/RuneStatusChatFilter` | RuneStatusChatFilter | `com.example.customguimod.RuneStatusChatFilter` |
| `main/RuneStatusRegistry` | RuneStatusRegistry | `com.example.customguimod.RuneStatusRegistry` |
| `main/RuneTargetedPower` | RuneTargetedPower | `com.example.customguimod.RuneTargetedPower` |
| `main/RuneTexturePreloader` | Client resource reload and rune texture warm-up | `com.example.customguimod.RuneTexturePreloader` |
| `main/RuneTooltipContent` | RuneTooltipContent | `com.example.customguimod.RuneTooltipContent` |
| `main/RuneTooltipRenderer` | Draws rune tooltips as a compact framed card instead of the vanilla tooltip box. | `com.example.customguimod.RuneTooltipRenderer` |
| `main/RuneUxUiHandler` | RuneUxUiHandler | `com.example.customguimod.RuneUxUiHandler` |
| `main/ServerInit` | Existing server lifecycle initialization | `com.example.customguimod.ServerInit` |
| `main/SettingsKeyHandler` | Open numeric display settings from the client key binding | `com.example.customguimod.number.SettingsKeyHandler` |
| `main/SettingsScreen` | Numeric display-mode selection UI | `com.example.customguimod.number.SettingsScreen` |
| `test/EarthRuneCatalogTest` | Regression coverage: EarthRuneCatalog | `com.example.customguimod.PrimaryRuneCatalogTest` |
| `test/GuiLayoutTest` | Frozen v1.1.5 viewport fixtures and click/slot alignment without Minecraft or OpenGL. | `com.example.customguimod.GuiLayoutTest` |
| `test/LargeNumberFormatterTest` | Regression coverage: LargeNumberFormatter | `com.example.customguimod.number.LargeNumberFormatterTest` |
| `test/MobRewardBonusTest` | Regression coverage: MobRewardBonus | `com.example.customguimod.MobRewardBonusTest` |
| `test/MobVirtualHealthTest` | Regression coverage: MobVirtualHealth | `com.example.customguimod.mob.MobVirtualHealthTest` |
| `test/MultiStatusRuneEconomyTest` | Regression checks proving economy/progression inputs are isolated per rune status. | `com.example.customguimod.MultiStatusRuneEconomyTest` |
| `test/ResourceActionFoundationTest` | Regression coverage: ResourceActionFoundation | `com.example.customguimod.resource.ResourceActionFoundationTest` |
| `test/RuneAwakeningInputTest` | Exercises the selector and left-click gate used by the Forge awakening handler. | `com.example.customguimod.RuneAwakeningInputTest` |
| `test/RuneCombatMathTest` | Regression coverage: RuneCombatMath | `com.example.customguimod.RuneCombatMathTest` |
| `test/RuneEconomyConfigTest` | Pure Java regression checks for configurable rune economy rules. | `com.example.customguimod.RuneEconomyConfigTest` |
| `test/RuneEnhancementPacketTest` | Regression coverage: RuneEnhancementPacket | `com.example.customguimod.RuneEnhancementPacketTest` |
| `test/RuneInventoryTest` | Exercises the fixed 26-rune catalog and rank states without Minecraft or MongoDB. | `com.example.customguimod.RuneInventoryTest` |
| `test/RuneProgressionTest` | Regression checks for the shared rune progression rules. | `com.example.customguimod.RuneProgressionTest` |
| `test/RuneStatContentTest` | Regression coverage: RuneStatContent | `com.example.customguimod.RuneStatContentTest` |
| `test/RuneStatusRegistryTest` | Pure Java regression checks for multi-status deck routing. | `com.example.customguimod.RuneStatusRegistryTest` |
| `test/RuneTooltipContentTest` | Regression coverage: RuneTooltipContent | `com.example.customguimod.RuneTooltipContentTest` |

## Verification and remaining scope

- Local JDK 8: `clean build` passed, including all original regression entry points and `runeTransportContractRegression`.
- Source check: current Java identifiers, package paths and every Gradle test mainClass resolve correctly.
- One-off baseline comparison: every string/character literal in 24 schema/config/protocol/catalog/UI/number classes is unchanged; all 16 client/server packet registrations retain IDs, sides and order.
- All original test assertion messages remain; 16 original tests plus one new wire test and three test-only bridge files (20 test source files total). Production: 90 baseline classes minus unused flight state plus three narrow APIs = 92.
- Local reobfuscated JAR: required production classes present, all source resources byte-identical, no old class names or test classes. Existing workflow now repeats source and JAR checks before artifact upload.
- Exact-head CI and its upload are checked separately on the PR. Runtime is NOT passed: GUI/HUD/admin, managed mob lifecycle, relog/respawn, rune actions, combat/rewards, Mongo state and vanilla-client compatibility still require a separate test.
- Full rune/client separation remains a later structural PR; the remaining root package is explicit in the class map. No mechanics, GUI feedback or contracts are changed here.
- tmp-noop-ignore has no unique work and is eligible for pointer deletion; no commits/tags/history need removal. The audit itself does not rewrite or prune Git history.
