float moveForward, moveStrafing, rotationYaw, yaw, pitch;
boolean swapped, swap, towering;
int swapdelay = 0, oldSlot, slot, inAirTicks;
Vec3 targetBlock, blockPos, hitVec; 
float[] rots;
Object[] block;
int[] blockInfo;
String side;
float[] crots;

void onLoad() {
    modules.registerButton("Swing", true);
}

void onEnable() {
    towering = false;
    oldSlot = inventory.getSlot();
    
    slot = grabBlockSlot();
    if (slot != -1) {
        inventory.setSlot(slot);
        swapped = true;
    }

    rots = null;
    blockInfo = null;
    if (!keybinds.isKeyDown(57)) {
        modules.disable("Scaffold");
    } else modules.enable("Scaffold");
}

void onDisable() {
    modules.disable("Scaffold");
    if (swapped) {
        inventory.setSlot(oldSlot);
        swapped = false;
    }

    rots = null;
    targetBlock = blockPos = null;
    blockInfo = null;
}

void onPreUpdate() {
    Entity player = client.getPlayer();
    Vec3 motion = client.getMotion();
    if (keybinds.isKeyDown(57)) {
        modules.enable("Scaffold");
        towering = true;
        return;
    } else {
        modules.disable("Scaffold");
        if (towering) {
            //client.print(player.getSpeed());
            client.setMotion(0, motion.y, 0);
            towering = false;
        }
    }

    if (client.getPlayer().onGround() && client.isMoving() && !modules.isEnabled("Bhop") && !modules.isEnabled("Bhop2")) {
        client.setSprinting(true);
        client.setSpeed(0.1785);
        client.jump();
        
    } 
    
    slot = grabBlockSlot();
    
    if (slot != -1) {
        if (swap) {
            swapdelay++;
        }
        if (swapdelay > 1) {
            inventory.setSlot(slot);
            swapped = true;
            swapdelay = 0;
        }
    }

    if (!player.onGround()) {
        if (inventory.getStackInSlot(inventory.getSlot()) != null && inventory.getStackInSlot(inventory.getSlot()).isBlock) {
            place();
            place();
            place();
        }
    }
}

boolean onPacketSent(CPacket packet) {
    if (packet instanceof C08) {
        C08 c08 = (C08) packet;
        if (c08.direction != 255) {
            swapdelay = 0;
            swap = true;
            Vec3 pos = c08.position;
            crots = client.getRotationsToBlock(pos);
        }
    }
    return true;
}

void onPreMotion(PlayerState event) {
    Entity player = client.getPlayer();
    Vec3 motion = client.getMotion(), pos = player.getPosition();
    rotationYaw = player.getYaw();
    inAirTicks = player.onGround() ? 0 : inAirTicks + 1;

    if (keybinds.isKeyDown(57)) return;
    
    if (!player.onGround()) {
        if (crots != null) {
            event.pitch = crots[1];
        }
        event.yaw = player.getYaw() - 180;
    }

    if (targetBlock != null) {
        rots = client.getRotationsToBlock(targetBlock, side);
        targetBlock = null;
    }
}

void onRenderTick(float partialTicks) {
    if (!client.getScreen().isEmpty()) {
        return;
    }
    if (rots != null) {
        
        rayCast(rots[0], rots[1]);
    }
    int blocks = totalBlocks();
    int[] displaySize = client.getDisplaySize();
    render.text(blocks + " block" + (blocks == 1 ? "" : "s"), displaySize[0] / 2 + 8, displaySize[1] / 2 + 4, 1, -1, true);
}


/*float[] getRotations() {
    if (inventory.getStackInSlot(inventory.getSlot()) != null && inventory.getStackInSlot(inventory.getSlot()).isBlock) {
        if (rots != null) {
            return new float[]{rots[0], rots[1]};
        }

        return new float[]{getDirection() + 180, 84};
    }
    return null;
}*/

void rayCast(float yaw, float pitch) {
    block = client.raycastBlock(4.0f, yaw, pitch);
    if (block != null) {
        blockPos = (Vec3) block[0];
        Block raycastedBlock = client.getWorld().getBlockAt(blockPos);
    }
}

void onPostPlayerInput() {
    moveForward = client.getForward();
    moveStrafing = client.getStrafe();
    
    if (modules.getButton(scriptName, "Hypixel")) {
        client.setSprinting(false);
    }
}

int totalBlocks() {
    int totalBlocks = 0;
    for (int i = 0; i < 9; ++i) {
        ItemStack stack = inventory.getStackInSlot(i);
        if (stack != null && stack.isBlock && stack.stackSize > 0) {
            totalBlocks += stack.stackSize;
        }
    }
    return totalBlocks;
}

int grabBlockSlot() {
    int slot = -1;
    int highestStack = -1;
    boolean didGetHotbar = false;
    ItemStack item;
    
    for (int i = 0; i < 9; i++) {
        item = inventory.getStackInSlot(i);
        if (item != null && item.isBlock && item.stackSize > 0) {
            if (item.stackSize > highestStack) {
                highestStack = item.stackSize;
                slot = i;
                if (slot == oldSlot) {
                    didGetHotbar = true;
                }
            }
        }
    }
    return slot;
}

Vec3 offsetPosition(int x, int y, int z, int facing) {
    switch (facing) {
        case 0: return new Vec3(x, y - 1, z);
        case 1: return new Vec3(x, y + 1, z);
        case 2: return new Vec3(x, y, z - 1);
        case 3: return new Vec3(x, y, z + 1);
        case 4: return new Vec3(x - 1, y, z);
        case 5: return new Vec3(x + 1, y, z);
        default: return new Vec3(x, y, z);
    }
}

int toOpposite(int facing) {
    switch (facing) {
        case 0: return 1;
        case 1: return 0;
        case 2: return 3;
        case 3: return 2;
        case 4: return 5;
        case 5: return 4;
        default: return facing;
    }
}

int[] findBlocks() {
    Entity player = client.getPlayer();
    World world = client.getWorld();
    int[] enumFacings = new int[]{0, 1, 2, 3, 4, 5};
    Vec3 playerPos = player.getPosition();
    int x = (int) Math.floor(playerPos.x);
    int y = (int) Math.floor(playerPos.y);
    int z = (int) Math.floor(playerPos.z);
    if (world.getBlockAt(x, y - 1, z).name.equals("air")) {
        for (int enumFacing : enumFacings) {
            if (enumFacing != 1) {
                Vec3 offsetPos = offsetPosition(x, y - 1, z, enumFacing);
                if (!world.getBlockAt((int) offsetPos.x, (int) offsetPos.y, (int) offsetPos.z).name.equals("air")) {
                    return new int[]{(int) offsetPos.x, (int) offsetPos.y, (int) offsetPos.z, toOpposite(enumFacing)};
                }
            }
        }
        for (int enumFacing : enumFacings) {
            if (enumFacing != 1) {
                Vec3 offsetPos1 = offsetPosition(x, y - 1, z, enumFacing);
                if (world.getBlockAt((int) offsetPos1.x, (int) offsetPos1.y, (int) offsetPos1.z).name.equals("air")) {
                    for (int enumFacing2 : enumFacings) {
                        if (enumFacing2 != 1) {
                            Vec3 offsetPos2 = offsetPosition((int) offsetPos1.x, (int) offsetPos1.y, (int) offsetPos1.z, enumFacing2);
                            if (!world.getBlockAt((int) offsetPos2.x, (int) offsetPos2.y, (int) offsetPos2.z).name.equals("air")) {
                                return new int[]{(int) offsetPos2.x, (int) offsetPos2.y, (int) offsetPos2.z, toOpposite(enumFacing2)};
                            }
                        }
                    }
                }
            }
        }
        for (int enumFacing : enumFacings) {
            if (enumFacing != 1) {
                Vec3 offsetPos1 = offsetPosition(x, y - 2, z, enumFacing);
                if (world.getBlockAt((int) offsetPos1.x, (int) offsetPos1.y, (int) offsetPos1.z).name.equals("air")) {
                    for (int enumFacing2 : enumFacings) {
                        if (enumFacing2 != 1) {
                            Vec3 offsetPos2 = offsetPosition((int) offsetPos1.x, (int) offsetPos1.y, (int) offsetPos1.z, enumFacing2);
                            if (!world.getBlockAt((int) offsetPos2.x, (int) offsetPos2.y, (int) offsetPos2.z).name.equals("air")) {
                                return new int[]{(int) offsetPos2.x, (int) offsetPos2.y, (int) offsetPos2.z, toOpposite(enumFacing2)};
                            }
                        }
                    }
                }
            }
        }
        for (int enumFacing : enumFacings) {
            if (enumFacing != 1) {
                Vec3 offsetPos1 = offsetPosition(x, y - 3, z, enumFacing);
                if (world.getBlockAt((int) offsetPos1.x, (int) offsetPos1.y, (int) offsetPos1.z).name.equals("air")) {
                    for (int enumFacing3 : enumFacings) {
                        if (enumFacing3 != 1) {
                            Vec3 offsetPos3 = offsetPosition((int) offsetPos1.x, (int) offsetPos1.y, (int) offsetPos1.z, enumFacing3);
                            if (!world.getBlockAt((int) offsetPos3.x, (int) offsetPos3.y, (int) offsetPos3.z).name.equals("air")) {
                                return new int[]{(int) offsetPos3.x, (int) offsetPos3.y, (int) offsetPos3.z, toOpposite(enumFacing3)};
                            }
                        }
                    }
                }
            }
        }
    }
    return null;
}


double getCoord(int facing, String axis) {
    switch (axis) {
        case "x": return (facing == 4) ? -0.5 : (facing == 5) ? 0.5 : 0;
        case "y": return (facing == 0) ? -0.5 : (facing == 1) ? 0.5 : 0;
        case "z": return (facing == 2) ? -0.5 : (facing == 3) ? 0.5 : 0;
        default: return 0;
    }
}

void locateBlocks() {
    blockInfo = findBlocks();
    
    if (blockInfo == null) {
        return;
    }

    int blockX = blockInfo[0];
    int blockY = blockInfo[1];
    int blockZ = blockInfo[2];
    int blockFacing = blockInfo[3];
    
    double hitX = (blockX + 0.5) + getCoord(blockFacing, "x") * 0.5;
    double hitY = (blockY + 0.5) + getCoord(blockFacing, "y") * 0.5;
    double hitZ = (blockZ + 0.5) + getCoord(blockFacing, "z") * 0.5;
    
    hitVec = new Vec3(hitX, hitY, hitZ);
    targetBlock = new Vec3(blockX, blockY, blockZ);
    side = getFace(blockFacing);
}

void place() {
    Entity player = client.getPlayer();
    Vec3 pos = player.getPosition();
    blockInfo = findBlocks();
    if (blockInfo == null) {
        return;
    }
    int blockFacing = blockInfo[3];
    side = getFace(blockFacing);
    if ("UP".equals(side)) {
        return;
    }

    blockPos = targetBlock;

    int blockX = blockInfo[0];
    int blockY = blockInfo[1];
    int blockZ = blockInfo[2];
    
    double hitX = (blockX + 0.5) + getCoord(blockFacing, "x") * 0.5;
    double hitY = (blockY + 0.5) + getCoord(blockFacing, "y") * 0.5;
    double hitZ = (blockZ + 0.5) + getCoord(blockFacing, "z") * 0.5;
    
    hitVec = new Vec3(hitX, hitY, hitZ);
    targetBlock = new Vec3(blockX, blockY, blockZ);

    client.placeBlock(targetBlock, side, hitVec);
    if (!modules.getButton(scriptName, "Swing")) {
        modules.setButton("Scaffold", "Silent swing", true);
        client.sendPacket(new C0A());
    } else {
        client.swing();
        modules.setButton("Scaffold", "Silent swing", false);
    }
}

float getDirection() {
    float yaw = rotationYaw;
    float forward = (moveForward > 0 ? 0.5F : moveForward < 0 ? -0.5F : 1);

    if (moveForward < 0) yaw += 180;
    if (moveStrafing > 0) yaw -= 110 * forward;
    if (moveStrafing < 0) yaw += 110 * forward;

    return yaw;
}

boolean isMoving() {
    if (keybinds.isKeyDown(17) || keybinds.isKeyDown(30) || keybinds.isKeyDown(31) || keybinds.isKeyDown(32)) { //check for  WASD as client.isMoving() only checks forward
        return true;
    }
    return false;
}

String getFace(int direction) {
    switch (direction) {
        case 2: return "NORTH";
        case 5: return "EAST";
        case 3: return "SOUTH";
        case 4: return "WEST";
        case 1: return "UP";
        case 0: return "DOWN";
        default: return "NORTH";
    }
}


double[] yawPos(float yaw, double value) {
    return new double[]{-Math.sin(Math.toRadians(yaw)) * value, Math.cos(Math.toRadians(yaw)) * value};
}
