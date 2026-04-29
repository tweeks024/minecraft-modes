1. The jars already exist.


securitycore/build/libs/securitycore-0.1.0.jar
securityguard/build/libs/securityguard-0.1.0.jar
The Security Guard mod now requires the Security Core library mod. Both jars must be installed.

2. Install NeoForge in your Minecraft launcher.

Need NeoForge 26.1.2.30-beta (must match what we built against). Download the installer:


https://maven.neoforged.net/releases/net/neoforged/neoforge/26.1.2.30-beta/neoforge-26.1.2.30-beta-installer.jar
Run it (double-click, or java -jar neoforge-26.1.2.30-beta-installer.jar). Pick "Install client", point at your normal .minecraft folder, hit OK. This adds a neoforge-26.1.2.30-beta profile to your launcher.

3. Drop both jars in the mods folder.


mkdir -p ~/Library/Application\ Support/minecraft/mods
cp securitycore/build/libs/securitycore-0.1.0.jar ~/Library/Application\ Support/minecraft/mods/
cp securityguard/build/libs/securityguard-0.1.0.jar ~/Library/Application\ Support/minecraft/mods/
4. Launch.

Open Minecraft Launcher → switch profile to neoforge-26.1.2.30-beta → Play. Look for both "Security Core 0.1.0" and "Security Guard 0.1.0" in the Mods list at title screen.

Want me to run that copy step now?