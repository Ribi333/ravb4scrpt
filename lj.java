private void onRenderTick(float partialTicks) {
    if (!clickGuiOpen && this.mc.currentScreen instanceof ClickGui) {
        clickGuiOpen = true;
        initTimer(500.0F);
        startTimer();
        openedTime = System.currentTimeMillis(); // Fresh start every time
        
        // First time opening logic
        if (firstTimeOpening) {
            firstTimeOpening = false;
            firstOpenTime = System.currentTimeMillis();
            showingWelcome = true;
        }
    } else if (!(this.mc.currentScreen instanceof ClickGui)) {
        clickGuiOpen = false;
    } else {
        // Show big welcome text for first 3 seconds
        if (showingWelcome && (System.currentTimeMillis() - firstOpenTime) < 3000) {
            String welcomeMsg = "welcome, " + userName;
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            
            // Draw large welcome text in center
            GlStateManager.pushMatrix();
            GlStateManager.scale(4.0f, 4.0f, 1.0f);
            this.drawCenteredString(this.fontRendererObj, welcomeMsg, centerX / 4, centerY / 4, Utils.getChroma(2L, 0L));
            GlStateManager.popMatrix();
            
            return; // Don't draw other text while showing welcome
        } else {
            showingWelcome = false;
        }
        
        // Normal GUI text (always shows)
        int[] displaySize = {this.width, this.height};
        int y = displaySize[1] + (8 - getValueInt(0, 30, 2));
        int test = displaySize[1] + (8 - getValueInt(0, 40, 2));

        this.fontRendererObj.drawString(clientName, 4, y, bluey, true);
        this.fontRendererObj.drawString(welcomeText, 4, test, bluey, true);
        
        // Rainbow username
        int startX = 48;
        for (int i = 0; i < userName.length(); i++) {
            char c = userName.charAt(i);
            int charColor = Utils.getChroma(2L, -i * 100L);
            this.fontRendererObj.drawString(String.valueOf(c), startX, test, charColor, true);
            startX += this.fontRendererObj.getCharWidth(c);
        }
        this.fontRendererObj.drawString(userNumber, 48, test, bluey, true);

        // Obfuscated developer name (now works every time)
        long elapsedTime = System.currentTimeMillis() - openedTime + 50L;
        int characterIndex = (int) (elapsedTime / 200L);
        y += this.fontRendererObj.FONT_HEIGHT + 1;

        if (characterIndex < developer.length()) {
            String obfuscated = "";

            for (int i = 0; i < developer.length(); ++i) {
                char currentChar = i < characterIndex
                        ? developer.charAt(i)
                        : (char) ((new Random()).nextInt(26) + 'a');
                obfuscated += currentChar;
            }

            this.fontRendererObj.drawString("dev. " + obfuscated, 4, y, bluey, true);
        } else {
            this.fontRendererObj.drawString("dev. " + developer, 4, y, bluey, true);
        }
    }
}
