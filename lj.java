    private void onRenderTick(float partialTicks) {
        if (!clickGuiOpen && this.mc.currentScreen instanceof ClickGui) {
            clickGuiOpen = true;
            initTimer(500.0F);
            startTimer();
            openedTime = System.currentTimeMillis();
        } else if (!(this.mc.currentScreen instanceof ClickGui)) {
            clickGuiOpen = false;
            // Don't reset openedTime here - let it reset when GUI opens again
        } else {
            int[] displaySize = {this.width, this.height};
            int y = displaySize[1] + (8 - getValueInt(0, 30, 2));
            int test = displaySize[1] + (8 - getValueInt(0, 40, 2));

            this.fontRendererObj.drawString(clientName, 4, y, bluey, true);
            this.fontRendererObj.drawString(welcomeText, 4, test, bluey, true);
            //this.fontRendererObj.drawString(userName, 48, test, Utils.getChroma(2L, 0L), true);
            int startX = 48;
            for (int i = 0; i < userName.length(); i++) {
                char c = userName.charAt(i);
                int charColor = Utils.getChroma(2L, -i * 100L);
                this.fontRendererObj.drawString(String.valueOf(c), startX, test, charColor, true);
                startX += this.fontRendererObj.getCharWidth(c);
            }
            this.fontRendererObj.drawString(userNumber, 48, test, bluey, true);


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
