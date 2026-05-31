# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build fat JAR (includes all dependencies via maven-shade-plugin)
mvn package

# Run the bot (requires DISCORD_TOKEN env var)
DISCORD_TOKEN=your_token java -jar target/ArianBot-1.0-SNAPSHOT.jar

# Compile only (no packaging)
mvn compile

# Clean build artifacts
mvn clean
```

There are no automated tests in this project.

## Architecture

ArianBot is a Discord bot built with **JDA 5** (Java Discord API) and **Java 21**. It uses SQLite (`arian.db` in the project root) for persistent interaction counters.

### Startup flow (`Main.java`)
1. Reads `DISCORD_TOKEN` from environment variables.
2. Calls `DataBaseManager.initialize()` to connect to SQLite and create tables.
3. Builds the JDA instance with three listeners registered: `SlashCommandListener`, `PrefixCommandListener`, `ButtonListener`.
4. Registers all slash commands globally after `awaitReady()`.

### Command routing
Commands are handled by three listeners that delegate to static handler methods:

- **`SlashCommandListener`** — routes `/ping`, `/hug`, `/kiss`, `/hit`, `/pat` slash commands.
- **`PrefixCommandListener`** — routes `a!ping`, `a!hug` (alias: `a!abrazo`), `a!kiss` (alias: `a!beso`), `a!hit` (alias: `a!golpe`), `a!pat` prefix commands. The prefix is `a!` (defined in `Main.PREFIX`).
- **`ButtonListener`** — handles return-action buttons (e.g. "Hug back"). Button component IDs encode `actionKey:targetUserId:originalAuthorId`. Only the intended recipient can press the button; pressing it disables the button on the original message.

### Social command pattern
All social commands (`hug`, `kiss`, `hit`, `pat`) follow a shared pattern via `SocialCommand`:
- Each command class (e.g. `HugCommand`) defines its own image list, action string, emoji, color, and `isPair` flag, then delegates everything to `SocialCommand.handlePrefix/handleSlash/handleButton`.
- `isPair = true` (kiss, hit): counts interactions between a specific pair of users using `DataBaseManager.incrementPairCount`. IDs are sorted so A→B and B→A count together.
- `isPair = false` (hug, pat): counts total interactions received by the target using `DataBaseManager.incrementReceivedCount`.
- `hasReturnButton = true` commands (hug, kiss, hit) show a button allowing the target to respond in kind.

### Database (`DataBaseManager.java`)
Uses a single static `Connection`. Two tables:
- `pair_interactions(user1_id, user2_id, action, count)` — for kiss/hit, keyed by sorted user ID pair.
- `received_interactions(user_id, action, count)` — for hug/pat, keyed by receiver.

### Adding a new social command
1. Create `src/main/java/com/arian/bot/commands/social/XxxCommand.java` following the pattern of `HugCommand` or `KissCommand`.
2. Add `handlePrefix`, `handleSlash`, and optionally `handleButton` static methods that call `SocialCommand`.
3. Register the slash command in `Main.java` (`jda.updateCommands()`).
4. Add a `case` in `SlashCommandListener`, `PrefixCommandListener`, and (if it has a return button) `ButtonListener`.


Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.
