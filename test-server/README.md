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
/wg create terrain_b 67890
/wg list
/wg tp terrain_a
/wg tp terrain_b
/wg lobby
```

Press Tab after `/wg ` to complete subcommands. Press Tab after `/wg tp ` to
list and complete all currently loaded worlds.

Custom worlds are stored under the server's `lobby/dimensions/worldgenerator/`
directory by Paper 26.1.2.
