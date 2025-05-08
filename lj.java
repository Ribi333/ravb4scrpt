    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        onGroundTicks = !mc.thePlayer.onGround ? 0 : ++onGroundTicks;
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
        int simpleY = (int) Math.round((e.posY % 1) * 10000);
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
            if (Utils.keysDown() && usingFastScaffold() && !ModuleManager.invmove.active() && fastScaffold.getInput() >= 1 && !ModuleManager.tower.canTower() && !LongJump.function) { // jump mode
                if (mc.thePlayer.onGround && Utils.isMoving()) {
                    if (scaffoldTicks > 1) {
                        jump = true;
                        rotateForward();
                        if (startYPos == -1 || Math.abs(startYPos - mc.thePlayer.posY) > 2) {
                            startYPos = mc.thePlayer.posY;
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
        else if (fastScaffoldKeepY) {
            fastScaffoldKeepY = firstKeepYPlace = false;
            startYPos = -1;
            keepYTicks = 0;
        }
        if (lowhop) {
            switch (simpleY) {
                case 4200:
                    mc.thePlayer.motionY = 0.39;
                    break;
                case 1138:
                    mc.thePlayer.motionY = mc.thePlayer.motionY - 0.13;
                    break;
                case 2031:
                    mc.thePlayer.motionY = mc.thePlayer.motionY - 0.2;
                    lowhop = false;
                    break;
            }
        }
