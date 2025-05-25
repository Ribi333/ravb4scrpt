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
