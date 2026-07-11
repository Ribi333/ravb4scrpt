package keystrokesmod.module.impl.player;

import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.GameTickEvent;
import keystrokesmod.event.PreInputEvent;
import keystrokesmod.event.PrePlayerInputEvent;
import keystrokesmod.helper.RotationHelper;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.script.model.Simulation;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class LadderClutch extends Module {
    private static final int LOOKAHEAD_TICKS = 20;
    private static final int GUIDE_TIMEOUT_TICKS = 26;
    private static final int GUIDE_INVALID_LIMIT = 4;
    private static final int STAGE_INVALID_TICK_LIMIT = 6;
    private static final int MIN_ENTRY_CONSECUTIVE_TICKS = 2;
    private static final double MIN_PLACE_BELOW_PLAYER_Y = 1.0;
    private static final double MAX_PLACE_BELOW_PLAYER_Y = 5.5;
    private static final double MAX_PLACE_XZ_DELTA = 4.5;
    private static final double REACH_EPSILON = 0.001;
    private static final double HITBOX_SAMPLE_EPSILON = 0.001;
    private static final double MAX_ENTRY_ALIGNMENT_ERROR_SQ = 0.36;
    private static final double SUPPORT_PLAN_PENALTY = 18.0;
    private static final double ENTRY_TICK_WEIGHT = 120.0;
    private static final double ENTRY_ALIGNMENT_WEIGHT = 90.0;
    private static final double TARGET_DISTANCE_WEIGHT = 10.0;
    private static final double[] HIT_OFFSETS = new double[]{0.5, 0.35, 0.65, 0.2, 0.8, 0.1, 0.9};
    private ExecutionStage stage = ExecutionStage.IDLE;
    private PlacementPlan activePlan;
    private int currentPlacementTick = Integer.MIN_VALUE;
    private int lastPlacementAttemptTick = Integer.MIN_VALUE;
    private int guideStartTick = Integer.MIN_VALUE;
    private int guideInvalidTicks;
    private int stageInvalidTicks;
    private int originalSlot = -1;
    private float stickyYaw;
    private float stickyPitch;
    private boolean hasStickyRotation;
    private boolean fallPlanCommitted;
    private String lastInvalidationReason = "idle";

    public LadderClutch() {
        super("Ladder Clutch", Module.category.player);
        this.closetModule = true;
    }

    @Override
    public void onDisable() {
        this.resetExecutionState(true, "disabled");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGameTick(GameTickEvent e) {
        this.currentPlacementTick = Utils.getTimerAwarePlacementTick();
        if (!this.isModuleReady()) {
            this.resetExecutionState(true, "module-not-ready");
            return;
        }
        if (!this.isCurrentlyFalling()) {
            this.fallPlanCommitted = false;
        }
        if (this.stage == ExecutionStage.IDLE) {
            if (!this.fallPlanCommitted && this.shouldStartPlanningNow()) {
                this.startExecutionPlan();
                this.fallPlanCommitted = true;
            }
            return;
        }
        if (this.activePlan == null) {
            this.resetExecutionState(true, "missing-plan");
            return;
        }
        if (!this.isPlanWithinPlacementBounds(this.activePlan)) {
            this.resetExecutionState(true, "plan-out-of-bounds");
            return;
        }
        if (LadderClutch.mc.thePlayer.onGround && !this.isInsideTargetLadder(this.activePlan.ladderPos)) {
            this.resetExecutionState(true, "grounded-away-from-ladder");
            return;
        }
        if (this.shouldAbortForMissingItems()) {
            this.resetExecutionState(true, "missing-required-item");
            return;
        }
        if (this.stage == ExecutionStage.PLAN_READY) {
            this.stage = this.activePlan.requiresSupportPlacement ? ExecutionStage.PLACE_SUPPORT : ExecutionStage.PLACE_LADDER;
        }
        this.prepareHeldItemForStage();
        if (this.stage == ExecutionStage.PLACE_SUPPORT && !BlockUtils.replaceable(this.activePlan.supportPlacePos)) {
            this.stage = ExecutionStage.PLACE_LADDER;
        }
        if ((this.stage == ExecutionStage.PLACE_LADDER || this.stage == ExecutionStage.GUIDE_ENTRY) && this.isInsideTargetLadder(this.activePlan.ladderPos)) {
            this.resetExecutionState(true, "ladder-occupied");
            return;
        }
        if (this.stage == ExecutionStage.PLACE_LADDER && this.isLadderBlock(this.activePlan.ladderPos)) {
            this.enterGuideState(this.currentPlacementTick);
            return;
        }
        if (this.stage == ExecutionStage.GUIDE_ENTRY) {
            this.tickGuideState();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onClientRotation(ClientRotationEvent e) {
        float basePitch;
        if (!this.isModuleReady()) {
            this.resetExecutionState(true, "module-not-ready");
            return;
        }
        if (this.stage == ExecutionStage.IDLE || this.activePlan == null) {
            this.hasStickyRotation = false;
            return;
        }
        if (!this.isPlanWithinPlacementBounds(this.activePlan)) {
            this.resetExecutionState(true, "plan-out-of-bounds");
            return;
        }
        float baseYaw = e.yaw != null ? e.yaw.floatValue() : LadderClutch.mc.thePlayer.rotationYaw;
        RotationHit hit = this.resolveRotationForCurrentStage(baseYaw, basePitch = e.pitch != null ? e.pitch.floatValue() : LadderClutch.mc.thePlayer.rotationPitch);
        if (hit == null) {
            float[] preAimRotations = this.resolvePreAimRotations(baseYaw, basePitch);
            if (preAimRotations != null) {
                this.stickyYaw = preAimRotations[0];
                this.stickyPitch = preAimRotations[1];
                this.hasStickyRotation = true;
                e.setRotationsAndLock(Float.valueOf(this.stickyYaw), Float.valueOf(this.stickyPitch));
                return;
            }
            if (!this.hasStickyRotation) {
                return;
            }
            e.setRotationsAndLock(Float.valueOf(this.stickyYaw), Float.valueOf(this.stickyPitch));
            return;
        }
        this.stickyYaw = hit.yaw;
        this.stickyPitch = hit.pitch;
        this.hasStickyRotation = true;
        e.setRotationsAndLock(Float.valueOf(hit.yaw), Float.valueOf(hit.pitch));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPreInput(PreInputEvent e) {
        float basePitch;
        if (!this.isModuleReady()) {
            this.resetExecutionState(true, "module-not-ready");
            return;
        }
        if (this.stage != ExecutionStage.PLACE_SUPPORT && this.stage != ExecutionStage.PLACE_LADDER) {
            return;
        }
        if (this.activePlan == null) {
            this.resetExecutionState(true, "missing-plan");
            return;
        }
        if (!this.isPlanStillValidForCurrentStage()) {
            ++this.stageInvalidTicks;
            if (this.stageInvalidTicks > 6) {
                this.resetExecutionState(true, "stage-invalid");
            }
            return;
        }
        this.stageInvalidTicks = 0;
        float baseYaw = this.getPlacementBaseYaw();
        RotationHit hit = this.resolveRotationForCurrentStage(baseYaw, basePitch = this.getPlacementBasePitch());
        if (hit == null && !this.isMovementAlignedForCurrentStage()) {
            return;
        }
        if (hit == null) {
            return;
        }
        int playerTick = this.getPlacementPlayerTick();
        if (playerTick == Integer.MIN_VALUE || this.lastPlacementAttemptTick == playerTick) {
            return;
        }
        if (PlacementTickArbiter.isClaimedByOther(playerTick, PlacementTickArbiter.CLAIM_CLUTCH)) {
            return;
        }
        if (!PlacementTickArbiter.tryClaim(playerTick, PlacementTickArbiter.CLAIM_CLUTCH)) {
            return;
        }
        ItemStack heldStack = this.resolveHeldStackForCurrentStage();
        if (heldStack == null) {
            PlacementTickArbiter.releaseIfOwner(playerTick, PlacementTickArbiter.CLAIM_CLUTCH);
            this.resetExecutionState(true, "no-stage-stack");
            return;
        }
        boolean placed = Utils.placeBlockWithModuleSuppression(LadderClutch.mc.thePlayer, LadderClutch.mc.theWorld, heldStack, hit.mop.getBlockPos(), hit.mop.sideHit, hit.mop.hitVec);
        if (!placed) {
            PlacementTickArbiter.releaseIfOwner(playerTick, PlacementTickArbiter.CLAIM_CLUTCH);
            return;
        }
        this.lastPlacementAttemptTick = playerTick;
        LadderClutch.mc.thePlayer.swingItem();
        mc.getItemRenderer().resetEquippedProgress();
        if (this.stage == ExecutionStage.PLACE_SUPPORT) {
            this.stage = ExecutionStage.PLACE_LADDER;
            return;
        }
        if (this.stage == ExecutionStage.PLACE_LADDER) {
            this.enterGuideState(playerTick);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPrePlayerInput(PrePlayerInputEvent e) {
        if (!this.isModuleReady() || this.activePlan == null || this.stage == ExecutionStage.IDLE) {
            return;
        }
        this.prepareHeldItemForStage();
        Vec3 movementTarget = this.getMovementTargetForStage(this.activePlan, this.stage);
        this.applyMovementOverride(e, movementTarget);
    }

    private void startExecutionPlan() {
        PlacementPlan plan = this.buildDeterministicPlan(LadderClutch.mc.thePlayer.rotationYaw, LadderClutch.mc.thePlayer.rotationPitch);
        if (plan == null) {
            this.resetExecutionState(true, "no-valid-plan");
            return;
        }
        this.activePlan = plan;
        this.stage = ExecutionStage.PLAN_READY;
        this.hasStickyRotation = false;
        this.guideStartTick = Integer.MIN_VALUE;
        this.guideInvalidTicks = 0;
        this.stageInvalidTicks = 0;
        this.lastInvalidationReason = "plan-ready";
    }

    private void tickGuideState() {
        int playerTick;
        if (this.activePlan == null || this.stage != ExecutionStage.GUIDE_ENTRY) {
            return;
        }
        if (LadderClutch.mc.thePlayer.isOnLadder() || this.isInsideTargetLadder(this.activePlan.ladderPos)) {
            this.resetExecutionState(true, "guide-success");
            return;
        }
        if (LadderClutch.mc.thePlayer.onGround) {
            this.resetExecutionState(true, "guide-grounded");
            return;
        }
        if (!this.isPlanWithinPlacementBounds(this.activePlan)) {
            this.resetExecutionState(true, "guide-plan-out-of-bounds");
            return;
        }
        if (!this.isLadderBlock(this.activePlan.ladderPos)) {
            this.resetExecutionState(true, "ladder-missing");
            return;
        }
        EntryEstimate estimate = this.estimateLadderEntry(this.activePlan.ladderPos, this.activePlan.ladderFace, null, 0);
        if (!estimate.valid()) {
            ++this.guideInvalidTicks;
            if (this.guideInvalidTicks > 4) {
                this.resetExecutionState(true, "guide-unreachable");
                return;
            }
        } else {
            this.guideInvalidTicks = 0;
        }
        if ((playerTick = this.getPlacementPlayerTick()) != Integer.MIN_VALUE && this.guideStartTick != Integer.MIN_VALUE && playerTick - this.guideStartTick > 26) {
            this.resetExecutionState(true, "guide-timeout");
        }
    }

    private void enterGuideState(int playerTick) {
        this.stage = ExecutionStage.GUIDE_ENTRY;
        this.guideStartTick = playerTick;
        this.guideInvalidTicks = 0;
        this.stageInvalidTicks = 0;
    }

    private RotationHit resolveRotationForCurrentStage(float baseYaw, float basePitch) {
        if (this.activePlan == null) {
            return null;
        }
        if (this.stage == ExecutionStage.PLAN_READY) {
            if (this.activePlan.requiresSupportPlacement) {
                return this.resolveSupportPlacementHit(this.activePlan, baseYaw, basePitch);
            }
            return this.resolveLadderPlacementHit(this.activePlan, baseYaw, basePitch);
        }
        if (this.stage == ExecutionStage.PLACE_SUPPORT) {
            return this.resolveSupportPlacementHit(this.activePlan, baseYaw, basePitch);
        }
        if (this.stage == ExecutionStage.PLACE_LADDER || this.stage == ExecutionStage.GUIDE_ENTRY) {
            return this.resolveLadderPlacementHit(this.activePlan, baseYaw, basePitch);
        }
        return null;
    }

    private float[] resolvePreAimRotations(float baseYaw, float basePitch) {
        Vec3 target;
        if (this.activePlan == null) {
            return null;
        }
        ExecutionStage aimStage = this.stage;
        if (aimStage == ExecutionStage.PLAN_READY) {
            aimStage = this.activePlan.requiresSupportPlacement ? ExecutionStage.PLACE_SUPPORT : ExecutionStage.PLACE_LADDER;
        }
        if ((target = this.getMovementTargetForStage(this.activePlan, aimStage)) == null) {
            return null;
        }
        return this.getFixedRotationsForVec(target, baseYaw, basePitch);
    }

    private RotationHit resolveSupportPlacementHit(PlacementPlan plan, float baseYaw, float basePitch) {
        if (plan == null || !plan.requiresSupportPlacement || plan.supportPlacePos == null || !BlockUtils.replaceable(plan.supportPlacePos)) {
            return null;
        }
        return this.findSupportBlockPlacementHit(plan.supportPlacePos, baseYaw, basePitch);
    }

    private RotationHit resolveLadderPlacementHit(PlacementPlan plan, float baseYaw, float basePitch) {
        if (plan == null || plan.ladderPos == null || plan.ladderSupportPos == null || plan.ladderFace == null) {
            return null;
        }
        if (!this.isReplaceableOrLadder(plan.ladderPos)) {
            return null;
        }
        if (!this.canUseAsLadderSupport(plan.ladderPos, plan.ladderFace, plan.ladderSupportPos)) {
            return null;
        }
        return this.findRotationHitForPlacement(plan.ladderPos, plan.ladderSupportPos, plan.ladderFace, baseYaw, basePitch);
    }

    private PlacementPlan buildDeterministicPlan(float baseYaw, float basePitch) {
        if (this.findLadderSlot() == -1) {
            return null;
        }
        int supportSlot = this.findSupportSlot();
        boolean canPlaceSupport = supportSlot != -1;
        int feetY = MathHelper.floor_double(this.getPlayerFeetY());
        int minX = MathHelper.floor_double(LadderClutch.mc.thePlayer.posX - 4.5);
        int maxX = MathHelper.floor_double(LadderClutch.mc.thePlayer.posX + 4.5);
        int minZ = MathHelper.floor_double(LadderClutch.mc.thePlayer.posZ - 4.5);
        int maxZ = MathHelper.floor_double(LadderClutch.mc.thePlayer.posZ + 4.5);
        PlacementPlan best = null;
        for (int y = feetY - 1; y >= feetY - 5; --y) {
            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) {
                    BlockPos ladderPos = new BlockPos(x, y, z);
                    if (!this.isWithinPlacementBounds(ladderPos) || !this.isReplaceableOrLadder(ladderPos)) continue;
                    PlacementPlan directPlan = this.buildDirectLadderPlanForCell(ladderPos);
                    best = this.pickBetterPlan(best, directPlan);
                    if (!canPlaceSupport) continue;
                    PlacementPlan supportPlan = this.buildSupportThenLadderPlanForCell(ladderPos);
                    best = this.pickBetterPlan(best, supportPlan);
                }
            }
        }
        return best;
    }

    private PlacementPlan buildDirectLadderPlanForCell(BlockPos ladderPos) {
        PlacementPlan best = null;
        for (EnumFacing ladderFace : EnumFacing.HORIZONTALS) {
            EntryEstimate estimate;
            BlockPos ladderSupportPos = ladderPos.offset(ladderFace.getOpposite());
            if (!this.isWithinPlacementBounds(ladderSupportPos) || !this.canUseAsLadderSupport(ladderPos, ladderFace, ladderSupportPos) || !(estimate = this.estimateLadderEntry(ladderPos, ladderFace, null, 1)).valid()) continue;
            double steerError = this.computeSteerError(this.getLadderSteerTarget(ladderPos, ladderFace));
            double planCost = this.computePlanCost(estimate.tickAhead, estimate.entryAlignmentErrorSq, this.getHorizontalDistanceSq(ladderPos), false);
            PlacementPlan candidate = PlacementPlan.direct(ladderPos, ladderSupportPos, ladderFace, estimate.tickAhead, steerError, planCost);
            best = this.pickBetterPlan(best, candidate);
        }
        return best;
    }

    private PlacementPlan buildSupportThenLadderPlanForCell(BlockPos ladderPos) {
        PlacementPlan best = null;
        for (EnumFacing ladderFace : EnumFacing.HORIZONTALS) {
            EntryEstimate estimate;
            BlockPos supportPlacePos = ladderPos.offset(ladderFace.getOpposite());
            if (!this.isWithinPlacementBounds(supportPlacePos) || !BlockUtils.replaceable(supportPlacePos) || BlockUtils.doesPlayerIntersectPlacementNowOrNextTick(supportPlacePos) || !this.hasSupportPlacementAnchor(supportPlacePos) || !(estimate = this.estimateLadderEntry(ladderPos, ladderFace, supportPlacePos, 2)).valid()) continue;
            double steerError = this.computeSteerError(this.getLadderSteerTarget(ladderPos, ladderFace));
            double planCost = this.computePlanCost(estimate.tickAhead, estimate.entryAlignmentErrorSq, this.getHorizontalDistanceSq(ladderPos), true);
            PlacementPlan candidate = PlacementPlan.withSupportPlacement(ladderPos, supportPlacePos, ladderFace, null, null, estimate.tickAhead, steerError, planCost);
            best = this.pickBetterPlan(best, candidate);
        }
        return best;
    }

    private PlacementPlan pickBetterPlan(PlacementPlan currentBest, PlacementPlan candidate) {
        if (candidate == null) {
            return currentBest;
        }
        if (currentBest == null) {
            return candidate;
        }
        if (Math.abs(candidate.planCost - currentBest.planCost) > 1.0E-6) {
            return candidate.planCost < currentBest.planCost ? candidate : currentBest;
        }
        return candidate.ladderPos.toLong() < currentBest.ladderPos.toLong() ? candidate : currentBest;
    }

    private double computePlanCost(int entryTick, double entryAlignmentErrorSq, double horizontalDistanceSq, boolean requiresSupportPlacement) {
        double supportPenalty = requiresSupportPlacement ? 18.0 : 0.0;
        return (double)entryTick * 120.0 + entryAlignmentErrorSq * 90.0 + horizontalDistanceSq * 10.0 + supportPenalty;
    }

    private RotationHit findSupportBlockPlacementHit(BlockPos placePos, float baseYaw, float basePitch) {
        RotationHit best = null;
        for (EnumFacing placeFace : EnumFacing.VALUES) {
            RotationHit hit;
            BlockPos anchorPos;
            if (placeFace == EnumFacing.DOWN || !this.isAnchorBlockValid(anchorPos = placePos.offset(placeFace.getOpposite())) || (hit = this.findRotationHitForPlacement(placePos, anchorPos, placeFace, baseYaw, basePitch)) == null || best != null && !(hit.score < best.score)) continue;
            best = hit;
        }
        return best;
    }

    private RotationHit findRotationHitForPlacement(BlockPos placePos, BlockPos supportPos, EnumFacing supportFace, float baseYaw, float basePitch) {
        if (placePos == null || supportPos == null || supportFace == null || LadderClutch.mc.thePlayer == null) {
            return null;
        }
        if (!this.isWithinReachBounds(placePos, supportPos)) {
            return null;
        }
        Vec3i faceDirection = supportFace.getDirectionVec();
        Vec3 supportFaceCenter = new Vec3((double)supportPos.getX() + 0.5 + (double)faceDirection.getX() * 0.5, (double)supportPos.getY() + 0.5 + (double)faceDirection.getY() * 0.5, (double)supportPos.getZ() + 0.5 + (double)faceDirection.getZ() * 0.5);
        RotationHit best = null;
        double reach = this.getPlacementReach();
        Vec3 eye = LadderClutch.mc.thePlayer.getPositionEyes(1.0f);
        for (double u : HIT_OFFSETS) {
            for (double v : HIT_OFFSETS) {
                float[] fixedRotations;
                MovingObjectPosition traced;
                Vec3 hitVec = this.buildFaceHitVec(supportPos, supportFace, u, v);
                if (hitVec == null || eye.distanceTo(hitVec) > reach || !this.isValidPlacementRay(traced = RotationUtils.rayCastBlock(reach + 0.001, (fixedRotations = this.getFixedRotationsForHitVec(hitVec, baseYaw, basePitch))[0], fixedRotations[1]), placePos, supportPos, supportFace)) continue;
                double yawDelta = Math.abs(MathHelper.wrapAngleTo180_float(fixedRotations[0] - baseYaw));
                double pitchDelta = Math.abs(fixedRotations[1] - basePitch);
                double centerPenalty = supportFaceCenter.squareDistanceTo(traced.hitVec);
                double score = yawDelta + pitchDelta + centerPenalty * 4.0;
                if (best != null && !(score < best.score)) continue;
                best = new RotationHit(fixedRotations[0], fixedRotations[1], traced, score);
            }
        }
        return best;
    }

    private boolean isValidPlacementRay(MovingObjectPosition mop, BlockPos expectedPlacePos, BlockPos expectedSupportPos, EnumFacing expectedFace) {
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || mop.getBlockPos() == null || mop.sideHit == null || mop.hitVec == null || expectedPlacePos == null || expectedSupportPos == null || expectedFace == null) {
            return false;
        }
        if (!BlockUtils.isBlockPosEqual(mop.getBlockPos(), expectedSupportPos) || mop.sideHit != expectedFace) {
            return false;
        }
        BlockPos placePos = BlockUtils.offsetPos(mop);
        return placePos != null && BlockUtils.isBlockPosEqual(placePos, expectedPlacePos);
    }

    private boolean canUseAsLadderSupport(BlockPos ladderPos, EnumFacing ladderFace, BlockPos supportPos) {
        if (ladderPos == null || ladderFace == null || supportPos == null || LadderClutch.mc.theWorld == null) {
            return false;
        }
        if (!this.isSolidLadderSupportBlock(supportPos)) {
            return false;
        }
        if (!this.isReplaceableOrLadder(ladderPos)) {
            return false;
        }
        return Blocks.ladder.canPlaceBlockOnSide((World)LadderClutch.mc.theWorld, ladderPos, ladderFace);
    }

    private boolean isAnchorBlockValid(BlockPos pos) {
        if (pos == null || BlockUtils.replaceable(pos) || BlockUtils.isInteractable(pos)) {
            return false;
        }
        Block block = BlockUtils.getBlock(pos);
        return block != null && block != Blocks.air;
    }

    private boolean hasSupportPlacementAnchor(BlockPos placePos) {
        if (placePos == null) {
            return false;
        }
        for (EnumFacing placeFace : EnumFacing.VALUES) {
            BlockPos anchorPos;
            if (placeFace == EnumFacing.DOWN || !this.isAnchorBlockValid(anchorPos = placePos.offset(placeFace.getOpposite()))) continue;
            return true;
        }
        return false;
    }

    private boolean isSolidLadderSupportBlock(BlockPos pos) {
        if (pos == null || BlockUtils.replaceable(pos) || BlockUtils.isInteractable(pos)) {
            return false;
        }
        Block block = BlockUtils.getBlock(pos);
        return block != null && block != Blocks.air && block.isFullCube();
    }

    private boolean isWithinReachBounds(BlockPos placePos, BlockPos supportPos) {
        if (LadderClutch.mc.thePlayer == null) {
            return false;
        }
        Vec3 eye = LadderClutch.mc.thePlayer.getPositionEyes(1.0f);
        Vec3 placeCenter = new Vec3((double)placePos.getX() + 0.5, (double)placePos.getY() + 0.5, (double)placePos.getZ() + 0.5);
        Vec3 supportCenter = new Vec3((double)supportPos.getX() + 0.5, (double)supportPos.getY() + 0.5, (double)supportPos.getZ() + 0.5);
        double reach = this.getPlacementReach() + 1.0;
        return eye.distanceTo(placeCenter) <= reach || eye.distanceTo(supportCenter) <= reach;
    }

    private float[] getFixedRotationsForHitVec(Vec3 hitVec, float baseYaw, float basePitch) {
        return this.getFixedRotationsForVec(hitVec, baseYaw, basePitch);
    }

    private float[] getFixedRotationsForVec(Vec3 targetVec, float baseYaw, float basePitch) {
        if (targetVec == null || LadderClutch.mc.thePlayer == null) {
            return null;
        }
        double dx = targetVec.xCoord - LadderClutch.mc.thePlayer.posX;
        double dy = targetVec.yCoord - (LadderClutch.mc.thePlayer.posY + (double)LadderClutch.mc.thePlayer.getEyeHeight());
        double dz = targetVec.zCoord - LadderClutch.mc.thePlayer.posZ;
        float[] target = RotationUtils.getRotationsTo(dx, dy, dz, baseYaw, basePitch);
        return RotationUtils.fixRotation(target[0], target[1], baseYaw, basePitch);
    }

    private Vec3 buildFaceHitVec(BlockPos blockPos, EnumFacing face, double u, double v) {
        double x;
        double y;
        double z;
        if (blockPos == null || face == null) {
            return null;
        }
        double cu = MathHelper.clamp_double(u, 0.01, 0.99);
        double cv = MathHelper.clamp_double(v, 0.01, 0.99);
        switch (face) {
            case DOWN: {
                x = (double)blockPos.getX() + cu;
                y = (double)blockPos.getY() + 0.01;
                z = (double)blockPos.getZ() + cv;
                break;
            }
            case UP: {
                x = (double)blockPos.getX() + cu;
                y = (double)blockPos.getY() + 0.99;
                z = (double)blockPos.getZ() + cv;
                break;
            }
            case NORTH: {
                x = (double)blockPos.getX() + cu;
                y = (double)blockPos.getY() + cv;
                z = (double)blockPos.getZ() + 0.01;
                break;
            }
            case SOUTH: {
                x = (double)blockPos.getX() + cu;
                y = (double)blockPos.getY() + cv;
                z = (double)blockPos.getZ() + 0.99;
                break;
            }
            case WEST: {
                x = (double)blockPos.getX() + 0.01;
                y = (double)blockPos.getY() + cv;
                z = (double)blockPos.getZ() + cu;
                break;
            }
            case EAST: {
                x = (double)blockPos.getX() + 0.99;
                y = (double)blockPos.getY() + cv;
                z = (double)blockPos.getZ() + cu;
                break;
            }
            default: {
                return null;
            }
        }
        return new Vec3(x, y, z);
    }

    private EntryEstimate estimateLadderEntry(BlockPos ladderPos, EnumFacing ladderFace, BlockPos supportPlacementPos, int ladderReadyTick) {
        if (ladderPos == null || LadderClutch.mc.thePlayer == null) {
            return EntryEstimate.invalid();
        }
        Simulation simulation = Simulation.create();
        int bestTick = Integer.MAX_VALUE;
        double bestErrorSq = Double.MAX_VALUE;
        int runStartTick = -1;
        double runBestErrorSq = Double.MAX_VALUE;
        for (int tickAhead = 1; tickAhead <= 20; ++tickAhead) {
            simulation.tick();
            keystrokesmod.script.model.Vec3 simPos = simulation.getPosition();
            if (simPos == null) {
                runStartTick = -1;
                runBestErrorSq = Double.MAX_VALUE;
                continue;
            }
            if (supportPlacementPos != null && tickAhead < ladderReadyTick && this.doesSimulationOccupyBlock(simPos, supportPlacementPos.up())) {
                return EntryEstimate.invalid();
            }
            if (tickAhead < ladderReadyTick) {
                runStartTick = -1;
                runBestErrorSq = Double.MAX_VALUE;
                continue;
            }
            if (!this.doesSimulationOccupyBlock(simPos, ladderPos)) {
                runStartTick = -1;
                runBestErrorSq = Double.MAX_VALUE;
                continue;
            }
            if (!this.isApproachCompatibleWithLadderFace(simPos, simulation.getMotion(), ladderPos, ladderFace)) {
                runStartTick = -1;
                runBestErrorSq = Double.MAX_VALUE;
                continue;
            }
            double errorSq = this.getSimulationEntryErrorSq(simPos, ladderPos, ladderFace);
            if (runStartTick == -1) {
                runStartTick = tickAhead;
                runBestErrorSq = errorSq;
            } else if (errorSq < runBestErrorSq) {
                runBestErrorSq = errorSq;
            }
            int runLength = tickAhead - runStartTick + 1;
            if (runLength < 2 || !(runBestErrorSq <= 0.36) || runStartTick >= bestTick && (runStartTick != bestTick || !(runBestErrorSq < bestErrorSq))) continue;
            bestTick = runStartTick;
            bestErrorSq = runBestErrorSq;
        }
        if (bestTick == Integer.MAX_VALUE) {
            return EntryEstimate.invalid();
        }
        return new EntryEstimate(bestTick, bestErrorSq);
    }

    private double getSimulationEntryErrorSq(keystrokesmod.script.model.Vec3 simPos, BlockPos ladderPos, EnumFacing ladderFace) {
        if (simPos == null || ladderPos == null) {
            return Double.MAX_VALUE;
        }
        Vec3 target = this.getLadderSteerTarget(ladderPos, ladderFace);
        double dx = target.xCoord - simPos.x;
        double dz = target.zCoord - simPos.z;
        return dx * dx + dz * dz;
    }

    private boolean doesSimulationOccupyBlock(keystrokesmod.script.model.Vec3 simPos, BlockPos targetPos) {
        if (simPos == null || targetPos == null || LadderClutch.mc.thePlayer == null) {
            return false;
        }
        int feetY = MathHelper.floor_double(simPos.y);
        int bodyY = MathHelper.floor_double(simPos.y + 1.8);
        if (targetPos.getY() < feetY - 1 || targetPos.getY() > bodyY) {
            return false;
        }
        double halfWidth = Math.max(0.3, (double)LadderClutch.mc.thePlayer.width * 0.5);
        int minX = MathHelper.floor_double(simPos.x - halfWidth + 0.001);
        int maxX = MathHelper.floor_double(simPos.x + halfWidth - 0.001);
        int minZ = MathHelper.floor_double(simPos.z - halfWidth + 0.001);
        int maxZ = MathHelper.floor_double(simPos.z + halfWidth - 0.001);
        return targetPos.getX() >= minX && targetPos.getX() <= maxX && targetPos.getZ() >= minZ && targetPos.getZ() <= maxZ;
    }

    private boolean isApproachCompatibleWithLadderFace(keystrokesmod.script.model.Vec3 simPos, keystrokesmod.script.model.Vec3 simMotion, BlockPos ladderPos, EnumFacing ladderFace) {
        double faceZ;
        double motionZ;
        double toCenterZ;
        if (simPos == null || ladderPos == null || ladderFace == null || ladderFace.getAxis() == EnumFacing.Axis.Y) {
            return true;
        }
        double toCenterX = (double)ladderPos.getX() + 0.5 - simPos.x;
        double centerLenSq = toCenterX * toCenterX + (toCenterZ = (double)ladderPos.getZ() + 0.5 - simPos.z) * toCenterZ;
        if (centerLenSq <= 1.0E-6) {
            return true;
        }
        double motionX = simMotion != null ? simMotion.x : 0.0;
        double motionLenSq = motionX * motionX + (motionZ = simMotion != null ? simMotion.z : 0.0) * motionZ;
        if (motionLenSq <= 1.0E-6) {
            return true;
        }
        double centerLen = Math.sqrt(centerLenSq);
        double dirToCenterX = toCenterX / centerLen;
        double dirToCenterZ = toCenterZ / centerLen;
        double motionLen = Math.sqrt(motionLenSq);
        double dirMotionX = motionX / motionLen;
        double dirMotionZ = motionZ / motionLen;
        double centerDot = dirMotionX * dirToCenterX + dirMotionZ * dirToCenterZ;
        double faceX = ladderFace.getFrontOffsetX();
        double faceLenSq = faceX * faceX + (faceZ = (double)ladderFace.getFrontOffsetZ()) * faceZ;
        if (faceLenSq <= 1.0E-6) {
            return centerDot > -0.2;
        }
        double faceLen = Math.sqrt(faceLenSq);
        double dirFaceX = faceX / faceLen;
        double dirFaceZ = faceZ / faceLen;
        double faceDot = dirMotionX * dirFaceX + dirMotionZ * dirFaceZ;
        return centerDot > -0.2 && faceDot > -0.4;
    }

    private boolean isPlanStillValidForCurrentStage() {
        if (this.activePlan == null || !this.isPlanWithinPlacementBounds(this.activePlan)) {
            return false;
        }
        if (this.stage == ExecutionStage.PLACE_SUPPORT) {
            if (!(this.activePlan.requiresSupportPlacement && this.activePlan.supportPlacePos != null && BlockUtils.replaceable(this.activePlan.supportPlacePos) && this.findSupportSlot() != -1 && this.hasSupportPlacementAnchor(this.activePlan.supportPlacePos))) {
                return false;
            }
            EntryEstimate estimate = this.estimateLadderEntry(this.activePlan.ladderPos, this.activePlan.ladderFace, this.activePlan.supportPlacePos, 2);
            return estimate.valid();
        }
        if (this.stage == ExecutionStage.PLACE_LADDER) {
            if (this.findLadderSlot() == -1 || !this.isReplaceableOrLadder(this.activePlan.ladderPos) || !this.canUseAsLadderSupport(this.activePlan.ladderPos, this.activePlan.ladderFace, this.activePlan.ladderSupportPos)) {
                return false;
            }
            EntryEstimate estimate = this.estimateLadderEntry(this.activePlan.ladderPos, this.activePlan.ladderFace, null, 1);
            return estimate.valid();
        }
        return true;
    }

    private boolean shouldAbortForMissingItems() {
        if (this.activePlan == null) {
            return false;
        }
        if (this.stage == ExecutionStage.PLAN_READY && this.activePlan.requiresSupportPlacement) {
            return this.findSupportSlot() == -1 || this.findLadderSlot() == -1;
        }
        if (this.stage == ExecutionStage.PLACE_SUPPORT) {
            return this.findSupportSlot() == -1;
        }
        if (this.stage == ExecutionStage.PLACE_LADDER || this.stage == ExecutionStage.GUIDE_ENTRY) {
            return this.findLadderSlot() == -1;
        }
        return false;
    }

    private void prepareHeldItemForStage() {
        int ladderSlot;
        if (LadderClutch.mc.thePlayer == null || this.activePlan == null) {
            return;
        }
        if (this.stage == ExecutionStage.PLACE_SUPPORT) {
            int supportSlot = this.findSupportSlot();
            if (supportSlot != -1) {
                this.switchToSlot(supportSlot);
            }
            return;
        }
        if ((this.stage == ExecutionStage.PLACE_LADDER || this.stage == ExecutionStage.GUIDE_ENTRY) && (ladderSlot = this.findLadderSlot()) != -1) {
            this.switchToSlot(ladderSlot);
        }
    }

    private ItemStack resolveHeldStackForCurrentStage() {
        if (LadderClutch.mc.thePlayer == null) {
            return null;
        }
        ItemStack held = LadderClutch.mc.thePlayer.getHeldItem();
        if (this.stage == ExecutionStage.PLACE_SUPPORT) {
            if (this.isSupportBlockStack(held)) {
                return held;
            }
            int slot = this.findSupportSlot();
            if (slot == -1) {
                return null;
            }
            this.switchToSlot(slot);
            held = LadderClutch.mc.thePlayer.getHeldItem();
            return this.isSupportBlockStack(held) ? held : null;
        }
        if (this.isLadderStack(held)) {
            return held;
        }
        int ladderSlot = this.findLadderSlot();
        if (ladderSlot == -1) {
            return null;
        }
        this.switchToSlot(ladderSlot);
        held = LadderClutch.mc.thePlayer.getHeldItem();
        return this.isLadderStack(held) ? held : null;
    }

    private void switchToSlot(int slot) {
        if (LadderClutch.mc.thePlayer == null || slot < 0 || slot > 8) {
            return;
        }
        int currentSlot = LadderClutch.mc.thePlayer.inventory.currentItem;
        if (this.originalSlot == -1) {
            this.originalSlot = currentSlot;
        }
        if (currentSlot != slot) {
            Utils.switchSlot(slot, true);
        }
    }

    private void restoreOriginalSlotIfNeeded() {
        if (LadderClutch.mc.thePlayer == null || this.originalSlot == -1) {
            this.originalSlot = -1;
            return;
        }
        if (LadderClutch.mc.thePlayer.inventory.currentItem != this.originalSlot) {
            Utils.switchSlot(this.originalSlot, true);
        }
        this.originalSlot = -1;
    }

    private int findLadderSlot() {
        if (LadderClutch.mc.thePlayer == null) {
            return -1;
        }
        for (int slot = 0; slot < 9; ++slot) {
            if (!this.isLadderStack(LadderClutch.mc.thePlayer.inventory.getStackInSlot(slot))) continue;
            return slot;
        }
        return -1;
    }

    private int findSupportSlot() {
        if (LadderClutch.mc.thePlayer == null) {
            return -1;
        }
        for (int slot = 0; slot < 9; ++slot) {
            if (!this.isSupportBlockStack(LadderClutch.mc.thePlayer.inventory.getStackInSlot(slot))) continue;
            return slot;
        }
        return -1;
    }

    private boolean isLadderStack(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0 || !(stack.getItem() instanceof ItemBlock)) {
            return false;
        }
        Block block = ((ItemBlock)stack.getItem()).getBlock();
        return block == Blocks.ladder;
    }

    private boolean isSupportBlockStack(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0 || !(stack.getItem() instanceof ItemBlock)) {
            return false;
        }
        ItemBlock itemBlock = (ItemBlock)stack.getItem();
        Block block = itemBlock.getBlock();
        return block != Blocks.ladder && block != null && block.isFullCube() && Utils.canBePlaced(itemBlock);
    }

    private void applyMovementOverride(PrePlayerInputEvent e, Vec3 target) {
        if (e == null || target == null || LadderClutch.mc.thePlayer == null) {
            return;
        }
        double dx = target.xCoord - LadderClutch.mc.thePlayer.posX;
        double dz = target.zCoord - LadderClutch.mc.thePlayer.posZ;
        double lenSq = dx * dx + dz * dz;
        float forward = 0.0f;
        float strafe = 0.0f;
        if (lenSq > 1.0E-6) {
            double distance = Math.sqrt(lenSq);
            float desiredYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            float yawDelta = MathHelper.wrapAngleTo180_float(desiredYaw - this.getSteeringYaw());
            float absYaw = Math.abs(yawDelta);
            if (distance <= 0.16) {
                forward = 0.0f;
                strafe = 0.0f;
            } else {
                float rad = (float)Math.toRadians(yawDelta);
                forward = MathHelper.cos(rad);
                strafe = -MathHelper.sin(rad);
                float max = Math.max(1.0f, Math.max(Math.abs(forward), Math.abs(strafe)));
                forward /= max;
                strafe /= max;
                if (absYaw > 75.0f) {
                    forward *= 0.15f;
                }
                if (absYaw > 105.0f) {
                    forward = 0.0f;
                }
                if (distance < 0.45) {
                    forward *= 0.35f;
                    strafe *= 0.65f;
                }
            }
        }
        e.setForward(forward);
        e.setStrafe(strafe);
        e.setJump(false);
        e.setSneak(false);
    }

    private Vec3 getMovementTargetForStage(PlacementPlan plan, ExecutionStage currentStage) {
        if (plan == null) {
            return null;
        }
        if (currentStage == ExecutionStage.PLACE_SUPPORT && plan.supportPlacePos != null) {
            double dz;
            double sx = (double)plan.supportPlacePos.getX() + 0.5;
            double sy = (double)plan.supportPlacePos.getY() + 0.5;
            double sz = (double)plan.supportPlacePos.getZ() + 0.5;
            double dx = (double)plan.ladderPos.getX() + 0.5 - sx;
            double len = Math.sqrt(dx * dx + (dz = (double)plan.ladderPos.getZ() + 0.5 - sz) * dz);
            if (len > 1.0E-6) {
                sx += dx / len * 0.35;
                sz += dz / len * 0.35;
            }
            return new Vec3(sx, sy, sz);
        }
        return this.getLadderSteerTarget(plan.ladderPos, plan.ladderFace);
    }

    private Vec3 getLadderSteerTarget(BlockPos ladderPos, EnumFacing ladderFace) {
        double cx = (double)ladderPos.getX() + 0.5;
        double cy = (double)ladderPos.getY() + 0.5;
        double cz = (double)ladderPos.getZ() + 0.5;
        if (ladderFace == null || ladderFace.getAxis() == EnumFacing.Axis.Y) {
            return new Vec3(cx, cy, cz);
        }
        double nudge = 0.22;
        return new Vec3(cx + (double)ladderFace.getFrontOffsetX() * nudge, cy, cz + (double)ladderFace.getFrontOffsetZ() * nudge);
    }

    private double computeSteerError(Vec3 target) {
        if (target == null || LadderClutch.mc.thePlayer == null) {
            return Double.MAX_VALUE;
        }
        double dx = target.xCoord - LadderClutch.mc.thePlayer.posX;
        double dz = target.zCoord - LadderClutch.mc.thePlayer.posZ;
        if (Math.abs(dx) < 1.0E-6 && Math.abs(dz) < 1.0E-6) {
            return 0.0;
        }
        float desiredYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float yawDelta = MathHelper.wrapAngleTo180_float(desiredYaw - this.getSteeringYaw());
        return Math.abs(yawDelta);
    }

    private double getHorizontalDistanceSq(BlockPos pos) {
        if (pos == null || LadderClutch.mc.thePlayer == null) {
            return Double.MAX_VALUE;
        }
        double dx = (double)pos.getX() + 0.5 - LadderClutch.mc.thePlayer.posX;
        double dz = (double)pos.getZ() + 0.5 - LadderClutch.mc.thePlayer.posZ;
        return dx * dx + dz * dz;
    }

    private float getSteeringYaw() {
        if (LadderClutch.mc.thePlayer == null) {
            return 0.0f;
        }
        if (Settings.movementFix != null && Settings.movementFix.isToggled() && RotationHelper.get().isActive() && this.hasStickyRotation) {
            return this.stickyYaw;
        }
        return LadderClutch.mc.thePlayer.rotationYaw;
    }

    private float getPlacementBaseYaw() {
        if (this.hasStickyRotation) {
            return this.stickyYaw;
        }
        return RotationUtils.serverRotations[0];
    }

    private float getPlacementBasePitch() {
        if (this.hasStickyRotation) {
            return this.stickyPitch;
        }
        return RotationUtils.serverRotations[1];
    }

    private boolean shouldStartPlanningNow() {
        if (!this.isModuleReady() || LadderClutch.mc.thePlayer.onGround || LadderClutch.mc.thePlayer.motionY >= 0.0 || LadderClutch.mc.thePlayer.isOnLadder()) {
            return false;
        }
        return this.findLadderSlot() != -1;
    }

    private boolean isCurrentlyFalling() {
        return LadderClutch.mc.thePlayer != null && !LadderClutch.mc.thePlayer.onGround && !LadderClutch.mc.thePlayer.isOnLadder() && LadderClutch.mc.thePlayer.motionY < 0.0;
    }

    private boolean isMovementAlignedForCurrentStage() {
        if (LadderClutch.mc.thePlayer == null || this.activePlan == null) {
            return false;
        }
        Vec3 target = this.getMovementTargetForStage(this.activePlan, this.stage);
        if (target == null) {
            return false;
        }
        double dx = target.xCoord - LadderClutch.mc.thePlayer.posX;
        double dz = target.zCoord - LadderClutch.mc.thePlayer.posZ;
        double distSq = dx * dx + dz * dz;
        if (distSq > 2.25) {
            return false;
        }
        float desiredYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float yawDelta = Math.abs(MathHelper.wrapAngleTo180_float(desiredYaw - this.getSteeringYaw()));
        return yawDelta <= 110.0f || distSq <= 0.16;
    }

    private boolean isPlanWithinPlacementBounds(PlacementPlan plan) {
        if (plan == null || plan.ladderPos == null) {
            return false;
        }
        if (!this.isWithinPlacementBounds(plan.ladderPos)) {
            return false;
        }
        if (plan.ladderSupportPos != null && !this.isWithinPlacementBounds(plan.ladderSupportPos)) {
            return false;
        }
        return plan.supportPlacePos == null || this.isWithinPlacementBounds(plan.supportPlacePos);
    }

    private boolean isWithinPlacementBounds(BlockPos pos) {
        if (pos == null || LadderClutch.mc.thePlayer == null) {
            return false;
        }
        double dx = Math.abs((double)pos.getX() + 0.5 - LadderClutch.mc.thePlayer.posX);
        double dz = Math.abs((double)pos.getZ() + 0.5 - LadderClutch.mc.thePlayer.posZ);
        if (dx > 4.5 || dz > 4.5) {
            return false;
        }
        double belowDelta = this.getPlayerFeetY() - (double)pos.getY();
        return belowDelta >= 1.0 && belowDelta <= 5.5;
    }

    private double getPlayerFeetY() {
        if (LadderClutch.mc.thePlayer == null) {
            return 0.0;
        }
        return LadderClutch.mc.thePlayer.getEntityBoundingBox() != null ? LadderClutch.mc.thePlayer.getEntityBoundingBox().minY : LadderClutch.mc.thePlayer.posY;
    }

    private boolean isReplaceableOrLadder(BlockPos pos) {
        if (pos == null) {
            return false;
        }
        Block block = BlockUtils.getBlock(pos);
        return block == Blocks.ladder || BlockUtils.replaceable(pos);
    }

    private boolean isInsideTargetLadder(BlockPos ladderPos) {
        return ladderPos != null && LadderClutch.mc.thePlayer != null && BlockUtils.doesPlayerOccupyPlacementBlock(ladderPos);
    }

    private boolean isLadderBlock(BlockPos pos) {
        return pos != null && BlockUtils.getBlock(pos) == Blocks.ladder;
    }

    private boolean isModuleReady() {
        if (!this.isEnabled() || !Utils.nullCheck() || LadderClutch.mc.playerController == null || mc.isGamePaused()) {
            return false;
        }
        EntityPlayerSP player = LadderClutch.mc.thePlayer;
        return player != null && LadderClutch.mc.currentScreen == null;
    }

    private int getPlacementPlayerTick() {
        return Utils.nullCheck() && LadderClutch.mc.thePlayer != null ? Utils.getTimerAwarePlacementTick() : Integer.MIN_VALUE;
    }

    private double getPlacementReach() {
        if (LadderClutch.mc.playerController == null) {
            return 4.5;
        }
        return (double)LadderClutch.mc.playerController.getBlockReachDistance() + 0.001;
    }

    private void resetExecutionState(boolean restoreOriginalSlot, String reason) {
        this.stage = ExecutionStage.IDLE;
        this.activePlan = null;
        this.hasStickyRotation = false;
        this.lastPlacementAttemptTick = Integer.MIN_VALUE;
        this.guideStartTick = Integer.MIN_VALUE;
        this.guideInvalidTicks = 0;
        this.stageInvalidTicks = 0;
        this.lastInvalidationReason = reason == null ? "reset" : reason;
        if (restoreOriginalSlot) {
            this.restoreOriginalSlotIfNeeded();
        }
    }

    private static final class RotationHit {
        private final float yaw;
        private final float pitch;
        private final MovingObjectPosition mop;
        private final double score;

        private RotationHit(float yaw, float pitch, MovingObjectPosition mop, double score) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.mop = mop;
            this.score = score;
        }
    }

    private static final class EntryEstimate {
        private final int tickAhead;
        private final double entryAlignmentErrorSq;

        private EntryEstimate(int tickAhead, double entryAlignmentErrorSq) {
            this.tickAhead = tickAhead;
            this.entryAlignmentErrorSq = entryAlignmentErrorSq;
        }

        private static EntryEstimate invalid() {
            return new EntryEstimate(-1, Double.MAX_VALUE);
        }

        private boolean valid() {
            return this.tickAhead >= 0;
        }
    }

    private static final class PlacementPlan {
        private final BlockPos ladderPos;
        private final BlockPos ladderSupportPos;
        private final EnumFacing ladderFace;
        private final boolean requiresSupportPlacement;
        private final BlockPos supportPlacePos;
        private final BlockPos supportAnchorPos;
        private final EnumFacing supportPlaceFace;
        private final int expectedEntryTick;
        private final double steerError;
        private final double planCost;

        private PlacementPlan(BlockPos ladderPos, BlockPos ladderSupportPos, EnumFacing ladderFace, boolean requiresSupportPlacement, BlockPos supportPlacePos, BlockPos supportAnchorPos, EnumFacing supportPlaceFace, int expectedEntryTick, double steerError, double planCost) {
            this.ladderPos = ladderPos;
            this.ladderSupportPos = ladderSupportPos;
            this.ladderFace = ladderFace;
            this.requiresSupportPlacement = requiresSupportPlacement;
            this.supportPlacePos = supportPlacePos;
            this.supportAnchorPos = supportAnchorPos;
            this.supportPlaceFace = supportPlaceFace;
            this.expectedEntryTick = expectedEntryTick;
            this.steerError = steerError;
            this.planCost = planCost;
        }

        private static PlacementPlan direct(BlockPos ladderPos, BlockPos ladderSupportPos, EnumFacing ladderFace, int expectedEntryTick, double steerError, double planCost) {
            return new PlacementPlan(ladderPos, ladderSupportPos, ladderFace, false, null, null, null, expectedEntryTick, steerError, planCost);
        }

        private static PlacementPlan withSupportPlacement(BlockPos ladderPos, BlockPos supportPlacePos, EnumFacing ladderFace, BlockPos supportAnchorPos, EnumFacing supportPlaceFace, int expectedEntryTick, double steerError, double planCost) {
            return new PlacementPlan(ladderPos, supportPlacePos, ladderFace, true, supportPlacePos, supportAnchorPos, supportPlaceFace, expectedEntryTick, steerError, planCost);
        }
    }

    private enum ExecutionStage {
        IDLE,
        PLAN_READY,
        PLACE_SUPPORT,
        PLACE_LADDER,
        GUIDE_ENTRY
    }
}
