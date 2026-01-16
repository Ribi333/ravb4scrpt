boolean blocking = false;
boolean attacking = false;
long attackStartTime = 0;
Entity currentTarget = null;
boolean waitingToBlock = false;
int ribisucksdick = -1; 

void onLoad() {
    modules.registerSlider("Mode", "", 0, new String[]{"Optibye", "Predict", "Legit"});
    modules.registerSlider("Block time", " ms", 100, 50, 250, 10);
    modules.registerSlider("Predict range", "", 3.5, 2.5, 4.5, 0.1);
    modules.registerButton("Dynamic block time", true);
    modules.registerButton("Debug", false);
}

void onDisable() {
    resetState();
}

void onPreUpdate() {
    int mode = (int) modules.getSlider(scriptName, "Mode");
    
    // Legit mode runs every tick
    if (mode == 2) {
        doLegitBlock();
    }
}

boolean onPacketSent(CPacket packet) {
    if (packet instanceof C02) {
        C02 c02 = (C02) packet;
        if (c02.action.equals("ATTACK") && hasSword()) {
            
            // Only trigger on KillAura attacks
            if (!modules.isEnabled("KillAura")) return true;
            
            Entity target = c02.entity;
            if (target == null) return true;
            
            int mode = (int) modules.getSlider(scriptName, "Mode");
            
            if (mode == 2) {
                // Legit mode - just set the counter
                ribisucksdick = 3;
            } else {
                // Other modes use async
                waitingToBlock = true;
                currentTarget = target;
                
                client.async(() -> {
                    client.sleep(50);
                    if (waitingToBlock) {
                        if (mode == 0) {
                            doOptibye();
                        } else if (mode == 1) {
                            startPredictBlock();
                        }
                    }
                });
            }
        }
    }
    return true;
}

boolean onMouse(int button, boolean state) {
    Entity player = client.getPlayer();
    if (player == null) return true;
    if (!client.getScreen().isEmpty()) return true;
    
    // Skip if KillAura is handling attacks
    if (modules.isEnabled("KillAura")) return true;
    
    if (button == 0 && state && hasSword()) {
        int mode = (int) modules.getSlider(scriptName, "Mode");
        
        double range = modules.getSlider(scriptName, "Predict range");
        Object[] raycast = client.raycastEntity(range);
        if (raycast == null || raycast[0] == null) return true;
        Entity target = (Entity) raycast[0];
        
        currentTarget = target;
        
        if (mode == 2) {
            // Legit mode
            ribisucksdick = 3;
        } else {
            client.async(() -> {
                client.sleep(50);
                if (mode == 0) {
                    doOptibye();
                } else if (mode == 1) {
                    startPredictBlock();
                }
            });
        }
    }
    
    return true;
}

// Optibye style blocking
void doOptibye() {
    if (!hasSword()) {
        resetState();
        return;
    }
    
    keybinds.setPressed("use", false);
    keybinds.setPressed("use", true);
    
    client.sleep(50);
    keybinds.setPressed("use", true);
    client.sleep(50);
    keybinds.setPressed("use", false);
    
    if (modules.getButton(scriptName, "Debug")) client.print("&a[Optibye] Block");
    
    resetState();
}

// Predict style blocking
void startPredictBlock() {
    Entity player = client.getPlayer();
    if (player == null || !hasSword()) {
        resetState();
        return;
    }
    
    attacking = true;
    attackStartTime = client.time();
    
    keybinds.setPressed("use", true);
    blocking = true;
    
    if (modules.getButton(scriptName, "Debug")) client.print("&a[Predict] Blocking");
    
    int baseBlockTime = (int) modules.getSlider(scriptName, "Block time");
    int blockTime = baseBlockTime;
    
    if (modules.getButton(scriptName, "Dynamic block time") && currentTarget != null) {
        double dist = player.getPosition().distanceTo(currentTarget.getPosition());
        if (dist < 2.5) blockTime += 50;
        if (dist < 2.0) blockTime += 25;
    }
    
    if (modules.getButton(scriptName, "Debug")) client.print("&e[Predict] Block time: " + blockTime + "ms");
    
    client.sleep(blockTime);
    
    keybinds.setPressed("use", false);
    blocking = false;
    
    if (modules.getButton(scriptName, "Debug")) client.print("&7[Predict] Unblocked");
    
    resetState();
}

// Legit mode blocking - runs every tick from onPreUpdate
void doLegitBlock() {
    if (ribisucksdick < 0) return; 
    ribisucksdick--;

    if (ribisucksdick == 1) { 
        keybinds.setPressed("use", true);
        if (modules.getButton(scriptName, "Debug")) client.print("&a[Legit] Blocking");
    }

    if (ribisucksdick == 0) {
        keybinds.setPressed("use", false);
        ribisucksdick = -1;
        if (modules.getButton(scriptName, "Debug")) client.print("&7[Legit] Unblocked");
    }
}

void resetState() {
    blocking = false;
    attacking = false;
    waitingToBlock = false;
    currentTarget = null;
    attackStartTime = 0;
    ribisucksdick = -1;
    keybinds.setPressed("use", false);
}

boolean hasSword() {
    Entity player = client.getPlayer();
    if (player == null) return false;
    ItemStack item = player.getHeldItem();
    return item != null && item.type.toLowerCase().contains("sword");
}
