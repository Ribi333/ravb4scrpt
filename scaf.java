package keystrokesmod.module.impl.player;

import keystrokesmod.Raven;
import keystrokesmod.event.*;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityPlayerSP;
import keystrokesmod.mixin.interfaces.IMixinItemRenderer;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.movement.Bhop;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.*;
import keystrokesmod.utility.Timer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockTNT;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.*;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static net.minecraft.util.MathHelper.wrapAngleTo180_float;

public class Scaffold extends Module {
    private final SliderSetting motion;
    public SliderSetting rotation, fakeRotation;
    private SliderSetting sprint;
    private SliderSetting floatFirstJump;
    private SliderSetting fastScaffold;
    private SliderSetting multiPlace;

    public ButtonSetting autoSwap;
    private ButtonSetting fastOnRMB;
    public ButtonSetting highlightBlocks;
    private ButtonSetting jumpFacingForward;
    public ButtonSetting safeWalk;
    public ButtonSetting showBlockCount;
    private ButtonSetting silentSwing;

    private String[] rotationModes = new String[] { "§cDisabled", "Simple", "Offset", "Precise" };
    private String[] fakeRotationModes = new String[] { "§cDisabled", "Strict", "Smooth", "Spin" };
    private String[] sprintModes = new String[] { "§cDisabled", "Vanilla", "Float" };
    private String[] fastScaffoldModes = new String[] { "§cDisabled", "Jump A", "Jump B", "Jump B Low", "Jump E", "Keep-Y", "Keep-Y Low" };
    private String[] multiPlaceModes = new String[] { "§cDisabled", "1 extra", "2 extra", "3 extra", "4 extra" };

    //Highlight blocks
    public Map<BlockPos, Timer> highlight = new HashMap<>();
    public boolean canBlockFade;

    //Block count
    private ScaffoldBlockCount scaffoldBlockCount;

    //swapping related
    public AtomicInteger lastSlot = new AtomicInteger(-1);
    private int spoofSlot;
    public boolean hasSwapped;
    private int blockSlot = -1;

    //placements related
    private boolean hasPlaced, finishProcedure, canPlace;
    private PlaceData lastPlacement;
    private EnumFacing[] facings = { EnumFacing.EAST, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.UP };
    private BlockPos[] offsets = { new BlockPos(-1, 0, 0), new BlockPos(1, 0, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, -1), new BlockPos(0, -1, 0) };
    private Vec3 targetBlock;
    private PlaceData blockInfo;
    private Vec3 blockPos, hitVec, lookVec;

    //bypass related
    private boolean rotateForward;
    private double startYPos = -1;
    public boolean fastScaffoldKeepY;
    private boolean firstKeepYPlace;
    private boolean rotatingForward;
    private int keepYTicks;
    public boolean lowhop;
    private int rotationDelay;
    private boolean floatJumped;
    private boolean floatStarted;
    private boolean floatWasEnabled;
    private boolean floatKeepY;
    public int offsetDelay;

    //disable checks
    public boolean moduleEnabled;
    public boolean isEnabled;
    private boolean disabledModule;
    private boolean dontDisable, towerEdge;
    private int disableTicks;
    private int scaffoldTicks;

    //rotation related
    private boolean was451, was452;
    private float minPitch, minOffset, pOffset;
    private float edge;
    private long firstStroke, yawEdge;
    private float lastEdge2, yawAngle, theYaw;
    private boolean enabledOffGround = false;
    private float[] blockRotations;
    public float yaw, pitch, blockYaw, yawOffset, lastOffset;
    private boolean set2;
    private float maxOffset;
    private int sameMouse;
    private int randomF, yawChanges, dynamic;
    //fake rotations
    private float fakeYaw, fakePitch;
    private float fakeYaw1, fakeYaw2;


    public Scaffold() {
        super("Scaffold", category.player);
        this.registerSetting(motion = new SliderSetting("Motion", "%", 100, 50, 150, 1));
        this.registerSetting(rotation = new SliderSetting("Rotation", 1, rotationModes));
        this.registerSetting(fakeRotation = new SliderSetting("Rotation (fake)", 0, fakeRotationModes));
        this.registerSetting(sprint = new SliderSetting("Sprint mode", 0, sprintModes));
        this.registerSetting(floatFirstJump = new SliderSetting("§eFloat §rfirst jump speed", "%", 100, 50, 100, 1));
        this.registerSetting(fastScaffold = new SliderSetting("Fast scaffold", 0, fastScaffoldModes));

        this.registerSetting(multiPlace = new SliderSetting("Multi-place", 0, multiPlaceModes));

        this.registerSetting(autoSwap = new ButtonSetting("Auto swap", true));
        this.registerSetting(fastOnRMB = new ButtonSetting("Fast on RMB", true));
        this.registerSetting(highlightBlocks = new ButtonSetting("Highlight blocks", true));
        this.registerSetting(jumpFacingForward = new ButtonSetting("Jump facing forward", false));
        this.registerSetting(safeWalk = new ButtonSetting("Safewalk", true));
        this.registerSetting(showBlockCount = new ButtonSetting("Show block count", true));
        this.registerSetting(silentSwing = new ButtonSetting("Silent swing", false));
        //this.registerSetting(pitchOffset = new SliderSetting("Pitch offset", "", 9, 0, 30, 1));

        //this.registerSetting(offsetAmount = new SliderSetting("Offset amount", "%", 100, 0, 100, 0.25));

        this.alwaysOn = true;
    }

    public void guiUpdate() {
        this.floatFirstJump.setVisible(sprint.getInput() == 2, this);
    }

    public void onDisable() {
        if (ModuleManager.tower.canTower() && (ModuleManager.tower.dCount == 0 || !Utils.isMoving())) {
            towerEdge = true;
        }
        disabledModule = true;
        moduleEnabled = false;
    }

    public void onEnable() {
        isEnabled = true;
        moduleEnabled = true;
        ModuleUtils.fadeEdge = 0;
        edge = -999999929;
        minPitch = 80F;
        if (!mc.thePlayer.onGround) {
            rotationDelay = 2;
            enabledOffGround = true;
        }
        lastEdge2 = mc.thePlayer.rotationYaw;

        FMLCommonHandler.instance().bus().register(scaffoldBlockCount = new ScaffoldBlockCount(mc));
        lastSlot.set(-1);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouse(MouseEvent e) {
        if (!isEnabled) {
            return;
        }
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        e.setCanceled(true);
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (!isEnabled) {
            return;
        }
        if (Utils.isMoving()) {
            scaffoldTicks++;
        }
        else {
            scaffoldTicks = 0;
        }
        canBlockFade = true;
        if (Utils.keysDown() && usingFastScaffold() && fastScaffold.getInput() >= 1 && !ModuleManager.tower.canTower() && !LongJump.function) { // jump mode
            if (mc.thePlayer.onGround && Utils.isMoving()) {
                if (scaffoldTicks > 1) {
                    rotateForward();
                    mc.thePlayer.jump();
                    Utils.setSpeed((getSpeed(getSpeedLevel()) - Utils.randomizeDouble(0.0003, 0.0001) * ModuleUtils.applyFrictionMulti()));
                    if (fastScaffold.getInput() == 6 || fastScaffold.getInput() == 3 && firstKeepYPlace) {
                        lowhop = true;
                    }
                    if (startYPos == -1 || Math.abs(startYPos - e.posY) > 2) {
                        startYPos = e.posY;
                        fastScaffoldKeepY = true;
                    }
                }
            }
        }
        else if (fastScaffoldKeepY) {
            fastScaffoldKeepY = firstKeepYPlace = false;
            startYPos = -1;
            keepYTicks = 0;
        }

        //Float
        if (sprint.getInput() == 2 && !usingFastScaffold() && !fastScaffoldKeepY && !ModuleManager.tower.canTower() && !LongJump.function) {
            floatWasEnabled = true;
            if (!floatStarted && offsetDelay == 0) {
                if (ModuleUtils.groundTicks > 8 && mc.thePlayer.onGround) {
                    floatKeepY = true;
                    startYPos = e.posY;
                    mc.thePlayer.jump();
                    if (Utils.isMoving()) {
                        double fvl = (getSpeed(getSpeedLevel()) - Utils.randomizeDouble(0.0003, 0.0001)) * (floatFirstJump.getInput() / 100);
                        Utils.setSpeed(fvl);
                    }
                    floatJumped = true;
                } else if (ModuleUtils.groundTicks <= 8 && mc.thePlayer.onGround) {
                    floatStarted = true;
                }
                if (floatJumped && !mc.thePlayer.onGround) {
                    floatStarted = true;
                }
            }

            if (floatStarted && mc.thePlayer.onGround) {
                floatKeepY = false;
                startYPos = -1;
                if (moduleEnabled && mc.thePlayer.posY % 1 == 0) {
                    e.setPosY(e.getPosY() + ModuleUtils.offsetValue);
                    ModuleUtils.groundTicks = 0;
                    if (Utils.isMoving()) Utils.setSpeed(getFloatSpeed(getSpeedLevel()));
                    offsetDelay = 2;
                }
            }
        } else if (floatWasEnabled && moduleEnabled) {
            if (floatKeepY) {
                startYPos = -1;
            }
            floatStarted = floatJumped = floatKeepY = floatWasEnabled = false;
        }

        if (blockRotations != null) {
            if (mc.thePlayer.rotationYaw == lastOffset) {
                sameMouse++;
            }
            else {
                sameMouse = 0;
                yawChanges++;
            }
            if (sameMouse > 2) {
                yawChanges = 0;
            }
            lastOffset = mc.thePlayer.rotationYaw;
            if (yawChanges > 15) {
                randomF = 1;
                yawEdge = Utils.time();
            }
            if (yawEdge > 0 && (Utils.time() - yawEdge) > 500) {
                yawEdge = 0;
            }
        }
        else {
            fakeYaw1 = mc.thePlayer.rotationYaw - hardcodedYaw();
        }

        dynamic = 0;
        if (targetBlock != null) {
            Vec3 lookAt = new Vec3(targetBlock.xCoord - lookVec.xCoord, targetBlock.yCoord - lookVec.yCoord, targetBlock.zCoord - lookVec.zCoord);
            blockRotations = RotationUtils.getRotations(lookAt);
            targetBlock = null;
            fakeYaw1 = mc.thePlayer.rotationYaw - hardcodedYaw();
            if (yawEdge == 0) {
                randomF = Utils.randomizeInt(0, 9);
                yawEdge = Utils.time();
            }
            dynamic++;
        }
        randomF = 0;

        switch ((int) rotation.getInput()) {
            case 1:
                yaw = mc.thePlayer.rotationYaw - hardcodedYaw();
                pitch = 78F;
                e.setRotations(yaw, pitch);
                break;
            case 2:
                float moveAngle = (float) getMovementAngle();
                float relativeYaw = mc.thePlayer.rotationYaw + moveAngle;
                float normalizedYaw = (relativeYaw % 360 + 360) % 360;
                float quad = normalizedYaw % 90;

                float side = wrapAngleTo180_float(getMotionYaw() - yaw);
                float yawBackwards = wrapAngleTo180_float(mc.thePlayer.rotationYaw) - hardcodedYaw();
                float blockYawOffset = wrapAngleTo180_float(yawBackwards - blockYaw);

                long strokeDelay = 250;

                if (quad <= 5 || quad >= 85) {
                    yawAngle = 125.625F;
                    minOffset = 18;
                }
                if (quad > 5 && quad <= 15 || quad >= 75 && quad < 85) {
                    yawAngle = 127.625F;
                    minOffset = 16;
                }
                if (quad > 15 && quad <= 25 || quad >= 65 && quad < 75) {
                    yawAngle = 129.625F;
                    minOffset = 14;
                }
                if (quad > 25 && quad <= 32 || quad >= 58 && quad < 65) {
                    yawAngle = 132.625F;
                    minOffset = 12;
                }
                if (quad > 32 && quad <= 38 || quad >= 52 && quad < 58) {
                    yawAngle = 134.625F;
                    minOffset = 9;
                }
                if (quad > 38 && quad <= 42 || quad >= 48 && quad < 52) {
                    yawAngle = 136.625F;
                    minOffset = 7;
                }
                if (quad > 42 && quad <= 45 || quad >= 45 && quad < 48) {
                    yawAngle = 139.625F;
                    minOffset = 5;

                }
                minPitch = 78.55F;
                //Utils.print("" + minOffset);
                //float offsetAmountD = ((((float) offsetAmount.getInput() / 10) - 10) * -2) - (((float) offsetAmount.getInput() / 10) - 10);
                //yawAngle += offsetAmountD;
                //Utils.print("" + offsetAmountD);

                float offset = yawAngle;//(!Utils.scaffoldDiagonal(false)) ? 125.500F : 143.500F;


                float nigger = 0;

                if (quad > 45) {
                    nigger = 10;
                } else {
                    nigger = -10;
                }


                if (firstStroke > 0 && (System.currentTimeMillis() - firstStroke) > strokeDelay) {
                    firstStroke = 0;
                }
                if (enabledOffGround) {
                    if (blockRotations != null) {
                        yaw = blockRotations[0];
                        pitch = blockRotations[1];
                    } else {
                        yaw = mc.thePlayer.rotationYaw - hardcodedYaw() - nigger;
                        pitch = minPitch;
                    }
                    e.setRotations(yaw, pitch);
                    break;
                }

                if (blockRotations != null) {
                    blockYaw = blockRotations[0];
                    pitch = blockRotations[1];
                    yawOffset = blockYawOffset;
                    if (pitch < minPitch) {
                        pitch = minPitch;
                    }
                } else {
                    pitch = minPitch;
                    if (edge == 1 && ((quad <= 5 || quad >= 85) && !Utils.scaffoldDiagonal(false))) {
                        firstStroke = Utils.time();
                    }
                    yawOffset = 5;
                    dynamic = 2;
                }

                if (!Utils.isMoving() || Utils.getHorizontalSpeed() == 0.0D) {
                    e.setRotations(theYaw, pitch);
                    break;
                }

                float motionYaw = getMotionYaw();

                float newYaw = motionYaw - offset * Math.signum(
                        wrapAngleTo180_float(motionYaw - yaw)
                );
                yaw = wrapAngleTo180_float(newYaw);

                if (quad > 5 && quad < 85 && dynamic > 0) {
                    if (quad < 45F) {
                        if (firstStroke == 0) {
                            if (side >= 0) {
                                set2 = false;
                            } else {
                                set2 = true;
                            }
                        }
                        if (was452) {
                            firstStroke = Utils.time();
                        }
                        was451 = true;
                        was452 = false;
                    } else {
                        if (firstStroke == 0) {
                            if (side >= 0) {
                                set2 = true;
                            } else {
                                set2 = false;
                            }
                        }
                        if (was451) {
                            firstStroke = Utils.time();
                        }
                        was452 = true;
                        was451 = false;
                    }
                }
                double minSwitch = (!Utils.scaffoldDiagonal(false)) ? 4 : 15;
                if (side >= 0) {
                    if (yawOffset <= -minSwitch && firstStroke == 0 && dynamic > 0) {
                        if (quad <= 5 || quad >= 85) {
                            if (set2) {
                                firstStroke = Utils.time();
                            }
                            set2 = false;
                        }
                    } else if (yawOffset >= 0 && firstStroke == 0 && dynamic > 0) {
                        if (quad <= 5 || quad >= 85) {
                            if (yawOffset >= minSwitch) {
                                if (!set2) {
                                    firstStroke = Utils.time();
                                }
                                set2 = true;
                            }
                        }
                    }
                    if (set2) {
                        if (yawOffset <= -0) yawOffset = -0;
                        if (yawOffset >= minOffset) yawOffset = minOffset;
                        theYaw = (yaw + offset * 2) - yawOffset;
                        e.setRotations(theYaw, pitch);
                        break;
                    }
                } else if (side <= -0) {
                    if (yawOffset >= minSwitch && firstStroke == 0 && dynamic > 0) {
                        if (quad <= 5 || quad >= 85) {
                            if (set2) {
                                firstStroke = Utils.time();
                            }
                            set2 = false;
                        }
                    } else if (yawOffset <= 0 && firstStroke == 0 && dynamic > 0) {
                        if (quad <= 5 || quad >= 85) {
                            if (yawOffset <= -minSwitch) {
                                if (!set2) {
                                    firstStroke = Utils.time();
                                }
                                set2 = true;
                            }
                        }
                    }
                    if (set2) {
                        if (yawOffset >= 0) yawOffset = 0;
                        if (yawOffset <= -minOffset) yawOffset = -minOffset;
                        theYaw = (yaw - offset * 2) - yawOffset;
                        e.setRotations(theYaw, pitch);
                        break;
                    }
                }

                if (side >= 0) {
                    if (yawOffset >= 0) yawOffset = 0;
                    if (yawOffset <= -minOffset) yawOffset = -minOffset;
                } else if (side <= -0) {
                    if (yawOffset <= -0) yawOffset = -0;
                    if (yawOffset >= minOffset) yawOffset = minOffset;
                }
                theYaw = yaw - yawOffset;
                e.setRotations(theYaw, pitch);
                break;
            case 3:
                if (blockRotations != null) {
                    yaw = blockRotations[0];
                    pitch = blockRotations[1];
                } else {
                    yaw = mc.thePlayer.rotationYaw - hardcodedYaw();
                    pitch = 80F;
                }
                e.setRotations(yaw, pitch);
                theYaw = e.getYaw();
                break;
        }
        if (edge != 1) {
            firstStroke = Utils.time();
            edge = 1;
        }
        if (mc.thePlayer.onGround) {
            enabledOffGround = false;
        }

        //get yaw - player yaw offset
        float yv = wrapAngleTo180_float(mc.thePlayer.rotationYaw) - hardcodedYaw();
        if (Raven.debug) {
            Utils.sendModuleMessage(this, "" + wrapAngleTo180_float(yv - e.getYaw()) + " " + e.getPitch() + " " + minOffset);
        }

        //Utils.print("" + mc.thePlayer.rotationYaw + " " + mc.thePlayer.rotationPitch);

        //jump facing forward
        if (ModuleUtils.inAirTicks >= 1) {
            rotateForward = false;
        }
        if (rotateForward && jumpFacingForward.isToggled()) {
            if (rotation.getInput() > 0) {
                if (!rotatingForward) {
                    rotationDelay = 2;
                    rotatingForward = true;
                }
                float forwardYaw = (mc.thePlayer.rotationYaw - hardcodedYaw() - 180);
                e.setYaw(forwardYaw);
                e.setPitch(10);
            }
        }
        else {
            rotatingForward = false;
        }

        if (ModuleManager.tower.isVerticalTowering()) {
            if (blockRotations != null) {
                e.setYaw(blockRotations[0]);
            }
            if (ModuleManager.tower.yaw != 0) {
                e.setYaw(ModuleManager.tower.yaw);
            }
            if (ModuleManager.tower.pitch != 0) {
                e.setPitch(ModuleManager.tower.pitch);
            }
        }

        //pitch fix
        if (e.getPitch() > 89.9F) {
            e.setPitch(89.9F);
        }

        if (rotationDelay > 0) --rotationDelay;

        //Fake rotations
        if (fakeRotation.getInput() > 0) {
            if (fakeRotation.getInput() == 1) {
                fakeYaw = fakeYaw1;
                if (blockRotations != null) {
                    fakePitch = blockRotations[1] + 5;
                } else {
                    fakePitch = 80f;
                }
            }
            else if (fakeRotation.getInput() == 2) {
                fakeYaw2 = mc.thePlayer.rotationYaw - hardcodedYaw();
                float yawDifference = getAngleDifference(lastEdge2, fakeYaw2);
                float smoothingFactor = (1.0f - (65.0f / 100.0f));
                fakeYaw2 = (lastEdge2 + yawDifference * smoothingFactor);
                lastEdge2 = fakeYaw2;

                fakeYaw = fakeYaw2;
                if (blockRotations != null) {
                    fakePitch = blockRotations[1] + 5;
                } else {
                    fakePitch = 80f;
                }
            }
            else if (fakeRotation.getInput() == 3) {
                fakeYaw += 25.71428571428571F;
                fakePitch = 90F;
            }
            RotationUtils.setFakeRotations(fakeYaw, fakePitch);
        }

    }

    @SubscribeEvent
    public void onPostPlayerInput(PostPlayerInputEvent e) {
        if (!ModuleManager.scaffold.isEnabled) {
            return;
        }
        if (!fastScaffoldKeepY && !floatKeepY) {
            return;
        }
        mc.thePlayer.movementInput.jump = false;
    }

    @SubscribeEvent
    public void onSlotUpdate(SlotUpdateEvent e) {
        if (isEnabled) {
            lastSlot.set(e.slot);
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (!isEnabled) {
            return;
        }
        if (LongJump.function) {
            startYPos = -1;
        }
        if (LongJump.stopModules) {
            return;
        }
        if (ModuleManager.killAura.isTargeting || ModuleManager.killAura.justUnTargeted) {
            return;
        }
        if (holdingBlocks() && setSlot()) {
            if (moduleEnabled && !finishProcedure) {
                if (Utils.distanceToGround(mc.thePlayer) < 2) {
                    canPlace = true;
                    finishProcedure = true;
                }
                if (Utils.distanceToGround(mc.thePlayer) > 5) {
                    canPlace = true;
                    if (hasPlaced) {
                        finishProcedure = true;
                    }
                }
                else if (!finishProcedure) {
                    return;
                }
            }

            hasSwapped = true;
            int mode = (int) fastScaffold.getInput();
            if (rotation.getInput() == 0 || rotationDelay == 0) {
                placeBlock(0, 0);
            }
            if (ModuleManager.tower.placeExtraBlock) {
                placeBlock(0, -1);
            }
            if (fastScaffoldKeepY && !ModuleManager.tower.canTower()) {
                ++keepYTicks;
                if ((int) mc.thePlayer.posY > (int) startYPos) {
                    switch (mode) {
                        case 1:
                            if (!firstKeepYPlace && keepYTicks == 3) {
                                placeBlock(1, 0);
                                firstKeepYPlace = true;
                            }
                            break;
                        case 2:
                            if (!firstKeepYPlace && keepYTicks == 8 || keepYTicks == 11) {
                                placeBlock(1, 0);
                                firstKeepYPlace = true;
                            }
                            break;
                        case 3:
                            if (!firstKeepYPlace && keepYTicks == 8 || firstKeepYPlace && keepYTicks == 7) {
                                placeBlock(1, 0);
                                firstKeepYPlace = true;
                            }
                            break;
                        case 4:
                            if (!firstKeepYPlace && keepYTicks == 7) {
                                placeBlock(1, 0);
                                firstKeepYPlace = true;
                            }
                            break;
                    }
                }
                if (mc.thePlayer.onGround) keepYTicks = 0;
                if ((int) mc.thePlayer.posY == (int) startYPos) firstKeepYPlace = false;
            }
            handleMotion();
        }

        if (disabledModule) {
            if (hasPlaced && (towerEdge || floatStarted && Utils.isMoving())) {
                dontDisable = true;
            }

            if (dontDisable && ++disableTicks >= 2) {
                isEnabled = false;
                //Utils.print("Extra tick");
            }
            if (!dontDisable) {
                isEnabled = false;
            }


            if (!isEnabled) {
                disabledModule = dontDisable = false;
                disableTicks = 0;
                //Utils.print("Disabled");

                if (ModuleManager.tower.speed) {
                    Utils.setSpeed(Utils.getHorizontalSpeed(mc.thePlayer) / 1.6);
                }

                if (lastSlot.get() != -1) {
                    mc.thePlayer.inventory.currentItem = lastSlot.get();
                    lastSlot.set(-1);
                }
                blockSlot = -1;
                if (autoSwap.isToggled() && ModuleManager.autoSwap.spoofItem.isToggled()) {
                    ((IMixinItemRenderer) mc.getItemRenderer()).setCancelUpdate(false);
                    ((IMixinItemRenderer) mc.getItemRenderer()).setCancelReset(false);
                }
                if (offsetDelay > 0) {
                    ModuleManager.sprint.requireJump = false;
                }
                scaffoldBlockCount.beginFade();
                hasSwapped = hasPlaced = false;
                targetBlock = null;
                blockInfo = null;
                blockRotations = null;
                fastScaffoldKeepY = firstKeepYPlace = rotateForward = rotatingForward = floatStarted = floatJumped = floatWasEnabled = towerEdge =
                        was451 = was452 = enabledOffGround = finishProcedure = canPlace = false;
                rotationDelay = keepYTicks = scaffoldTicks = 0;
                firstStroke = 0;
                startYPos = -1;
                lookVec = null;
                lastPlacement = null;
            }
        }
    }

    @Override
    public String getInfo() {
        String info;
        if (fastOnRMB.isToggled()) {
            info = Mouse.isButtonDown(1) && Utils.tabbedIn() ? fastScaffoldModes[(int) fastScaffold.getInput()] : sprintModes[(int) sprint.getInput()];
        }
        else {
            info = fastScaffold.getInput() > 0 ? fastScaffoldModes[(int) fastScaffold.getInput()] : sprintModes[(int) sprint.getInput()];
        }
        return info;
    }

    public boolean stopFastPlace() {
        return this.isEnabled();
    }

    float getAngleDifference(float from, float to) {
        float difference = (to - from) % 360.0F;
        if (difference < -180.0F) {
            difference += 360.0F;
        } else if (difference >= 180.0F) {
            difference -= 360.0F;
        }
        return difference;
    }

    public void rotateForward() {
        rotateForward = true;
        rotatingForward = false;
    }

    public boolean blockAbove() {
        return !(BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 2, mc.thePlayer.posZ)) instanceof BlockAir);
    }

    public boolean sprint() {
        if (isEnabled) {
            return handleFastScaffolds() > 0 || !holdingBlocks();
        }
        return false;
    }

    private int handleFastScaffolds() {
        if (fastOnRMB.isToggled()) {
            return fastScaffold.getInput() > 0 && Mouse.isButtonDown(1) && Utils.tabbedIn() ? (int) fastScaffold.getInput() : (int) sprint.getInput();
        }
        else {
            return fastScaffold.getInput() > 0 ? (int) fastScaffold.getInput() : (int) sprint.getInput();
        }
    }

    private boolean usingFloat() {
        return sprint.getInput() == 2 && Utils.isMoving() && !usingFastScaffold();
    }

    public boolean usingFastScaffold() {
        return fastScaffold.getInput() > 0 && (!fastOnRMB.isToggled() || (Mouse.isButtonDown(1) || ModuleManager.bhop.isEnabled()) && Utils.tabbedIn());
    }

    public boolean safewalk() {
        return this.isEnabled() && safeWalk.isToggled();
    }

    public boolean stopRotation() {
        return this.isEnabled() && rotation.getInput() > 0;
    }

    private void place(PlaceData block) {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock) heldItem.getItem())) {
            return;
        }
        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, heldItem, block.blockPos, block.enumFacing, block.hitVec)) {
            if (silentSwing.isToggled()) {
                mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
            }
            else {
                mc.thePlayer.swingItem();
                if (holdingBlocks()) {
                    mc.getItemRenderer().resetEquippedProgress();
                }
            }
            highlight.put(block.blockPos.offset(block.enumFacing), null);
            hasPlaced = true;
        }
    }

    private float getOffsetYaw2() {
        float[][] angleRanges = {
                {0, 10, 60, 45}, {270, 280, 60, 45}, {180, 190, 60, 45}, {90, 100, 60, 45}, {350, 360, -60, -45},
                {260, 270, -60, -45}, {170, 180, -60, -45}, {80, 90, -65, -45}, {10, 20, 55, 45}, {280, 290, 55, 45},
                {190, 200, 55, 45}, {100, 110, 55, 45}, {340, 350, -55, -45}, {250, 260, -55, -45}, {160, 170, -55, -45},
                {70, 80, -55, -45}, {20, 30, 50, 45}, {290, 300, 50, 45}, {200, 210, 50, 45}, {110, 120, 50, 45},
                {330, 340, -50, -45}, {240, 250, -50, -45}, {150, 160, -50, -45}, {60, 70, -50, -45}, {30, 40, 45, 45},
                {300, 310, 45, 45}, {210, 220, 45, 45}, {120, 130, 45, 45}, {315, 330, -45, -45}, {225, 240, -45, -45},
                {135, 150, -45, -45}, {45, 60, -45, -45}        };
        float yaw = getCustomYaw() + 45, bodyYaw = -45;
        for (float[] range : angleRanges) {
            if (getCustomYaw() > range[0] && getCustomYaw() < range[1]) {
                yaw = getCustomYaw() + range[2];
                bodyYaw = yaw + (yaw > mc.thePlayer.rotationYaw ? -45 : 45);
                break;
            }
        }

        return yaw;
    }

    public float getCustomYaw() {
        float yaw = 0.0f;
        double moveForward = mc.thePlayer.moveForward;
        double moveStrafe = mc.thePlayer.moveStrafing;
        if (moveForward == 0.0) {
            if (moveStrafe == 0.0) {
                yaw = 180.0f;
            }
            else if (moveStrafe > 0.0) {
                yaw = 90.0f;
            }
            else if (moveStrafe < 0.0) {
                yaw = -90.0f;
            }
        }
        else if (moveForward > 0.0) {
            if (moveStrafe == 0.0) {
                yaw = 180.0f;
            }
            else if (moveStrafe > 0.0) {
                yaw = 135.0f;
            }
            else if (moveStrafe < 0.0) {
                yaw = -135.0f;
            }
        }
        else if (moveForward < 0.0) {
            if (moveStrafe == 0.0) {
                yaw = 0.0f;
            }
            else if (moveStrafe > 0.0) {
                yaw = 45.0f;
            }
            else if (moveStrafe < 0.0) {
                yaw = -45.0f;
            }
        }
        return (wrapAngleTo180_float(mc.thePlayer.rotationYaw) + yaw % 360 + 360) % 360;
    }

    public boolean canSafewalk() {
        if (!safeWalk.isToggled()) {
            return false;
        }
        if (usingFastScaffold()) {
            return false;
        }
        if (ModuleManager.tower.canTower()) {
            return false;
        }
        if (!isEnabled) {
            return false;
        }
        return true;
    }

    public int totalBlocks() {
        int totalBlocks = 0;
        for (int i = 0; i < 9; ++i) {
            final ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null && stack.getItem() instanceof ItemBlock && Utils.canBePlaced((ItemBlock) stack.getItem()) && stack.stackSize > 0) {
                totalBlocks += stack.stackSize;
            }
        }
        return totalBlocks;
    }

    private void placeBlock(int yOffset, int xOffset) {
        locateAndPlaceBlock(yOffset, xOffset);
        int input = (int) multiPlace.getInput();
        if (input >= 1) {
            locateAndPlaceBlock(yOffset, xOffset);
            if (input >= 2) {
                locateAndPlaceBlock(yOffset, xOffset);
                if (input >= 3) {
                    locateAndPlaceBlock(yOffset, xOffset);
                    if (input >= 4) {
                        locateAndPlaceBlock(yOffset, xOffset);
                    }
                }
            }
        }
    }

    private void locateAndPlaceBlock(int yOffset, int xOffset) {
        locateBlocks(yOffset, xOffset);
        if (blockInfo == null) {
            return;
        }
        lastPlacement = blockInfo;
        place(blockInfo);
        blockInfo = null;
    }

    private void locateBlocks(int yOffset, int xOffset) {
        List<PlaceData> blocksInfo = findBlocks(yOffset, xOffset);

        if (blocksInfo == null) {
            return;
        }

        double sumX = 0, sumY = !mc.thePlayer.onGround ? 0 : blocksInfo.get(0).blockPos.getY(), sumZ = 0;
        int index = 0;
        for (PlaceData blockssInfo : blocksInfo) {
            if (index > 1 || (!Utils.isDiagonal(false) && index > 0 && mc.thePlayer.onGround)) {
                break;
            }
            sumX += blockssInfo.blockPos.getX();
            if (!mc.thePlayer.onGround) {
                sumY += blockssInfo.blockPos.getY();
            }
            sumZ += blockssInfo.blockPos.getZ();
            index++;
        }

        double avgX = sumX / index;
        double avgY = !mc.thePlayer.onGround ? sumY / index : blocksInfo.get(0).blockPos.getY();
        double avgZ = sumZ / index;

        targetBlock = new Vec3(avgX, avgY, avgZ);

        PlaceData blockInfo2 = blocksInfo.get(0);
        int blockX = blockInfo2.blockPos.getX();
        int blockY = blockInfo2.blockPos.getY();
        int blockZ = blockInfo2.blockPos.getZ();
        EnumFacing blockFacing = blockInfo2.enumFacing;
        blockInfo = blockInfo2;

        double hitX = (blockX + 0.5D) + getCoord(blockFacing.getOpposite(), "x") * 0.5D;
        double hitY = (blockY + 0.5D) + getCoord(blockFacing.getOpposite(), "y") * 0.5D;
        double hitZ = (blockZ + 0.5D) + getCoord(blockFacing.getOpposite(), "z") * 0.5D;
        lookVec = new Vec3(0.5D + getCoord(blockFacing.getOpposite(), "x") * 0.5D, 0.5D + getCoord(blockFacing.getOpposite(), "y") * 0.5D, 0.5D + getCoord(blockFacing.getOpposite(), "z") * 0.5D);
        hitVec = new Vec3(hitX, hitY, hitZ);
        blockInfo.hitVec = hitVec;
    }

    private double getCoord(EnumFacing facing, String axis) {
        switch (axis) {
            case "x": return (facing == EnumFacing.WEST) ? -0.5 : (facing == EnumFacing.EAST) ? 0.5 : 0;
            case "y": return (facing == EnumFacing.DOWN) ? -0.5 : (facing == EnumFacing.UP) ? 0.5 : 0;
            case "z": return (facing == EnumFacing.NORTH) ? -0.5 : (facing == EnumFacing.SOUTH) ? 0.5 : 0;
        }
        return 0;
    }

    private List<PlaceData> findBlocks(int yOffset, int xOffset) {
        int x = (int) Math.floor(mc.thePlayer.posX + xOffset);
        int y = (int) Math.floor(((startYPos != -1) ? startYPos : mc.thePlayer.posY) + yOffset);
        int z = (int) Math.floor(mc.thePlayer.posZ);
        BlockPos base = new BlockPos(x, y - 1, z);
        if (!BlockUtils.replaceable(base)) {
            return null;
        }
        EnumFacing[] allFacings = getFacingsSorted();
        List<EnumFacing> validFacings = new ArrayList<>(5);
        for (EnumFacing facing : allFacings) {
            if (facing != EnumFacing.UP && placeConditions(facing, yOffset, xOffset)) {
                validFacings.add(facing);
            }
        }
        int maxLayer = 3;
        List<PlaceData> possibleBlocks = new ArrayList<>();

        main:
        for (int dy = 1; dy <= maxLayer; dy++) {
            BlockPos layerBase = new BlockPos(x, y - dy, z);
            if (dy == 1) {
                for (EnumFacing facing : validFacings) {
                    BlockPos neighbor = layerBase.offset(facing);
                    if (!BlockUtils.replaceable(neighbor) && !BlockUtils.isInteractable(BlockUtils.getBlock(neighbor))) {
                        possibleBlocks.add(new PlaceData(neighbor, facing.getOpposite()));
                    }
                }
            }
            for (EnumFacing facing : validFacings) {
                BlockPos adjacent = layerBase.offset(facing);
                if (BlockUtils.replaceable(adjacent)) {
                    for (EnumFacing nestedFacing : validFacings) {
                        BlockPos nestedNeighbor = adjacent.offset(nestedFacing);
                        if (!BlockUtils.replaceable(nestedNeighbor) && !BlockUtils.isInteractable(BlockUtils.getBlock(nestedNeighbor))) {
                            possibleBlocks.add(new PlaceData(nestedNeighbor, nestedFacing.getOpposite()));
                        }
                    }
                }
            }
        }

        return possibleBlocks.isEmpty() ? null : possibleBlocks;
    }

    private EnumFacing[] getFacingsSorted() {
        EnumFacing lastFacing = EnumFacing.getHorizontal(MathHelper.floor_double((((IAccessorEntityPlayerSP)mc.thePlayer).getLastReportedYaw() * 4.0F / 360.0F) + 0.5D) & 3);

        EnumFacing perpClockwise = lastFacing.rotateY();
        EnumFacing perpCounterClockwise = lastFacing.rotateYCCW();

        EnumFacing opposite = lastFacing.getOpposite();

        float yaw = ((IAccessorEntityPlayerSP)mc.thePlayer).getLastReportedYaw() % 360;
        if (yaw > 180) {
            yaw -= 360;
        }
        else if (yaw < -180) {
            yaw += 360;
        }

        // Calculates the difference from the last placed angle and gets the closest one
        float diffClockwise = Math.abs(wrapAngleTo180_float(yaw - getFacingAngle(perpClockwise)));
        float diffCounterClockwise = Math.abs(wrapAngleTo180_float(yaw - getFacingAngle(perpCounterClockwise)));

        EnumFacing firstPerp, secondPerp;
        if (diffClockwise <= diffCounterClockwise) {
            firstPerp = perpClockwise;
            secondPerp = perpCounterClockwise;
        }
        else {
            firstPerp = perpCounterClockwise;
            secondPerp = perpClockwise;
        }

        return new EnumFacing[]{EnumFacing.UP, EnumFacing.DOWN, lastFacing, firstPerp, secondPerp, opposite};
    }

    private float getFacingAngle(EnumFacing facing) {
        switch (facing) {
            case WEST:
                return 90;
            case NORTH:
                return 180;
            case EAST:
                return -90;
            default:
                return 0;
        }
    }

    private boolean placeConditions(EnumFacing enumFacing, int yCondition, int xCondition) {
        if (xCondition == -1) {
            return enumFacing == EnumFacing.EAST;
        }
        if (yCondition == 1) {
            return enumFacing == EnumFacing.DOWN;
        }
        return true;
    }

    /*private boolean allowedFaces(EnumFacing enumFacing) {
        if (yaw >= 0 && yaw < 90) {
            //Utils.print("1");
            //west south
            return enumFacing == EnumFacing.DOWN || enumFacing == EnumFacing.WEST || enumFacing == EnumFacing.SOUTH;
        }
        else if (yaw >= 90 && yaw < 180) {
            //Utils.print("2");
            //north west
            return enumFacing == EnumFacing.DOWN || enumFacing == EnumFacing.NORTH || enumFacing == EnumFacing.WEST;
        }
        else if (yaw == 180 || yaw >= -180 && yaw < -90) {
            //Utils.print("3");
            //north east
            return enumFacing == EnumFacing.DOWN || enumFacing == EnumFacing.NORTH || enumFacing == EnumFacing.EAST;
        }
        else if (yaw >= -90 && yaw <= 0) {
            //Utils.print("4");
            //east south
            return enumFacing == EnumFacing.DOWN || enumFacing == EnumFacing.EAST || enumFacing == EnumFacing.SOUTH;
        }

        return false;
    }*/

    float applyGcd(float value) {
        float gcd = 0.2F * 0.2F * 0.2F * 8.0F;
        return (float) ((double) value - (double) value % ((double) gcd * 0.15D));
    }

    public float getMotionYaw() {
        return (float) Math.toDegrees(Math.atan2(mc.thePlayer.motionZ, mc.thePlayer.motionX)) - 90.0F;
    }

    private int getSpeedLevel() {
        for (PotionEffect potionEffect : mc.thePlayer.getActivePotionEffects()) {
            if (potionEffect.getEffectName().equals("potion.moveSpeed")) {
                return potionEffect.getAmplifier() + 1;
            }
            return 0;
        }
        return 0;
    }

    double[] speedLevels = {0.48, 0.5, 0.52, 0.58, 0.68};

    double getSpeed(int speedLevel) {
        if (speedLevel >= 0) {
            return speedLevels[speedLevel];
        }
        return speedLevels[0];
    }

    double[] floatSpeedLevels = {0.2, 0.22, 0.28, 0.29, 0.3};

    double getFloatSpeed(int speedLevel) {
        double min = 0;
        double value = 0;
        double input = (motion.getInput() / 100);
        if (mc.thePlayer.moveStrafing != 0 && mc.thePlayer.moveForward != 0) min = 0.003;
        value = floatSpeedLevels[0] - min;
        if (speedLevel >= 0) {
            value = floatSpeedLevels[speedLevel] - min;
        }
        value *= input;
        return value;
    }

    private void handleMotion() {
        if (handleFastScaffolds() > 0 || ModuleManager.tower.canTower()) {
            return;
        }
        double input = (motion.getInput() / 100);
        mc.thePlayer.motionX *= input;
        mc.thePlayer.motionZ *= input;
    }

    public float hardcodedYaw() {
        float simpleYaw = 0F;
        float f = 0.8F;

        if (mc.thePlayer.moveForward >= f) {
            simpleYaw -= 180;
            if (mc.thePlayer.moveStrafing >= f) simpleYaw += 45;
            if (mc.thePlayer.moveStrafing <= -f) simpleYaw -= 45;
        }
        else if (mc.thePlayer.moveForward == 0) {
            simpleYaw -= 180;
            if (mc.thePlayer.moveStrafing >= f) simpleYaw += 90;
            if (mc.thePlayer.moveStrafing <= -f) simpleYaw -= 90;
        }
        else if (mc.thePlayer.moveForward <= -f) {
            if (mc.thePlayer.moveStrafing >= f) simpleYaw -= 45;
            if (mc.thePlayer.moveStrafing <= -f) simpleYaw += 45;
        }
        return simpleYaw;
    }

    public boolean holdingBlocks() {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (autoSwap.isToggled() && ModuleManager.autoSwap.spoofItem.isToggled() && lastSlot.get() != mc.thePlayer.inventory.currentItem && totalBlocks() > 0) {
            ((IMixinItemRenderer) mc.getItemRenderer()).setCancelUpdate(true);
            ((IMixinItemRenderer) mc.getItemRenderer()).setCancelReset(true);
        }
        if (!autoSwap.isToggled() || getSlot() == -1) {
            if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock) heldItem.getItem())) {
                return false;
            }
        }
        return true;
    }

    private double getMovementAngle() {
        double angle = Math.toDegrees(Math.atan2(-mc.thePlayer.moveStrafing, mc.thePlayer.moveForward));
        return angle == -0 ? 0 : angle;
    }

    private int getSlot() {
        int slot = -1;
        int highestStack = -1;
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        for (int i = 0; i < 9; ++i) {
            final ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack != null && itemStack.getItem() instanceof ItemBlock && Utils.canBePlaced((ItemBlock) itemStack.getItem()) && itemStack.stackSize > 0) {
                if (Utils.getBedwarsStatus() == 2 && ((ItemBlock) itemStack.getItem()).getBlock() instanceof BlockTNT) {
                    continue;
                }
                if (itemStack != null && heldItem != null && (heldItem.getItem() instanceof ItemBlock) && Utils.canBePlaced((ItemBlock) heldItem.getItem()) && ModuleManager.autoSwap.sameType.isToggled() && !(itemStack.getItem().getClass().equals(heldItem.getItem().getClass()))) {
                    continue;
                }
                if (itemStack.stackSize > highestStack) {
                    highestStack = itemStack.stackSize;
                    slot = i;
                }
            }
        }
        return slot;
    }

    public static boolean bypassRots() {
        return (ModuleManager.scaffold.rotation.getInput() == 2 || ModuleManager.scaffold.rotation.getInput() == 0);
    }

    public boolean setSlot() {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        int slot = getSlot();
        if (slot == -1) {
            return false;
        }
        if (blockSlot == -1) {
            blockSlot = slot;
        }
        if (lastSlot.get() == -1) {
            lastSlot.set(mc.thePlayer.inventory.currentItem);
        }
        if (autoSwap.isToggled() && blockSlot != -1) {
            if (ModuleManager.autoSwap.swapToGreaterStack.isToggled()) {
                mc.thePlayer.inventory.currentItem = slot;
                spoofSlot = slot;
            }
            else {
                if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock) heldItem.getItem()) || mc.thePlayer.getHeldItem().stackSize <= ModuleManager.autoSwap.swapAt.getInput()) {
                    mc.thePlayer.inventory.currentItem = slot;
                    spoofSlot = slot;
                }
            }
        }

        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock) heldItem.getItem())) {
            blockSlot = -1;
            return false;
        }
        return true;
    }


    static class PlaceData {
        EnumFacing enumFacing;
        BlockPos blockPos;
        Vec3 hitVec;

        PlaceData(BlockPos blockPos, EnumFacing enumFacing) {
            this.enumFacing = enumFacing;
            this.blockPos = blockPos;
        }

        public PlaceData(EnumFacing enumFacing, BlockPos blockPos) {
            this.enumFacing = enumFacing;
            this.blockPos = blockPos;
        }
    }
}
