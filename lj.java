package keystrokesmod.module.impl.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SlotUpdateEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityPlayerSP;
import keystrokesmod.mixin.interfaces.IMixinItemRenderer;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.setting.Setting;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.ScaffoldBlockCount;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.Utils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class Scaffold extends Module {
  private final SliderSetting motion;
  
  public SliderSetting rotation;
  
  public SliderSetting fakeRotation;
  
  public SliderSetting sprint;
  
  private SliderSetting floatFirstJump;
  
  public SliderSetting fastScaffold;
  
  private SliderSetting multiPlace;
  
  public ButtonSetting autoSwap;
  
  private ButtonSetting fastOnRMB;
  
  public ButtonSetting highlightBlocks;
  
  private ButtonSetting jumpFacingForward;
  
  public ButtonSetting safeWalk;
  
  public ButtonSetting showBlockCount;
  
  private ButtonSetting silentSwing;
  
  private ButtonSetting prioritizeSprintWithSpeed;
  
  private String[] rotationModes = new String[] { ", "Simple", "Offset", "Precise" };
  
  private String[] fakeRotationModes = new String[] { ", "None", "Strict", "Smooth", "Spin", "Precise" };
  
  private String[] sprintModes = new String[] { ", "Vanilla", "Float" };
  
  private String[] fastScaffoldModes = new String[] { ", "Jump A", "Jump B", "Jump B Low", "Jump E", "Keep-Y", "Keep-Y Low" };
  
  private String[] multiPlaceModes = new String[] { ", "1 extra", "2 extra", "3 extra", "4 extra" };
  
  public Map<BlockPos, Timer> highlight = new HashMap<>();
  
  public boolean canBlockFade;
  
  private ScaffoldBlockCount scaffoldBlockCount;
  
  public AtomicInteger lastSlot = new AtomicInteger(-1);
  
  private int spoofSlot;
  
  public boolean hasSwapped;
  
  private int blockSlot = -1;
  
  public boolean hasPlaced;
  
  private boolean finishProcedure;
  
  private boolean stopUpdate;
  
  private boolean stopUpdate2;
  
  private PlaceData lastPlacement;
  
  private EnumFacing[] facings = new EnumFacing[] { EnumFacing.EAST, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.UP };
  
  private BlockPos[] offsets = new BlockPos[] { new BlockPos(-1, 0, 0), new BlockPos(1, 0, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, -1), new BlockPos(0, -1, 0) };
  
  private Vec3 targetBlock;
  
  private PlaceData blockInfo;
  
  private Vec3 blockPos;
  
  private Vec3 hitVec;
  
  private Vec3 lookVec;
  
  private boolean rotateForward;
  
  private double startYPos = -1.0D;
  
  public boolean fastScaffoldKeepY;
  
  public boolean firstKeepYPlace;
  
  private boolean rotatingForward;
  
  private int keepYTicks;
  
  public boolean lowhop;
  
  private int rotationDelay;
  
  private boolean floatJumped;
  
  private boolean floatStarted;
  
  private boolean floatWasEnabled;
  
  private boolean floatKeepY;
  
  public int offsetDelay;
  
  public boolean placedVP;
  
  public boolean jump;
  
  private int floatTicks;
  
  public boolean blink;
  
  public boolean canSprint;
  
  public boolean canSprint2;
  
  private boolean idle;
  
  private int idleTicks;
  
  private boolean didJump;
  
  public boolean moduleEnabled;
  
  public boolean isEnabled;
  
  private boolean disabledModule;
  
  private boolean dontDisable;
  
  private boolean towerEdge;
  
  private int disableTicks;
  
  private int scaffoldTicks;
  
  private boolean was451;
  
  private boolean was452;
  
  private float minPitch;
  
  private float minOffset;
  
  private float pOffset;
  
  private float edge;
  
  private long firstStroke;
  
  private long yawEdge;
  
  private long vlS;
  
  private long swDelay;
  
  private float lastEdge2;
  
  private float yawAngle;
  
  private float theYaw;
  
  private boolean enabledOffGround = false;
  
  private float[] blockRotations;
  
  public float yaw;
  
  public float pitch;
  
  public float blockYaw;
  
  public float yawOffset;
  
  public float lastOffset;
  
  private boolean set2;
  
  private float maxOffset;
  
  private int sameMouse;
  
  private int randomF;
  
  private int yawChanges;
  
  private int dynamic;
  
  private boolean getVTR;
  
  private boolean resetm;
  
  private float VTRY;
  
  private float normalYaw;
  
  private float normalPitch;
  
  private int switchvl;
  
  private int dt;
  
  private float getSmooth;
  
  private float lastYawS;
  
  private float smoothedYaw;
  
  private boolean neg;
  
  private float yawWithOffset;
  
  private int rt;
  
  private float fakeYaw;
  
  private float fakePitch;
  
  private float fakeYaw1;
  
  private float fakeYaw2;
  
  private float lastMY;
  
  private float lastey;
  
  double[] speedLevels;
  
  double[] floatSpeedLevels;
  
  public Scaffold() {
    super("Scaffold", Module.category.player);
    this.speedLevels = new double[] { 0.48D, 0.5D, 0.52D, 0.58D, 0.68D };
    this.floatSpeedLevels = new double[] { 0.2D, 0.22D, 0.27D, 0.29D, 0.3D };
    registerSetting((Setting)(this.motion = new SliderSetting("Motion", "%", 100.0D, 50.0D, 150.0D, 1.0D)));
    registerSetting((Setting)(this.rotation = new SliderSetting("Rotation", 1, this.rotationModes)));
    registerSetting((Setting)(this.fakeRotation = new SliderSetting("Rotation (fake)", 0, this.fakeRotationModes)));
    registerSetting((Setting)(this.sprint = new SliderSetting("Sprint mode", 0, this.sprintModes)));
    registerSetting((Setting)(this.prioritizeSprintWithSpeed = new ButtonSetting("Prioritize sprint with speed", false)));
    registerSetting((Setting)(this.floatFirstJump = new SliderSetting("jump speed", "%", 100.0D, 50.0D, 100.0D, 1.0D)));
    registerSetting((Setting)(this.fastScaffold = new SliderSetting("Fast scaffold", 0, this.fastScaffoldModes)));
    registerSetting((Setting)(this.multiPlace = new SliderSetting("Multi-place", 0, this.multiPlaceModes)));
    registerSetting((Setting)(this.autoSwap = new ButtonSetting("Auto swap", true)));
    registerSetting((Setting)(this.fastOnRMB = new ButtonSetting("Fast on RMB", true)));
    registerSetting((Setting)(this.highlightBlocks = new ButtonSetting("Highlight blocks", true)));
    registerSetting((Setting)(this.jumpFacingForward = new ButtonSetting("Jump facing forward", false)));
    registerSetting((Setting)(this.safeWalk = new ButtonSetting("Safewalk", true)));
    registerSetting((Setting)(this.showBlockCount = new ButtonSetting("Show block count", true)));
    registerSetting((Setting)(this.silentSwing = new ButtonSetting("Silent swing", false)));
    this.alwaysOn = true;
  }
  
  public void guiUpdate() {
    this.prioritizeSprintWithSpeed.setVisible((this.sprint.getInput() > 0.0D), this);
    this.floatFirstJump.setVisible((this.sprint.getInput() == 2.0D), this);
  }
  
  public void onDisable() {
    if (ModuleManager.tower.canTower() && (ModuleManager.tower.dCount == 0 || !Utils.isMoving()))
      this.towerEdge = true; 
    this.disabledModule = true;
    this.moduleEnabled = false;
    if (!this.isEnabled)
      this.scaffoldBlockCount.beginFade(); 
  }
  
  public void onEnable() {
    this.dt = 0;
    this.isEnabled = true;
    this.moduleEnabled = true;
    ModuleUtils.fadeEdge = 0;
    this.edge = -9.9999994E8F;
    this.minPitch = 80.0F;
    if (!mc.field_71439_g.field_70122_E) {
      this.rotationDelay = 3;
      this.enabledOffGround = true;
    } 
    this.lastEdge2 = mc.field_71439_g.field_70177_z;
    FMLCommonHandler.instance().bus().register(this.scaffoldBlockCount = new ScaffoldBlockCount(mc));
    this.lastSlot.set(-1);
    this.hasPlaced = false;
  }
  
  @SubscribeEvent(priority = EventPriority.HIGHEST)
  public void onMouse(MouseEvent e) {
    if (!this.isEnabled)
      return; 
    if (e.button == 0 || e.button == 1)
      e.setCanceled(true); 
  }
  
  @SubscribeEvent
  public void onPreMotion(PreMotionEvent e) {
    if (!Utils.nullCheck())
      return; 
    this.normalYaw = mc.field_71439_g.field_70177_z;
    this.normalPitch = mc.field_71439_g.field_70125_A;
    if (this.dt > 0)
      return; 
    if (this.fakeRotation.getInput() > 0.0D) {
      if (this.fakeRotation.getInput() == 1.0D) {
        this.fakeYaw = this.normalYaw;
        this.fakePitch = this.normalPitch;
      } else if (this.fakeRotation.getInput() == 2.0D) {
        this.fakeYaw = this.fakeYaw1;
        if (this.blockRotations != null) {
          this.fakePitch = this.blockRotations[1] + 5.0F;
        } else {
          this.fakePitch = 80.0F;
        } 
      } else if (this.fakeRotation.getInput() == 3.0D) {
        this.fakeYaw2 = mc.field_71439_g.field_70177_z - hardcodedYaw();
        float yawDifference = Utils.getAngleDifference(this.lastEdge2, this.fakeYaw2);
        float smoothingFactor = 0.35F;
        this.fakeYaw2 = this.lastEdge2 + yawDifference * smoothingFactor;
        this.lastEdge2 = this.fakeYaw2;
        this.fakeYaw = this.fakeYaw2;
        if (this.blockRotations != null) {
          this.fakePitch = this.blockRotations[1] + 5.0F;
        } else {
          this.fakePitch = 80.0F;
        } 
      } else if (this.fakeRotation.getInput() == 4.0D) {
        this.fakeYaw += 25.714285F;
        this.fakePitch = 90.0F;
      } else if (this.fakeRotation.getInput() == 5.0D) {
        if (this.blockRotations != null) {
          this.fakeYaw2 = this.blockRotations[0];
          this.fakePitch = this.blockRotations[1];
        } else {
          this.fakeYaw2 = mc.field_71439_g.field_70177_z - hardcodedYaw() - 180.0F;
          this.fakePitch = 88.0F;
        } 
        float yawDifference = Utils.getAngleDifference(this.lastEdge2, this.fakeYaw2);
        float smoothingFactor = 0.35F;
        this.fakeYaw2 = this.lastEdge2 + yawDifference * smoothingFactor;
        this.lastEdge2 = this.fakeYaw2;
        this.fakeYaw = this.fakeYaw2;
      } 
      RotationUtils.setFakeRotations(this.fakeYaw, this.fakePitch);
    } 
    if (!this.isEnabled) {
      this.dt++;
      return;
    } 
    if (Utils.isMoving()) {
      this.scaffoldTicks++;
    } else {
      this.scaffoldTicks = 0;
    } 
    this.canBlockFade = true;
    if (Utils.keysDown() && usingFastScaffold() && !ModuleManager.invmove.active() && this.fastScaffold.getInput() >= 1.0D && !ModuleManager.tower.canTower() && !LongJump.function) {
      if (mc.field_71439_g.field_70122_E && Utils.isMoving() && this.scaffoldTicks > 1) {
        this.jump = true;
        rotateForward(true);
        if (this.startYPos == -1.0D || Math.abs(this.startYPos - mc.field_71439_g.field_70163_u) > 2.0D) {
          this.startYPos = mc.field_71439_g.field_70163_u;
          this.fastScaffoldKeepY = true;
        } 
      } 
    } else if (this.fastScaffoldKeepY) {
      this.fastScaffoldKeepY = this.firstKeepYPlace = false;
      this.startYPos = -1.0D;
      this.keepYTicks = 0;
    } 
    if (this.sprint.getInput() == 1.0D)
      this.canSprint2 = (!usingFastScaffold() && !this.fastScaffoldKeepY && !ModuleManager.tower.canTower() && !LongJump.function); 
    if (this.sprint.getInput() == 2.0D) {
      if (Utils.isMoving() && this.idle && this.idleTicks++ > 4) {
        if (this.floatKeepY)
          this.startYPos = -1.0D; 
        this.floatStarted = this.floatJumped = this.floatKeepY = this.floatWasEnabled = false;
        this.floatTicks = this.rt = 0;
        this.canSprint2 = false;
        this.offsetDelay = 0;
        this.idle = false;
        this.idleTicks = 0;
        ModuleUtils.groundTicks = 9;
      } 
      if (!usingFastScaffold() && !this.fastScaffoldKeepY && !ModuleManager.tower.canTower() && !LongJump.function) {
        if (ModuleUtils.stillTicks > 2 && mc.field_71439_g.field_70122_E) {
          this.idle = true;
          this.idleTicks = 0;
        } 
        this.floatWasEnabled = true;
        if (!this.floatStarted && this.offsetDelay == 0) {
          if (ModuleUtils.groundTicks > 8 && mc.field_71439_g.field_70122_E) {
            this.canSprint2 = true;
            this.floatKeepY = true;
            this.startYPos = e.posY;
            rotateForward(true);
            mc.field_71439_g.func_70664_aZ();
            if (Utils.isMoving()) {
              double fvl = (getSpeed(getSpeedLevel()) - Utils.randomizeDouble(3.0E-4D, 1.0E-4D)) * this.floatFirstJump.getInput() / 100.0D;
              Utils.setSpeed(fvl);
            } 
            this.floatJumped = true;
          } else if (ModuleUtils.groundTicks <= 8 && mc.field_71439_g.field_70122_E) {
            this.floatStarted = true;
          } 
          if (this.floatJumped && !mc.field_71439_g.field_70122_E)
            this.floatStarted = true; 
        } 
        if (this.floatStarted && mc.field_71439_g.field_70122_E) {
          this.floatKeepY = false;
          this.startYPos = -1.0D;
          if (this.moduleEnabled && mc.field_71439_g.field_70163_u % 1.0D == 0.0D) {
            this.floatTicks++;
            this.rotateForward = false;
            this.rotationDelay = 0;
            ModuleManager.tower.delay = false;
            this.canSprint2 = true;
            if (this.didJump && !mc.field_71474_y.field_74314_A.func_151470_d() && mc.field_71439_g.field_70122_E) {
              mc.field_71439_g.func_70664_aZ();
            } else if (!this.idle) {
              switch (this.floatTicks) {
                case 1:
                case 4:
                case 6:
                  ModuleManager.tower.delay = true;
                  ModuleManager.tower.delayTicks = 0;
                  e.setPosY(e.getPosY() + 0.001D);
                  break;
                case 8:
                  this.floatTicks = 0;
                  break;
              } 
              if (Utils.isMoving() && !ModuleManager.invmove.active())
                Utils.setSpeed(getFloatSpeed(getSpeedLevel())); 
            } 
            ModuleUtils.groundTicks = 0;
            this.offsetDelay = 2;
          } 
        } 
      } else if (this.floatWasEnabled && this.moduleEnabled) {
        if (mc.field_71439_g.field_70122_E)
          Utils.setSpeed(Utils.getHorizontalSpeed() / 2.0D); 
        if (this.floatKeepY)
          this.startYPos = -1.0D; 
        this.floatStarted = this.floatJumped = this.floatKeepY = this.floatWasEnabled = false;
        this.floatTicks = this.rt = 0;
        this.canSprint2 = false;
        this.idle = false;
        this.idleTicks = 0;
      } 
      this.didJump = false;
      if (ModuleManager.tower.delay && mc.field_71474_y.field_74314_A.func_151470_d()) {
        this.didJump = true;
        this.canSprint2 = true;
      } 
    } 
    if (this.blockRotations != null) {
      if (mc.field_71439_g.field_70177_z == this.lastOffset) {
        this.sameMouse++;
      } else {
        this.sameMouse = 0;
        this.yawChanges++;
      } 
      if (this.sameMouse > 2)
        this.yawChanges = 0; 
      this.lastOffset = mc.field_71439_g.field_70177_z;
      if (this.yawChanges > 15) {
        this.randomF = 1;
        this.yawEdge = Utils.time();
      } 
      if (this.yawEdge > 0L && Utils.time() - this.yawEdge > 500L)
        this.yawEdge = 0L; 
    } else {
      this.fakeYaw1 = mc.field_71439_g.field_70177_z - hardcodedYaw();
    } 
    this.dynamic = 0;
    if (this.targetBlock != null) {
      Vec3 lookAt = new Vec3(this.targetBlock.field_72450_a - this.lookVec.field_72450_a, this.targetBlock.field_72448_b - this.lookVec.field_72448_b, this.targetBlock.field_72449_c - this.lookVec.field_72449_c);
      this.blockRotations = RotationUtils.getRotations(lookAt);
      this.targetBlock = null;
      this.fakeYaw1 = mc.field_71439_g.field_70177_z - hardcodedYaw();
      if (this.yawEdge == 0L) {
        this.randomF = Utils.randomizeInt(0, 9);
        this.yawEdge = Utils.time();
      } 
      this.dynamic++;
    } 
    this.randomF = 0;
  }
  
  @SubscribeEvent(priority = EventPriority.LOW)
  public void onClientRotation(ClientRotationEvent e) {
    if (!Utils.nullCheck())
      return; 
    this.canSprint = false;
    if (!this.isEnabled) {
      this.lastMY = getMotionYaw();
      return;
    } 
    switch ((int)this.rotation.getInput()) {
      case 1:
        this.yaw = mc.field_71439_g.field_70177_z - hardcodedYaw();
        if (this.blockRotations != null) {
          this.pitch = this.blockRotations[1];
          if (this.pitch >= 90.0F)
            this.pitch = 90.0F; 
        } else {
          this.pitch = 74.0F;
        } 
        e.setRotations(Float.valueOf(this.yaw), Float.valueOf(this.pitch));
        this.theYaw = this.yaw;
        break;
      case 2:
        offsetRots(e);
        break;
      case 3:
        preciseRots(e);
        break;
    } 
    if (this.edge != 1.0F) {
      this.switchvl++;
      this.edge = 1.0F;
    } 
    if (mc.field_71439_g.field_70122_E)
      this.enabledOffGround = false; 
    if (this.rotationDelay > 0)
      this.rotationDelay--; 
    if (ModuleUtils.inAirTicks >= 1)
      this.rotateForward = false; 
    if (this.rotateForward && this.jumpFacingForward.isToggled()) {
      if (this.rotation.getInput() > 0.0D) {
        float sideYaw = mc.field_71439_g.field_70177_z - hardcodedYaw() - 90.0F + (float)Utils.randomizeDouble(0.12924D, 4.87653D);
        float forwardYaw = mc.field_71439_g.field_70177_z - getForwardYaw();
        e.setYaw(Float.valueOf(forwardYaw));
        e.setPitch(Float.valueOf(60.0F));
        this.blockRotations = null;
        this.theYaw = forwardYaw;
      } 
    } else {
      this.rotatingForward = false;
    } 
    if (this.jump && mc.field_71439_g.field_70122_E)
      mc.field_71439_g.func_70031_b(true); 
    if (!Settings.movementFix.isToggled() && mc.field_71439_g.field_70159_w == 0.0D && mc.field_71439_g.field_70179_y == 0.0D && this.blockRotations != null)
      e.setYaw(Float.valueOf(this.blockRotations[0])); 
    if (ModuleManager.tower.isVerticalTowering()) {
      if (this.blockRotations != null && (!this.getVTR || ModuleManager.tower.ebDelay <= 1 || !ModuleManager.tower.firstVTP)) {
        this.VTRY = this.blockRotations[0];
        this.getVTR = true;
      } 
      if (this.getVTR)
        e.setYaw(Float.valueOf(this.VTRY)); 
      if (ModuleManager.tower.yaw != 0.0F)
        e.setYaw(Float.valueOf(ModuleManager.tower.yaw)); 
      if (ModuleManager.tower.pitch != 0.0F)
        e.setPitch(Float.valueOf(ModuleManager.tower.pitch)); 
    } else {
      this.getVTR = false;
    } 
    this.lastey = this.theYaw;
  }
  
  private void handleV() {
    float yawBackwards2 = MathHelper.func_76142_g(mc.field_71439_g.field_70177_z) - hardcodedYaw();
    double dif = (this.lastMY - getMotionYaw());
    double v = 2.5D;
    float offset = this.yawWithOffset - yawBackwards2;
    if (offset > this.yawAngle || offset < -this.yawAngle) {
      this.lastYawS = this.getSmooth = this.smoothedYaw = this.yaw;
      return;
    } 
    if ((dif >= 0.0D && dif < v) || (dif <= 0.0D && dif > -v) || mc.field_71439_g.field_70122_E) {
      this.lastYawS = this.getSmooth = this.smoothedYaw = this.yaw;
      return;
    } 
    this.getSmooth = this.yaw;
    float yawDifference = Utils.getAngleDifference(this.lastYawS, this.getSmooth);
    float smoothingFactor = 0.1F;
    this.getSmooth = this.lastYawS + yawDifference * smoothingFactor;
    this.lastYawS = this.getSmooth;
    this.smoothedYaw = this.getSmooth;
    this.yaw = this.smoothedYaw;
  }
  
  private void handleSmoothing() {
    handleV();
    this.lastMY = getMotionYaw();
  }
  
  private void offsetRots(ClientRotationEvent e) {
    float moveAngle = (float)getMovementAngle();
    float relativeYaw = mc.field_71439_g.field_70177_z + moveAngle;
    float normalizedYaw = (relativeYaw % 360.0F + 360.0F) % 360.0F;
    float quad = normalizedYaw % 90.0F;
    float side = MathHelper.func_76142_g(getMotionYaw() - this.yaw);
    float yawBackwards = MathHelper.func_76142_g(mc.field_71439_g.field_70177_z) - hardcodedYaw();
    float blockYawOffset = MathHelper.func_76142_g(yawBackwards - this.blockYaw);
    long strokeDelay = 250L;
    float first = 77.0F;
    float sec = first;
    if (quad <= 5.0F || quad >= 85.0F) {
      this.yawAngle = 121.525F;
      this.minOffset = 11.0F;
      this.minPitch = first;
    } 
    if ((quad > 5.0F && quad <= 15.0F) || (quad >= 75.0F && quad < 85.0F)) {
      this.yawAngle = 123.425F;
      this.minOffset = 9.0F;
      this.minPitch = first;
    } 
    if ((quad > 15.0F && quad <= 25.0F) || (quad >= 65.0F && quad < 75.0F)) {
      this.yawAngle = 127.425F;
      this.minOffset = 8.0F;
      this.minPitch = first;
    } 
    if ((quad > 25.0F && quad <= 32.0F) || (quad >= 58.0F && quad < 65.0F)) {
      this.yawAngle = 131.325F;
      this.minOffset = 7.0F;
      this.minPitch = sec;
    } 
    if ((quad > 32.0F && quad <= 38.0F) || (quad >= 52.0F && quad < 58.0F)) {
      this.yawAngle = 133.525F;
      this.minOffset = 6.0F;
      this.minPitch = sec;
    } 
    if ((quad > 38.0F && quad <= 42.0F) || (quad >= 48.0F && quad < 52.0F)) {
      this.yawAngle = 135.825F;
      this.minOffset = 4.0F;
      this.minPitch = sec;
    } 
    if ((quad > 42.0F && quad <= 45.0F) || (quad >= 45.0F && quad < 48.0F)) {
      this.yawAngle = 138.625F;
      this.minOffset = 3.0F;
      this.minPitch = sec;
    } 
    float offset = this.yawAngle;
    float nigger = 0.0F;
    if (quad > 45.0F) {
      nigger = 10.0F;
    } else {
      nigger = -10.0F;
    } 
    if (this.switchvl > 0) {
      this.firstStroke = Utils.time();
      this.switchvl = 0;
      this.vlS = 0L;
      this.resetm = true;
    } else {
      this.vlS = Utils.time();
    } 
    if (this.firstStroke > 0L && Utils.time() - this.firstStroke > strokeDelay)
      this.firstStroke = 0L; 
    if (Utils.fallDist() <= 2.0D && Utils.getHorizontalSpeed() > 0.1D)
      this.enabledOffGround = false; 
    if (this.enabledOffGround) {
      if (this.blockRotations != null) {
        this.yaw = this.blockRotations[0];
        this.pitch = this.blockRotations[1];
      } else {
        this.yaw = mc.field_71439_g.field_70177_z - hardcodedYaw() - nigger;
        this.pitch = this.minPitch;
      } 
      e.setRotations(Float.valueOf(this.yaw), Float.valueOf(this.pitch));
      return;
    } 
    if (this.blockRotations != null) {
      this.blockYaw = this.blockRotations[0];
      this.pitch = this.blockRotations[1];
      this.yawOffset = blockYawOffset;
      if (this.pitch < this.minPitch)
        this.pitch = this.minPitch; 
    } else {
      this.pitch = this.minPitch;
      if (this.edge == 1.0F && (quad <= 3.0F || quad >= 87.0F) && !Utils.scaffoldDiagonal(false))
        this.firstStroke = Utils.time(); 
      this.yawOffset = 5.0F;
      this.dynamic = 2;
    } 
    if (!Utils.isMoving() || Utils.getHorizontalSpeed() == 0.0D) {
      e.setRotations(Float.valueOf(this.theYaw), Float.valueOf(this.pitch));
      return;
    } 
    float motionYaw = getMotionYaw();
    float newYaw = motionYaw - offset * Math.signum(MathHelper.func_76142_g(motionYaw - this.yaw));
    this.yaw = MathHelper.func_76142_g(newYaw);
    if (quad > 3.0F && quad < 87.0F && this.dynamic > 0)
      if (quad < 45.0F) {
        if (this.firstStroke == 0L)
          if (side >= 0.0F) {
            this.set2 = false;
          } else {
            this.set2 = true;
          }  
        if (this.was452)
          this.switchvl++; 
        this.was451 = true;
        this.was452 = false;
      } else {
        if (this.firstStroke == 0L)
          if (side >= 0.0F) {
            this.set2 = true;
          } else {
            this.set2 = false;
          }  
        if (this.was451)
          this.switchvl++; 
        this.was452 = true;
        this.was451 = false;
      }  
    double minSwitch = !Utils.scaffoldDiagonal(false) ? 9.0D : 15.0D;
    if (side >= 0.0F) {
      if (this.yawOffset <= -minSwitch && this.firstStroke == 0L && this.dynamic > 0) {
        if (quad <= 3.0F || quad >= 87.0F) {
          if (this.set2)
            this.switchvl++; 
          this.set2 = false;
        } 
      } else if (this.yawOffset >= 0.0F && this.firstStroke == 0L && this.dynamic > 0 && (quad <= 3.0F || quad >= 87.0F) && this.yawOffset >= minSwitch) {
        if (!this.set2)
          this.switchvl++; 
        this.set2 = true;
      } 
      if (this.set2) {
        if (this.yawOffset <= 0.0F)
          this.yawOffset = 0.0F; 
        if (this.yawOffset >= this.minOffset)
          this.yawOffset = this.minOffset; 
        this.theYaw = this.yaw + offset * 2.0F - this.yawOffset;
        e.setRotations(Float.valueOf(this.theYaw), Float.valueOf(this.pitch));
        return;
      } 
    } else if (side <= 0.0F) {
      if (this.yawOffset >= minSwitch && this.firstStroke == 0L && this.dynamic > 0) {
        if (quad <= 3.0F || quad >= 87.0F) {
          if (this.set2)
            this.switchvl++; 
          this.set2 = false;
        } 
      } else if (this.yawOffset <= 0.0F && this.firstStroke == 0L && this.dynamic > 0 && (quad <= 3.0F || quad >= 87.0F) && this.yawOffset <= -minSwitch) {
        if (!this.set2)
          this.switchvl++; 
        this.set2 = true;
      } 
      if (this.set2) {
        if (this.yawOffset >= 0.0F)
          this.yawOffset = 0.0F; 
        if (this.yawOffset <= -this.minOffset)
          this.yawOffset = -this.minOffset; 
        this.theYaw = this.yaw - offset * 2.0F - this.yawOffset;
        e.setRotations(Float.valueOf(this.theYaw), Float.valueOf(this.pitch));
        return;
      } 
    } 
    if (side >= 0.0F) {
      if (this.yawOffset >= 0.0F)
        this.yawOffset = 0.0F; 
      if (this.yawOffset <= -this.minOffset)
        this.yawOffset = -this.minOffset; 
    } else if (side <= 0.0F) {
      if (this.yawOffset <= 0.0F)
        this.yawOffset = 0.0F; 
      if (this.yawOffset >= this.minOffset)
        this.yawOffset = this.minOffset; 
    } 
    this.theYaw = this.yaw - this.yawOffset;
    e.setRotations(Float.valueOf(this.theYaw), Float.valueOf(this.pitch));
  }
  
  private void offsetRots2(ClientRotationEvent e) {
    float moveAngle2 = (float)getMovementAngle();
    float relativeYaw2 = mc.field_71439_g.field_70177_z + moveAngle2;
    float normalizedYaw2 = (relativeYaw2 % 360.0F + 360.0F) % 360.0F;
    float quad2 = normalizedYaw2 % 90.0F;
    float yawBackwards2 = MathHelper.func_76142_g(mc.field_71439_g.field_70177_z) - hardcodedYaw();
    float blockYawOffset2 = MathHelper.func_76142_g(yawBackwards2 - this.blockYaw);
    long sDelay = 250L;
    if (this.switchvl > 0) {
      this.firstStroke = Utils.time();
      this.switchvl = 0;
      this.vlS = 0L;
      this.resetm = true;
    } else {
      this.vlS = Utils.time();
    } 
    if (this.firstStroke > 0L && System.currentTimeMillis() - this.firstStroke > sDelay)
      this.firstStroke = 0L; 
    float firstv = 77.5F;
    float secv = 77.5F;
    if (quad2 <= 5.0F || quad2 >= 85.0F) {
      this.yawAngle = 58.685F;
      this.minOffset = 13.0F;
      this.minPitch = firstv;
    } 
    if ((quad2 > 5.0F && quad2 <= 15.0F) || (quad2 >= 75.0F && quad2 < 85.0F)) {
      this.yawAngle = 56.185F;
      this.minOffset = 11.0F;
      this.minPitch = firstv;
    } 
    if ((quad2 > 15.0F && quad2 <= 25.0F) || (quad2 >= 65.0F && quad2 < 75.0F)) {
      this.yawAngle = 53.385F;
      this.minOffset = 9.0F;
      this.minPitch = firstv;
    } 
    if ((quad2 > 25.0F && quad2 <= 32.0F) || (quad2 >= 58.0F && quad2 < 65.0F)) {
      this.yawAngle = 50.385F;
      this.minOffset = 7.0F;
      this.minPitch = secv;
    } 
    if ((quad2 > 32.0F && quad2 <= 38.0F) || (quad2 >= 52.0F && quad2 < 58.0F)) {
      this.yawAngle = 48.185F;
      this.minOffset = 6.0F;
      this.minPitch = secv;
    } 
    if ((quad2 > 38.0F && quad2 <= 42.0F) || (quad2 >= 48.0F && quad2 < 52.0F)) {
      this.yawAngle = 46.185F;
      this.minOffset = 4.0F;
      this.minPitch = secv;
    } 
    if ((quad2 > 42.0F && quad2 <= 45.0F) || (quad2 >= 45.0F && quad2 < 48.0F)) {
      this.yawAngle = 42.885F;
      this.minOffset = 3.0F;
      this.minPitch = secv;
    } 
    if (this.blockRotations != null) {
      this.blockYaw = this.blockRotations[0];
      this.pitch = this.blockRotations[1];
      this.yawOffset = blockYawOffset2;
      if (this.pitch < this.minPitch)
        this.pitch = this.minPitch; 
    } else {
      this.pitch = this.minPitch;
      if (this.edge == 1.0F && (quad2 <= 3.0F || quad2 >= 87.0F) && !Utils.scaffoldDiagonal(false))
        this.switchvl++; 
      this.yawOffset = 5.0F;
      this.dynamic = 2;
    } 
    if (quad2 > 3.0F && quad2 < 87.0F && this.dynamic > 0)
      if (quad2 < 45.0F) {
        if (this.firstStroke == 0L)
          this.set2 = true; 
        if (this.was452)
          this.switchvl++; 
        this.was451 = true;
        this.was452 = false;
      } else {
        if (this.firstStroke == 0L)
          this.set2 = false; 
        if (this.was451)
          this.switchvl++; 
        this.was452 = true;
        this.was451 = false;
      }  
    double minSwitch2 = !Utils.scaffoldDiagonal(false) ? 9.0D : 15.0D;
    if (this.yawOffset <= -minSwitch2 && this.firstStroke == 0L && this.dynamic > 0) {
      if (quad2 <= 3.0F || quad2 >= 87.0F) {
        if (this.set2)
          this.switchvl++; 
        this.set2 = true;
      } 
    } else if (this.yawOffset >= 0.0F && this.firstStroke == 0L && this.dynamic > 0 && (quad2 <= 3.0F || quad2 >= 87.0F) && this.yawOffset >= minSwitch2) {
      if (!this.set2)
        this.switchvl++; 
      this.set2 = false;
    } 
    if (this.set2) {
      this.yaw = yawBackwards2;
      if (this.yawOffset <= 0.0F)
        this.yawOffset = 0.0F; 
      if (this.yawOffset >= this.minOffset)
        this.yawOffset = this.minOffset; 
      this.yawWithOffset = this.yaw + this.yawAngle - this.yawOffset;
      this.neg = false;
      handleSmoothing();
      e.setRotations(Float.valueOf(this.yaw + this.yawAngle - this.yawOffset), Float.valueOf(this.pitch));
      return;
    } 
    this.yaw = yawBackwards2;
    if (this.yawOffset >= 0.0F)
      this.yawOffset = 0.0F; 
    if (this.yawOffset <= -this.minOffset)
      this.yawOffset = -this.minOffset; 
    this.yawWithOffset = this.yaw - this.yawAngle - this.yawOffset;
    this.neg = true;
    handleSmoothing();
    e.setRotations(Float.valueOf(this.yaw - this.yawAngle - this.yawOffset), Float.valueOf(this.pitch));
  }
  
  private void preciseRots(ClientRotationEvent e) {
    if (this.blockRotations != null) {
      this.yaw = this.blockRotations[0];
      this.pitch = this.blockRotations[1];
    } else {
      this.yaw = mc.field_71439_g.field_70177_z - hardcodedYaw();
      this.pitch = 80.0F;
    } 
    e.setRotations(Float.valueOf(this.yaw), Float.valueOf(this.pitch));
    this.theYaw = this.yaw;
  }
  
  private boolean canJump() {
    return (!ModuleManager.tower.canTower() && !ModuleManager.tower.delay && Utils.jumpDown());
  }
  
  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void onPostPlayerInput(PostPlayerInputEvent e) {
    if (!ModuleManager.scaffold.isEnabled)
      return; 
    if ((this.fastScaffoldKeepY && !this.floatKeepY && Settings.movementFix.isToggled() && !this.jump && !ModuleManager.tower.delay) || canJump())
      return; 
    mc.field_71439_g.field_71158_b.field_78901_c = false;
    if (this.jump && mc.field_71439_g.field_70122_E) {
      this.canSprint = true;
      if (Settings.movementFix.isToggled()) {
        mc.field_71439_g.field_71158_b.field_78901_c = true;
      } else {
        mc.field_71439_g.func_70664_aZ();
      } 
      this.jump = false;
      if (!Settings.movementFix.isToggled())
        Utils.setSpeed(getSpeed(getSpeedLevel()) * ModuleUtils.applyFrictionMulti()); 
      if (this.fastScaffold.getInput() == 6.0D || (this.fastScaffold.getInput() == 3.0D && this.firstKeepYPlace))
        this.lowhop = true; 
    } 
  }
  
  @SubscribeEvent
  public void onSlotUpdate(SlotUpdateEvent e) {
    if (this.isEnabled && this.autoSwap.isToggled()) {
      this.lastSlot.set(e.slot);
      e.setCanceled(true);
    } 
  }
  
  @SubscribeEvent
  public void onPreUpdate(PreUpdateEvent e) {
    this.stopUpdate = this.stopUpdate2 = false;
    if (!this.isEnabled)
      this.stopUpdate2 = true; 
    if (LongJump.function)
      this.startYPos = -1.0D; 
    if (LongJump.stopModules)
      this.stopUpdate2 = true; 
    if (!this.stopUpdate2) {
      KeyBinding.func_74510_a(mc.field_71474_y.field_74312_F.func_151463_i(), false);
      KeyBinding.func_74510_a(mc.field_71474_y.field_74313_G.func_151463_i(), false);
      if (holdingBlocks() && setSlot()) {
        this.hasSwapped = true;
        if (!this.stopUpdate) {
          int mode = (int)this.fastScaffold.getInput();
          if (!ModuleManager.tower.placeExtraBlock) {
            if (this.rotation.getInput() == 0.0D || this.rotationDelay == 0)
              placeBlock(0, 0); 
          } else if (ModuleManager.tower.ebDelay == 0 || !ModuleManager.tower.firstVTP) {
            placeBlock(0, 0);
            this.placedVP = true;
          } 
          if (ModuleManager.tower.placeExtraBlock)
            placeBlock(0, -1); 
          if (this.fastScaffoldKeepY && !ModuleManager.tower.canTower()) {
            this.keepYTicks++;
            if ((int)mc.field_71439_g.field_70163_u > (int)this.startYPos)
              switch (mode) {
                case 1:
                  if (!this.firstKeepYPlace && this.keepYTicks == 3) {
                    placeBlock(1, 0);
                    this.firstKeepYPlace = true;
                  } 
                  break;
                case 2:
                  if ((!this.firstKeepYPlace && this.keepYTicks == 8) || this.keepYTicks == 11) {
                    placeBlock(1, 0);
                    this.firstKeepYPlace = true;
                  } 
                  break;
                case 3:
                  if ((!this.firstKeepYPlace && this.keepYTicks == 8) || (this.firstKeepYPlace && this.keepYTicks == 7)) {
                    placeBlock(1, 0);
                    this.firstKeepYPlace = true;
                  } 
                  break;
                case 4:
                  if (!this.firstKeepYPlace && this.keepYTicks == 7) {
                    placeBlock(1, 0);
                    this.firstKeepYPlace = true;
                  } 
                  break;
              }  
            if (mc.field_71439_g.field_70122_E)
              this.keepYTicks = 0; 
            if ((int)mc.field_71439_g.field_70163_u == (int)this.startYPos)
              this.firstKeepYPlace = false; 
          } 
          handleMotion();
        } 
      } 
    } 
    if (this.disabledModule) {
      if (this.hasPlaced && (this.towerEdge || (this.floatStarted && Utils.isMoving())))
        this.dontDisable = true; 
      if (this.dontDisable && ++this.disableTicks >= 2)
        this.isEnabled = false; 
      if (!this.dontDisable)
        this.isEnabled = false; 
      if (!this.isEnabled) {
        this.disabledModule = this.dontDisable = false;
        this.disableTicks = 0;
        if (ModuleManager.tower.speed)
          Utils.setSpeed(Utils.getHorizontalSpeed((Entity)mc.field_71439_g) / 1.6D); 
        if (this.lastSlot.get() != -1) {
          mc.field_71439_g.field_71071_by.field_70461_c = this.lastSlot.get();
          this.lastSlot.set(-1);
        } 
        this.blockSlot = -1;
        if (this.autoSwap.isToggled() && ModuleManager.autoSwap.spoofItem.isToggled()) {
          ((IMixinItemRenderer)mc.func_175597_ag()).setCancelUpdate(false);
          ((IMixinItemRenderer)mc.func_175597_ag()).setCancelReset(false);
        } 
        if (this.offsetDelay > 0)
          ModuleManager.sprint.requireJump = false; 
        this.scaffoldBlockCount.beginFade();
        this.hasSwapped = this.hasPlaced = false;
        this.targetBlock = null;
        this.blockInfo = null;
        this.blockRotations = null;
        this.fastScaffoldKeepY = this.firstKeepYPlace = this.rotateForward = this.rotatingForward = this.floatStarted = this.floatJumped = this.floatWasEnabled = this.towerEdge = this.was451 = this.was452 = this.enabledOffGround = this.finishProcedure = this.jump = this.blink = this.canSprint = this.canSprint2 = this.idle = this.didJump = false;
        this.rotationDelay = this.keepYTicks = this.scaffoldTicks = this.floatTicks = this.rt = this.idleTicks = 0;
        this.firstStroke = this.vlS = 0L;
        this.startYPos = -1.0D;
        this.lookVec = null;
        this.lastPlacement = null;
      } 
    } 
  }
  
  public String getInfo() {
    String s;
    String info;
    if (this.sprint.getInput() > 0.0D) {
      s = this.sprintModes[(int)this.sprint.getInput()];
    } else {
      s = this.rotationModes[(int)this.rotation.getInput()];
    } 
    if (this.fastOnRMB.isToggled()) {
      info = fastOnRMB() ? this.fastScaffoldModes[(int)this.fastScaffold.getInput()] : s;
    } else {
      info = (this.fastScaffold.getInput() > 0.0D) ? this.fastScaffoldModes[(int)this.fastScaffold.getInput()] : s;
    } 
    return info;
  }
  
  public boolean stopFastPlace() {
    return isEnabled();
  }
  
  public boolean sprint() {
    return (this.isEnabled && (this.canSprint || this.canSprint2));
  }
  
  public void rotateForward(boolean delay) {
    if (this.jumpFacingForward.isToggled() && this.rotation.getInput() > 0.0D) {
      if (!this.rotatingForward && delay)
        this.rotationDelay = 3; 
      this.rotatingForward = this.rotateForward = true;
    } 
  }
  
  public boolean blockAbove() {
    return !(BlockUtils.getBlock(new BlockPos(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u + 2.0D, mc.field_71439_g.field_70161_v)) instanceof net.minecraft.block.BlockAir);
  }
  
  private boolean usingFloat() {
    return (this.sprint.getInput() == 2.0D && Utils.isMoving() && !usingFastScaffold());
  }
  
  private boolean sprintScaf() {
    return (this.sprint.getInput() > 0.0D && Utils.isMoving() && mc.field_71439_g.field_70122_E && !usingFastScaffold() && !ModuleManager.tower.canTower());
  }
  
  public boolean usingFastScaffold() {
    return (this.fastScaffold.getInput() > 0.0D && (!this.fastOnRMB.isToggled() || (fastOnRMB() && Utils.tabbedIn())) && !prioritizeSprint());
  }
  
  public boolean fastOnRMB() {
    return (this.fastOnRMB.isToggled() && Utils.tabbedIn() && (Mouse.isButtonDown(1) || ModuleManager.bhop.isEnabled() || defPS()));
  }
  
  private boolean defPS() {
    return (this.prioritizeSprintWithSpeed.isToggled() && (this.sprint.getInput() == 0.0D || getSpeedLevel() == 0));
  }
  
  private boolean prioritizeSprint() {
    return (this.prioritizeSprintWithSpeed.isToggled() && this.sprint.getInput() > 0.0D && getSpeedLevel() > 0 && !fastOnRMB());
  }
  
  public boolean safewalk() {
    return (isEnabled() && this.safeWalk.isToggled());
  }
  
  public boolean stopRotation() {
    return (isEnabled() && this.rotation.getInput() > 0.0D);
  }
  
  private void place(PlaceData block) {
    ItemStack heldItem = mc.field_71439_g.func_70694_bm();
    if (heldItem == null || !(heldItem.func_77973_b() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock)heldItem.func_77973_b()))
      return; 
    if (mc.field_71442_b.func_178890_a(mc.field_71439_g, mc.field_71441_e, heldItem, block.blockPos, block.enumFacing, block.hitVec)) {
      if (this.silentSwing.isToggled()) {
        mc.field_71439_g.field_71174_a.func_147297_a((Packet)new C0APacketAnimation());
      } else {
        mc.field_71439_g.func_71038_i();
        if (holdingBlocks())
          mc.func_175597_ag().func_78444_b(); 
      } 
      if (ModuleManager.tower.placeExtraBlock)
        ModuleManager.tower.firstVTP = true; 
      this.highlight.put(block.blockPos.func_177972_a(block.enumFacing), null);
    } 
  }
  
  public boolean canSafewalk() {
    if (!this.safeWalk.isToggled())
      return false; 
    if (usingFastScaffold())
      return false; 
    if (ModuleManager.tower.canTower())
      return false; 
    if (!this.isEnabled)
      return false; 
    return true;
  }
  
  public int totalBlocks() {
    int totalBlocks = 0;
    for (int i = 0; i < 9; i++) {
      ItemStack stack = mc.field_71439_g.field_71071_by.field_70462_a[i];
      if (stack != null && stack.func_77973_b() instanceof ItemBlock && Utils.canBePlaced((ItemBlock)stack.func_77973_b()) && stack.field_77994_a > 0)
        totalBlocks += stack.field_77994_a; 
    } 
    return totalBlocks;
  }
  
  private void placeBlock(int yOffset, int xOffset) {
    locateAndPlaceBlock(yOffset, xOffset);
    int input = (int)this.multiPlace.getInput();
    if (input >= 1) {
      locateAndPlaceBlock(yOffset, xOffset);
      if (input >= 2) {
        locateAndPlaceBlock(yOffset, xOffset);
        if (input >= 3) {
          locateAndPlaceBlock(yOffset, xOffset);
          if (input >= 4)
            locateAndPlaceBlock(yOffset, xOffset); 
        } 
      } 
    } 
  }
  
  private void locateAndPlaceBlock(int yOffset, int xOffset) {
    locateBlocks(yOffset, xOffset);
    if (this.blockInfo == null)
      return; 
    this.lastPlacement = this.blockInfo;
    place(this.blockInfo);
    this.blockInfo = null;
  }
  
  private void locateBlocks(int yOffset, int xOffset) {
    List<PlaceData> blocksInfo = findBlocks(yOffset, xOffset);
    if (blocksInfo == null)
      return; 
    double sumX = 0.0D, sumY = !mc.field_71439_g.field_70122_E ? 0.0D : ((PlaceData)blocksInfo.get(0)).blockPos.func_177956_o(), sumZ = 0.0D;
    int index = 0;
    for (PlaceData blockssInfo : blocksInfo) {
      if (index > 1 || (!Utils.isDiagonal(false) && index > 0 && mc.field_71439_g.field_70122_E))
        break; 
      sumX += blockssInfo.blockPos.func_177958_n();
      if (!mc.field_71439_g.field_70122_E)
        sumY += blockssInfo.blockPos.func_177956_o(); 
      sumZ += blockssInfo.blockPos.func_177952_p();
      index++;
    } 
    double avgX = sumX / index;
    double avgY = !mc.field_71439_g.field_70122_E ? (sumY / index) : ((PlaceData)blocksInfo.get(0)).blockPos.func_177956_o();
    double avgZ = sumZ / index;
    this.targetBlock = new Vec3(avgX, avgY, avgZ);
    PlaceData blockInfo2 = blocksInfo.get(0);
    int blockX = blockInfo2.blockPos.func_177958_n();
    int blockY = blockInfo2.blockPos.func_177956_o();
    int blockZ = blockInfo2.blockPos.func_177952_p();
    EnumFacing blockFacing = blockInfo2.enumFacing;
    this.blockInfo = blockInfo2;
    double hitX = blockX + 0.5D + getCoord(blockFacing.func_176734_d(), "x") * 0.5D;
    double hitY = blockY + 0.5D + getCoord(blockFacing.func_176734_d(), "y") * 0.5D;
    double hitZ = blockZ + 0.5D + getCoord(blockFacing.func_176734_d(), "z") * 0.5D;
    this.lookVec = new Vec3(0.5D + getCoord(blockFacing.func_176734_d(), "x") * 0.5D, 0.5D + getCoord(blockFacing.func_176734_d(), "y") * 0.5D, 0.5D + getCoord(blockFacing.func_176734_d(), "z") * 0.5D);
    this.hitVec = new Vec3(hitX, hitY, hitZ);
    this.blockInfo.hitVec = this.hitVec;
  }
  
  private double getCoord(EnumFacing facing, String axis) {
    switch (axis) {
      case "x":
        return (facing == EnumFacing.WEST) ? -0.5D : ((facing == EnumFacing.EAST) ? 0.5D : 0.0D);
      case "y":
        return (facing == EnumFacing.DOWN) ? -0.5D : ((facing == EnumFacing.UP) ? 0.5D : 0.0D);
      case "z":
        return (facing == EnumFacing.NORTH) ? -0.5D : ((facing == EnumFacing.SOUTH) ? 0.5D : 0.0D);
    } 
    return 0.0D;
  }
  
  private List<PlaceData> findBlocks(int yOffset, int xOffset) {
    int x = (int)Math.floor(mc.field_71439_g.field_70165_t + xOffset);
    int y = (int)Math.floor(((this.startYPos != -1.0D) ? this.startYPos : mc.field_71439_g.field_70163_u) + yOffset);
    int z = (int)Math.floor(mc.field_71439_g.field_70161_v);
    BlockPos base = new BlockPos(x, y - 1, z);
    if (!BlockUtils.replaceable(base))
      return null; 
    EnumFacing[] allFacings = getFacingsSorted();
    List<EnumFacing> validFacings = new ArrayList<>(5);
    for (EnumFacing facing : allFacings) {
      if (facing != EnumFacing.UP && placeConditions(facing, yOffset, xOffset))
        validFacings.add(facing); 
    } 
    int maxLayer = 1;
    List<PlaceData> possibleBlocks = new ArrayList<>();
    for (int dy = 1; dy <= maxLayer; dy++) {
      BlockPos layerBase = new BlockPos(x, y - dy, z);
      if (dy == 1)
        for (EnumFacing facing : validFacings) {
          BlockPos neighbor = layerBase.func_177972_a(facing);
          if (!BlockUtils.replaceable(neighbor) && !BlockUtils.isInteractable(BlockUtils.getBlock(neighbor)))
            possibleBlocks.add(new PlaceData(neighbor, facing.func_176734_d())); 
        }  
      for (EnumFacing facing : validFacings) {
        BlockPos adjacent = layerBase.func_177972_a(facing);
        if (BlockUtils.replaceable(adjacent))
          for (EnumFacing nestedFacing : validFacings) {
            BlockPos nestedNeighbor = adjacent.func_177972_a(nestedFacing);
            if (!BlockUtils.replaceable(nestedNeighbor) && !BlockUtils.isInteractable(BlockUtils.getBlock(nestedNeighbor)))
              possibleBlocks.add(new PlaceData(nestedNeighbor, nestedFacing.func_176734_d())); 
          }  
      } 
      for (EnumFacing facing : validFacings) {
        BlockPos adjacent = layerBase.func_177972_a(facing);
        if (BlockUtils.replaceable(adjacent))
          for (EnumFacing nestedFacing : validFacings) {
            BlockPos nestedNeighbor = adjacent.func_177972_a(nestedFacing);
            if (BlockUtils.replaceable(nestedNeighbor))
              for (EnumFacing thirdFacing : validFacings) {
                BlockPos thirdNeighbor = nestedNeighbor.func_177972_a(thirdFacing);
                if (!BlockUtils.replaceable(thirdNeighbor) && !BlockUtils.isInteractable(BlockUtils.getBlock(thirdNeighbor)))
                  possibleBlocks.add(new PlaceData(thirdNeighbor, thirdFacing.func_176734_d())); 
              }  
          }  
      } 
    } 
    return possibleBlocks.isEmpty() ? null : possibleBlocks;
  }
  
  private EnumFacing[] getFacingsSorted() {
    EnumFacing firstPerp, secondPerp, lastFacing = EnumFacing.func_176731_b(MathHelper.func_76128_c((((IAccessorEntityPlayerSP)mc.field_71439_g).getLastReportedYaw() * 4.0F / 360.0F) + 0.5D) & 0x3);
    EnumFacing perpClockwise = lastFacing.func_176746_e();
    EnumFacing perpCounterClockwise = lastFacing.func_176735_f();
    EnumFacing opposite = lastFacing.func_176734_d();
    float yaw = ((IAccessorEntityPlayerSP)mc.field_71439_g).getLastReportedYaw() % 360.0F;
    if (yaw > 180.0F) {
      yaw -= 360.0F;
    } else if (yaw < -180.0F) {
      yaw += 360.0F;
    } 
    float diffClockwise = Math.abs(MathHelper.func_76142_g(yaw - getFacingAngle(perpClockwise)));
    float diffCounterClockwise = Math.abs(MathHelper.func_76142_g(yaw - getFacingAngle(perpCounterClockwise)));
    if (diffClockwise <= diffCounterClockwise) {
      firstPerp = perpClockwise;
      secondPerp = perpCounterClockwise;
    } else {
      firstPerp = perpCounterClockwise;
      secondPerp = perpClockwise;
    } 
    return new EnumFacing[] { EnumFacing.UP, EnumFacing.DOWN, lastFacing, firstPerp, secondPerp, opposite };
  }
  
  private float getFacingAngle(EnumFacing facing) {
    switch (facing) {
      case WEST:
        return 90.0F;
      case NORTH:
        return 180.0F;
      case EAST:
        return -90.0F;
    } 
    return 0.0F;
  }
  
  private boolean placeConditions(EnumFacing enumFacing, int yCondition, int xCondition) {
    if (xCondition == -1) {
      if (!ModuleManager.tower.placeExtraBlock)
        return (enumFacing == EnumFacing.EAST); 
      return (enumFacing == EnumFacing.DOWN);
    } 
    if (ModuleManager.tower.placeExtraBlock)
      return (enumFacing == EnumFacing.WEST); 
    if (yCondition == 1)
      return (enumFacing == EnumFacing.DOWN); 
    return true;
  }
  
  float applyGcd(float value) {
    float gcd = 0.064F;
    return (float)(value - value % gcd * 0.15D);
  }
  
  public float getMotionYaw() {
    return (float)Math.toDegrees(Math.atan2(mc.field_71439_g.field_70179_y, mc.field_71439_g.field_70159_w)) - 90.0F;
  }
  
  public int getSpeedLevel() {
    Iterator<PotionEffect> iterator = mc.field_71439_g.func_70651_bq().iterator();
    if (iterator.hasNext()) {
      PotionEffect potionEffect = iterator.next();
      if (potionEffect.func_76453_d().equals("potion.moveSpeed"))
        return potionEffect.func_76458_c() + 1; 
      return 0;
    } 
    return 0;
  }
  
  public double getSpeed(int speedLevel) {
    if (speedLevel >= 0)
      return this.speedLevels[speedLevel]; 
    return this.speedLevels[0];
  }
  
  double getFloatSpeed(int speedLevel) {
    double min = 0.0D;
    double value = 0.0D;
    double input = this.motion.getInput() / 100.0D;
    if (mc.field_71439_g.field_70702_br != 0.0F && mc.field_71439_g.field_70701_bs != 0.0F)
      min = 0.003D; 
    value = this.floatSpeedLevels[speedLevel] - min;
    if (speedLevel == 2)
      value = (Utils.scaffoldDiagonal(false) ? 0.255D : 0.265D) - min; 
    value *= input;
    return value;
  }
  
  private void handleMotion() {
    if (usingFastScaffold() || usingFloat() || ModuleManager.tower.canTower() || this.motion.getInput() == 100.0D || !mc.field_71439_g.field_70122_E)
      return; 
    double input = this.motion.getInput() / 100.0D;
    mc.field_71439_g.field_70159_w *= input;
    mc.field_71439_g.field_70179_y *= input;
  }
  
  public float hardcodedYaw() {
    float simpleYaw = 0.0F;
    boolean w = Keyboard.isKeyDown(mc.field_71474_y.field_74351_w.func_151463_i());
    boolean s = Keyboard.isKeyDown(mc.field_71474_y.field_74368_y.func_151463_i());
    boolean a = Keyboard.isKeyDown(mc.field_71474_y.field_74370_x.func_151463_i());
    boolean d = Keyboard.isKeyDown(mc.field_71474_y.field_74366_z.func_151463_i());
    boolean dupe = a & d;
    if (w) {
      simpleYaw -= 180.0F;
      if (!dupe) {
        if (a)
          simpleYaw += 45.0F; 
        if (d)
          simpleYaw -= 45.0F; 
      } 
    } else if (!s) {
      simpleYaw -= 180.0F;
      if (!dupe) {
        if (a)
          simpleYaw += 90.0F; 
        if (d)
          simpleYaw -= 90.0F; 
      } 
    } else if (!w && 
      !dupe) {
      if (a)
        simpleYaw -= 45.0F; 
      if (d)
        simpleYaw += 45.0F; 
    } 
    return simpleYaw;
  }
  
  private float getForwardYaw() {
    float simpleYaw = 0.0F;
    boolean w = Keyboard.isKeyDown(mc.field_71474_y.field_74351_w.func_151463_i());
    boolean s = Keyboard.isKeyDown(mc.field_71474_y.field_74368_y.func_151463_i());
    boolean a = Keyboard.isKeyDown(mc.field_71474_y.field_74370_x.func_151463_i());
    boolean d = Keyboard.isKeyDown(mc.field_71474_y.field_74366_z.func_151463_i());
    boolean dupe = a & d;
    if (w) {
      if (!dupe) {
        if (a)
          simpleYaw += 45.0F; 
        if (d)
          simpleYaw -= 45.0F; 
      } 
    } else if (!s) {
      if (!dupe) {
        if (a)
          simpleYaw += 90.0F; 
        if (d)
          simpleYaw -= 90.0F; 
      } 
    } else if (!w) {
      simpleYaw -= 180.0F;
      if (!dupe) {
        if (a)
          simpleYaw -= 45.0F; 
        if (d)
          simpleYaw += 45.0F; 
      } 
    } 
    return simpleYaw;
  }
  
  public boolean holdingBlocks() {
    ItemStack heldItem = mc.field_71439_g.func_70694_bm();
    if (this.autoSwap.isToggled() && ModuleManager.autoSwap.spoofItem.isToggled() && this.lastSlot.get() != mc.field_71439_g.field_71071_by.field_70461_c && totalBlocks() > 0) {
      ((IMixinItemRenderer)mc.func_175597_ag()).setCancelUpdate(true);
      ((IMixinItemRenderer)mc.func_175597_ag()).setCancelReset(true);
    } 
    if ((!this.autoSwap.isToggled() || getSlot() == -1) && (
      heldItem == null || !(heldItem.func_77973_b() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock)heldItem.func_77973_b())))
      return false; 
    return true;
  }
  
  private double getMovementAngle() {
    double angle = Math.toDegrees(Math.atan2(-mc.field_71439_g.field_70702_br, mc.field_71439_g.field_70701_bs));
    return (angle == 0.0D) ? 0.0D : angle;
  }
  
  private int getSlot() {
    int slot = -1;
    int highestStack = -1;
    ItemStack heldItem = mc.field_71439_g.func_70694_bm();
    for (int i = 0; i < 9; i++) {
      ItemStack itemStack = mc.field_71439_g.field_71071_by.field_70462_a[i];
      if (itemStack != null && itemStack.func_77973_b() instanceof ItemBlock && Utils.canBePlaced((ItemBlock)itemStack.func_77973_b()) && itemStack.field_77994_a > 0 && (
        Utils.getBedwarsStatus() != 2 || !(((ItemBlock)itemStack.func_77973_b()).func_179223_d() instanceof net.minecraft.block.BlockTNT)))
        if (itemStack == null || heldItem == null || !(heldItem.func_77973_b() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock)heldItem.func_77973_b()) || !ModuleManager.autoSwap.sameType.isToggled() || itemStack.func_77973_b().getClass().equals(heldItem.func_77973_b().getClass()))
          if (itemStack.field_77994_a > highestStack) {
            highestStack = itemStack.field_77994_a;
            slot = i;
          }   
    } 
    return slot;
  }
  
  public static boolean bypassRots() {
    return (ModuleManager.scaffold.rotation.getInput() == 2.0D || ModuleManager.scaffold.rotation.getInput() == 0.0D);
  }
  
  public boolean setSlot() {
    ItemStack heldItem = mc.field_71439_g.func_70694_bm();
    int slot = getSlot();
    if (slot == -1)
      return false; 
    if (this.blockSlot == -1)
      this.blockSlot = slot; 
    if (this.lastSlot.get() == -1)
      this.lastSlot.set(mc.field_71439_g.field_71071_by.field_70461_c); 
    if (this.autoSwap.isToggled() && this.blockSlot != -1)
      if (ModuleManager.autoSwap.swapToGreaterStack.isToggled()) {
        mc.field_71439_g.field_71071_by.field_70461_c = slot;
        this.spoofSlot = slot;
      } else if (heldItem == null || !(heldItem.func_77973_b() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock)heldItem.func_77973_b()) || (mc.field_71439_g.func_70694_bm()).field_77994_a <= ModuleManager.autoSwap.swapAt.getInput()) {
        mc.field_71439_g.field_71071_by.field_70461_c = slot;
        this.spoofSlot = slot;
      }  
    if (heldItem == null || !(heldItem.func_77973_b() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock)heldItem.func_77973_b())) {
      this.blockSlot = -1;
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
