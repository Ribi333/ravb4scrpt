// Import settings from the settings.js file
import settings from "./settings";

// Import necessary Java classes
const C02PacketUseEntity = Java.type("net.minecraft.network.play.client.C02PacketUseEntity");
const C0APacketAnimation = Java.type("net.minecraft.network.play.client.C0APacketAnimation");
const EntityAction = C02PacketUseEntity.Action;
const S19PacketEntityStatus = Java.type("net.minecraft.network.play.server.S19PacketEntityStatus");

// Function to check if an entity is a valid target
const isValidTarget = (entity) => {
    if (!entity) return false;
    const playerName = Player.getName();
    if (entity.getName && entity.getName() === playerName) return false;
    return true;
};

// Improved approach: on attack, immediately show hit effect
register("clickMouse", (x, y, button, isPressed) => {
    const config = settings();
    if (!config.Enabled) return;
    
    // Only process left clicks when pressed
    if (button !== 0 || !isPressed) return;
    
    if (config.AdvancedDebug) {
        ChatLib.chat("§b[§3ZPPVP§b] » Left click detected!");
    }

    // Apply hit effect immediately on any attack
    Player.getPlayer().field_70737_aN = config.HitEffectDuration;
    
    if (config.AdvancedDebug) {
        ChatLib.chat("§b[§3ZPPVP§b] » Hit effects applied early!");
    }
});

// Register packet listener for attack packets (for debugging)
register("packetSent", (packet) => {
    const config = settings();
    if (!config.Enabled) return;

    if (config.DebugMode) {
        ChatLib.chat(`§b[§3DEBUG§b] Sent packet: ${packet.getClass().getSimpleName()}`);
    }

    if (packet instanceof C02PacketUseEntity) {
        if (config.AdvancedDebug) {
            ChatLib.chat("§b[§3ZPPVP§b] » C02PacketUseEntity detected!");
        }
        
        try {
            // Try to get action type
            let action;
            try {
                action = packet.func_149565_c();
                if (config.AdvancedDebug) {
                    ChatLib.chat(`§b[§3ZPPVP§b] » Action type: ${action}`);
                }
            } catch (e) {
                if (config.AdvancedDebug) {
                    ChatLib.chat("§c[§3ZPPVP§b] » Could not get action type: " + e.message);
                }
            }
            
            if (action === EntityAction.ATTACK && config.AdvancedDebug) {
                ChatLib.chat("§b[§3ZPPVP§b] » Attack action confirmed!");
            }
        } catch (e) {
            if (config.AdvancedDebug) {
                ChatLib.chat(`§c[§3ZPPVP§b] » Error: ${e.message}`);
            }
        }
    }
});

// Handler for received packets
register("packetReceived", (packet, event) => {
    const config = settings();
    if (!config.Enabled) return;

    if (config.DebugMode) {
        ChatLib.chat(`§b[§3DEBUG§b] Received packet: ${packet.getClass().getSimpleName()}`);
    }

    if (packet instanceof S19PacketEntityStatus) {
        const status = packet.func_149160_c();
        if (status === 2) { // Hurt animation status
            if (config.AdvancedDebug) {
                ChatLib.chat("§b[§3ZPPVP§b] » Hit confirmed by server!");
            }
            // Cancel server's animation since we already showed it
            if (config.CancelServerEffect) {
                cancel(event);
            }
        }
    }
});

// Register help command for legacy support
register("command", () => {
    const config = settings();
    ChatLib.chat("§1§l[§b§lZeroPing§9§lPVP§1§l]§r Commands:");
    ChatLib.chat("§b/zppvp settings §3- Open the settings GUI");
    ChatLib.chat(`§b/zppvp toggle §3- Toggle the module (currently ${config.Enabled ? "§aenabled" : "§cdisabled"})`);
}).setName("zppvp");

// Register setting sub-command
register("command", () => {
    ChatLib.command("settings zppvp", true);
}).setName("zppvpsettings");

// Register toggle sub-command
register("command", () => {
    const config = settings();
    config.Enabled = !config.Enabled;
    ChatLib.chat(`§1§l[§b§lZeroPing§9§lPVP§1§l]§r Module ${config.Enabled ? "§aenabled" : "§cdisabled"}.`);
}).setName("zppvptoggle");

// Register a step event to link to module key binds and detect config changes
register("step", () => {
    const config = settings();
    
    // Any per-tick logic can go here if needed
}).setFps(1);

// Initialization message
ChatLib.chat("§1§l[§b§lZeroPing§9§lPVP§1§l]§r Module loaded. Type §b/zppvp§r for commands or use §b/settings zppvp§r to open settings.");
