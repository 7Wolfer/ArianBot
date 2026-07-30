# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build fat JAR (includes all dependencies via maven-shade-plugin)
mvn package

# Run the bot (requires DISCORD_TOKEN and LAVALINK_PASSWORD env vars,
# plus a running Lavalink server for music)
DISCORD_TOKEN=your_token LAVALINK_PASSWORD=your_lavalink_password java -jar target/ArianBot-1.0-SNAPSHOT.jar

# Compile only (no packaging)
mvn compile

# Clean build artifacts
mvn clean
```

`ANTHROPIC_API_KEY` (AI personality + news summaries) and the Spotify credentials
(`SPOTIFY_CLIENT_ID`, `SPOTIFY_CLIENT_SECRET`, `SPOTIFY_REFRESH_TOKEN`) are optional —
those features degrade gracefully without them. Music additionally requires `yt-dlp`
on `PATH` (or `~/.local/bin/yt-dlp`) and `~/.deno/bin/deno`.

There are no automated tests in this project.

## Architecture

ArianBot is a Discord bot built with **JDA 6** (Java Discord API) and **Java 21**, using
**Lavalink** for audio playback and **SQLite** (`arian.db` in the project root) for
persistent storage across 7 tables.

### Startup flow (`Main.java`)
1. Reads `DISCORD_TOKEN` and `LAVALINK_PASSWORD` from environment variables (both required).
2. Calls `DataBaseManager.initialize()` to connect to SQLite and create tables.
3. Builds the `LavalinkClient` (`MusicManagers.init`) *before* JDA, since JDA needs its
   voice-dispatch interceptor to forward voice events to Lavalink.
4. Builds the JDA instance with five listeners registered: `SlashCommandListener`,
   `PrefixCommandListener`, `ButtonListener`, `ArianListener`, `VoiceIdleListener`.
5. Registers all slash commands globally after `awaitReady()`.
6. Starts `NewsScheduler`.

### Command routing
Commands are handled by listeners that delegate to static handler methods:

- **`SlashCommandListener`** — routes all `/slash` commands (`ping`, `hug`, `kiss`, `hit`,
  `pat`, `channel`, `download`, `newschannel`, `play`, `skip`, `pause`, `queue`, `remove`,
  `priority`, `stop`), plus `/play` autocomplete (`onCommandAutoCompleteInteraction` →
  `PlayCommand.handleAutoComplete`, which searches Spotify).
- **`PrefixCommandListener`** — routes `a!<cmd>` for the same set, plus owner-only
  `a!guilds`, `a!leave <server_id>`, `a!testnews` (no slash equivalents). Aliases: `abrazo`
  (hug), `beso` (kiss), `golpe` (hit), `dl`/`descargar` (download), `p` (play),
  `s`/`next` (skip), `q`/`cola` (queue), `rm` (remove), `prio` (priority). The prefix is
  `a!` (`Main.PREFIX`).
- **`ButtonListener`** — handles return-action buttons (e.g. "Hug back") on hug/kiss/hit.
  Button component IDs encode `actionKey:targetUserId:originalAuthorId`. Only the intended
  recipient can press the button; pressing it disables the button on the original message.
- **`ArianListener`** — decides when the AI personality speaks (see below) and sends the
  response.
- **`VoiceIdleListener`** — disconnects Arian from a voice channel ~7 minutes after
  everyone else leaves it (re-checks before actually disconnecting, in case someone rejoins).

### Social command pattern
All social commands (`hug`, `kiss`, `hit`, `pat`) follow a shared pattern via `SocialCommand`:
- Each command class (e.g. `HugCommand`) defines its own image list, action string, emoji, color, and `isPair` flag, then delegates everything to `SocialCommand.handlePrefix/handleSlash/handleButton`.
- `isPair = true` (kiss, hit): counts interactions between a specific pair of users using `DataBaseManager.incrementPairCount`. IDs are sorted so A→B and B→A count together.
- `isPair = false` (hug, pat): counts total interactions received by the target using `DataBaseManager.incrementReceivedCount`.
- `hasReturnButton = true` commands (hug, kiss, hit) show a button allowing the target to respond in kind.

### Music system (`music/` + `commands/music/`)
- **`MusicManagers`** — single global `LavalinkClient` (`ws://127.0.0.1:2333`), one
  `TrackScheduler` per guild.
- **`TrackScheduler`** — per-guild queue, skip/pause/remove/priority, auto-advances on
  track end, lazily resolves not-yet-downloaded playlist entries right before they play.
- **`YoutubeResolver`** — downloads audio locally via `yt-dlp` and hands Lavalink the local
  file path instead of a remote URL. This is deliberate: streaming directly from a
  googlevideo URL was intermittently returning 403s from Lavalink's own fetch; downloading
  first and playing from disk fixed it. Temp files are cleaned up after a track
  finishes/is skipped.
- **`SpotifyResolver`** — reads real title/artist metadata from Spotify's Web API for
  track/playlist/album URLs, then hands each song to `YoutubeResolver` as a
  `ytsearch1:<name> <artist>` query (Spotify doesn't allow raw audio streaming). Also
  powers `/play` autocomplete via `search()`. Playlist listing needs `SPOTIFY_REFRESH_TOKEN`
  (Spotify requires a user-authorized token for that, not just client credentials); reading
  playlists not owned by that authorized account will 403 — a Spotify Developer
  "Extended Quota Mode" platform restriction, not a code bug.
- **`PlayCommand`** won't move Arian away from a voice channel that still has real
  listeners in it — it declines instead of stealing the bot.

### AI personality (`ai/` + `listeners/ArianListener.java`)
- **`ArianListener`** decides *whether* to respond: always if it's a reply to one of
  Arian's own messages; ~80% if @mentioned or "arian" appears in the text; otherwise ~10%,
  gated by a 25s per-channel cooldown. Only active in channels enabled via `/channel`.
- **`ChannelContext`** keeps an in-memory rolling history (last 20 messages) per channel,
  formatted as `"Author: message"`. Arian's own replies are pushed back into this history
  too, and Discord replies are annotated `(responde a X)`, so Arian can tell a reply to
  someone else apart from a reply to him.
- **`ArianAI`** calls Claude (`claude-haiku-4-5-20251001`) with a single user turn embedding
  the history, current Mexico City time, per-user/server memory, and — when true — an
  explicit `(DATO SEGURO: ...)` signal that this message really is a mention or reply to
  Arian. The whole personality/behavior prompt lives in one string here
  (`SYSTEM_PROMPT`) — edit it directly to change how Arian talks, no other files need to
  change. Responses can carry `[MEM:...]` (per-user memory), `[SERVERMEM:...]` (per-server
  "culture" memory), and `[REACT:emoji]` tags, parsed out before sending.
- `/channel` and `a!channel` (`ChannelCommand`) are locked to a hardcoded owner Discord ID
  (`OWNER_ID`), not admin permissions — only the bot owner decides where Arian's AI talks,
  in any server.

### News digest (`news/`)
`NewsScheduler` posts a 5-item digest (2 Hacker News, 1 arXiv `cs.*`, 1 PubMed, 1 arXiv
`q-bio.NC`) to every guild's configured channel (`/newschannel`, admin-permission-gated)
every Tuesday and Friday at 10:00 `America/Mexico_City`. `NewsSummarizer` writes each
item's summary in Arian's voice via Claude, strictly grounded in the real title/abstract.
`posted_news` dedupes items already posted.

### Download command (`DownloadCommand`)
Downloads a video via `yt-dlp` (size-capped to the guild's upload limit) and re-uploads it
to the channel. YouTube downloads can fail from a VPS IP without `~/yt-cookies.txt` configured.

### Database (`DataBaseManager.java`)
Uses a single static `Connection`. Tables:
- `pair_interactions(user1_id, user2_id, action, count)` — kiss/hit, keyed by sorted user ID pair.
- `received_interactions(user_id, action, count)` — hug/pat, keyed by receiver.
- `arian_channels(channel_id)` — channels where Arian's AI personality is active.
- `user_memory(user_id, username, memory)` — per-user AI memory.
- `server_memory(guild_id, memory)` — per-server "culture" AI memory.
- `news_channel(guild_id, channel_id)` — each guild's configured news digest channel.
- `posted_news(item_id, posted_at)` — dedupe log for the news digest.

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
