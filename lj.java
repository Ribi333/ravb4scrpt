package keystrokesmod.module.impl.player;

import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.mixin.interfaces.IMixinItemRenderer;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;

public class AutoHeadHitter extends Module {

    private final SliderSetting speed;
    private final ButtonSetting silentAim;
    private final ButtonSetting silentSwing;

    private float serverYaw, serverPitch;
    private float aimYaw, aimPitch;
    private BlockPos targetHitPos;
    private EnumFacing targetSide;
    private BlockPos hitPos;
    private EnumFacing face;
    private Vec3 hitVec;
    private boolean pendingPlace;
    
    private List<BlockPos> goals = new ArrayList<>();
    private int plannedSlot = -1;
    private int prevSlot = -1;
    private boolean placingActive = false;
    private boolean swapped = false;

    private final Set<String> placeThrough = new HashSet<>(Arrays.asList("air", "water", "lava", "fire"));

    public AutoHeadHitter() {
        super("AutoHeadHitter", category.player);
        this.registerSetting(speed = new SliderSetting("Rotation Speed", 15, 0, 100, 1));
        this.registerSetting(silentAim = new ButtonSetting("Silent aim", true));
        this.registerSetting(silentSwing = new ButtonSetting("Silent swing", false));
    }

    @Override
    public void onEnable() {
        if (!Utils.nullCheck()) return;
        serverYaw = mc.thePlayer.rotationYaw;
        serverPitch = mc.thePlayer.rotationPitch;
        
        // 1. Force a jump
        mc.thePlayer.jump();

        // 2. Generate the exact structure goals
        goals.clear();
        BlockPos base = new BlockPos(Math.floor(mc.thePlayer.posX), Math.floor(mc.thePlayer.posY), Math.floor(mc.thePlayer.posZ));
        EnumFacing forward = mc.thePlayer.getHorizontalFacing();
        
        BlockPos pillarBase = base.offset(forward, 3);
        
        goals.add(pillarBase);                   // Y
        goals.add(pillarBase.up(1));             // Y+1
        goals.add(pillarBase.up(2));             // Y+2
        goals.add(pillarBase.up(3));             // Y+3 (Top of pillar)
        goals.add(pillarBase.up(3).offset(forward.getOpposite(), 1)); // The head-hitting overhang (back towards player)

        prevSlot = mc.thePlayer.inventory.currentItem;
        placingActive = true;
    }

    @Override
    public void onDisable() {
        if (swapped && prevSlot != -1) {
            mc.thePlayer.inventory.currentItem = prevSlot;
            ((IMixinItemRenderer) mc.getItemRenderer()).setCancelUpdate(false);
            ((IMixinItemRenderer) mc.getItemRenderer()).setCancelReset(false);
        }
        placingActive = false;
        swapped = false;
        prevSlot = -1;
        targetHitPos = null;
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
        if (!Utils.nullCheck() || !placingActive) return;

        plannedSlot = pickBlockSlot();
        if (plannedSlot == -1 || checkFinished()) {
            this.disable();
            return;
        }

        if (plannedSlot != mc.thePlayer.inventory.currentItem) {
            mc.thePlayer.inventory.currentItem = plannedSlot;
            swapped = true;
        }

        Object[] target = getTarget();
        if (target != null) {
            targetHitPos = (BlockPos) target[0];
            aimYaw = (float) target[1];
            targetSide = (EnumFacing) target[2];
            aimPitch = (float) target[3];

            Float[] sm = getRotationsSmoothed(aimYaw, aimPitch);
            MovingObjectPosition chk = RotationUtils.rayCastBlock(5.0, sm[0], sm[1]);

            if (chk != null && chk.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                if (chk.getBlockPos().equals(targetHitPos) && chk.sideHit == targetSide) {
                    if (Math.abs(sm[0] - serverYaw) <= 25 && Math.abs(sm[1] - serverPitch) <= 25) {
                        hitPos = chk.getBlockPos();
                        face = chk.sideHit;
                        hitVec = chk.hitVec;
                        pendingPlace = true;
                    }
                }
            }

            if (silentAim.isToggled()) {
                e.yaw = sm[0]; e.pitch = sm[1];
            } else {
                mc.thePlayer.rotationYaw = sm[0]; mc.thePlayer.rotationPitch = sm[1];
                e.yaw = sm[0]; e.pitch = sm[1];
            }
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (placingActive) {
            ((IMixinItemRenderer) mc.getItemRenderer()).setCancelUpdate(true);
            ((IMixinItemRenderer) mc.getItemRenderer()).setCancelReset(true);
        }

        if (pendingPlace && hitPos != null && face != null) {
            pendingPlace = false;
            ItemStack held = mc.thePlayer.inventory.getStackInSlot(plannedSlot);
            if (held != null && mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, held, hitPos, face, hitVec)) {
                if (silentSwing.isToggled()) mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
                else mc.thePlayer.swingItem();
            }
        }
    }

    private boolean checkFinished() {
        for (BlockPos g : goals) {
            if (canPlaceThrough(getBlockName(mc.theWorld.getBlockState(g).getBlock()))) return false;
        }
        return true;
    }

    private Object[] getTarget() {
        Vec3 eye = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
        List<BlockPos> emptyGoals = new ArrayList<>();
        
        for (BlockPos g : goals) {
            if (canPlaceThrough(getBlockName(mc.theWorld.getBlockState(g).getBlock()))) emptyGoals.add(g);
        }
        
        if (emptyGoals.isEmpty()) return null;

        Object[] r = findBestForGoals(emptyGoals, 5.0, eye);
        if (r != null) return r;

        ArrayList<BlockPos> layer = new ArrayList<>(emptyGoals);
        for (int i = 0; i < 4 && !layer.isEmpty(); i++) {
            ArrayList<BlockPos> nextLayer = new ArrayList<>();
            for (BlockPos g : layer) {
                for (EnumFacing f : EnumFacing.values()) {
                    BlockPos s = g.offset(f);
                    if (canPlaceThrough(getBlockName(mc.theWorld.getBlockState(s).getBlock()))) nextLayer.add(s);
                }
            }
            if (!nextLayer.isEmpty()) {
                Object[] rLayer = findBestForGoals(nextLayer, 5.0, eye);
                if (rLayer != null) return rLayer;
            }
            layer = nextLayer;
        }
        return null;
    }

    private Object[] findBestForGoals(List<BlockPos> searchGoals, double reach, Vec3 eye) {
        float curYawW = normYaw(serverYaw);
        MovingObjectPosition now = RotationUtils.rayCastBlock(reach, curYawW, serverPitch);
        if (now != null && now.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            if (!canPlaceThrough(getBlockName(mc.theWorld.getBlockState(now.getBlockPos()).getBlock()))) {
                for (BlockPos g : searchGoals) {
                    Object[] ok = tryPlacement(reach, serverYaw, serverPitch, now.getBlockPos(), now.sideHit, g);
                    if (ok != null) return ok;
                }
            }
        }
        ArrayList<Object[]> cands = new ArrayList<>();
        double INSET = 0.05, STEP = 0.2, JIT = 0.1;
        int GRID = (int) Math.round(1 / STEP);

        for (BlockPos g : searchGoals) {
            for (EnumFacing offFace : EnumFacing.values()) {
                BlockPos support = g.offset(offFace);
                if (canPlaceThrough(getBlockName(mc.theWorld.getBlockState(support).getBlock()))) continue;
                for (int r = 0; r <= GRID; r++) {
                    double v = Math.min(1, Math.max(0, r * STEP + Utils.randomizeDouble(-STEP * JIT, STEP * JIT)));
                    for (int c = 0; c <= GRID; c++) {
                        double u = Math.min(1, Math.max(0, c * STEP + Utils.randomizeDouble(-STEP * JIT, STEP * JIT)));
                        double px = support.getX(), py = support.getY(), pz = support.getZ();
                        switch (offFace.getOpposite()) {
                            case UP: py += 1 - INSET; px += u; pz += v; break; case DOWN: py += INSET; px += u; pz += v; break;
                            case NORTH: pz += INSET; px += u; py += v; break; case SOUTH: pz += 1 - INSET; px += u; py += v; break;
                            case WEST: px += INSET; pz += u; py += v; break; case EAST: px += 1 - INSET; pz += u; py += v; break;
                        }
                        float[] rot = getRotationsWrapped(eye, px, py, pz);
                        double cost = Math.abs(wrapYawDelta(curYawW, rot[0])) + Math.abs(rot[1] - serverPitch);
                        if (cost > 0.1) cands.add(new Object[]{cost, rot[0], rot[1], support, offFace.getOpposite(), g});
                    }
                }
            }
        }
        cands.sort((a, b) -> Double.compare((Double) a[0], (Double) b[0]));
        for (Object[] cand : cands) {
            Object[] ok = tryPlacement(reach, unwrapYaw((Float) cand[1], serverYaw), (Float) cand[2], (BlockPos) cand[3], (EnumFacing) cand[4], (BlockPos) cand[5]);
            if (ok != null) return ok;
        }
        return null;
    }

    private Object[] tryPlacement(double reach, float yaw, float pit, BlockPos support, EnumFacing face, BlockPos goal) {
        MovingObjectPosition ray = RotationUtils.rayCastBlock(reach, yaw, pit);
        if (ray == null || ray.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return null;
        if (!ray.getBlockPos().equals(support) || ray.sideHit != face || !ray.getBlockPos().offset(face).equals(goal)) return null;
        return new Object[]{ray.getBlockPos(), yaw, ray.sideHit, pit};
    }

    private int pickBlockSlot() {
        for (int slot = 8; slot >= 0; --slot) {
            ItemStack s = mc.thePlayer.inventory.getStackInSlot(slot);
            if (s != null && s.stackSize > 0 && s.getItem() instanceof ItemBlock) return slot;
        }
        return -1;
    }

    private Float[] getRotationsSmoothed(float targetYaw, float targetPitch) {
        float maxStep = (float) speed.getInput() * (1f - (float) Utils.randomizeDouble(0, 0.2));
        float dYaw = targetYaw - serverYaw, dPit = targetPitch - serverPitch;
        return new Float[]{ serverYaw + Math.max(-maxStep, Math.min(maxStep, dYaw)), serverPitch + Math.max(-maxStep, Math.min(maxStep, dPit)) };
    }

    private String getBlockName(Block b) { return b == null ? "air" : Block.blockRegistry.getNameForObject(b).getResourcePath(); }
    private boolean canPlaceThrough(String n) { return n == null || placeThrough.contains(n.toLowerCase()); }
    private float normYaw(float yaw) { yaw = ((yaw % 360f) + 360f) % 360f; return yaw > 180f ? yaw - 360f : yaw; }
    private float wrapYawDelta(float base, float target) { float d = target - base; while (d <= -180f) d += 360f; while (d > 180f) d -= 360f; return d; }
    private float unwrapYaw(float yaw, float prevYaw) { return prevYaw + ((((yaw - prevYaw + 180f) % 360f) + 360f) % 360f - 180f); }
    private float[] getRotationsWrapped(Vec3 eye, double tx, double ty, double tz) {
        double dx = tx - eye.xCoord, dy = ty - eye.yCoord, dz = tz - eye.zCoord;
        return new float[]{ normYaw((float) Math.toDegrees(Math.atan2(dz, dx)) - 90f), (float) Math.toDegrees(-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))) };
    }
}
