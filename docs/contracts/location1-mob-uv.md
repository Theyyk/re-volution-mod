# Location 1 managed mob UV contract

This document is the visual contract between `MobAppearanceCatalog`, `ForestMobModel`, and static PNG assets under `assets/revolution/textures/entity/location1/`.

## Runtime routing

The runtime route is:

`rvvis:<locationId>:<mobSlotId>` -> `MobVisualWire.resolve()` -> `MobAppearanceCatalog.bySlot()` -> `MobAppearanceDefinition` -> `ForestMobModel(familyId)` + `ForestMobTextureLibrary(appearanceId)`.

Grouped art-spec ordering is not a runtime index. Location 1 catalog slots interleave families.

## Minecraft 1.12.2 ModelBox face layout

For a box with texture offset `(U,V)` and dimensions `(W,H,D)`, the complete UV footprint is:

- width: `2 * (W + D)`
- height: `H + D`

Face rectangles inside that footprint are:

- DOWN:  `(U + D,         V)       size W x D`
- UP:    `(U + D + W,     V)       size W x D`
- WEST:  `(U,             V + D)   size D x H`
- NORTH: `(U + D,         V + D)   size W x H`
- EAST:  `(U + D + W,     V + D)   size D x H`
- SOUTH: `(U + D + W + D, V + D)   size W x H`

Artwork must be authored against these six rectangles. A coarse fill of the whole footprint is not a face-aware texture template.

## Turtle, 64x64

| Part | Offset | Box W,H,D | Footprint |
|---|---:|---:|---:|
| shell | 0,0 | 12,5,14 | 52x19 |
| body | 0,20 | 10,4,12 | 44x16 |
| head | 42,37 | 6,4,5 | 22x9 |
| legs (shared) | 0,38 | 4,3,5 | 18x8 |
| tail | 20,38 | 2,2,4 | 12x6 |
| thorn extras (shared) | 0,48 | 2,4,2 | 8x6 |
| root extra | 10,48 | 2,5,2 | 8x7 |
| bloom extras (shared) | 20,48 | 4,2,4 | 16x6 |

The common moss turtle UV offsets are unchanged from the pre-PR #163 baseline. The pre-#163 `moss_turtle.png` is the regression reference.

## Serpent, 64x64

| Part | Offset | Box W,H,D | Footprint |
|---|---:|---:|---:|
| head | 0,0 | 8,6,7 | 30x13 |
| body A | 32,0 | 6,6,6 | 24x12 |
| body B | 0,14 | 6,6,6 | 24x12 |
| tail | 26,14 | 4,4,8 | 24x12 |
| root/wood plate | 0,28 | 10,3,4 | 28x7 |
| thorn/ancient spikes | 30,28 | 2,4,2 | 8x6 |

Body segments use A/B/A/B/A.

## Mantis, 64x64

| Part | Offset | Box W,H,D | Footprint |
|---|---:|---:|---:|
| head | 0,0 | 8,5,6 | 28x11 |
| torso | 30,0 | 6,10,4 | 20x14 |
| forearms (shared) | 0,16 | 3,10,3 | 12x13 |
| rear legs (shared) | 14,16 | 3,9,3 | 12x12 |
| support limbs (shared) | 28,16 | 2,7,2 | 8x9 |
| thorn/ancient extras | 38,16 | 2,4,2 | 8x6 |
| emerald plate | 48,16 | 4,5,2 | 12x7 |

## Spore, 64x64

| Part | Offset | Box W,H,D | Footprint |
|---|---:|---:|---:|
| cap/head | 0,0 | 14,5,14 | 56x19 |
| body | 0,20 | 8,9,8 | 32x17 |
| legs (shared) | 34,20 | 3,5,3 | 12x8 |
| crawler legs (shared) | 48,20 | 4,3,2 | 12x5 |
| guardian/mycelium growths | 34,28 | 4,5,4 | 16x9 |

## Golem, 64x64

| Part | Offset | Box W,H,D | Footprint |
|---|---:|---:|---:|
| torso | 0,0 | 12,13,8 | 40x21 |
| arms (shared) | 40,0 | 5,15,7 | 24x22 |
| head | 0,22 | 10,7,10 | 40x17 |
| legs (shared) | 40,22 | 5,8,6 | 22x14 |
| moss layer | 0,40 | 12,2,12 | 48x14 |
| root/ancient extras | 50,40 | 2,7,2 | 8x9 |

## Spirit, 64x64

| Part | Offset | Box W,H,D | Footprint |
|---|---:|---:|---:|
| crown/keeper plate | 0,0 | 12,2,12 | 48x14 |
| head | 0,16 | 8,7,8 | 32x15 |
| torso | 34,16 | 8,11,6 | 28x17 |
| tail/lower body | 0,32 | 6,7,4 | 20x11 |
| arms (shared) | 22,34 | 3,9,3 | 12x12 |
| flower extras | 36,34 | 4,3,4 | 16x7 |

## Beast / Deer, 64x64

| Part | Offset | Box W,H,D | Footprint |
|---|---:|---:|---:|
| torso | 0,0 | 10,9,16 | 52x25 |
| head | 0,26 | 8,7,8 | 32x15 |
| legs (shared) | 34,26 | 3,8,3 | 12x11 |
| tail | 0,42 | 4,4,7 | 22x11 |
| base antlers (shared) | 24,42 | 2,8,2 | 8x10 |
| thorn/keeper extras | 34,42 | 2,6,2 | 8x8 |
| ancient/keeper antlers | 44,42 | 5,2,2 | 14x4 |

## Dryad, 64x64

| Part | Offset | Box W,H,D | Footprint |
|---|---:|---:|---:|
| head | 0,0 | 8,7,8 | 32x15 |
| torso | 34,0 | 8,11,4 | 24x15 |
| arms (shared) | 0,16 | 3,12,3 | 12x15 |
| legs (shared) | 14,16 | 3,8,3 | 12x11 |
| staff | 28,16 | 2,19,2 | 8x21 |
| guardian plate | 38,16 | 10,12,2 | 24x14 |
| thorn/ancient extras | 0,32 | 2,6,2 | 8x8 |
| flower extras | 10,32 | 4,3,4 | 16x7 |

## Ancient Forest Guardian, 128x128

| Part | Offset | Box W,H,D | Footprint |
|---|---:|---:|---:|
| torso | 0,0 | 16,17,12 | 56x29 |
| head | 58,0 | 14,10,12 | 52x22 |
| arms (shared) | 0,32 | 7,20,9 | 32x29 |
| legs (shared) | 34,32 | 7,10,8 | 30x18 |
| vertical branches | 66,32 | 3,11,3 | 12x14 |
| horizontal branches | 80,32 | 7,3,3 | 20x6 |
| spirit core | 102,32 | 6,6,2 | 16x8 |
| back extension | 0,64 | 6,6,10 | 32x16 |

## Acceptance rules

1. `appearanceId` selects only its matching `<appearanceId>.png`.
2. `familyId` selects the model family and therefore the UV contract.
3. HP, reward, population, stage provisioning, and future `Mob1_N` identifiers must not affect this route.
4. All regular Location 1 atlases are 64x64; `ancient_forest_guardian` is 128x128.
5. Static PNG artwork must target the face rectangles above, not only the overall part footprint.
6. The pre-PR #163 `moss_turtle.png` is the visual regression baseline for turtle common UVs.
7. Visual acceptance is performed in actual catalog-slot order 1..41, not grouped family-spec order.
