import PogObject from "PogData";

// Import necessary Java classes
const C02PacketUseEntity = Java.type("net.minecraft.network.play.client.C02PacketUseEntity");
const C0APacketAnimation = Java.type("net.minecraft.network.play.client.C0APacketAnimation");
const EntityAction = C02PacketUseEntity.Action;
const S19PacketEntityStatus = Java.type("net.minecraft.network.play.server.S19PacketEntityStatus");

// Initialize data storage
const dataObject = new PogObject("ZeroPingPvP", {
    enabled: false,
    debugMode: false,
    debug2Mode: false,
}, "zppvpData.json");

// Register the command and its subcommands
register("command", (arg) => {
    if (!arg) {
        ChatLib.chat("&b[&3ZPPVP&b] Commands:");
        ChatLib.chat("&b/zppvp toggle - &3Toggle the module");
        ChatLib.chat("&b/zppvp debug - &3Toggle debug mode");
        ChatLib.chat("&b/zppvp debug2 - &3Toggle advanced debug mode");
        return;
    }

    switch (arg.toLowerCase()) {
        case "toggle":
            dataObject.enabled = !dataObject.enabled;
            ChatLib.chat(`&b[&3ZPPVP&b] Module ${dataObject.enabled ? "&aenabled" : "&cdisabled"}.`);
            break;
        case "debug":
            dataObject.debugMode = !dataObject.debugMode;
            ChatLib.chat(`&b[&3ZPPVP&b] Debug mode ${dataObject.debugMode ? "&aenabled" : "&cdisabled"}.`);
            break;
        case "debug2":
            dataObject.debug2Mode = !dataObject.debug2Mode;
            ChatLib.chat(`&b[&3ZPPVP&b] Advanced debug mode ${dataObject.debug2Mode ? "&aenabled" : "&cdisabled"}.`);
            break;
        default:
            ChatLib.chat("&cUnknown command argument. Use &b/zppvp&c for help.");
    }
    dataObject.save();
}).setName("zppvp");

// Function to check if an entity is a valid target
const isValidTarget = (entity) => {
    if (!entity) return false;
    const playerName = Player.getName();
    if (entity.getName && entity.getName() === playerName) return false;
    return true;
};

// Improved approach: on attack, immediately show hit effect
register("clickMouse", (x, y, button, isPressed) => {
    if (!dataObject.enabled) return;
    
    // Only process left clicks when pressed
    if (button !== 0 || !isPressed) return;
    
    if (dataObject.debug2Mode) {
        ChatLib.chat("&b[&3ZPPVP&b] » Left click detected!");
    }

    // Apply hit effect immediately on any attack
    Player.getPlayer().field_70737_aN = 3;
    
    if (dataObject.debug2Mode) {
        ChatLib.chat("&b[&3ZPPVP&b] » Hit effects applied early!");
    }
});

// Register packet listener for attack packets (for debugging)
register("packetSent", (packet) => {
    if (!dataObject.enabled) return;

    if (dataObject.debugMode) {
        ChatLib.chat(`&b[&3DEBUG&b] Sent packet: ${packet.getClass().getSimpleName()}`);
    }

    if (packet instanceof C02PacketUseEntity) {
        if (dataObject.debug2Mode) {
            ChatLib.chat("&b[&3ZPPVP&b] » C02PacketUseEntity detected!");
        }
        
        try {
            // Try to get action type
            let action;
            try {
                action = packet.func_149565_c();
                if (dataObject.debug2Mode) {
                    ChatLib.chat(`&b[&3ZPPVP&b] » Action type: ${action}`);
                }
            } catch (e) {
                if (dataObject.debug2Mode) {
                    ChatLib.chat("&c[&3ZPPVP&b] » Could not get action type: " + e.message);
                }
            }
            
            if (action === EntityAction.ATTACK && dataObject.debug2Mode) {
                ChatLib.chat("&b[&3ZPPVP&b] » Attack action confirmed!");
            }
        } catch (e) {
            if (dataObject.debug2Mode) {
                ChatLib.chat(`&c[&3ZPPVP&b] » Error: ${e.message}`);
            }
        }
    }
});

// Handler for received packets
register("packetReceived", (packet, event) => {
    if (!dataObject.enabled) return;

    if (dataObject.debugMode) {
        ChatLib.chat(`&b[&3DEBUG&b] Received packet: ${packet.getClass().getSimpleName()}`);
    }

    if (packet instanceof S19PacketEntityStatus) {
        const status = packet.func_149160_c();
        if (status === 2) { // Hurt animation status
            if (dataObject.debug2Mode) {
                ChatLib.chat("&b[&3ZPPVP&b] » Hit confirmed by server!");
            }
            // Cancel server's animation since we already showed it
            cancel(event);
        }
    }
});

// Optional: Register a world load listener to ensure data is saved
register("worldLoad", () => {
    dataObject.save();
});
