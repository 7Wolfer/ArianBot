# ArianBot

A Discord bot for a furry community server, built with Java and JDA 5. ArianBot combines standard server utility commands with an AI-powered personality that participates in conversations on its own.

## What it does

ArianBot has two sides to it:

**Regular commands** — social interaction commands like hugs, kisses, hits and pats, each with GIF embeds and persistent counters stored in SQLite. These work via both slash commands and a text prefix (`a!`).

**AI personality (Arian)** — a Claude-powered character that reads the chat and joins conversations spontaneously. He responds to mentions, replies, and occasionally chimes in on his own. He adapts his tone to each person, remembers things about server members over time, and knows the current time in Mexico City.

## Requirements

- Java 21
- Maven
- A Discord bot token
- An Anthropic API key (for the AI personality)

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
DISCORD_TOKEN=your_token ANTHROPIC_API_KEY=your_key java -jar target/ArianBot-1.0-SNAPSHOT.jar
```

Both variables are required. The bot will not start without `DISCORD_TOKEN`, and the AI personality will be silently disabled without `ANTHROPIC_API_KEY`.

## Commands

### Social commands

All social commands are available as both slash commands (`/hug`) and prefix commands (`a!hug`). They generate a GIF embed and track interaction counts in the database.

| Command | Description |
|---------|-------------|
| `/hug @user` | Hug someone. Shows total hugs received. |
| `/kiss @user` | Kiss someone. Tracks kisses between that specific pair. |
| `/hit @user` | Hit someone. Tracks hits between that specific pair. |
| `/pat @user` | Pat someone. Shows total pats received. |
| `/ping` | Check if the bot is online and measure response time. |

Hug, kiss, and hit commands include a return button so the target can respond in kind.

### AI channel management (admin only)

| Command | Description |
|---------|-------------|
| `a!channel #channel` or `/channel #channel` | Toggle Arian on or off in that channel. |
| `a!channel` or `/channel` | List all channels where Arian is active. |

Arian only speaks in channels that have been explicitly enabled with this command. By default he is silent everywhere.

### Owner-only commands

These commands are locked to the bot owner's Discord user ID and are not visible to anyone else.

| Command | Description |
|---------|-------------|
| `a!guilds` | List all servers the bot is currently in. |
| `a!leave <server_id>` | Make the bot leave a specific server. |

## AI Personality

Arian is a white tiger furro character powered by Claude Haiku. He is not a command-response assistant — he behaves more like a server member who happens to be reading the chat.

**When he speaks:**
- Always when someone replies directly to one of his messages
- With ~80% chance when someone mentions him (`@Arian`) or writes his name
- With ~15% chance on any other message, subject to a 25-second cooldown per channel

**How he behaves:**
- Adapts his tone to each person — calm with calm people, sharp with rude ones
- Follows along with dark humor and absurd jokes instead of treating them seriously
- Occasionally flirty in a lighthearted way, stops if the person seems uncomfortable
- Knows Wolfer is his owner and will say so if asked
- Uses the current Mexico City time when time or date comes up in conversation
- If he has nothing relevant to say, shares a random curious fact instead of going silent

**Memory:**
Arian builds a short profile for each user over time. When someone shares something about themselves (a preference, a detail, something that happened), he stores it and uses it naturally in future conversations — without explicitly mentioning that he remembers.

**Response format:**
When Arian is mentioned or replied to, he responds using Discord's reply feature (the message is visually linked to the original) without sending a ping notification. Spontaneous messages are sent as plain channel messages.

## Database

The bot uses SQLite (`arian.db`) stored in the working directory. It creates the following tables automatically on startup:

- `pair_interactions` — tracks interactions between specific pairs of users (kiss, hit)
- `received_interactions` — tracks total interactions received by a user (hug, pat)
- `arian_channels` — stores which channels Arian is allowed to speak in
- `user_memory` — stores Arian's memory profile for each server member

## Project structure

```
src/main/java/com/arian/bot/
├── Main.java                        # Entry point, JDA setup, slash command registration
├── DataBaseManager.java             # SQLite connection and all database operations
├── ai/
│   ├── ArianAI.java                 # Anthropic API calls and response parsing
│   ├── ArianListener.java           # Decides when Arian speaks and sends responses
│   ├── ArianResponse.java           # Response model (text, emoji reaction, memory update)
│   └── ChannelContext.java          # Per-channel message history and cooldown tracking
├── commands/
│   ├── PingCommand.java
│   ├── ChannelCommand.java          # Channel enable/disable (admin only)
│   ├── OwnerCommand.java            # Guild list and leave (owner only)
│   └── social/
│       ├── SocialCommand.java       # Shared logic for all social commands
│       ├── HugCommand.java
│       ├── KissCommand.java
│       ├── HitCommand.java
│       └── PatCommand.java
└── listeners/
    ├── SlashCommandListener.java
    ├── PrefixCommandListener.java
    └── ButtonListener.java          # Handles return-action buttons on social commands
```

## Changing Arian's personality

All of Arian's personality, tone, interests, and behavior rules live in a single string in [`ArianAI.java`](src/main/java/com/arian/bot/ai/ArianAI.java), clearly marked with comments. You can edit it directly and restart the bot — no other files need to change.

## Tech stack

- [JDA 5](https://github.com/discord-jda/JDA) — Discord API wrapper for Java
- [Claude Haiku](https://www.anthropic.com) — AI model powering the personality
- [SQLite](https://www.sqlite.org) via `sqlite-jdbc` — persistent storage
- Maven with the Shade plugin for building a self-contained JAR
