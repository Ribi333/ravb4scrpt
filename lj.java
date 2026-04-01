package keystrokesmod.module.impl.player;

import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.mixin.interfaces.IMixinItemRenderer;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemBlock;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.*;

public class BlockIn extends Module {

    private final SliderSetting delay;
    private final SliderSetting speed;
    private final ButtonSetting disableInCreative;
    private final ButtonSetting onlyWhileMiddleClicking;
    private final ButtonSetting prioritizeStrongerBlocks;
    private final ButtonSetting showPercentage;
    private final ButtonSetting silentAim;
    private final ButtonSetting silentSwing;
    private final ButtonSetting skipNearBed;
    private final KeySetting keybind;

    private final Map<String, Integer> BLOCK_SCORE = new HashMap<>();
    {
        BLOCK_SCORE.put("obsidian", 0);
        BLOCK_SCORE.put("end_stone", 1);
        BLOCK_SCORE.put("planks", 2);
        BLOCK_SCORE.put("log", 2);
        BLOCK_SCORE.put("log2", 2);
        BLOCK_SCORE.put("glass", 3);
        BLOCK_SCORE.put("stained_glass", 3);
        BLOCK_SCORE.put("hardened_clay", 4);
        BLOCK_SCORE.put("stained_hardened_clay", 4);
        BLOCK_SCORE.put("wool", 5);
        BLOCK_SCORE.put("sandstone", 6);
        BLOCK_SCORE.put("cobblestone", 6);
        BLOCK_SCORE.put("stone", 6);
    }

    private final Set<String> placeThrough = new HashSet<>(Arrays.asList(
            "air", "water", "lava", "fire"
    ));

    private final int[][] DIRS = {{1, 0, 0}, {0, 0, 1}, {-1, 0, 0}, {0, 0, -1}};

    private float serverYaw, serverPitch;
    private double filled;
    private Vec3 hitVec;
    private BlockPos hitPos;
    private EnumFacing face;
    private boolean pendingPlace;
    private boolean placingActive;
    private boolean skipTick;
    public int origSlot = -1;
    private int plannedPlaceSlot = -1;
    private int leftUnpressed;
    private int rightUnpressed;
    private boolean swapped;
    public boolean active;
    private boolean toggledOn = false;
    private int ticksSincePlace = 0;
    private float circleProgress = 0f;
    private float animStartProgress = 0f;
    private float animTargetProgress = 0f;
    private long animStartTime = 0L;
    private double lastFillCount = -1;
    private float aimYaw, aimPitch;
    private BlockPos targetHitPos;
    private EnumFacing targetSide;
    private boolean needsCenter = false;
    private double centerX, centerZ;
    private String skipDirection = null;
    private boolean keyWasDown = false;
    private int noTargetTicks = 0;

    public BlockIn() {
        super("BlockIn", category.player);
        this.registerSetting(delay = new SliderSetting("Delay", "ticks", 1, 0, 10, 1));
        this.registerSetting(speed = new SliderSetting("Speed", 8, 0, 100, 1));
        this.registerSetting(disableInCreative = new ButtonSetting("Disable in creative", true));
        this.registerSetting(onlyWhileMiddleClicking = new ButtonSetting("Only while middle clicking", false));
        this.registerSetting(prioritizeStrongerBlocks = new ButtonSetting("Prioritize stronger blocks", false));
        this.registerSetting(showPercentage = new ButtonSetting("Show percentage", true));
        this.registerSetting(silentAim = new ButtonSetting("Silent aim", true));
        this.registerSetting(silentSwing = new ButtonSetting("Silent swing", false));
        this.registerSetting(skipNearBed = new ButtonSetting("Skip near bed", true));
        this.registerSetting(keybind = new KeySetting("Current bind", Keyboard.KEY_NONE));
        this.closetModule = true;
    }

    @Override
    public void onEnable() {
        if (!Utils.nullCheck()) return;
        serverYaw = mc.thePlayer.rotationYaw;
        serverPitch = mc.thePlayer.rotationPitch;
        toggledOn = false;
        needsCenter = false;
        circleProgress = 0f;
    }

    @Override
    public void onDisable() {
        disablePlacing(true);
        toggledOn = false;
        keyWasDown = false;
        needsCenter = false;
        circleProgress = 0f;
        noTargetTicks = 0;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!Utils.nullCheck()) return;

        if (active) {
            ticksSincePlace++;
        }
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
        if (!showPercentage.isToggled()) return;
        if (filled <= 0 && !active) return;

        long elapsed = System.currentTimeMillis() - animStartTime;
        if (elapsed < 50L) {
            float t = (float) elapsed / 50f;
            float eased = quadInOutEasing(t);
            circleProgress = lerp(animStartProgress, animTargetProgress, eased);
        } else {
            circleProgress = animTargetProgress;
        }

        if (circleProgress <= 0.01f && !active) return;

        ScaledResolution sr = new ScaledResolution(mc);
        float cx = sr.getScaledWidth() / 2f - 1f;
        float cy = sr.getScaledHeight() / 2f;
        float radius = 10f;
        float thickness = 3f;

        drawCircle(cx, cy, radius, 100, thickness, 0f, 0f, 0f, 0.5f);

        if (circleProgress >= 0.999f) {
            drawCircle(cx, cy, radius, 100, thickness, 0f, 1f, 0f, 1f);
            return;
        }

        float startAngle = 90f;
        float endAngle = startAngle + circleProgress * 360f + 0.5f;
        float ratio = Math.max(0f, Math.min(1f, circleProgress));
        int r = (int) ((1f - ratio) * 255f + 0.5f);
        int g = (int) (ratio * 255f + 0.5f);
        int color = ((255 & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8);

        drawCircleArc(cx, cy, radius, startAngle, endAngle, thickness, color);
    }

    @SubscribeEvent
    public void onPacketSend(SendPacketEvent e) {
        if (e.getPacket() instanceof C03PacketPlayer.C05PacketPlayerLook ||
                e.getPacket() instanceof C03PacketPlayer.C06PacketPlayerPosLook) {
            C03PacketPlayer c03 = (C03PacketPlayer) e.getPacket();
            serverYaw = c03.getYaw();
            serverPitch = c03.getPitch();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onClientRotation(ClientRotationEvent e) {
        if (!Utils.nullCheck()) return;

        if (disableInCreative.isToggled() && mc.thePlayer.capabilities.isCreativeMode) {
            disablePlacing(true);
            toggledOn = false;
            return;
        }

        if (onlyWhileMiddleClicking.isToggled()) {
            boolean middleDown = Mouse.isButtonDown(2);
            if (middleDown && !toggledOn) {
                toggledOn = true;
                startPlacing();
            } else if (!middleDown && toggledOn) {
                toggledOn = false;
                disablePlacing(true);
                return;
            }
        } else {
            boolean keyDown = keybind.getKey() != 0 && Keyboard.isKeyDown(keybind.getKey());
            boolean keyJustPressed = keyDown && !keyWasDown;
            keyWasDown = keyDown;

            if (!toggledOn) {
                if (keyJustPressed || keyDown) {
                    toggledOn = true;
                    startPlacing();
                }
            } else {
                if (keyJustPressed) {
                    toggledOn = false;
                    disablePlacing(true);
                    return;
                }
            }
        }

        if (!toggledOn) return;

        leftUnpressed = Mouse.isButtonDown(0) ? 0 : leftUnpressed + 1;
        rightUnpressed = Mouse.isButtonDown(1) ? 0 : rightUnpressed + 1;

        if (mc.currentScreen != null) return;

        if (needsCenter) {
            moveToCenter();
            return;
        }

        if (skipNearBed.isToggled()) {
            skipDirection = findBedDirection();
        } else {
            skipDirection = null;
        }

        plannedPlaceSlot = pickBlockSlot();
        if (plannedPlaceSlot == -1) {
            disablePlacing(true);
            toggledOn = false;
            return;
        }

        if (!getTarget()) {
            noTargetTicks++;
            if (noTargetTicks > 4) {
                disablePlacing(true);
                toggledOn = false;
                noTargetTicks = 0;
            }
            return;
        } else {
            noTargetTicks = 0;
        }

        if (!placingActive) {
            if (enablePlacing()) return;
        }

        if (skipTick) {
            skipTick = false;
            return;
        }

        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);

        int currentSlot = mc.thePlayer.inventory.currentItem;
        if (plannedPlaceSlot != -1 && plannedPlaceSlot != currentSlot) {
            mc.thePlayer.inventory.currentItem = plannedPlaceSlot;
            swapped = true;
        }

        Float[] sm = getRotationsSmoothed(aimYaw, aimPitch);

        int delayTicks = (int) delay.getInput();
        boolean canPlace = delayTicks == 0 || ticksSincePlace > delayTicks;

        if (canPlace && targetHitPos != null) {
            double reach = 4.5;
            MovingObjectPosition chk = RotationUtils.rayCastBlock(reach, sm[0], sm[1]);

            if (chk != null && chk.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                BlockPos hit = chk.getBlockPos();
                EnumFacing side = chk.sideHit;
                if (hit.equals(targetHitPos) && side == targetSide) {
                    double tol = 25;
                    if (Math.abs(sm[0] - serverYaw) <= tol && Math.abs(sm[1] - serverPitch) <= tol) {
                        hitPos = hit;
                        face = side;
                        hitVec = chk.hitVec;
                        pendingPlace = true;
                        ticksSincePlace = 0;
                    }
                }
            }
        }

        if (silentAim.isToggled()) {
            e.yaw = sm[0];
            e.pitch = sm[1];
        } else {
            mc.thePlayer.rotationYaw = sm[0];
            mc.thePlayer.rotationPitch = sm[1];
            e.yaw = sm[0];
            e.pitch = sm[1];
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (active) {
            ((IMixinItemRenderer) mc.getItemRenderer()).setCancelUpdate(true);
            ((IMixinItemRenderer) mc.getItemRenderer()).setCancelReset(true);
        }

        if (pendingPlace) {
            pendingPlace = false;
            if (hitPos != null && face != null) {
                ItemStack held = mc.thePlayer.inventory.getStackInSlot(plannedPlaceSlot >= 0 ? plannedPlaceSlot : mc.thePlayer.inventory.currentItem);
                if (held != null && mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, held, hitPos, face, hitVec)) {
                    if (silentSwing.isToggled()) {
                        mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
                    } else {
                        mc.thePlayer.swingItem();
                    }
                }
            }
        }

        filled = 0;
        if (toggledOn && mc.currentScreen == null) {
            BlockPos feet = new BlockPos(Math.floor(mc.thePlayer.posX), Math.floor(mc.thePlayer.posY), Math.floor(mc.thePlayer.posZ));

            // Count roof block
            if (!canPlaceThrough(getBlockName(mc.theWorld.getBlockState(feet.up(2)).getBlock()))) filled++;

            // Count sides
            for (int[] d : DIRS) {
                String dirName = getDirectionName(d[0], d[2]);
                if (skipNearBed.isToggled() && dirName.equals(skipDirection)) continue;

                BlockPos posFeet = feet.add(d[0], 0, d[2]);
                if (!canPlaceThrough(getBlockName(mc.theWorld.getBlockState(posFeet).getBlock()))) filled++;

                BlockPos posHead = feet.add(d[0], 1, d[2]);
                if (!canPlaceThrough(getBlockName(mc.theWorld.getBlockState(posHead).getBlock()))) filled++;
            }

            int totalSpots = skipDirection != null ? 7 : 9;
            if (filled != lastFillCount) {
                animStartProgress = circleProgress;
                animTargetProgress = (float) Math.max(0f, Math.min(1f, filled / totalSpots));
                animStartTime = System.currentTimeMillis();
                lastFillCount = filled;
            }

            if (filled >= totalSpots) {
                disablePlacing(true);
                toggledOn = false;
            }
        }
    }

    private void startPlacing() {
        mc.thePlayer.motionX = 0;
        mc.thePlayer.motionZ = 0;
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);

        double px = mc.thePlayer.posX;
        double pz = mc.thePlayer.posZ;
        centerX = Math.floor(px) + 0.5;
        centerZ = Math.floor(pz) + 0.5;

        double distFromCenter = Math.sqrt(Math.pow(px - centerX, 2) + Math.pow(pz - centerZ, 2));
        needsCenter = distFromCenter > 0.15;

        ticksSincePlace = 100;
        lastFillCount = -1;
        animStartProgress = 0f;
        animTargetProgress = 0f;
        circleProgress = 0f;
        noTargetTicks = 0;
    }

    private void moveToCenter() {
        double px = mc.thePlayer.posX;
        double pz = mc.thePlayer.posZ;

        double dx = centerX - px;
        double dz = centerZ - pz;
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist < 0.05) {
            needsCenter = false;
            mc.thePlayer.motionX = 0;
            mc.thePlayer.motionZ = 0;
            return;
        }

        double moveSpeed = Math.min(0.15, dist);
        mc.thePlayer.motionX = (dx / dist) * moveSpeed;
        mc.thePlayer.motionZ = (dz / dist) * moveSpeed;
    }

    private boolean enablePlacing() {
        if (placingActive) return false;
        placingActive = true;
        if (leftUnpressed < 2 || rightUnpressed < 2) skipTick = true;
        swapped = false;
        active = true;
        origSlot = mc.thePlayer.inventory.currentItem;

        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        return true;
    }

    private void disablePlacing(boolean resetSlot) {
        if (!placingActive) return;

        if (resetSlot && swapped && origSlot != -1 && origSlot != mc.thePlayer.inventory.currentItem) {
            mc.thePlayer.inventory.currentItem = origSlot;
            if (active) {
                ((IMixinItemRenderer) mc.getItemRenderer()).setCancelUpdate(false);
                ((IMixinItemRenderer) mc.getItemRenderer()).setCancelReset(false);
            }
        }

        placingActive = false;
        swapped = false;
        skipTick = false;
        origSlot = -1;
        plannedPlaceSlot = -1;
        active = false;
        needsCenter = false;
    }

    private int pickBlockSlot() {
        int best = -1;
        int bestScore = prioritizeStrongerBlocks.isToggled() ? Integer.MAX_VALUE : -1;

        for (int slot = 8; slot >= 0; --slot) {
            ItemStack s = mc.thePlayer.inventory.getStackInSlot(slot);
            if (s == null || s.stackSize == 0 || !(s.getItem() instanceof ItemBlock)) continue;

            String name = getBlockName(((ItemBlock)s.getItem()).getBlock());
            Integer score = BLOCK_SCORE.get(name);
            if (score == null) score = 10;

            if (prioritizeStrongerBlocks.isToggled()) {
                if (score < bestScore) {
                    bestScore = score;
                    best = slot;
                    if (score == 0) break;
                }
            } else {
                if (best == -1 && BLOCK_SCORE.containsKey(name)) {
                    best = slot;
                }
            }
        }
        return best;
    }

    private boolean getTarget() {
        targetHitPos = null;
        targetSide = null;

        Object[] res = unifiedAim();

        if (res == null) return false;

        Object[] ray = (Object[]) res[0];
        targetHitPos = (BlockPos) ray[0];
        targetSide = (EnumFacing) ray[2];
        aimYaw = (float) res[1];
        aimPitch = (float) res[2];
        return true;
    }

    private Object[] unifiedAim() {
        if (mc.thePlayer == null) return null;

        BlockPos feet = new BlockPos(Math.floor(mc.thePlayer.posX), Math.floor(mc.thePlayer.posY), Math.floor(mc.thePlayer.posZ));
        BlockPos head = feet.up(1);
        BlockPos roof = feet.up(2);

        double reach = 4.5;
        Vec3 eye = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);

        ArrayList<BlockPos> primaryGoals = new ArrayList<>(9);

        // Roof goal
        if (canPlaceThrough(getBlockName(mc.theWorld.getBlockState(roof).getBlock()))) {
            primaryGoals.add(roof);
        }

        // Side goals
        for (int[] d : DIRS) {
            String dirName = getDirectionName(d[0], d[2]);
            if (skipNearBed.isToggled() && dirName.equals(skipDirection)) continue;

            BlockPos pFeet = feet.add(d[0], 0, d[2]);
            if (canPlaceThrough(getBlockName(mc.theWorld.getBlockState(pFeet).getBlock()))) primaryGoals.add(pFeet);

            BlockPos pHead = head.add(d[0], 0, d[2]);
            if (canPlaceThrough(getBlockName(mc.theWorld.getBlockState(pHead).getBlock()))) primaryGoals.add(pHead);
        }

        if (primaryGoals.isEmpty()) return null;

        return findBestForGoals(primaryGoals, reach, eye);
    }

    private Object[] findBestForGoals(List<BlockPos> goals, double reach, Vec3 eye) {
        if (goals == null || goals.isEmpty()) return null;

        float curYawW = normYaw(serverYaw), curPitch = serverPitch;

        MovingObjectPosition now = RotationUtils.rayCastBlock(reach, curYawW, curPitch);
        if (now != null && now.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos support = now.getBlockPos();
            EnumFacing faceHit = now.sideHit;

            if (!canPlaceThrough(getBlockName(mc.theWorld.getBlockState(support).getBlock()))) {
                for (BlockPos g : goals) {
                    Object[] ok = tryPlacement(reach, serverYaw, serverPitch, support, faceHit, g);
                    if (ok != null) return ok;
                }
            }
        }

        double INSET = 0.05, STEP = 0.2, JIT = 0.1, insetTop = 1 - INSET - 1e-3, insetBot = INSET + 1e-3;
        int GRID = (int) Math.round(1 / STEP);
        int cells = (GRID + 1) * (GRID + 1);
        ArrayList<Object[]> cands = new ArrayList<>(Math.max(16, goals.size() * 6 * cells));

        for (BlockPos g : goals) {
            for (EnumFacing offsetFace : EnumFacing.values()) {
                BlockPos support = g.offset(offsetFace);
                EnumFacing supportFace = offsetFace.getOpposite(); // The side of the support block we need to right click

                String supportName = getBlockName(mc.theWorld.getBlockState(support).getBlock());
                if (canPlaceThrough(supportName)) continue;

                for (int rr = 0; rr <= GRID; rr++) {
                    boolean ltr = (rr & 1) == 0;
                    double v = rr * STEP + Utils.randomizeDouble(-STEP * JIT, STEP * JIT);
                    if (v < 0) v = 0; else if (v > 1) v = 1;

                    for (int cc = 0; cc <= GRID; cc++) {
                        double cu = cc * STEP + Utils.randomizeDouble(-STEP * JIT, STEP * JIT);
                        if (cu < 0) cu = 0; else if (cu > 1) cu = 1;
                        double u = ltr ? cu : 1 - cu;

                        double px = support.getX();
                        double py = support.getY();
                        double pz = support.getZ();

                        switch (supportFace) {
                            case UP:    py += insetTop; px += u; pz += v; break;
                            case DOWN:  py += insetBot; px += u; pz += v; break;
                            case NORTH: pz += insetBot; px += u; py += v; break;
                            case SOUTH: pz += insetTop; px += u; py += v; break;
                            case WEST:  px += insetBot; pz += u; py += v; break;
                            case EAST:  px += insetTop; pz += u; py += v; break;
                        }

                        float[] rot = getRotationsWrapped(eye, px, py, pz);
                        float yawW = rot[0], pit = rot[1];

                        float dYaw = Math.abs(wrapYawDelta(curYawW, yawW));
                        float dPit = Math.abs(pit - curPitch);
                        if (dYaw < 0.1f && dPit < 0.1f) continue;

                        double cost = dYaw + dPit;
                        cands.add(new Object[]{cost, yawW, pit, support, supportFace, g});
                    }
                }
            }
        }

        if (cands.isEmpty()) return null;

        cands.sort((a, b) -> Double.compare((Double) a[0], (Double) b[0]));

        for (Object[] cand : cands) {
            float yawUnwrapped = unwrapYaw((Float) cand[1], serverYaw);
            float pit = (Float) cand[2];
            BlockPos support = (BlockPos) cand[3];
            EnumFacing supportFace = (EnumFacing) cand[4];
            BlockPos g = (BlockPos) cand[5];

            Object[] ok = tryPlacement(reach, yawUnwrapped, pit, support, supportFace, g);
            if (ok != null) return ok;
        }

        return null;
    }

    private Object[] tryPlacement(double reach, float yaw, float pit, BlockPos expectedSupport, EnumFacing expectedFace, BlockPos goal) {
        MovingObjectPosition ray = RotationUtils.rayCastBlock(reach, yaw, pit);
        if (ray == null || ray.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return null;
        BlockPos hitGrid = ray.getBlockPos();
        EnumFacing faceHit = ray.sideHit;
        if (!hitGrid.equals(expectedSupport)) return null;
        if (expectedFace != faceHit) return null;
        BlockPos plc = hitGrid.offset(faceHit);
        if (!plc.equals(goal)) return null;
        return new Object[]{new Object[]{hitGrid, ray.hitVec, faceHit}, yaw, pit};
    }

    private String findBedDirection() {
        if (mc.thePlayer == null || mc.theWorld == null) return null;

        BlockPos playerPos = new BlockPos(Math.floor(mc.thePlayer.posX), Math.floor(mc.thePlayer.posY), Math.floor(mc.thePlayer.posZ));
        for (int x = -3; x <= 3; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos checkPos = playerPos.add(x, y, z);
                    if (mc.theWorld.getBlockState(checkPos).getBlock() instanceof BlockBed) {
                        double dx = checkPos.getX() - playerPos.getX();
                        double dz = checkPos.getZ() - playerPos.getZ();
                        if (Math.abs(dx) > Math.abs(dz)) {
                            return dx > 0 ? "EAST" : "WEST";
                        } else {
                            return dz > 0 ? "SOUTH" : "NORTH";
                        }
                    }
                }
            }
        }
        return null;
    }

    private String getDirectionName(int x, int z) {
        if (x == 1) return "EAST";
        if (x == -1) return "WEST";
        if (z == 1) return "SOUTH";
        if (z == -1) return "NORTH";
        return "";
    }

    private Float[] getRotationsSmoothed(float targetYaw, float targetPitch) {
        float curYaw = serverYaw;
        float curPitch = serverPitch;

        float dYaw = targetYaw - curYaw;
        float dPit = targetPitch - curPitch;

        if (Math.abs(dYaw) < 0.1f) curYaw = targetYaw;
        if (Math.abs(dPit) < 0.1f) curPitch = targetPitch;
        if (curYaw == targetYaw && curPitch == targetPitch)
            return new Float[]{curYaw, curPitch};

        float maxStep = (float) speed.getInput();
        float random = 20;

        if (random > 0f) {
            float factor = 1f - (float) Utils.randomizeDouble(0, random / 100f);
            maxStep *= factor;
        }

        float stepYaw = Math.max(-maxStep, Math.min(maxStep, dYaw));
        float stepPit = Math.max(-maxStep, Math.min(maxStep, dPit));

        curYaw += stepYaw;
        curPitch += stepPit;

        if (Math.signum(targetYaw - curYaw) != Math.signum(dYaw)) curYaw = targetYaw;
        if (Math.signum(targetPitch - curPitch) != Math.signum(dPit)) curPitch = targetPitch;

        return new Float[]{curYaw, curPitch};
    }

    private String getBlockName(Block b) {
        if (b == null) return "air";
        ResourceLocation rl = Block.blockRegistry.getNameForObject(b);
        return rl == null ? "air" : rl.getResourcePath();
    }

    private boolean canPlaceThrough(String name) {
        if (name == null) return true;
        return placeThrough.contains(name.toLowerCase());
    }

    private float normYaw(float yaw) {
        yaw = ((yaw % 360f) + 360f) % 360f;
        return (yaw > 180f) ? (yaw - 360f) : yaw;
    }

    private float wrapYawDelta(float base, float target) {
        float d = target - base;
        while (d <= -180f) d += 360f;
        while (d > 180f) d -= 360f;
        return d;
    }

    private float unwrapYaw(float yaw, float prevYaw) {
        return prevYaw + ((((yaw - prevYaw + 180f) % 360f) + 360f) % 360f - 180f);
    }

    private float[] getRotationsWrapped(Vec3 eye, double tx, double ty, double tz) {
        double dx = tx - eye.xCoord, dy = ty - eye.yCoord, dz = tz - eye.zCoord;
        double hd = Math.sqrt(dx * dx + dz * dz);
        float yawWrapped = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        yawWrapped = normYaw(yawWrapped);
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, hd));
        return new float[]{yawWrapped, pitch};
    }

    private String key(BlockPos v) {
        return v.getX() + "|" + v.getY() + "|" + v.getZ();
    }

    private float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }

    private float quadInOutEasing(float t) {
        if (t < 0.5) return 2 * t * t;
        return -1 + (4 - 2 * t) * t;
    }

    private void drawCircle(float cx, float cy, float radius, int segments, float lineWidth, float r, float g, float b, float a) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(lineWidth);
        GL11.glColor4f(r, g, b, a);

        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i <= segments; i++) {
            double theta = 2 * Math.PI * i / segments;
            float x = (float) (radius * Math.cos(theta)) + cx;
            float y = (float) (radius * Math.sin(theta)) + cy;
            GL11.glVertex2f(x, y);
        }
        GL11.glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glLineWidth(1);
        GL11.glPopMatrix();
    }

    private void drawCircleArc(float cx, float cy, float radius, float startAngle, float endAngle, float lineWidth, int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(lineWidth);
        GL11.glColor4f(r, g, b, a);

        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (float angle = startAngle; angle <= endAngle; angle += 1) {
            double theta = Math.toRadians(angle + 180);
            float x = (float) (radius * Math.cos(theta)) + cx;
            float y = (float) (radius * Math.sin(theta)) + cy;
            GL11.glVertex2f(x, y);
        }
        GL11.glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glLineWidth(1);
        GL11.glPopMatrix();
    }

    @Override
    public String getInfo() {
        return active ? "Active" : "";
    }
}
