package keystrokesmod.module.impl.player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AutoHeadHitter
extends Module {
    private static final double MAX_PLACE_REACH = 4.5;
    private static final double PLACE_RANGE_EPSILON = 0.05;
    private static final int FIRST_BLOCKS_REQUIRE_PRE_ROTATE = 2;
    private static final int PLACE_DELAY = 0;
    private static final int ANCHOR_WAIT_TIMEOUT = 4;
    private static final int PLACE_ATTEMPT_TIMEOUT = 6;
    private static final int PLAN_RETRY_TICKS = 2;
    private static final int MIN_FORWARD_SEARCH_BLOCKS = 3;
    private static final int MAX_FORWARD_SEARCH_BLOCKS = 8;
    private static final double FORWARD_SAMPLE_STEP = 0.35;
    private static final int MAX_CAP_EXTENSION_BLOCKS_STRAIGHT = 6;
    private static final double[][] LEGIT_FACE_SAMPLE_OFFSETS = new double[][]{{0.5, 0.5}, {0.35, 0.5}, {0.65, 0.5}, {0.5, 0.35}, {0.5, 0.65}, {0.35, 0.35}, {0.35, 0.65}, {0.65, 0.35}, {0.65, 0.65}};
    private static int placeDelay;
    private Phase phase;
    private final Deque<PlacementAction> actions = new ArrayDeque<PlacementAction>();
    private BlockPos capBlockPos;
    private boolean hasRotation;
    private float rotationYaw;
    private float rotationPitch;
    private EnumFacing lockedForward;
    private int originalSlot = -1;
    private int ticksInPhase;
    private int jumpAirTicks;
    private int underBlockAirTicks;
    private int underBlockGroundTicks;
    private int placementStallTicks;
    private int placeAttemptStallTicks;
    private boolean waitUnderJumpStarted;
    private PlacementAction rotatedAction;
    private int rotatedActionTick = -1;
    private Vec3 rotatedHitVec;
    private int placedBlocksThisBuild;
    private int nextPlanRetryTick = Integer.MIN_VALUE;
    private long cachedGroundColumnKey = Long.MIN_VALUE;
    private int cachedGroundY = Integer.MIN_VALUE;
    private int currentClientTick = Integer.MIN_VALUE;

    public AutoHeadHitter() {
        super("AutoHeadHitter", Module.category.player);
    }

    @Override
    public void onEnable() {
        this.resetState(false);
        this.phase = Phase.SIMULATE_AND_PLAN;
    }

    @Override
    public void onDisable() {
        this.resetState(true);
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (Utils.isLocalPlayerSubUpdate()) {
            return;
        }
        this.syncCurrentClientTick();
        if (!Utils.nullCheck() || mc.isGamePaused() || AutoHeadHitter.mc.thePlayer.isDead) {
            this.disable();
            return;
        }
        this.handleJumpingPreUpdate();
        if (!this.isEnabled()) {
            return;
        }
        if (this.phase == Phase.BUILD && !this.attemptPlaceCurrentAction()) {
            this.disable();
        }
    }

    @SubscribeEvent
    public void onClientRotation(ClientRotationEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (Utils.isLocalPlayerSubUpdate()) {
            return;
        }
        if (!Utils.nullCheck() || mc.isGamePaused() || AutoHeadHitter.mc.thePlayer.isDead) {
            this.disable();
            return;
        }
        ++this.ticksInPhase;
        switch (this.phase) {
            case SIMULATE_AND_PLAN: {
                if (!AutoHeadHitter.mc.thePlayer.onGround) {
                    this.disable();
                    return;
                }
                if (!this.prepareEarly()) {
                    this.disable();
                    return;
                }
                placeDelay = 0;
                this.phase = Phase.JUMP_START;
                this.ticksInPhase = 0;
                break;
            }
            case JUMP_START: {
                break;
            }
            case WAIT_FOR_LAST_TICK: {
                if (AutoHeadHitter.mc.thePlayer.onGround && this.ticksInPhase > 3) {
                    this.disable();
                    return;
                }
                this.releaseJumpKey();
                if (this.ticksInPhase < 2) {
                    // empty if block
                }
                if (++placeDelay < 0 || AutoHeadHitter.mc.thePlayer.ticksExisted < this.nextPlanRetryTick) break;
                if (this.buildPlanNow()) {
                    placeDelay = 0;
                    this.phase = Phase.BUILD;
                    this.ticksInPhase = 0;
                    this.placedBlocksThisBuild = 0;
                    this.placeAttemptStallTicks = 0;
                    this.clearRotatedActionState();
                    break;
                }
                this.nextPlanRetryTick = AutoHeadHitter.mc.thePlayer.ticksExisted + 2;
                break;
            }
            case BUILD: {
                if (!this.preparePlacementTick(event)) {
                    this.disable();
                    return;
                }
                if (!this.actions.isEmpty()) break;
                this.clearRotatedActionState();
                this.restoreOriginalSlot();
                this.placeAttemptStallTicks = 0;
                this.phase = Phase.WAIT_UNDER_AND_JUMP;
                this.ticksInPhase = 0;
                this.waitUnderJumpStarted = false;
                break;
            }
            case WAIT_UNDER_AND_JUMP: {
                break;
            }
            case WAIT_EXIT: {
                break;
            }
        }
        if (!this.isEnabled()) {
            return;
        }
        if (this.hasRotation && this.phase == Phase.BUILD) {
            event.setYaw(Float.valueOf(this.rotationYaw));
            event.setPitch(Float.valueOf(this.rotationPitch));
        }
    }

    private boolean prepareEarly() {
        this.actions.clear();
        this.hasRotation = false;
        this.lockedForward = this.resolveForward();
        if (this.lockedForward == null) {
            return false;
        }
        if (this.getBlockSlot() == -1) {
            return false;
        }
        if (this.originalSlot == -1) {
            this.originalSlot = AutoHeadHitter.mc.thePlayer.inventory.currentItem;
        }
        return true;
    }

    private boolean buildPlanNow() {
        this.actions.clear();
        this.hasRotation = false;
        this.placementStallTicks = 0;
        this.placeAttemptStallTicks = 0;
        this.cachedGroundColumnKey = Long.MIN_VALUE;
        this.cachedGroundY = Integer.MIN_VALUE;
        if (this.getBlockSlot() == -1) {
            return false;
        }
        List<PlacementAction> plan = this.buildHeadHitter();
        if (plan.isEmpty()) {
            return false;
        }
        this.actions.addAll(plan);
        this.capBlockPos = plan.get(plan.size() - 1).placePos;
        return true;
    }

    private int findGroundY() {
        int y;
        int z;
        int x = MathHelper.floor_double((double) AutoHeadHitter.mc.thePlayer.posX);
        long columnKey = new BlockPos(x, 0, z = MathHelper.floor_double((double) AutoHeadHitter.mc.thePlayer.posZ)).toLong();
        if (this.cachedGroundColumnKey == columnKey && this.cachedGroundY != Integer.MIN_VALUE) {
            return this.cachedGroundY;
        }
        for (int check = y = MathHelper.floor_double((double) AutoHeadHitter.mc.thePlayer.posY); check > 0; --check) {
            if (!this.isSupportAvailable(new BlockPos(x, check - 1, z))) continue;
            this.cachedGroundColumnKey = columnKey;
            this.cachedGroundY = check;
            return check;
        }
        this.cachedGroundColumnKey = columnKey;
        this.cachedGroundY = y;
        return y;
    }

    private List<PlacementAction> buildHeadHitter() {
        int groundY = this.findGroundY();
        BlockPos playerBlock = new BlockPos(MathHelper.floor_double((double) AutoHeadHitter.mc.thePlayer.posX), groundY, MathHelper.floor_double((double) AutoHeadHitter.mc.thePlayer.posZ));
        EnumFacing left = this.lockedForward.rotateYCCW();
        EnumFacing right = this.lockedForward.rotateY();
        AxisAlignedBB projectedPath = this.getProjectedForwardPathBox();
        for (BlockPos base : this.getForwardPathCandidates(playerBlock)) {
            EnumFacing primarySide;
            List<PlacementAction> plan = this.tryBuildOnSide(base, primarySide = this.chooseSaferSide(base, left, right), projectedPath);
            if (plan.isEmpty()) continue;
            return plan;
        }
        return new ArrayList<PlacementAction>();
    }

    private List<BlockPos> getForwardPathCandidates(BlockPos playerBlock) {
        ArrayList<BlockPos> candidates = new ArrayList<BlockPos>();
        long lastCandidateKey = Long.MIN_VALUE;
        float yawRad = (float)Math.toRadians(AutoHeadHitter.mc.thePlayer.rotationYaw);
        double vx = -MathHelper.sin((float)yawRad);
        double vz = MathHelper.cos((float)yawRad);
        double startX = (double)playerBlock.getX() + 0.5;
        double startZ = (double)playerBlock.getZ() + 0.5;
        for (double distance = 3.0; distance <= 8.000001; distance += 0.35) {
            int blockX = MathHelper.floor_double((double)(startX + vx * distance));
            int blockZ = MathHelper.floor_double((double)(startZ + vz * distance));
            BlockPos candidate = new BlockPos(blockX, playerBlock.getY(), blockZ);
            long candidateKey = candidate.toLong();
            if (candidateKey == lastCandidateKey) continue;
            candidates.add(candidate);
            lastCandidateKey = candidateKey;
        }
        return candidates;
    }

    private EnumFacing chooseSaferSide(BlockPos forwardBase, EnumFacing left, EnumFacing right) {
        int playerBlockX = MathHelper.floor_double((double) AutoHeadHitter.mc.thePlayer.posX);
        int playerBlockZ = MathHelper.floor_double((double) AutoHeadHitter.mc.thePlayer.posZ);
        double blockCenterX = (double)playerBlockX + 0.5;
        double blockCenterZ = (double)playerBlockZ + 0.5;
        double relX = AutoHeadHitter.mc.thePlayer.posX - blockCenterX;
        double relZ = AutoHeadHitter.mc.thePlayer.posZ - blockCenterZ;
        double lateral = relX * (double)left.getFrontOffsetX() + relZ * (double)left.getFrontOffsetZ();
        if (lateral > 0.0) {
            return right;
        }
        if (lateral < 0.0) {
            return left;
        }
        return this.deterministicSideTieBreak(left, right);
    }

    private EnumFacing deterministicSideTieBreak(EnumFacing left, EnumFacing right) {
        if (this.lockedForward == EnumFacing.NORTH || this.lockedForward == EnumFacing.SOUTH) {
            return left;
        }
        return right;
    }

    private AxisAlignedBB getProjectedForwardPathBox() {
        AxisAlignedBB current = AutoHeadHitter.mc.thePlayer.getEntityBoundingBox();
        float yawRad = (float)Math.toRadians(AutoHeadHitter.mc.thePlayer.rotationYaw);
        double forwardX = -MathHelper.sin((float)yawRad);
        double forwardZ = MathHelper.cos((float)yawRad);
        double sweepDistance = 7.5;
        AxisAlignedBB swept = current.offset(forwardX * sweepDistance, 0.0, forwardZ * sweepDistance);
        return current.union(swept).expand(0.05, 0.0, 0.05);
    }

    private void handleJumpingPreUpdate() {
        switch (this.phase) {
            case JUMP_START: {
                if (AutoHeadHitter.mc.thePlayer.onGround || this.jumpAirTicks < 2) {
                    this.tickJumpKey();
                }
                if (!AutoHeadHitter.mc.thePlayer.onGround) {
                    ++this.jumpAirTicks;
                }
                if (this.jumpAirTicks < 2) break;
                placeDelay = 0;
                this.phase = Phase.WAIT_FOR_LAST_TICK;
                this.ticksInPhase = 0;
                break;
            }
            case WAIT_UNDER_AND_JUMP: {
                if (this.capBlockPos == null) {
                    this.disable();
                    return;
                }
                if (!this.waitUnderJumpStarted) {
                    if (!AutoHeadHitter.mc.thePlayer.onGround) break;
                    this.waitUnderJumpStarted = true;
                    this.underBlockAirTicks = 0;
                    this.underBlockGroundTicks = 0;
                    break;
                }
                this.tickJumpKey();
                if (AutoHeadHitter.mc.thePlayer.onGround) {
                    ++this.underBlockGroundTicks;
                    this.underBlockAirTicks = 0;
                } else {
                    ++this.underBlockAirTicks;
                    this.underBlockGroundTicks = 0;
                }
                if (this.underBlockAirTicks < 4 && this.underBlockGroundTicks < 4) break;
                this.disable();
                break;
            }
        }
    }

    private List<PlacementAction> tryBuildOnSide(BlockPos forwardBlock, EnumFacing side, AxisAlignedBB projectedPath) {
        BlockPos pillarBase = forwardBlock.offset(side);
        BlockPos pillarMid = pillarBase.up();
        BlockPos pillarTop = pillarBase.up(2);
        if (!(this.isPlacementTargetAvailable(pillarBase) && this.isPlacementTargetAvailable(pillarMid) && this.isPlacementTargetAvailable(pillarTop))) {
            return new ArrayList<PlacementAction>();
        }
        PlacementAction first = this.findPlacementForSpecificPos(pillarBase);
        if (first == null) {
            return new ArrayList<PlacementAction>();
        }
        Vec3 midHit = this.getFaceCenterVec(pillarBase, EnumFacing.UP);
        Vec3 topHit = this.getFaceCenterVec(pillarMid, EnumFacing.UP);
        if (midHit == null || topHit == null) {
            return new ArrayList<PlacementAction>();
        }
        Vec3 eye = AutoHeadHitter.mc.thePlayer.getPositionEyes(1.0f);
        double midDistance = eye.distanceTo(midHit);
        double topDistance = eye.distanceTo(topHit);
        if (midDistance > 4.5 || topDistance > 4.5) {
            return new ArrayList<PlacementAction>();
        }
        EnumFacing capDirection = side.getOpposite();
        List<PlacementAction> capActions = this.buildCapActionsUntilOverPath(pillarTop, capDirection, projectedPath);
        if (capActions.isEmpty()) {
            return new ArrayList<PlacementAction>();
        }
        ArrayList<PlacementAction> plan = new ArrayList<PlacementAction>(3 + capActions.size());
        plan.add(first);
        plan.add(new PlacementAction(pillarMid, pillarBase, EnumFacing.UP, midHit));
        plan.add(new PlacementAction(pillarTop, pillarMid, EnumFacing.UP, topHit));
        plan.addAll(capActions);
        return plan;
    }

    private List<PlacementAction> buildCapActionsUntilOverPath(BlockPos pillarTop, EnumFacing capDirection, AxisAlignedBB projectedPath) {
        ArrayList<PlacementAction> capActions = new ArrayList<PlacementAction>();
        BlockPos anchor = pillarTop;
        Vec3 eye = AutoHeadHitter.mc.thePlayer.getPositionEyes(1.0f);
        int maxCapExtension = 6;
        for (int i = 0; i < maxCapExtension; ++i) {
            BlockPos cap = anchor.offset(capDirection);
            if (!this.isPlacementTargetAvailable(cap)) {
                return new ArrayList<PlacementAction>();
            }
            Vec3 capHit = this.getFaceCenterVec(anchor, capDirection);
            if (capHit == null) {
                return new ArrayList<PlacementAction>();
            }
            double capDistance = eye.distanceTo(capHit);
            if (capDistance > 4.5) {
                return new ArrayList<PlacementAction>();
            }
            capActions.add(new PlacementAction(cap, anchor, capDirection, capHit));
            if (this.isOverProjectedPath(cap, projectedPath)) {
                return capActions;
            }
            anchor = cap;
        }
        return new ArrayList<PlacementAction>();
    }

    private boolean isOverProjectedPath(BlockPos blockPos, AxisAlignedBB projectedPath) {
        double minX = blockPos.getX();
        double maxX = (double)blockPos.getX() + 1.0;
        double minZ = blockPos.getZ();
        double maxZ = (double)blockPos.getZ() + 1.0;
        return projectedPath.maxX > minX && projectedPath.minX < maxX && projectedPath.maxZ > minZ && projectedPath.minZ < maxZ;
    }

    private EnumFacing resolveForward() {
        float yawRad = (float)Math.toRadians(AutoHeadHitter.mc.thePlayer.rotationYaw);
        double vx = -MathHelper.sin((float)yawRad);
        double vz = MathHelper.cos((float)yawRad);
        return this.horizontalFacingFromVector(vx, vz);
    }

    private EnumFacing horizontalFacingFromVector(double x, double z) {
        EnumFacing best = EnumFacing.SOUTH;
        double bestDot = -1.7976931348623157E308;
        for (EnumFacing facing : new EnumFacing[]{EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST}) {
            double dot = x * (double)facing.getFrontOffsetX() + z * (double)facing.getFrontOffsetZ();
            if (!(dot > bestDot)) continue;
            bestDot = dot;
            best = facing;
        }
        return best;
    }

    private PlacementAction findPlacementForSpecificPos(BlockPos placePos) {
        if (!this.isPlacementTargetAvailable(placePos)) {
            return null;
        }
        Vec3 eyePos = AutoHeadHitter.mc.thePlayer.getPositionEyes(1.0f);
        PlacementAction bestAction = null;
        double bestDistance = Double.MAX_VALUE;
        for (EnumFacing face : EnumFacing.VALUES) {
            double distance;
            Vec3 hitVec;
            BlockPos anchorPos = placePos.offset(face.getOpposite());
            if (!this.isSupportAvailable(anchorPos) || (hitVec = this.getFaceCenterVec(anchorPos, face)) == null || (distance = eyePos.distanceTo(hitVec)) > 4.5 || !(distance < bestDistance)) continue;
            bestDistance = distance;
            bestAction = new PlacementAction(placePos, anchorPos, face, hitVec);
        }
        return bestAction;
    }

    private boolean preparePlacementTick(ClientRotationEvent event) {
        if (this.getBlockSlot() == -1) {
            return false;
        }
        this.hasRotation = false;
        PlacementAction current = this.actions.peekFirst();
        if (current == null) {
            this.clearRotatedActionState();
            this.placeAttemptStallTicks = 0;
            return true;
        }
        if (!this.isPlacementTargetAvailable(current.placePos)) {
            PlacementAction removed = this.actions.pollFirst();
            if (removed == this.rotatedAction) {
                this.clearRotatedActionState();
            }
            this.placementStallTicks = 0;
            this.placeAttemptStallTicks = 0;
            return true;
        }
        if (!this.isSupportAvailable(current.anchorPos)) {
            ++this.placementStallTicks;
            if (this.placementStallTicks > 4) {
                PlacementAction refreshed = this.refreshCurrentActionForPlacePos(current);
                this.placementStallTicks = 0;
                if (refreshed == null) {
                    return false;
                }
                this.placeAttemptStallTicks = 0;
            }
            return true;
        }
        if (!this.setRotationTo(current, event)) {
            ++this.placementStallTicks;
            if (this.placementStallTicks > 4) {
                PlacementAction refreshed = this.refreshCurrentActionForPlacePos(current);
                this.placementStallTicks = 0;
                if (refreshed == null) {
                    return false;
                }
                this.placeAttemptStallTicks = 0;
            }
            return true;
        }
        this.placementStallTicks = 0;
        if (current != this.rotatedAction) {
            this.rotatedAction = current;
            this.rotatedActionTick = AutoHeadHitter.mc.thePlayer.ticksExisted;
            this.placeAttemptStallTicks = 0;
        }
        return true;
    }

    private boolean attemptPlaceCurrentAction() {
        Vec3 hitVec;
        boolean requirePreRotateTick;
        if (Utils.isLocalPlayerSubUpdate()) {
            return true;
        }
        int slot = this.getBlockSlot();
        if (slot == -1) {
            return false;
        }
        Utils.switchSlot(slot, true);
        PlacementAction current = this.actions.peekFirst();
        if (current == null) {
            this.clearRotatedActionState();
            this.placeAttemptStallTicks = 0;
            return true;
        }
        if (!this.hasRotation || current != this.rotatedAction) {
            return true;
        }
        int currentTick = AutoHeadHitter.mc.thePlayer.ticksExisted;
        boolean bl = requirePreRotateTick = this.placedBlocksThisBuild < 2;
        if (requirePreRotateTick && currentTick <= this.rotatedActionTick) {
            return true;
        }
        Vec3 vec3 = hitVec = this.rotatedHitVec != null ? this.rotatedHitVec : current.hitVec;
        if (hitVec == null) {
            return true;
        }
        this.flushBlinkOutboundIfActive();
        boolean placed = Utils.placeBlockWithModuleSuppression(AutoHeadHitter.mc.thePlayer, AutoHeadHitter.mc.theWorld, AutoHeadHitter.mc.thePlayer.getHeldItem(), current.anchorPos, current.face, hitVec);
        if (placed) {
            AutoHeadHitter.mc.thePlayer.swingItem();
            PlacementAction placedAction = this.actions.pollFirst();
            ++this.placedBlocksThisBuild;
            this.placementStallTicks = 0;
            this.placeAttemptStallTicks = 0;
            if (placedAction == this.rotatedAction) {
                this.clearRotatedActionState();
            } else {
                this.hasRotation = false;
            }
            return true;
        }
        ++this.placeAttemptStallTicks;
        return this.placeAttemptStallTicks <= 6;
    }

    private void clearRotatedActionState() {
        this.rotatedAction = null;
        this.rotatedActionTick = -1;
        this.rotatedHitVec = null;
        this.hasRotation = false;
    }

    private Vec3 getFaceCenterVec(BlockPos blockPos, EnumFacing face) {
        if (blockPos == null || face == null) {
            return null;
        }
        double x = (double)blockPos.getX() + 0.5 + (double)face.getFrontOffsetX() * 0.5;
        double y = (double)blockPos.getY() + 0.5 + (double)face.getFrontOffsetY() * 0.5;
        double z = (double)blockPos.getZ() + 0.5 + (double)face.getFrontOffsetZ() * 0.5;
        return new Vec3(x, y, z);
    }

    private boolean setRotationTo(PlacementAction action, ClientRotationEvent event) {
        PlacementRayResult legitRay;
        float basePitch;
        if (action == null || action.anchorPos == null || action.face == null || action.placePos == null) {
            return false;
        }
        float baseYaw = event.yaw != null ? event.yaw.floatValue() : RotationUtils.serverRotations[0];
        float f = basePitch = event.pitch != null ? event.pitch.floatValue() : RotationUtils.serverRotations[1];
        if (event.yaw == null && event.pitch == null) {
            baseYaw = RotationUtils.serverRotations[0];
            basePitch = RotationUtils.serverRotations[1];
        }
        if ((legitRay = this.resolveLegitPlacementRay(action, baseYaw, basePitch)) == null || legitRay.hitVec == null) {
            this.rotatedHitVec = null;
            this.hasRotation = false;
            return false;
        }
        this.rotationYaw = legitRay.yaw;
        this.rotationPitch = legitRay.pitch;
        this.rotatedHitVec = legitRay.hitVec;
        this.hasRotation = true;
        return true;
    }

    private float[] getFixedRotationsForHitVec(Vec3 hitVec, float baseYaw, float basePitch) {
        double dx = hitVec.xCoord - AutoHeadHitter.mc.thePlayer.posX;
        double dy = hitVec.yCoord - (AutoHeadHitter.mc.thePlayer.posY + (double) AutoHeadHitter.mc.thePlayer.getEyeHeight());
        double dz = hitVec.zCoord - AutoHeadHitter.mc.thePlayer.posZ;
        float[] target = RotationUtils.getRotationsTo(dx, dy, dz, baseYaw, basePitch);
        return RotationUtils.fixRotation(target[0], target[1], baseYaw, basePitch);
    }

    private PlacementRayResult resolveLegitPlacementRay(PlacementAction action, float baseYaw, float basePitch) {
        if (action == null || action.anchorPos == null || action.face == null || action.placePos == null) {
            return null;
        }
        Vec3 eyePos = AutoHeadHitter.mc.thePlayer.getPositionEyes(1.0f);
        PlacementRayResult best = null;
        double bestScore = Double.MAX_VALUE;
        List<Vec3> hitCandidates = this.buildFaceSampleHitVecs(action.anchorPos, action.face, action.hitVec);
        for (Vec3 candidateHitVec : hitCandidates) {
            BlockPos tracedPlacePos;
            if (candidateHitVec == null || eyePos.distanceTo(candidateHitVec) > 4.55) continue;
            float[] fixed = this.getFixedRotationsForHitVec(candidateHitVec, baseYaw, basePitch);
            MovingObjectPosition mop = this.rayCastBlockDataFace(action.anchorPos, action.face, fixed[0], fixed[1], 4.55);
            if (mop == null || mop.hitVec == null || (tracedPlacePos = BlockUtils.offsetPos(mop)) == null || !BlockUtils.isBlockPosEqual(tracedPlacePos, action.placePos)) continue;
            double yawDelta = Math.abs(MathHelper.wrapAngleTo180_float((float)(fixed[0] - baseYaw)));
            double pitchDelta = Math.abs(fixed[1] - basePitch);
            double d = action.hitVec != null ? action.hitVec.squareDistanceTo(mop.hitVec) : 0.0;
            double centerDistanceSq = d;
            double score = yawDelta + pitchDelta + centerDistanceSq * 6.0;
            if (!(score + 1.0E-6 < bestScore)) continue;
            bestScore = score;
            best = new PlacementRayResult(fixed[0], fixed[1], mop.hitVec);
        }
        return best;
    }

    private MovingObjectPosition rayCastBlockDataFace(BlockPos blockPos, EnumFacing face, float yaw, float pitch, double reach) {
        if (blockPos == null || face == null) {
            return null;
        }
        MovingObjectPosition mop = RotationUtils.rayCastBlock(reach, yaw, pitch);
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || mop.getBlockPos() == null || mop.sideHit == null || mop.hitVec == null) {
            return null;
        }
        if (!BlockUtils.isBlockPosEqual(mop.getBlockPos(), blockPos) || mop.sideHit != face) {
            return null;
        }
        return mop;
    }

    private List<Vec3> buildFaceSampleHitVecs(BlockPos blockPos, EnumFacing face, Vec3 preferredHitVec) {
        ArrayList<Vec3> candidates = new ArrayList<Vec3>(LEGIT_FACE_SAMPLE_OFFSETS.length + 1);
        if (preferredHitVec != null) {
            candidates.add(preferredHitVec);
        }
        for (double[] sample : LEGIT_FACE_SAMPLE_OFFSETS) {
            Vec3 sampleVec;
            if (sample == null || sample.length < 2 || (sampleVec = this.getFaceSampleVec(blockPos, face, sample[0], sample[1])) == null) continue;
            candidates.add(sampleVec);
        }
        return candidates;
    }

    private Vec3 getFaceSampleVec(BlockPos blockPos, EnumFacing face, double axisA, double axisB) {
        if (blockPos == null || face == null) {
            return null;
        }
        double clampedA = MathHelper.clamp_double((double)axisA, (double)0.05, (double)0.95);
        double clampedB = MathHelper.clamp_double((double)axisB, (double)0.05, (double)0.95);
        double x = blockPos.getX();
        double y = blockPos.getY();
        double z = blockPos.getZ();
        switch (face) {
            case UP: {
                return new Vec3(x + clampedA, y + 1.0, z + clampedB);
            }
            case DOWN: {
                return new Vec3(x + clampedA, y, z + clampedB);
            }
            case NORTH: {
                return new Vec3(x + clampedA, y + clampedB, z);
            }
            case SOUTH: {
                return new Vec3(x + clampedA, y + clampedB, z + 1.0);
            }
            case EAST: {
                return new Vec3(x + 1.0, y + clampedB, z + clampedA);
            }
            case WEST: {
                return new Vec3(x, y + clampedB, z + clampedA);
            }
        }
        return null;
    }

    private PlacementAction refreshCurrentActionForPlacePos(PlacementAction current) {
        if (current == null || current.placePos == null) {
            return null;
        }
        PlacementAction refreshed = this.findPlacementForSpecificPos(current.placePos);
        if (refreshed == null) {
            return null;
        }
        PlacementAction replaced = this.actions.pollFirst();
        if (replaced == this.rotatedAction) {
            this.clearRotatedActionState();
        }
        this.actions.addFirst(refreshed);
        return refreshed;
    }

    private int getBlockSlot() {
        int current = AutoHeadHitter.mc.thePlayer.inventory.currentItem;
        ItemStack held = AutoHeadHitter.mc.thePlayer.getHeldItem();
        if (this.isUsableBlock(held)) {
            return current;
        }
        for (int slot = 0; slot < 9; ++slot) {
            ItemStack stack = AutoHeadHitter.mc.thePlayer.inventory.getStackInSlot(slot);
            if (!this.isUsableBlock(stack)) continue;
            return slot;
        }
        return -1;
    }

    private boolean isUsableBlock(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0 || !(stack.getItem() instanceof ItemBlock)) {
            return false;
        }
        Block block = ((ItemBlock)stack.getItem()).getBlock();
        if (block == null) {
            return false;
        }
        return Utils.canBePlaced((ItemBlock)stack.getItem()) && BlockUtils.isSolid(block);
    }

    private void tickJumpKey() {
        int key = AutoHeadHitter.mc.gameSettings.keyBindJump.getKeyCode();
        KeyBinding.setKeyBindState((int)key, (boolean)true);
        KeyBinding.onTick((int)key);
    }

    private void releaseJumpKey() {
        KeyBinding.setKeyBindState((int) AutoHeadHitter.mc.gameSettings.keyBindJump.getKeyCode(), (boolean)false);
    }

    private void resetJumpKeyState() {
        int key = AutoHeadHitter.mc.gameSettings.keyBindJump.getKeyCode();
        if (Utils.isBindDown(AutoHeadHitter.mc.gameSettings.keyBindJump)) {
            KeyBinding.setKeyBindState((int)key, (boolean)true);
            KeyBinding.onTick((int)key);
            return;
        }
        KeyBinding.setKeyBindState((int)key, (boolean)false);
    }

    private void restoreOriginalSlot() {
        if (this.originalSlot != -1 && AutoHeadHitter.mc.thePlayer != null) {
            Utils.switchSlot(this.originalSlot, true);
            this.originalSlot = -1;
        }
    }

    private void resetState(boolean restoreSlot) {
        this.actions.clear();
        this.clearRotatedActionState();
        this.capBlockPos = null;
        this.lockedForward = null;
        this.ticksInPhase = 0;
        this.jumpAirTicks = 0;
        this.underBlockAirTicks = 0;
        this.underBlockGroundTicks = 0;
        this.placementStallTicks = 0;
        this.placeAttemptStallTicks = 0;
        this.waitUnderJumpStarted = false;
        placeDelay = 0;
        this.nextPlanRetryTick = Integer.MIN_VALUE;
        this.placedBlocksThisBuild = 0;
        this.cachedGroundColumnKey = Long.MIN_VALUE;
        this.cachedGroundY = Integer.MIN_VALUE;
        this.currentClientTick = Integer.MIN_VALUE;
        this.resetJumpKeyState();
        this.phase = Phase.SIMULATE_AND_PLAN;
        if (restoreSlot) {
            this.restoreOriginalSlot();
        }
    }

    private void syncCurrentClientTick() {
        if (AutoHeadHitter.mc.thePlayer == null) {
            this.currentClientTick = Integer.MIN_VALUE;
            return;
        }
        this.currentClientTick = AutoHeadHitter.mc.thePlayer.ticksExisted;
    }

    private boolean isPlacementTargetAvailable(BlockPos pos) {
        return pos != null && BlockUtils.isReplaceable(pos);
    }

    private boolean isSupportAvailable(BlockPos pos) {
        if (pos == null || BlockUtils.isInteractable(pos)) {
            return false;
        }
        return !BlockUtils.isReplaceable(pos);
    }

    private void flushBlinkOutboundIfActive() {
        // our UnifiedLagHandler has no blink-outbound flush API; no-op
    }

    private static class PlacementRayResult {
        private final float yaw;
        private final float pitch;
        private final Vec3 hitVec;

        private PlacementRayResult(float yaw, float pitch, Vec3 hitVec) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.hitVec = hitVec;
        }
    }

    private static class PlacementAction {
        private final BlockPos placePos;
        private final BlockPos anchorPos;
        private final EnumFacing face;
        private final Vec3 hitVec;

        private PlacementAction(BlockPos placePos, BlockPos anchorPos, EnumFacing face, Vec3 hitVec) {
            this.placePos = placePos;
            this.anchorPos = anchorPos;
            this.face = face;
            this.hitVec = hitVec;
        }
    }

    private static enum Phase {
        SIMULATE_AND_PLAN,
        JUMP_START,
        WAIT_FOR_LAST_TICK,
        BUILD,
        WAIT_UNDER_AND_JUMP,
        WAIT_EXIT;

    }
}
