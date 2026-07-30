# ArianBot 🐯

A Discord bot for a furry community server, built with Java 21 and JDA 6. ArianBot combines a full music player, social interaction commands, a programming/neuroscience news digest, a video downloader, and an AI-driven personality (**Arian**) who participates in conversations on his own.

## Overview

ArianBot is really five features under one roof:

- **AI personality** — Arian reads the chat and joins in on his own, powered by Claude. He knows who he's talking to, remembers people and server culture over time, and adapts his tone.
- **Music** — full queue-based player (YouTube search/links, Spotify tracks/playlists/albums) with autocomplete search, played through Lavalink.
- **Social commands** — hug, kiss, hit, pat, each with a GIF embed, persistent counters, and a "return the favor" button.
- **News digest** — a Tuesday/Friday roundup of programming and neuroscience news, summarized in Arian's voice.
- **Downloader** — grabs videos from TikTok, Instagram, YouTube, etc. and drops them straight into the channel.

Everything works through both slash commands (`/play`) and a text prefix (`a!play`).

## Requirements

- Java 21
- Maven
- A running [Lavalink](https://github.com/lavalink-devs/Lavalink) server (default `ws://127.0.0.1:2333`) with its `local` file source enabled — required for music
- [`yt-dlp`](https://github.com/yt-dlp/yt-dlp) on `PATH` or at `~/.local/bin/yt-dlp` — required for music and downloads
- [Deno](https://deno.com) at `~/.deno/bin/deno` — used by `yt-dlp` to solve YouTube's signature challenges
- A Discord bot token
- An Anthropic API key — optional, but without it the AI personality and news summaries are silently disabled
- Spotify API credentials — optional, only needed to resolve Spotify links and power `/play` autocomplete

## Setup

**1. Clone the repository**

```bash
git clone https://github.com/7Wolfer/ArianBot.git
cd ArianBot
```

**2. Build the JAR**

```bash
mvn package
```

This produces a fat JAR at `target/ArianBot-1.0-SNAPSHOT.jar` with all dependencies included.

**3. Run the bot**

```bash
DISCORD_TOKEN=your_token LAVALINK_PASSWORD=your_lavalink_password java -jar target/ArianBot-1.0-SNAPSHOT.jar
```

### Environment variables

| Variable | Required | Purpose |
|---|---|---|
| `DISCORD_TOKEN` | Yes | Discord bot token — the bot won't start without it |
| `LAVALINK_PASSWORD` | Yes | Password for the local Lavalink server — the bot won't start without it |
| `ANTHROPIC_API_KEY` | No | Powers the AI personality and news summaries; both degrade gracefully without it |
| `SPOTIFY_CLIENT_ID` / `SPOTIFY_CLIENT_SECRET` | No | Enables resolving Spotify links and `/play` autocomplete |
| `SPOTIFY_REFRESH_TOKEN` | No | Needed specifically to read Spotify **playlists** (Spotify requires a user-authorized token for that, not just client credentials) |

## Commands

### 🎵 Music

All available as `/slash` and `a!prefix` commands. Songs are resolved via `yt-dlp` and downloaded locally before being handed to Lavalink — this avoids YouTube's intermittent blocking of direct stream URLs. Spotify links are read for their real metadata and searched on YouTube. If Arian is already playing for people in another voice channel, he won't abandon them to follow a new request — he'll politely decline instead.

| Command | Aliases | Description |
|---|---|---|
| `/play cancion:<nombre o link>` | `a!play`, `a!p` | Queues a song, YouTube playlist, or Spotify track/playlist/album. Autocompletes song search via Spotify. |
| `/skip` | `a!skip`, `a!s`, `a!next` | Skips the current track. |
| `/pause` | `a!pause` | Pauses or resumes playback. |
| `/queue` | `a!queue`, `a!q`, `a!cola` | Shows what's playing and what's queued. |
| `/remove posicion:<n>` | `a!remove`, `a!rm` | Removes a track from the queue by its position. |
| `/priority posicion:<n>` | `a!priority`, `a!prio` | Moves a queued track to play next. |
| `/stop` | `a!stop` | Clears the queue and disconnects. |

Arian also leaves the voice channel on his own about 7 minutes after everyone else does.

### 🤗 Social

Each generates a GIF embed and tracks interaction counts in the database. Hug, kiss, and hit include a return button so the target can respond in kind.

| Command | Description |
|---|---|
| `/hug usuario:<@user>` (`a!hug`, `a!abrazo`) | Hug someone — tracks total hugs received. |
| `/kiss usuario:<@user>` (`a!kiss`, `a!beso`) | Kiss someone — tracks kisses between that specific pair. |
| `/hit usuario:<@user>` (`a!hit`, `a!golpe`) | Hit someone — tracks hits between that specific pair. |
| `/pat usuario:<@user>` (`a!pat`) | Pat someone — tracks total pats received. |
| `/ping` (`a!ping`) | Checks Arian's gateway latency. |

### 📰 News digest

Every **Tuesday and Friday at 10:00 (America/Mexico_City)**, Arian posts a 5-item digest to every server's configured news channel: 2 programming stories from Hacker News, 1 from arXiv (`cs.SE`/`cs.PL`/`cs.DC`), and 2 neuroscience items (1 PubMed, 1 arXiv `q-bio.NC`). Each item gets a short summary written in Arian's own voice, grounded strictly in the real title/abstract.

| Command | Description |
|---|---|
| `/newschannel canal:<#channel>` (`a!newschannel`) | Sets the news channel for this server (admin only). No argument shows the current one. |

### ⬇️ Download

| Command | Description |
|---|---|
| `/download url:<link>` (`a!download`, `a!dl`, `a!descargar`) | Downloads a video (TikTok, Instagram, YouTube, etc.) via `yt-dlp` and uploads it to the channel, respecting Discord's file size limit. YouTube downloads can fail from a VPS IP without cookies configured. |

### 🔒 Owner / admin only

| Command | Who | Description |
|---|---|---|
| `/channel canal:<#channel>` (`a!channel`) | Bot owner only | Toggles whether Arian's AI personality is active in that channel. No argument lists active channels. Locked to the owner's Discord ID in every server, regardless of admin permissions there. |
| `a!guilds` | Bot owner only | Lists every server Arian is currently in. |
| `a!leave <server_id>` | Bot owner only | Makes Arian leave a specific server. |
| `a!testnews` | Bot owner only | Forces the news digest to run immediately, for testing. |

## AI personality (Arian)

Arian is a white tiger furry character powered by Claude Haiku. He isn't a command-response assistant — he behaves like a server member who happens to be reading along, and only speaks in channels explicitly enabled with `/channel`.

**When he speaks:**
- Always, if someone replies directly to one of his own messages
- ~80% of the time, if someone @mentions him or writes "Arian"
- ~10% of the time on any other message, and only once a 25-second per-channel cooldown has passed

**Staying on top of who's talking to whom:**
Group chats are noisy — most messages aren't for him. Arian tracks Discord reply-chains (so a reply to someone else isn't mistaken for a reply to him), gets an explicit signal whenever he's genuinely mentioned or replied to, and keeps his own past messages in the conversation history so he doesn't lose the thread or contradict himself.

**Memory:**
Arian builds a short profile of each user over time (preferences, personal details, running jokes) and a separate profile of each server's culture (in-jokes, slang, running gags) — storing both in SQLite and using them naturally without ever saying "I remember that."

**Behavior:**
- Adapts his tone: warm with calm people, sharp (once) with rude ones
- Follows along with dark humor and absurd questions instead of taking them seriously
- Occasionally flirty in a lighthearted, joking way
- Knows Wolfer is his owner
- Can react to messages with an emoji
- Always has something to say — if nothing comes to mind, he shares a related fun fact instead of going silent

All of his personality, tone, and behavior rules live in a single prompt in [`ArianAI.java`](src/main/java/com/arian/bot/ai/ArianAI.java) — edit it directly and restart the bot, no other files need to change.

## Database

SQLite (`arian.db`), created automatically on startup:

| Table | Purpose |
|---|---|
| `pair_interactions` | Kiss/hit counts between specific pairs of users |
| `received_interactions` | Total hugs/pats received per user |
| `arian_channels` | Channels where Arian's AI personality is active |
| `user_memory` | Arian's per-user memory profile |
| `server_memory` | Arian's per-server "culture" memory |
| `news_channel` | Each guild's configured news digest channel |
| `posted_news` | Dedupe log of already-posted news items |

## Project structure

```
src/main/java/com/arian/bot/
├── Main.java                   # Entry point, JDA setup, slash command registration
├── DataBaseManager.java        # SQLite connection and all database operations
├── ai/                         # Arian's chat personality
│   ├── ArianAI.java            # Claude API calls, system prompt, response parsing
│   ├── ArianResponse.java      # Response model (text, emoji, memory updates)
│   └── ChannelContext.java     # Per-channel rolling message history + cooldown
├── music/                      # Playback engine
│   ├── MusicManagers.java      # Lavalink client, per-guild TrackScheduler registry
│   ├── TrackScheduler.java     # Queue, skip, pause, auto-advance, cleanup
│   ├── YoutubeResolver.java    # yt-dlp search/download, local-file playback
│   ├── SpotifyResolver.java    # Spotify metadata lookup + autocomplete search
│   └── QueuedTrack.java        # A single queue entry
├── news/                       # Programming/neuroscience digest
│   ├── NewsScheduler.java      # Tue/Fri 10:00 CDMX scheduling
│   ├── NewsFetcher.java        # Assembles the 5-item digest
│   ├── HackerNewsSource.java / ArxivSource.java / PubMedSource.java
│   ├── NewsSummarizer.java     # Claude-written summaries
│   ├── NewsPoster.java         # Builds and posts embeds
│   └── NewsItem.java
├── commands/
│   ├── PingCommand.java
│   ├── ChannelCommand.java     # AI channel toggle (owner only)
│   ├── DownloadCommand.java
│   ├── NewsChannelCommand.java # News channel config (admin only)
│   ├── OwnerCommand.java       # Guild list / leave / force news (owner only)
│   ├── music/                  # PlayCommand, SkipCommand, PauseCommand, QueueCommand,
│   │                           # RemoveCommand, PriorityCommand, StopCommand
│   └── social/                 # SocialCommand (shared logic), HugCommand, KissCommand,
│                                # HitCommand, PatCommand
└── listeners/
    ├── SlashCommandListener.java
    ├── PrefixCommandListener.java
    ├── ButtonListener.java     # Return-action buttons on social commands
    ├── ArianListener.java      # Decides when Arian speaks and sends responses
    └── VoiceIdleListener.java  # Auto-disconnect after 7 idle minutes
```

## Tech stack

- [JDA 6](https://github.com/discord-jda/JDA) — Discord API wrapper for Java
- [Lavalink](https://github.com/lavalink-devs/Lavalink) — audio playback
- [yt-dlp](https://github.com/yt-dlp/yt-dlp) — song/video resolution and download
- [Claude Haiku](https://www.anthropic.com) — AI personality and news summaries
- [SQLite](https://www.sqlite.org) via `sqlite-jdbc` — persistent storage
- Maven with the Shade plugin — self-contained fat JAR
