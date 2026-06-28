# More Swords Mod Changelog

## 0.1.1-26.1.2

A major combat, progression, and polish update for More Swords Mod. This release focuses on giving each weapon a clearer identity, adding real progression paths, improving tooltips/audio/visuals, and preparing the mod for a cleaner public release.

### Compatibility

- Updated for Minecraft `26.1.2`.
- Requires Java `25`.
- Requires Fabric Loader `0.18.4` or newer.
- Built with Fabric API `0.146.0+26.1.2`.
- The mod metadata allows Minecraft `26.1.x`, but `26.1.2` is the primary tested version.

### Highlights

- Added the full Bone Scythe soul harvesting and summoning system.
- Added Wind Staff mobility, gliding, entity launch, and mounted gust mechanics.
- Reworked Lightning Staff into a simple direct `Stormcall` lightning caster.
- Added Obsidian Greatsword heavy combat identity with armor pressure and `Grand Slam`.
- Added component-based katana crafting with hilts, guards, blades, and smithing upgrades.
- Added custom sounds, subtitles, tooltips, flavor text, recipes, loot drops, creative-tab organization, and release metadata polish.

### Added

#### Bone Scythe

- Added `Soul Reaping`.
- Hostile kills with the Bone Scythe can store `Soul Imprints`.
- Valid kills can grant `Soul Charges` with tier-scaled chances.
- Added an imprint selection system using `Sneak + Right Click`.
- Added a summon window that converts stored soul charges into summon weight.
- The Bone Scythe can store up to 10 Soul Charges and 8 Soul Imprints.
- The summon window lasts 10 seconds and uses a direct 1 charge to 1 summon weight economy.
- Added reaped soul summons using the actual harvested mob types.
- Added a 5-tier summon roster with weighted summons.
- Current summon roster:
  - Tier 1 / Weight 1: Silverfish, Endermite, Baby Zombified Piglin, Baby Hoglin
  - Tier 2 / Weight 2: Zombie, Baby Zombie, Husk, Baby Husk, Drowned, Baby Drowned, Skeleton, Spider, Slime
  - Tier 3 / Weight 3: Bogged, Cave Spider, Stray, Parched, Pillager, Piglin, Zombified Piglin, Magma Cube, Wither Skeleton
  - Tier 4 / Weight 4: Hoglin, Zoglin, Blaze, Vindicator
  - Tier 5 / Weight 5: Piglin Brute, Iron Golem, Ravager, Evoker
- Added baby soul variants for supported mobs.
- Added slime and magma cube size preservation.
- Added evoker support, including owner-bound helper `Reaped Vex` summons.
- Added a cap of 3 active reaped souls at once.
- Added persistent action-bar summon window feedback showing remaining time, summon weight, and active summon count.
- Added owner-bound temporary ally behavior for reaped summons.
- Added `Grave-Bound` soul particle links from active summons back toward the owner while the Bone Scythe is held.
- Added unused summon weight refunds.
- Added `Final Recall` to dismiss summons early and refund eligible weight.
- Added `Grave Garrison` to teleport active summons back to the player and focus nearby threats.
- Added editable Bone Scythe keybinds for `Final Recall` and `Grave Garrison`.
- Added custom networking payloads for Bone Scythe command inputs and katana animation sync.
- Added cross-dimension summon tracking and recovery behavior.
- Added cleanup for orphaned reaped summons.
- Added the `Ossuary` enchantment for the Bone Scythe, extending reaped summon lifetime by 5 seconds per level.
- Added dissipate particles and sound when reaped souls are recalled or expire.
- Added custom Bone Scythe sounds and subtitles.

#### Wind Staff

- Added Wind Staff mobility and control behavior.
- Added `Wind Leap`.
- Added active `Gale Glide`.
- Added fall-damage protection while gliding.
- Added target-use launch/control behavior.
- Added Wind Staff functionality while held in either hand.
- Added mounted gust boost for living mounts.
- Added stabilization for mounted gust movement to reduce momentum loss and packet-related stalls.
- Added client-side looping glide audio with fade-in/fade-out.
- Added subtitle refresh behavior while gliding.

#### Lightning Staff

- Reworked the Lightning Staff into `Stormcall`.
- Right click now casts a direct lightning strike where the player aims.
- Added a 16-block range.
- Added a 3-second cooldown.
- Added 3 durability cost per cast.
- Lightning is now visual-only for block impact, preventing ground fire.
- Direct entity hits still receive vanilla `thunderHit`, preserving lightning damage, burning, and mob transformations.
- Removed the older static buildup/chain lightning behavior.

#### Katanas

- Added `Sheath Strike` shield piercing.
- Primed `Sheath Strike` now bypasses shields using a dedicated damage type.
- Shield-piercing hits disable shields like a vanilla axe.
- Added a dedicated shield-pierce cue.
- Added short dash protection during committed `Sheath Strike` lunges: projectiles are blocked, melee damage is reduced, and knockback is ignored.
- Added airborne combo synergy: `Sheath Strike` gives airborne targets extra vertical knockback.
- Reduced dash distance against airborne targets for better combo readability.
- Diamond and Netherite Katanas can inflict brief Weakness on hit.
- Added custom katana strike sounds.
- Added katana ready sound when `Sheath Strike` recharge completes.
- Added a recharge bar for `Sheath Strike` readiness.
- Added client-side `Sheath Strike` animation support.
- Added first-person katana dash pose, FOV kick, item motion, third-person pose, and trail particles.
- Added held katana guard overlay models so guards can visually protrude in held views while preserving clean inventory rendering.
- Added component-based katana crafting:
  - Katana Hilt
  - Iron/Golden/Diamond/Netherite Guards
  - Iron/Golden/Diamond/Netherite Katana Blades
- Added smithing upgrades for Netherite Katana components.

#### Obsidian Greatsword

- Added heavy weapon identity around armor pressure and area control.
- Heavy melee hits punch through roughly half of the target's armor.
- Added `Grand Slam`.
- `Grand Slam` now requires at least 65% swing charge.
- Added red action-bar feedback when slam is not ready.
- Added directional ground-crack particles and heavy impact sound.
- Slam now excludes the player's current vehicle from damage and knockback.
- Reworked crafting into a late-game Netherite Sword upgrade-style recipe.

#### Progression and Crafting

- Added new progression components:
  - Arcane Rod
  - Withered Rib
  - Scattered Wings
  - Soul Essence
  - Withered Stalk
  - Zephyr Gem
- Added loot drops for Withered Ribs from Wither Skeletons.
- Added loot drops for Scattered Wings from Phantoms.
- Added Looting-aware drop chances for progression materials.
- Added Soul Essence blasting from Soul Sand.
- Added Withered Stalk and Zephyr Gem intermediate recipes.
- Reworked Bone Scythe and Wind Staff recipes around progression materials.
- Updated Wind Staff recipe to use one Arcane Rod.
- Updated Lightning Staff recipe to use Amethyst Shard, Lightning Rod, and one Arcane Rod.
- Reworked all katana recipes around hilt + guard + blade assembly.

#### Tooltips, Flavor Text, and Localization

- Added shared tooltip formatting support.
- Added structured tooltip sections for special weapons.
- Added flavor text support for simple ingredient/component items.
- Added flavor text for katana components and progression materials.
- Added tooltip spacing before vanilla enchantment lines when custom tooltip blocks are present.
- Updated katana, Wind Staff, Lightning Staff, Bone Scythe, and Obsidian Greatsword tooltips to reflect their current mechanics.
- Added and updated `en_us.json` entries for new items, tooltips, sounds, messages, and Mod Menu text.

#### Creative Tabs

- Added the custom `Magical` creative tab for the full mod item lineup.
- Added mod weapons to the vanilla Combat tab.
- Added mod components and progression materials to the vanilla Ingredients tab.

#### Enchanting

- Added the treasure-only `Ossuary` enchantment for the Bone Scythe.
- `Ossuary` levels I-III increase reaped summon lifetime from 15 seconds up to 30 seconds.
- Added modern enchantability tags for custom weapons.
- Vanilla enchantments such as Looting now recognize the intended melee weapons.
- Lightning Staff and Wind Staff can now receive durability-related enchantments such as Unbreaking, Mending, and Curse of Vanishing.

#### Compatibility and Metadata

- Added lightweight Mod Menu support.
- Added Mod Menu name, summary, description, and links.
- Updated the mod icon.
- Updated mod homepage/source/issues metadata.
- Updated mod description for public release.
- Updated version to `0.1.0-26.1.2`.

#### Build and Release Support

- Added dedicated server smoke test support for CI.
- Added Fabric Loom `ciServer` run configuration.
- Updated GitHub Actions workflow for Java 25 and newer runner/action versions.
- Updated Gradle wrapper to `9.5.1`.

### Changed

- Rebalanced the mod toward distinct weapon roles instead of simple stat upgrades.
- Renamed the Arcane Rod registry id to `arcane_rod` so the internal id matches the display name.
- Updated finished gold katana naming to vanilla-style `Golden Katana`.
- Renamed gold katana components to `Golden` naming.
- Renamed katana blade component IDs to `*_katana_blade`.
- Reworked Wind Staff glide audio from repeated server sounds into a client-side looping sound.
- Reworked Lightning Staff from a buildup/chain design into a simple direct-cast lightning staff.
- Reworked Obsidian Greatsword crafting into a stronger late-game recipe.
- Refreshed and adjusted multiple item textures, including katana component/weapon textures and Obsidian Greatsword cleanup.
- Updated README into a complete public mod overview.
- Replaced the old contribution guide with a mod-specific `CONTRIBUTING.md`.
- Switched the project license from MIT to PolyForm Shield License 1.0.0.
- Moved recipes to the current `data/moreswordsmod/recipe/` path used by this Minecraft version.
- Removed leftover Fabric example scaffolding from the source/resources.
- Removed development-only commands and shield test dummy code from the release build.
- Removed unused `package.json` and `package-lock.json`.

### Fixed

- Fixed `Sheath Strike` shield piercing only working reliably from behind.
- Fixed katana shield-pierce audio layering over the normal strike cue.
- Fixed katana recharge behavior that previously made the held model stay visually lowered.
- Fixed Bone Scythe summon tracking through dimension changes.
- Fixed reaped summon orphan edge cases.
- Fixed `Grave Garrison` failing when summons were left in unloaded dimensions.
- Fixed reaped slime and magma cube summons splitting on death.
- Fixed Wind Staff mounted gust stalls and momentum loss.
- Fixed Wind Staff glide subtitles not refreshing after switching to client-side looping audio.
- Fixed Obsidian Greatsword compile issue around slam-not-ready messages.
- Fixed Obsidian Greatsword slam affecting the player's current mount.
- Fixed katana and Obsidian Greatsword texture transparency artifacts.
- Fixed held katana guard overlay placement and shifted the guard alignment upward by one texture-pixel step.
- Fixed CI workflow issues and added better smoke-test handling.
