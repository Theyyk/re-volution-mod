# UI-03-B — E combat damage projection / hover diagnostics

Owner reported E main menu showed Rune baseline (~1–1.08) while real managed-mob damage was much higher due to legitimate, persistent Mastery effects (CLICK x2264.66, POISON x6543.84, ALL_DAMAGE x955201). Owner confirmed these are earned through earlier runs/resets; **do not reset progress, nerf, cap balance or touch PROD**.

## Contract

- In main E Rune menu, replace click and poison baseline with a **server-authored current standard-hit projection**.
- Compute from the real stable RuneCombatMath baseline (or legacy additive fallback when no rune combat snapshot), then the already-existing Unique Rune, shared EffectResolver (Mastery + Pets), active God Worship, and Aura layers, respecting each parent/child feature gate. No gameplay rules are applied or duplicated by E.
- Hover click or poison stat to show Rune baseline, effect multipliers, per-source Mastery/Pet additive contributions, and Unique/God/Aura before/after factors when active. The client renders only server-authored strings and values, never invents bonuses.
- Differentiate **normal current hit prediction** from **guaranteed future hit**. Random procs/crits, target vulnerability/armor, hit cooldown, real-time state changes, effects tied to a specific mob and other situational combat effects cannot be predicted without a target and attack event. E cannot promise exactly the damage delivered by the next hit.
- Stable base HUD remains explicitly labeled "База" rather than pretending to represent total damage. Existing game combat event handlers stay unchanged.
- Mastery automatic rank thresholds, permanent bonuses and reset semantics remain unchanged.
- On E open, existing stats request refreshes projection. Reopen E to read new values when active effects change.
- Existing PlayerStatsPacket byte contract remains unchanged. New CombatProjectionPacket is an additive channel at discriminator 31 (after Mastery 29/30); **same DEV Gameplay/client binary required**. Do not mix old and new jars.

## Tests and acceptance

- Verify PlayerStatsPacket projection values and source descriptions round-trip (automated).
- Test large Mastery saved values without any reset/cap; E should show a meaningfully higher prediction than Rune baseline.
- Hover both click and poison; source details should include Mastery factors and any active Pets/Unique/God/Aura contributions.
- Test ranks at 0/100/500/1000 progress and ordinary Reset. Current-run rank contributions should activate/deactivate; permanent saved multipliers should remain.
- Compare E forecast to live managed-mob hit with random/target/timed variables controlled. If they differ, inspect the omitted event and record what was excluded; do not rewrite damage balance to force equality.
- Reconnect, release/client version parity, gameplay exact-head CI, post-merge CI, DEV publication and Minecraft QA are required before accepted.

No PROD, Stable v2.12.2, reset/retention, Mongo schema or Balance Core numerical changes.
