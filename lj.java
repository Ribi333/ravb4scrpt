String[] autoblocks = {"None", "Via", "Myau 1.8"};
int interactTicks = 0;
boolean blocked, blink = false;
List<CPacket> blinkPackets = Collections.synchronizedList(new ArrayList<>());
Entity target = modules.getKillAuraTarget();
Entity player = client.getPlayer();
int mode = (int) modules.getSlider(scriptName, "Autoblock");
//vertical tower
boolean aligningVT, alignedVT, VTplaced;
double firstX;
double VTmotiony;
boolean vvt, vtm;
boolean hasTowered, hasTowered2;
boolean dmg;
int dmgTicks, vtBdelay;

void onLoad() {
    registerThingies();
}

void onPreUpdate() {
    if (modules.getButton(scriptName, "Switch KA ab to FAKE")) {
        modules.setSlider("KillAura", "Autoblock", 3);
    }
    
    if (target == null) {
        if (blocked) {
        client.sendPacketNoEvent(new C07(new Vec3(0, 0, 0), "RELEASE_USE_ITEM", "DOWN"));
        blocked = false;
    }
        interactTicks = 0;
        return;
    }

if (mode == 1) { // VIA
    if (interactTicks >= 3) {
        interactTicks = 0;
    }

    interactTicks++;

    switch (interactTicks) {
        case 1:
            blink = true;

            if (blocked) {
                client.sendPacketNoEvent(new C07(new Vec3(0, 0, 0), "RELEASE_USE_ITEM", "DOWN"));
                blocked = false;
            }
            break;

        case 2:
            if (inAttackRange() && holdingSword()) {
                blinkPackets.add(new C02(modules.getKillAuraTarget(), "ATTACK", null));
                blinkPackets.add(new C02(modules.getKillAuraTarget(), "INTERACT", null));
            }

            if (holdingSword()) {
                blinkPackets.add(new C08(client.getPlayer().getHeldItem(), new Vec3(-1, -1, -1), 255, new Vec3(0, 0, 0)));
                blocked = true;
            }

            synchronized (blinkPackets) {
                for (CPacket packet : blinkPackets) {
                    client.sendPacketNoEvent(packet);
                }
            }

            blinkPackets.clear();
            blink = false;
            break;
    }

} else if (mode == 2) { // HYPIXEL (5 APS)
    if (interactTicks >= 4) {
        interactTicks = 0;
    }

    interactTicks++;

    switch (interactTicks) {
        case 1:
            blinkPackets.clear();
            if (blocked) {
                client.sendPacketNoEvent(new C07(new Vec3(0, 0, 0), "RELEASE_USE_ITEM", "DOWN"));
                blocked = false;
            }
            break;

        case 2:
            if (inAttackRange() && holdingSword()) {
                blinkPackets.add(new C02(modules.getKillAuraTarget(), "ATTACK", null));
                blinkPackets.add(new C02(modules.getKillAuraTarget(), "INTERACT", null));
            }

            if (holdingSword()) {
                blinkPackets.add(new C08(client.getPlayer().getHeldItem(), new Vec3(-1, -1, -1), 255, new Vec3(0, 0, 0)));
                blocked = true;
            }

            synchronized (blinkPackets) {
                for (CPacket packet : blinkPackets) {
                    client.sendPacketNoEvent(packet);
                }
            }

            blinkPackets.clear();
            break;
        }
    }
    double fixedPos = (int) pos.x;
    if (modules.getButton(scriptName, "Vertical Tower")) {
        if (getJumpLevel() == 0 && isTowering() && !isMoving() && !dmg) {
            if (!alignedVT) {
                if (player.onGround()) {
                    if (!aligningVT) firstX = fixedPos;
                    client.setMotion(0.22, motion.y, motion.z);
                    hasTowered2 = aligningVT = true;
                }
                if (aligningVT && fixedPos > firstX || alignedVT) {
                    alignedVT = true;
                }
                state.yaw = 90;
                state.pitch = 80;
            }
            if (alignedVT) {
                client.setMotion(0, VTmotiony, 0);
                if (VTplaced) {
                    state.yaw = 270;
                    state.pitch = 88;
                } else {
                    state.yaw = 90;
                    state.pitch = 80;
                }
            }
        } else {
            aligningVT = alignedVT = VTplaced = false;
            vtBdelay = 0;
        }
    }    
}

void onPreMotion() {
    //Damage check
    if (dmg && dmgTicks > 0) dmgTicks--;
    if (dmg && dmgTicks == 0) dmg = false;
}

boolean onPacketSent(CPacket packet) {
    if (packet instanceof C01) {
        C01 c01 = (C01) packet;
        String[] parts = c01.message.split(" ");

        if (c01.message.startsWith(".selfban") && modules.getButton(scriptName, "Selfban Command")) {
            client.print("Banning...");
            for (int i = 0; i < 20; i++) {
                client.sendPacketNoEvent(new C09(0));
                client.sendPacketNoEvent(new C07(new Vec3(-1, -2, -1), "RELEASE_USE_ITEM", "UP"));
            }
            return false;
        }
            switch (c01.message) {
            case "/1S":
                client.chat("/play bedwars_eight_one");
                return false;
            case "/2S":
                client.chat("/play bedwars_eight_two");
                return false;
            case "/3S":
                client.chat("/play bedwars_four_three");
                return false;
            case "/4S":
                client.chat("/play bedwars_four_four");
                return false;
            case "/1s":
                client.chat("/play bedwars_eight_one");
                return false;
            case "/2s":
                client.chat("/play bedwars_eight_two");
                return false;
            case "/3s":
                client.chat("/play bedwars_four_three");
                return false;
            case "/4s":
                client.chat("/play bedwars_four_four");
                return false;
            case "/sw":
                client.chat("/play solo_normal");
                return false;    
            case "/swi":
                client.chat("/play solo_insane");
                return false;   
            case "/SW":
                client.chat("/play solo_normal");
                return false;    
            case "/SWI":
                client.chat("/play solo_insane");
                return false;                                         
        }     
    }
    return true;
}
boolean inAttackRange() {
    if (modules.getKillAuraTarget() == null) return false;
    return client.getPlayer().getPosition().distanceTo(modules.getKillAuraTarget().getPosition()) <= 3.19;//3.39/3.11
}
void onDisable() {
    client.sendPacketNoEvent(new C07(new Vec3(0, 0, 0), "RELEASE_USE_ITEM", "DOWN"));
}
void registerThingies() {
    modules.registerDescription("* KillAura:");
    modules.registerSlider("Autoblock", "", 0, autoblocks);
    modules.registerDescription("> Via is 7 aps, Myau 1.8 is 5 aps <");
    modules.registerButton("Switch KA ab to FAKE", false);
    modules.registerDescription("* Scaffold:");
    modules.registerButton("Vertical Tower", false);
    modules.registerDescription("* Miscellaneous:");
    modules.registerButton("Selfban Command", true);
    modules.registerButton("Queue Commands", true);
    modules.registerDescription("made by @lquifi and @vivivox");
}
boolean holdingSword() {
    return player.getHeldItem() != null && player.getHeldItem().type.contains("Sword");
}
boolean isMoving() {
    if (keybinds.isKeyDown(17) || keybinds.isKeyDown(30) || keybinds.isKeyDown(31) || keybinds.isKeyDown(32)) {
        return true;
    }
    return false;
}
boolean isTowering() {
    return (isMoving() && keybinds.isKeyDown(57) && !modules.isEnabled("Long Jump"));
}
boolean isVerticallyTowering() {
    return keybinds.isKeyDown(57) && !isMoving();
}
int getJumpLevel() {
    for (Object[] effect : client.getPlayer().getPotionEffects()) {
        if (((String) effect[1]).contains("jump")) {
            return ((int) effect[2]) + 1;
        }
        return 0;
    }
    return 0;
}
