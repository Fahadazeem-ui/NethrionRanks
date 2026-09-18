# NethrionRanks 1.1.0 — GUI Update

## What's New
- `/rankgui` — Opens a full in-game chest GUI dashboard
- All existing commands still work exactly as before
- GUI adapts per player rank/skill/state — no junk buttons

## GUI Screens

| Screen | How to open | Description |
|--------|-------------|-------------|
| Dashboard | `/rankgui` | Main hub — all actions in one place |
| My Profile | Click in dashboard | Full rank, skill XP, outlaw status |
| Rank Ladder | Click in dashboard | All National seat holders |
| Skills Board | Click in dashboard | Top players per skill |
| Admin Panel | Click (OP only) | Shortcut hints for admin commands |

## How to Build

**Requirements:** JDK 21+, Maven 3.8+

```bash
./BUILD.sh
```

Output: `target/NethrionRanks-1.1.0.jar`

Put that jar in `/plugins/`, remove the old one, restart.

## Manual Maven Build

```bash
# Step 1 — install original JAR to local Maven cache
mvn install:install-file \
  -Dfile=libs/NethrionRanks-original.jar \
  -DgroupId=com.nethrion \
  -DartifactId=NethrionRanks-original \
  -Dversion=1.0.0 \
  -Dpackaging=jar

# Step 2 — build
mvn clean package
```

## No data loss
Config, player profiles, PvP prefs, miner stats — all unchanged.
The new GUI reads the same data the commands use.
