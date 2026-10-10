# GUI v1.2 — first element

These are historical design notes. The rune names, types and placeholder icons described below predate the current poison-status catalog; see `src/main/java/com/example/customguimod/EarthRuneCatalog.java` for the current definitions.

The first element (the first deck, index 0) contains 26 separate positions in the order shown in the user's reference. Names may repeat: each slot has its own definition and acquisition state. Duplicate counters are not displayed.

| Slots | Name | Type |
|---|---|---|
| 1–4 | Stone Hammer | Click |
| 5–7 | Shadow Blade | Click |
| 8 | Forest Heart | Click |
| 9–10 | Blacksmith's Seal | Boost |
| 11 | Golem Heart | Boost |
| 12–13 | Mossy Shard | Click / Earth |
| 14–17 | Ancient Root | Earth |
| 18–20 | Obsidian Spike | Earth |
| 21–22 | Ancient Forest Seed | Boost |
| 23–25 | Cracked Seal | Boost |
| 26 | Gold Nugget | Resource |

Only names and types were populated. Effect values, prices, boosts and images from the reference were not imported. Unowned slots are empty, with no icons, types, labels or tooltips. Owned runes display an item icon and a small type badge in the bottom-right corner. The name and type text are available only on hover. Item icons are the existing temporary Minecraft placeholders; Gold Nugget uses the corresponding Minecraft item.

New purchases for the first element receive the ID of their fixed slot. Existing test records are neither rewritten nor deleted: acquisition and rank are displayed using the saved slot, while the name and type come from the new fixed definition. Legacy records outside the 26 positions are retained and count toward the limit. Other decks retain their previous display and allocation behavior.

The purchase limit is 26 runes per deck, enforced consistently by the server and the interface.

Planned duplicate-purchase mechanic: the player receives currency used to upgrade the corresponding element. Compensation and upgrades are not implemented yet.

Type badges: lightning for Click, a leaf for Earth, an upward arrow for Boost, and a coin for Resource. Click / Earth uses a lightning-and-leaf pair. The legacy Water type uses a droplet.
