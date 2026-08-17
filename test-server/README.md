# Local test server

This directory contains Paper 26.1.2 build 74 and is bound to `127.0.0.1:25565`.

## First start

1. Read the Minecraft EULA at https://aka.ms/MinecraftEULA.
2. If you accept it, change `eula=false` to `eula=true` in `eula.txt`.
3. Double-click `start-server.bat`. A CMD server console will stay visible.
4. Join `localhost:25565` from Minecraft Java Edition 26.1.2.
5. In the server console, run `op YourMinecraftName`.

The start script copies the latest locally built plugin JAR into `plugins/`
before every launch.

Use `stop` in the CMD console to save all worlds and shut down safely. Do not
close the window while the server is saving.

## World commands

```text
/wg create terrain_a 12345
/wg create terrain_b 67890 5000x5000
/wg create terrain_c random 10000x10000
/wg create hydro_5000 12345 5000x5000
/wg overview 12345 5000x5000
/wg list
/wg tp terrain_a
/wg tp terrain_b
/wg delete terrain_b confirm
/wg lobby
```

Press Tab after `/wg ` to complete subcommands. Press Tab after `/wg tp ` to
list and complete all currently loaded worlds.

`/wg overview` runs in the background and reports the absolute path when the PNG
is ready. On this test server the files are stored in
`plugins/WorldGenerator/overviews/`. Use the overview before creating a world to
compare seeds without flying across thousands of blocks.

Delete only accepts WorldGenerator worlds with no players inside. Deleted worlds
are moved to `plugins/WorldGenerator/trash/` so accidental deletion is recoverable.

Custom worlds are stored under the server's `lobby/dimensions/worldgenerator/`
directory by Paper 26.1.2.

For v0.8.5 water QA, always create a new world so old chunks are not reused.
Follow a river from its mountain source to the ocean and verify that its water
never climbs uphill, lakes widen an existing river instead of appearing as
isolated circles, gravel banks blend into nearby ground, and roads either avoid
water or cross it over a short reserved span.
