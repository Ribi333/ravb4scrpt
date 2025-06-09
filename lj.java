String[] autoblocks = {"None", "Via", "Myau 1.8"};
int interactTicks = 0;
boolean blocked, blink = false;
List<CPacket> blinkPackets = Collections.synchronizedList(new ArrayList<>());
Entity target = modules.getKillAuraTarget();
Entity player = client.getPlayer();
int mode = (int) modules.getSlider(scriptName, "Autoblock");


void onLoad() {
    shitauthload();
}

void onEnable() {
    shitauthenable();
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
    modules.registerDescription("> KillAura:");
    modules.registerSlider("Autoblock", "", 0, autoblocks);
    modules.registerDescription("> Via is 7 aps, Myau 1.8 is 5 aps <");
    modules.registerButton("Switch KA ab to FAKE", false);
    modules.registerDescription("> Miscellaneous:");
    modules.registerButton("Selfban Command", true);
    modules.registerButton("Queue Commands", true);
    modules.registerDescription("made by @lquifi and @vivivox");
}

boolean holdingSword() {
    return player.getHeldItem() != null && player.getHeldItem().type.contains("Sword");
}

boolean isAuthorized() {
    String allowedUIDs = "420,383,392";
    String allowedUsers = "lquifi,drag,Marko1";

    String uidStr = String.valueOf(client.getUID());
    String userStr = client.getUser();

    boolean uidAllowed = Arrays.asList(allowedUIDs.split(",")).contains(uidStr);
    boolean userAllowed = Arrays.asList(allowedUsers.split(",")).contains(userStr);

    return uidAllowed && userAllowed;
}



void shitauthload() {
    if (isAuthorized()) {
        registerThingies();
    } else {
        modules.disable(scriptName);
        client.print("&7[&bL&7] &cYou are not authorized!");
    }
}

void shitauthenable() {
    if (!isAuthorized()) {
        modules.disable(scriptName);
        client.print("&7[&bL&7] &cYou are not authorized!");
    }
}



