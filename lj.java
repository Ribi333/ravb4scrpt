init:
#NoEnv
#MaxHotkeysPerInterval 127
SendMode Input
SetWorkingDir %A_ScriptDir%
#SingleInstance, Force

; Initial Screen Coordinates
FirX := 200
FirY := 200

; Screen Mid Coordinates
MidX := A_ScreenWidth / 2
MidY := A_ScreenHeight / 2

; Optimization Settings
#MaxHotkeysPerInterval 99000000
#HotkeyInterval 99000000
#KeyHistory 0
ListLines Off
Process, Priority, , A
SetBatchLines, -1 ; Use SetBatchLines -1 to never sleep (i.e. have the script run at maximum speed). The default setting is 10m
SetKeyDelay, -1, -1
SetMouseDelay, -1
SetWinDelay, -1
SetControlDelay, -1

CoordMode, Mouse, Screen
CoordMode, Pixel, Screen

aim := 0xCB3031  ; Color to search for
variation := 11  ; Variation range for color search
searchArea := 400  ; Default Search Area
Sense := +2
FovSense := -1
TriggerSense := 4
OSDEnabled := 1  ; Default OSD to enabled

; GUI Creation
Gui, -AlwaysOnTop -ToolWindow -Caption +OwnDialogs
Gui, Margin, 20, 20
Gui, Font, s10, Segoe UI

; Custom Title Bar
Gui, Color, White
Gui, Add, Text, x0 y0 w320 h30 Center BackgroundTrans cBlack, LeUniversal
Gui, Add, Text, x300 y0 w20 h20 Center BackgroundTrans cRed gCloseGUI, X
Gui, Add, Text, x0 y0 w320 h30 Center BackgroundTrans cWhite gLeDrag, ; To make the title bar draggable

; Background Image
Gui, Add, Picture, x0 y30 w320 h390 vBackground, background.jpg  ; Add a background image

; Tabs
Gui, Add, Tab2, x0 y30 w320 h390 vMainTab, Settings|Toggles

; Settings Tab
Gui, Tab, Settings
Gui, Add, Edit, x10 y50 w80 vSenseInput Number, %Sense%
Gui, Add, Button, x100 y50 w50 gApplySense, Apply
Gui, Add, Text, x10 y80 w300 vSenLabel, Aim Sensitivity: %Sense%
Gui, Add, Edit, x10 y110 w80 vFovSenseInput Number, %FovSense%
Gui, Add, Button, x100 y110 w50 gApplyFov, Apply
Gui, Add, Text, x10 y140 w300 vFovLabel, Sensitivity Smoothing: %FovSense%
Gui, Add, Edit, x10 y170 w80 vTriggerSenseInput Number, %TriggerSense%
Gui, Add, Button, x100 y170 w50 gApplyTrigger, Apply
Gui, Add, Text, x10 y200 w300 vTriggerLabel, Trigger Sensitivity: %TriggerSense%
Gui, Add, Edit, x10 y230 w80 vSearchAreaInput Number, %searchArea%
Gui, Add, Button, x100 y230 w50 gApplySearchArea, Apply
Gui, Add, Text, x10 y260 w300 vSearchAreaLabel, Search Area: %searchArea%

; Toggles Tab
Gui, Tab, Toggles
Gui, Add, Checkbox, x10 y50 w300 gFovBox vFov, Fov Box (1036p)
Gui, Add, Checkbox, x10 y80 w300 vTriggerbot, Triggerbot
Gui, Add, Checkbox, x10 y110 w300 vOSDEnabled gToggleOSD Checked, Enable OSD
Gui, Add, Button, x10 y170 w300 gCreds, CREDITS

Gui, Tab
Gui, Show, x%FirX% y%FirY% w320 h420, LeUniversal

MyColor = 0xCB3031
Gui OSD:+LastFound +AlwaysOnTop -Caption +ToolWindow
Gui, OSD:Color, %MyColor%
Gui, OSD:Font, s26
Gui, OSD:Add, Text, vMyText cLime, LeUniversal
WinSet, TransColor, %MyColor% 155
if (OSDEnabled) {
    Gui, OSD:Show, x0 y0 NoActivate
}

SetTimer, UpdateFovBox, 1500 ; Update FOV box every 2 seconds
return

Aimbot:
Gui, Submit, NoHide
~RButton::
{

    While GetKeyState("RButton") {
        searchAreaHalf := searchArea / 2
        PixelSearch, TargetX, TargetY, MidX-searchAreaHalf, MidY-searchAreaHalf, MidX+searchAreaHalf, MidY+searchAreaHalf, aim, variation, Fast RGB
        If (ErrorLevel = 0) {
            MoveX := (TargetX - MidX) / Sense
            MoveY := (TargetY - MidY) / Sense
            ; Adjust the multiplier for snappier aiming
            ;MouseMove, % MoveX, % MoveY + 8.3, 0, R
            ; Replace MouseMove with DllCall to move the mouse
            DllCall("mouse_event", UInt, 1, Int, MoveX, Int, MoveY + 5, UInt, 0, UInt, 0)
            if (Triggerbot == 1) {
                Click, down
            }
        }
    }
}
return

~RButton up::
{
    if (Triggerbot == 1) {
        Click, up
    }
}
return

Creds:
Msgbox, 0, LeUniversal Made By: Khris, HackerHansen and Leplix!
return

ApplySense:
Gui, Submit, NoHide
Sense := SenseInput
GuiControl,, SenLabel, Aim Sensitivity: %Sense%
return

ApplyFov:
Gui, Submit, NoHide
FovSense := FovSenseInput
GuiControl,, FovLabel, Sensitivity Smoothing: %FovSense%
return

ApplyTrigger:
Gui, Submit, NoHide
TriggerSense := TriggerSenseInput
GuiControl,, TriggerLabel, Trigger Sensitivity: %TriggerSense%
return

ApplySearchArea:
Gui, Submit, NoHide
searchArea := SearchAreaInput
GuiControl,, SearchAreaLabel, Search Area: %searchArea%
return

ToggleOSD:
Gui, Submit, NoHide
if (OSDEnabled) {
    Gui, OSD:Show, x0 y0 NoActivate
} else {
    Gui, OSD:Hide
}
return

CloseGUI:
ExitApp

LeDrag:
PostMessage, 0xA1, 2,,, A
return

FovBox:
Gui, Submit, NoHide
if (Fov == 1) {
    CreateBox("01FF01")
} else {
    RemoveBox()
}
return

UpdateFovBox:
if (Fov == 1) {
    RemoveBox()
    searchAreaHalf := searchArea / 2
    Box(MidX - searchAreaHalf, MidY - searchAreaHalf, searchArea, searchArea, 1, 0)
}
return

`::exitapp

; Box Functions
CreateBox(Color)
{
    Gui 81:color, %Color%
    Gui 81:+ToolWindow -SysMenu -Caption +AlwaysOnTop
    Gui 82:color, %Color%
    Gui 82:+ToolWindow -SysMenu -Caption +AlwaysOnTop
    Gui 83:color, %Color%
    Gui 83:+ToolWindow -SysMenu -Caption +AlwaysOnTop
    Gui 84:color, %Color%
    Gui 84:+ToolWindow -SysMenu -Caption +AlwaysOnTop
}

Box(XCor, YCor, Width, Height, Thickness, Offset)
{
    Side := (InStr(Offset, "In") ? -1 : 1)
    StringTrimLeft, offset, offset, (Side == -1 ? 2 : 3)
    if !Offset
        Offset := 0

    x := XCor - (Side + 1) / 2 * Thickness - Side * Offset
    y := YCor - (Side + 1) / 2 * Thickness - Side * Offset
    h := Height + Side * Thickness + Side * Offset * 2
    w := Thickness
    Gui 81:Show, x%x% y%y% w%w% h%h% NA
    x += Thickness
    w := Width + Side * Thickness + Side * Offset * 2
    h := Thickness
    Gui 82:Show, x%x% y%y% w%w% h%h% NA
    x := x + w - Thickness
    y += Thickness
    h := Height + Side * Thickness + Side * Offset * 2
    w := Thickness
    Gui 83:Show, x%x% y%y% w%w% h%h% NA
    x := XCor - (Side + 1) / 2 * Thickness - Side * Offset
    y += h - Thickness
    w := Width + Side + Thickness + Side + Offset * 2
    h := Thickness
    Gui 84:Show, x%x% y%y% w%w% h%h% NA
}

RemoveBox()
{
    Gui 81:destroy
    Gui 82:destroy
    Gui 83:destroy
    Gui 84:destroy
}
