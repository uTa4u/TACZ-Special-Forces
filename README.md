# TaCZ: Special Forces
An addon for [Timeless and Classics Guns Zero](https://github.com/MCModderAnchor/TACZ)

Adds [SWAT](https://en.wikipedia.org/wiki/SWAT) agents that will confront the player in various ways depending on the player's actions.

> [!WARNING]
> TaCZ:SF is still early in development, bugs (ranging from minor to major) are expected, but I do play-test the mod before releasing to minimize their amount. Some features such as different behaviours of agents and different tactics employed during missions are not implemented yet. Most of what is written below is a vision of the finished mod. As of right now the difference between specialties is their starting gear; difference between missions is specialties and their numbers participating in them.

### Download
- [Curseforge]()
- [Modrinth]()

### Specialties
- Commander (Summons the squad, equipped with an assault rifle and a pistol)
- Assaulter (Regular dude, equipped with an assault rifle and a pistol)
- Grenadier (Wields explosives, equipped with an explosion causing gun and a shotgun)
- Bulldozer (Slow and healthy, equipped with a machinegun and a pistol)
- Engineer (Can interact with redstone, mines and traps, equipped with a shotgun and a pistol)
- Sniper (Very accurate, equipped with a sniper rifle and a smg)
- Medic (Heals others, equipped with a smg and a pistol)
- Scout (Moves fast and jumps high, equipped with a shotgun and a pistol)
- Spy (Can disguise as other mobs (players if in multiplayer), equipped with a smg and a pistol)

### Missions
- Scouting (Observe player's actions to decide what mission should be started next)
- Rescue (Rescue entities observed during Scouting)
- Raid (Take blocks and items from containers observed during Scouting)
- Siege (Build defensive positions around the player's base and kill on sight)
- Arrest (Target player's respawn point)
- Sabotage (Destroy redstone contraptions, rails, place mines etc.)

### Customizability
All guns are packed with their respective ammo into a single loottable (located in `data/taczsf/loot_tables/guns`).
They are then split into 7 loottables by gun type (located in `data/taczsf/loot_tables/gun_types`).
Each agent has a loottable (located in `data/taczsf/loot_tanles/spawn_inv`) which consists of 2 rolls from `gun_types`. Additionally, to that they get some rolls from `heal_potion` and `food` loottables.
Each branch of this loottable tree can be modified with a [Datapack](https://minecraft.wiki/w/Data_pack).
Sadly it is impossible to add-on to a loottable without overriding it. TaCZ:SF implements a simple [GlobalLootModifier](https://docs.minecraftforge.net/en/1.20.1/resources/server/glm/) as a workaround. Example usage can be found in `data/taczsf/loot_modifiers/example_add_items.json`. Don't forget to add your loot modifier in `data/forge/loot_modifiers/global_loot_modifiers.json`!

### TODO
- Implement different goals and behaviours for agents depending on their specialty
- Implement different tactics for agents depending on their mission
