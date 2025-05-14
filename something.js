import PogObject from "PogData";

const C0EPacketClickWindow = Java.type("net.minecraft.network.play.client.C0EPacketClickWindow");
const S2DPacketOpenWindow = Java.type("net.minecraft.network.play.server.S2DPacketOpenWindow");
const S2FPacketSetSlot = Java.type("net.minecraft.network.play.server.S2FPacketSetSlot");

// Initialize data storage
const dataObject = new PogObject("ZeroPingGUI", {
    enabled: false,
    debugMode: false,
    highPingMode: true,
    clickDelay: 0,
    timeout: 500,
    renderPredictions: true,
}, "zpgui.json");

let currentWindowId = -1;
let windowTitle = "";
let windowSize = 0;
let isInGUI = false;
let slotStates = [];
let pendingClicks = [];
let clicked = false;
let guiOpenedAt = 0;

register("command", (arg1, arg2) => {
    if (!arg1) {
        ChatLib.chat("&b[&3ZPGUI&b] Commands:");
        ChatLib.chat("&b/zpgui toggle - &3Toggle the module");
        ChatLib.chat("&b/zpgui debug - &3Toggle debug mode");
        ChatLib.chat("&b/zpgui highping - &3Toggle high-ping mode");
        ChatLib.chat("&b/zpgui render - &3Toggle click prediction rendering");
        ChatLib.chat("&b/zpgui delay <ms> - &3Set first click delay");
        ChatLib.chat("&b/zpgui timeout <ms> - &3Set click timeout");
        ChatLib.chat("&b/zpgui status - &3Show current settings");
        return;
    }

    switch (arg1.toLowerCase()) {
        case "toggle":
            dataObject.enabled = !dataObject.enabled;
            ChatLib.chat(`&b[&3ZPGUI&b] Module ${dataObject.enabled ? "&aenabled" : "&cdisabled"}.`);
            break;
        case "debug":
            dataObject.debugMode = !dataObject.debugMode;
            ChatLib.chat(`&b[&3ZPGUI&b] Debug mode ${dataObject.debugMode ? "&aenabled" : "&cdisabled"}.`);
            break;
        case "highping":
            dataObject.highPingMode = !dataObject.highPingMode;
            ChatLib.chat(`&b[&3ZPGUI&b] High-ping mode ${dataObject.highPingMode ? "&aenabled" : "&cdisabled"}.`);
            break;
        case "render":
            dataObject.renderPredictions = !dataObject.renderPredictions;
            ChatLib.chat(`&b[&3ZPGUI&b] Render predictions ${dataObject.renderPredictions ? "&aenabled" : "&cdisabled"}.`);
            break;
        case "delay":
            if (!arg2) {
                ChatLib.chat(`&b[&3ZPGUI&b] Current first click delay: &e${dataObject.clickDelay}ms`);
                return;
            }
            const delay = parseInt(arg2);
            if (isNaN(delay) || delay < 0) {
                ChatLib.chat("&c[&3ZPGUI&b] Invalid delay! Must be a positive number.");
                return;
            }
            dataObject.clickDelay = delay;
            ChatLib.chat(`&b[&3ZPGUI&b] First click delay set to &e${delay}ms`);
            break;
        case "timeout":
            if (!arg2) {
                ChatLib.chat(`&b[&3ZPGUI&b] Current click timeout: &e${dataObject.timeout}ms`);
                return;
            }
            const timeout = parseInt(arg2);
            if (isNaN(timeout) || timeout < 100) {
                ChatLib.chat("&c[&3ZPGUI&b] Invalid timeout! Must be at least 100ms.");
                return;
            }
            dataObject.timeout = timeout;
            ChatLib.chat(`&b[&3ZPGUI&b] Click timeout set to &e${timeout}ms`);
            break;
        case "status":
            ChatLib.chat("&b[&3ZPGUI&b] Status:");
            ChatLib.chat(`&b Module: ${dataObject.enabled ? "&aEnabled" : "&cDisabled"}`);
            ChatLib.chat(`&b Debug: ${dataObject.debugMode ? "&aEnabled" : "&cDisabled"}`);
            ChatLib.chat(`&b High-ping Mode: ${dataObject.highPingMode ? "&aEnabled" : "&cDisabled"}`);
            ChatLib.chat(`&b Render Predictions: ${dataObject.renderPredictions ? "&aEnabled" : "&cDisabled"}`);
            ChatLib.chat(`&b First Click Delay: &e${dataObject.clickDelay}ms`);
            ChatLib.chat(`&b Click Timeout: &e${dataObject.timeout}ms`);
            break;
        default:
            ChatLib.chat("&cUnknown command argument. Use &b/zpgui&c for help.");
    }
    dataObject.save();
}).setName("zpgui");

const clickHandler = register("guiMouseClick", (x, y, button, gui, event) => {
    if (!dataObject.enabled || !isInGUI) return;
    
    cancel(event);
    
    if (guiOpenedAt + dataObject.clickDelay > Date.now()) {
        if (dataObject.debugMode) {
            ChatLib.chat(`&b[&3ZPGUI&b] » First click delay, waiting...`);
        }
        return;
    }
    
    const slot = gui.getSlotUnderMouse();
    if (!slot) return;
    
    const slotId = slot.getSlotIndex();
    if (slotId < 0 || slotId >= windowSize) return;
    
    const item = slot.getStack();
    
    if (dataObject.highPingMode || !clicked) {
        predictSlotChange(slotId, button);
        
        if (dataObject.debugMode) {
            ChatLib.chat(`&b[&3ZPGUI&b] » Predicted click on slot ${slotId}`);
        }
    }
    
    if (dataObject.highPingMode && clicked) {
        pendingClicks.push([slotId, button]);
        
        if (dataObject.debugMode) {
            ChatLib.chat(`&b[&3ZPGUI&b] » Queued click on slot ${slotId}`);
        }
    } else {
        sendClick(slotId, button, item);
    }
}).unregister();

const renderHandler = register("renderOverlay", () => {
    if (!dataObject.enabled || !isInGUI || !dataObject.renderPredictions || pendingClicks.length === 0) return;
    
    if (dataObject.debugMode && pendingClicks.length > 0) {
        ChatLib.chat(`&b[&3ZPGUI&b] » Pending clicks: ${pendingClicks.length}`);
    }
}).unregister();

register("packetReceived", (packet, event) => {
    if (!dataObject.enabled) return;
    
    if (packet instanceof S2DPacketOpenWindow) {

        currentWindowId = packet.func_148901_c();
        windowTitle = packet.func_148902_e();
        windowSize = packet.func_148898_f();
        
        slotStates = [];
        pendingClicks = [];
        clicked = false;
        isInGUI = true;
        guiOpenedAt = Date.now();
        
        clickHandler.register();
        if (dataObject.renderPredictions) {
            renderHandler.register();
        }
        
        if (dataObject.debugMode) {
            ChatLib.chat(`&b[&3ZPGUI&b] » GUI opened: ID=${currentWindowId}, Title=${windowTitle}, Size=${windowSize}`);
        }
    }
});

    if (!isInGUI) return;
    
    isInGUI = false;
    pendingClicks = [];
    slotStates = [];
    
    clickHandler.unregister();
    renderHandler.unregister();
    
    if (dataObject.debugMode) {
        ChatLib.chat("&b[&3ZPGUI&b] » GUI closed");
    }
});

register("packetReceived", (packet, event) => {
    if (!dataObject.enabled || !isInGUI) return;
    
    if (packet instanceof S2FPacketSetSlot) {
        const slotId = packet.func_149173_d();
        const item = packet.func_149174_e();
        
        updateSlot(slotId, item);
        
        if (pendingClicks.length > 0) {
            const [nextSlot, nextButton] = pendingClicks[0];
            sendClick(nextSlot, nextButton, null);
            pendingClicks.shift();
            
            if (dataObject.debugMode) {
                ChatLib.chat(`&b[&3ZPGUI&b] » Processed queued click on slot ${nextSlot}`);
            }
        }
    }
});

function updateSlot(slotId, item) {
    if (slotId < 0 || !isInGUI) return;
    
    slotStates[slotId] = item ? {
        id: Item.fromMCItem(item).getID(),
        meta: Item.fromMCItem(item).getMetadata(),
        name: Item.fromMCItem(item).getName(),
    } : null;
}

function predictSlotChange(slotId, button) {
    // For now, we just mark that we've clicked
    // In a more advanced implementation, we could predict inventory changes
}

function sendClick(slotId, button, item) {
    if (!isInGUI || currentWindowId === -1) return;
    
    clicked = true;
    
    const clickType = button;
    const actionNumber = 0;
    const mode = 0;
    
    Client.sendPacket(new C0EPacketClickWindow(
        currentWindowId,
        slotId,
        clickType,
        mode,
        item || null,
        actionNumber
    ));
    
    if (dataObject.debugMode) {
        ChatLib.chat(`&b[&3ZPGUI&b] » Sent click packet: slot=${slotId}, button=${button}`);
    }
    
    const initialWindowId = currentWindowId;
    setTimeout(() => {
        if (!isInGUI || initialWindowId !== currentWindowId) return;
        
        pendingClicks = [];
        clicked = false;
        
        if (dataObject.debugMode) {
            ChatLib.chat(`&b[&3ZPGUI&b] » Click timeout reached, reset state`);
        }
    }, dataObject.timeout);
}

register("worldLoad", () => {
    dataObject.save();
});

ChatLib.chat("&b[&3ZPGUI&b] Module loaded! Use &b/zpgui toggle&3 to enable.");
