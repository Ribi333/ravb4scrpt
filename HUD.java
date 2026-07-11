package keystrokesmod.module.impl.render;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.Setting;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.font.FontManager;
import keystrokesmod.utility.font.MosquiFont;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

public class HUD extends Module {
  public static SliderSetting theme;
  private static SliderSetting outline;
  public static ButtonSetting alphabeticalSort;
  private static ButtonSetting drawBackground;
  private static SliderSetting backgroundOpacity;
  public static ButtonSetting alignRight;
  public static ButtonSetting lowercase;
  private static ButtonSetting removeCloset;
  private static ButtonSetting removeRender;
  public static ButtonSetting showInfo;
  public static ButtonSetting dropShadow;
  private static SliderSetting fontSetting;
  private static SliderSetting color1Red, color1Green, color1Blue, color2Red, color2Green, color2Blue;
  private static ButtonSetting glow;
  private static SliderSetting glowStrength;
  private static SliderSetting glowRadius;
  private static SliderSetting glowQuality;

  public static int posX = 5;
  public static int posY = 70;
  private boolean isAlphabeticalSort;
  private boolean canShowInfo;
    private String[] outlineModes = new String[] { "None", "Full", "Side" };
  
  public HUD() {
    super("HUD", Module.category.render);
    registerSetting((Setting)new DescriptionSetting("Right click bind to hide modules."));
    String[] themeModes = new String[Theme.themes.length + 1];
    System.arraycopy(Theme.themes, 0, themeModes, 0, Theme.themes.length);
    themeModes[Theme.themes.length] = "Custom";
    registerSetting((Setting)(theme = new SliderSetting("Theme", 0, themeModes)));
    registerSetting((Setting)(outline = new SliderSetting("Outline", 0, this.outlineModes)));
    registerSetting((Setting)new ButtonSetting("Edit position", () -> mc.displayGuiScreen(new EditScreen())));
    registerSetting((Setting)(alignRight = new ButtonSetting("Align right", false)));
    registerSetting((Setting)(alphabeticalSort = new ButtonSetting("Alphabetical sort", false)));
    registerSetting((Setting)(drawBackground = new ButtonSetting("Draw background", false)));
    this.registerSetting(backgroundOpacity = new SliderSetting("Background Opacity", 110, 0, 255, 1));
    registerSetting((Setting)(lowercase = new ButtonSetting("Lowercase", false)));
    registerSetting((Setting)(removeCloset = new ButtonSetting("Remove closet modules", false)));
    registerSetting((Setting)(removeRender = new ButtonSetting("Remove render modules", false)));
    registerSetting((Setting)(showInfo = new ButtonSetting("Show module info", true)));
    registerSetting((Setting)(dropShadow = new ButtonSetting("Drop shadow", true)));
    registerSetting((Setting)(fontSetting = new SliderSetting("Font", 0, FontManager.FONT_NAMES)));
    registerSetting((Setting)(color1Red = new SliderSetting("Color1 Red", 255, 0, 255, 1)));
    registerSetting((Setting)(color1Green = new SliderSetting("Color1 Green", 255, 0, 255, 1)));
    registerSetting((Setting)(color1Blue = new SliderSetting("Color1 Blue", 255, 0, 255, 1)));
    registerSetting((Setting)(color2Red = new SliderSetting("Color2 Red", 255, 0, 255, 1)));
    registerSetting((Setting)(color2Green = new SliderSetting("Color2 Green", 255, 0, 255, 1)));
    registerSetting((Setting)(color2Blue = new SliderSetting("Color2 Blue", 255, 0, 255, 1)));
    registerSetting((Setting)(glow = new ButtonSetting("Glow", false)));
    registerSetting((Setting)(glowStrength = new SliderSetting("Glow Strength", "%", 75.0, 10.0, 100.0, 1.0)));
    registerSetting((Setting)(glowRadius = new SliderSetting("Glow Radius", 1.8, 0.3, 3.5, 0.05)));
    registerSetting((Setting)(glowQuality = new SliderSetting("Glow Quality", 10.0, 8.0, 36.0, 2.0)));
  }

  @Override
  public void guiUpdate() {
    boolean isCustom = (int)theme.getInput() == Theme.themes.length;
    color1Red.setVisible(isCustom, this);
    color1Green.setVisible(isCustom, this);
    color1Blue.setVisible(isCustom, this);
    color2Red.setVisible(isCustom, this);
    color2Green.setVisible(isCustom, this);
    color2Blue.setVisible(isCustom, this);
    backgroundOpacity.setVisible(drawBackground.isToggled(), this);
    boolean glowEnabled = glow.isToggled();
    glowStrength.setVisible(glowEnabled, this);
    glowRadius.setVisible(glowEnabled, this);
    glowQuality.setVisible(glowEnabled, this);
  }

  public void onEnable() {
    ModuleManager.sort();
  }
  
  public void guiButtonToggled(ButtonSetting b) {
    if (b == alphabeticalSort || b == showInfo)
      ModuleManager.sort(); 
  }
  
  public static MosquiFont getCustomFont() {
      FontManager.selectedIndex = (int)fontSetting.getInput();
      return FontManager.getSelected();
  }
  
  public static int getThemeColor(double offset) {
      if ((int)theme.getInput() == Theme.themes.length) {
          double timeOffset = (System.currentTimeMillis() % 1200L) / 5.0;
          double angle = ((offset - timeOffset) * Math.PI) / 120.0;
          double t = (Math.sin(angle) + 1.0) / 2.0;
          int r = Math.max(0, Math.min(255, (int)(color1Red.getInput() + (color2Red.getInput() - color1Red.getInput()) * t)));
          int g = Math.max(0, Math.min(255, (int)(color1Green.getInput() + (color2Green.getInput() - color1Green.getInput()) * t)));
          int b = Math.max(0, Math.min(255, (int)(color1Blue.getInput() + (color2Blue.getInput() - color1Blue.getInput()) * t)));
          return 0xFF000000 | (r << 16) | (g << 8) | b;
      }
      return Theme.getGradient((int)theme.getInput(), offset);
  }
  
  @SubscribeEvent
  public void onRenderTick(TickEvent.RenderTickEvent ev) {
    if (ev.phase != TickEvent.Phase.END || !Utils.nullCheck())
      return; 
    if (this.isAlphabeticalSort != alphabeticalSort.isToggled()) {
      this.isAlphabeticalSort = alphabeticalSort.isToggled();
      ModuleManager.sort();
    } 
    if (this.canShowInfo != showInfo.isToggled()) {
      this.canShowInfo = showInfo.isToggled();
      ModuleManager.sort();
    } 
    if (!(mc.currentScreen instanceof HUD.EditScreen) && mc.currentScreen != null || mc.gameSettings.showDebugInfo)
      return; 
    for (Module module : ModuleManager.organizedModules) {
      module.getInfoUpdate();
      if (Module.sort)
        break; 
    } 
    if (Module.sort)
      ModuleManager.sort(); 
    Module.sort = false;
    int yPos = posY;
    long renderTime = System.currentTimeMillis();
    double n2 = 0.0D;
    MosquiFont font = getCustomFont();
    GlStateManager.pushMatrix();
    GlStateManager.enableBlend();
    try {
      boolean drawBg = drawBackground.isToggled();
      boolean rightAligned = alignRight.isToggled();
      List<PreparedTextEntry> textEntries = new ArrayList<PreparedTextEntry>();

      for (Module module : ModuleManager.organizedModules) {
        if (!module.isEnabled() || module == this || module.isHidden() || module == ModuleManager.commandLine
            || (removeRender.isToggled() && module.moduleCategory() == Module.category.render)
            || (removeCloset.isToggled() && module.closetModule)) {
            continue;
        }
        String moduleName = module.getNameInHud();
        String moduleInfo = (showInfo.isToggled() && !module.getInfo().isEmpty()) ? " " + module.getInfo().trim() : "";
        if (lowercase.isToggled()) moduleName = moduleName.toLowerCase();

        int color;
        if ((int)theme.getInput() == Theme.themes.length) {
          double timeOffset = (renderTime % 1200L) / 5.0;
          double angle = ((n2 - timeOffset) * Math.PI) / 120.0;
          double t = (Math.sin(angle) + 1.0) / 2.0;
          int r = Math.max(0, Math.min(255, (int)(color1Red.getInput() + (color2Red.getInput() - color1Red.getInput()) * t)));
          int g = Math.max(0, Math.min(255, (int)(color1Green.getInput() + (color2Green.getInput() - color1Green.getInput()) * t)));
          int b = Math.max(0, Math.min(255, (int)(color1Blue.getInput() + (color2Blue.getInput() - color1Blue.getInput()) * t)));
          color = 0xFF000000 | (r << 16) | (g << 8) | b;
        } else {
          color = Theme.getGradient((int)theme.getInput(), n2);
        }
        n2 -= (theme.getInput() == 0.0D) ? 120.0D : 12.0D;

        int nameWidth = (font != null)
                ? font.getStringWidth(moduleName) + font.getStringWidth(moduleInfo)
                : mc.fontRendererObj.getStringWidth(moduleName + moduleInfo);
        int fontHeight = (font != null) ? font.getHeight() : mc.fontRendererObj.FONT_HEIGHT;

        float textX = posX;
        if (rightAligned) {
          textX -= nameWidth;
        }

        textEntries.add(new PreparedTextEntry(moduleName, moduleInfo, textX, yPos, nameWidth, fontHeight, color));
        yPos += fontHeight + 2;
      }

      if (glow.isToggled() && !textEntries.isEmpty()) {
        this.renderHudAmbientGlow(textEntries);
      }

      for (PreparedTextEntry entry : textEntries) {
        if (drawBg) {
          drawBackground(entry.x - 2.2f, entry.y - 1.0f, entry.x + entry.width + 2.2f, entry.y + entry.height + 1.0f);
        }
        if (font != null) {
          font.drawStringWithShadow(entry.name, entry.x, entry.y, entry.color);
          if (!entry.info.isEmpty())
            font.drawStringWithShadow(entry.info, entry.x + font.getStringWidth(entry.name), entry.y, 0xFFAAAAAA);
        } else {
          mc.fontRendererObj.drawString(entry.name, entry.x, entry.y, entry.color, true);
          if (!entry.info.isEmpty())
            mc.fontRendererObj.drawString(entry.info, entry.x + mc.fontRendererObj.getStringWidth(entry.name), entry.y, 0xFFAAAAAA, true);
        }
      }
    } catch (Throwable t) {
      t.printStackTrace();
    } finally {
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.disableBlend();
      GlStateManager.popMatrix();
    } 
  }
  
  private void drawBackground(float left, float top, float right, float bottom) {
    int alpha = Math.max(0, Math.min(255, (int) backgroundOpacity.getInput()));
    int background = (alpha << 24) | 0x000000;
    RenderUtils.drawRoundedRectangle(left, top, right, bottom, 6.0f, background);
  }

  private int getGlowAlpha() {
    int strength = (int)Math.round(glowStrength.getInput());
    return Math.max(20, Math.min(255, Math.round((float)strength * 2.4f)));
  }

  private int withAlpha(int color, int alpha) {
    alpha = Math.max(0, Math.min(255, alpha));
    return alpha << 24 | color & 0xFFFFFF;
  }

  private void renderHudAmbientGlow(List<PreparedTextEntry> textEntries) {
    if (textEntries.isEmpty()) return;
    int baseAlpha = this.getGlowAlpha();
    if (baseAlpha <= 0) return;
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

    MosquiFont font = HUD.getCustomFont();
    float radiusInput = (float) glowRadius.getInput();
    int qualityInput = (int) Math.round(glowQuality.getInput());

    // Number of radius steps: 2 at low quality, up to 4 at high quality
    int radiusSteps = Math.max(2, Math.min(4, 1 + qualityInput / 6));

    // 8-direction offsets: cardinal (N/S/E/W) + diagonal (NE/SE/SW/NW)
    float[] dirX = {-1, 1, 0, 0, -1, 1, -1, 1};
    float[] dirY = {0, 0, -1, 1, -1, -1, 1, 1};
    // Diagonal offsets are at 1/sqrt(2) ≈ 0.707 to keep true distance = radius
    float[] dirScale = {1.0f, 1.0f, 1.0f, 1.0f, 0.707f, 0.707f, 0.707f, 0.707f};

    for (PreparedTextEntry entry : textEntries) {
      if (entry.width <= 0.0f || entry.height <= 0.0f) continue;

      String fullText = entry.name; // glow only the module name (colored part)
      // Note: info text is gray/white — only glow the colored name

      for (int step = 0; step < radiusSteps; step++) {
        // step=0 is innermost (brightest), step=radiusSteps-1 is outermost (dimmest)
        float stepProgress = (float) step / (float) Math.max(1, radiusSteps - 1);
        float radius = radiusInput * (0.5f + stepProgress * 0.5f);
        // Alpha falls off: innermost ~55% of baseAlpha, outermost ~8%
        float alphaFraction = 0.55f * (float) Math.pow(1.0f - stepProgress, 1.2f) + 0.08f * (1.0f - stepProgress);
        // But clamp so outermost step is still visible
        int stepAlpha = Math.max(2, Math.min(255, Math.round(baseAlpha * alphaFraction)));

        int glowColor = this.withAlpha(entry.color, stepAlpha);

        for (int dir = 0; dir < 8; dir++) {
          float ox = dirX[dir] * dirScale[dir] * radius;
          float oy = dirY[dir] * dirScale[dir] * radius;

          if (font != null) {
            font.drawString(fullText, entry.x + ox, entry.y + oy, glowColor);
          } else {
            // vanilla font renderer — use no-shadow version so alpha works
            mc.fontRendererObj.drawString(
                fullText, entry.x + ox, entry.y + oy, glowColor, false);
          }
        }
      }
    }
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
  }

  private static class PreparedTextEntry {
    private final String name;
    private final String info;
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final int color;

    private PreparedTextEntry(String name, String info, float x, float y, float width, float height, int color) {
      this.name = name;
      this.info = info;
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      this.color = color;
    }
  }

  static class EditScreen extends GuiScreen {
    final String example = "This is an-Example-HUD";
    GuiButtonExt resetPosition;
    boolean d = false;
    int aX = 5;
    int aY = 70;
    int lmX = 0, lmY = 0, laX = 0, laY = 0;
    
    public void initGui() {
      super.initGui();
      this.buttonList.add(this.resetPosition = new GuiButtonExt(1, this.width - 90, this.height - 25, 85, 20, "Reset position"));
      this.aX = HUD.posX;
      this.aY = HUD.posY;
    }
    
    public void drawScreen(int mX, int mY, float pt) {
      drawDefaultBackground();
      HUD.posX = this.aX;
      HUD.posY = this.aY;
      super.drawScreen(mX, mY, pt);
    }
    
    protected void mouseClicked(int mX, int mY, int b) throws IOException {
        super.mouseClicked(mX, mY, b);
        MosquiFont font = HUD.getCustomFont();
        int fontHeight = font != null ? font.getHeight() : mc.fontRendererObj.FONT_HEIGHT;

        int visibleCount = 0;
        int maxWidth = 60;
        for (Module module : ModuleManager.organizedModules) {
            if (!module.isEnabled() || module.isHidden()) continue;
            String name = module.getNameInHud();
            int w = font != null ? font.getStringWidth(name) : mc.fontRendererObj.getStringWidth(name);
            if (w > maxWidth) maxWidth = w;
            visibleCount++;
        }
        if (visibleCount == 0) visibleCount = 3;

        int previewWidth = maxWidth + 6;
        int previewHeight = visibleCount * (fontHeight + 2);

        int hitX1 = HUD.alignRight.isToggled() ? this.aX - previewWidth : this.aX - 3;
        int hitX2 = HUD.alignRight.isToggled() ? this.aX + 3 : this.aX + previewWidth;
        int hitY1 = this.aY - 2;
        int hitY2 = this.aY + previewHeight + 2;

        if (b == 0 && mX >= hitX1 && mX <= hitX2 && mY >= hitY1 && mY <= hitY2) {
            this.d = true;
            this.lmX = mX;
            this.lmY = mY;
            this.laX = this.aX;
            this.laY = this.aY;
        }
    }
    
    protected void mouseClickMove(int mX, int mY, int b, long t) {
      super.mouseClickMove(mX, mY, b, t);
      if (this.d) {
        this.aX = this.laX + mX - this.lmX;
        this.aY = this.laY + mY - this.lmY;
      }
    }
    
    protected void mouseReleased(int mX, int mY, int s) {
      super.mouseReleased(mX, mY, s);
      this.d = false;
    }
    
    public void actionPerformed(GuiButton b) {
      if (b == this.resetPosition) {
        this.aX = HUD.posX = 5;
        this.aY = HUD.posY = 70;
      } 
    }
    
    public boolean doesGuiPauseGame() {
      return false;
    }
  }
}
