            // Obfuscated developer name using &k formatting
            long elapsedTime = System.currentTimeMillis() - openedTime;
            int characterIndex = Math.min((int) (elapsedTime / 200L), developer.length());
            y += this.fontRendererObj.FONT_HEIGHT + 1;

            String devText = "dev. ";
            if (characterIndex < developer.length()) {
                // Show revealed characters + obfuscated rest
                String revealed = developer.substring(0, characterIndex);
                String obfuscated = developer.substring(characterIndex);
                devText += revealed + "&k" + obfuscated;
            } else {
                // Fully revealed
                devText += developer;
            }

            this.fontRendererObj.drawString(devText, 4, y, bluey, true);
        }
