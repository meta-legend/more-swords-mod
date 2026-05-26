# Contributing to More Swords Mod

When submitting a pull request, you are granting metalegend the right to license your contributions under the
[PolyForm Shield License 1.0.0](LICENSE.md).

Thanks for taking the time to contribute. This project is a Fabric Minecraft mod, so contributions can include code,
textures, models, recipes, loot tables, translations, balance changes, documentation, and bug reports.

## Before You Start

- Check the existing issues and pull requests before starting a larger change.
- Keep changes focused. A small bug fix, recipe update, texture improvement, or single feature is easier to review than
  a broad rewrite.
- For balance or gameplay changes, explain the intended role of the item and why the change improves it.
- Do not include generated files from `build/`, `.gradle/`, `run/`, or IDE-specific folders.

## Development Setup

This project uses the versions listed in `gradle.properties`.

Requirements:

- Java 25
- The included Gradle wrapper
- A local Minecraft/Fabric development setup if you want to test in game

Useful commands:

```powershell
.\gradlew.bat build --console=plain
.\gradlew.bat processResources --console=plain
```

On Linux or macOS:

```sh
./gradlew build --console=plain
./gradlew processResources --console=plain
```

Use `processResources` for resource-only changes such as recipes, language files, models, and mod metadata. Use `build`
for code changes or anything that may affect runtime behavior.

## Code Style

- Match the style already used in the surrounding file.
- Prefer clear, direct Java over clever abstractions.
- Keep lines under 120 characters when practical.
- Use descriptive names for methods, constants, translation keys, and resource IDs.
- Add comments only when they explain non-obvious behavior.
- Avoid unrelated cleanup in the same pull request as a feature or bug fix.

## Mod Content Guidelines

- New weapons should have a distinct gameplay identity, not only higher stats.
- Tooltips should be concise and use existing tooltip helper patterns where possible.
- New items should include display names, models, textures, recipes or obtainment logic, and relevant flavor text.
- New recipes should fit the progression of the item and avoid making powerful gear too cheap.
- Texture contributions must be original work or use assets with a license that allows inclusion in this mod.
- Keep texture transparency clean. Stray semi-transparent pixels can cause visible artifacts in generated item models.
- Language keys should be added to `en_us.json` for all player-facing text.

## AI-Assisted Contributions

AI tools may be used as part of your workflow, but you are responsible for everything you submit.

By opening a pull request, you confirm that:

- You understand the code, assets, and text you are submitting.
- You have tested the change as appropriate.
- You have the right to contribute the submitted work under this project's license.
- You will disclose substantial AI assistance in the pull request description.

Do not submit code or assets copied from sources that you do not have permission to use.

## Pull Requests

Your pull request should include:

- A short summary of what changed.
- Why the change is useful for the mod.
- Any relevant screenshots or short clips for visual changes.
- Testing performed, such as `.\gradlew.bat build --console=plain` or in-game checks.
- Linked issues, if the pull request fixes one.

Maintainers may request changes for balance, style, scope, licensing, or compatibility reasons.
