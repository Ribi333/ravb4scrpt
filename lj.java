#NoEnv
#SingleInstance, Force
SendMode Input
SetWorkingDir %A_ScriptDir%
#MaxHotkeysPerInterval 127
CoordMode, Mouse, Screen
CoordMode, Pixel, Screen

; Initial screen coordinates
FirX := 200
FirY := 200

; Screen center
MidX := A_ScreenWidth / 2
MidY := A_ScreenHeight / 2

; Settings
aim := 0xCB3031 ; Target color
variation := 11
searchArea := 400
Sense := 2
FovSense := -1
TriggerSense := 4
OSDEnabled := 1

; GUI setup
Gui, -AlwaysOnTop -ToolWindow -Caption +OwnDialogs
Gui, Margin, 20, 20
Gui, Font, s10, Segoe UI
Gui, Color, White
Gui, Add, Tab2, x0 y30 w320 h390 vMainTab, Settings|Toggles

; Settings tab
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

; Toggles tab
Gui, Tab, Toggles
Gui, Add, Checkbox, x10 y50 w300 gFovBox vFov, Show FOV Box
Gui, Add, Checkbox, x10 y80 w300 vTriggerbot, Enable Triggerbot
Gui, Add, Checkbox, x10 y110 w300 vOSDEnabled gToggleOSD Checked, Enable OSD

Gui, Tab
Gui, Show, x%FirX% y%FirY% w320 h420, Aim Assistant

; Transparent overlay (optional)
MyColor = 0xCB3031
Gui OSD:+LastFound +AlwaysOnTop -Caption +ToolWindow
Gui, OSD:Color, %MyColor%
Gui, OSD:Font, s26
Gui, OSD:Add, Text, vMyText cLime, Aim Assistant
WinSet, TransColor, %MyColor% 155
if (OSDEnabled) {
    Gui, OSD:Show, x0 y0 NoActivate
}

SetTimer, UpdateFovBox, 1500
return

; === Aimbot Activation ===
~LButton::
{
    Gui, Submit, NoHide
    While GetKeyState("LButton", "P") {
        searchAreaHalf := searchArea / 2
        PixelSearch, TargetX, TargetY, MidX-searchAreaHalf, MidY-searchAreaHalf, MidX+searchAreaHalf, MidY+searchAreaHalf, aim, variation, Fast RGB
        If (ErrorLevel = 0) {
            MoveX := (TargetX - MidX) / Sense
            MoveY := (TargetY - MidY) / Sense
            DllCall("mouse_event", UInt, 1, Int, MoveX, Int, MoveY + 5, UInt, 0, UInt, 0)
            if (Triggerbot == 1) {
                Click, down
            }
        }
    }
}
return

~LButton up::
{
    if (Triggerbot == 1) {
        Click, up
    }
}
return

; === Apply Buttons ===
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

`::ExitApp

; === FOV Box Drawing Functions ===
CreateBox(Color)
{
    Loop 4 {
        Gui %A_Index%:Color, %Color%
        Gui %A_Index%:+ToolWindow -SysMenu -Caption +AlwaysOnTop
    }
}

Box(XCor, YCor, Width, Height, Thickness, Offset)
{
    Side := 1
    x := XCor - Thickness
    y := YCor - Thickness
    w := Width + 2 * Thickness
    h := Height + 2 * Thickness
    Gui 1:Show, x%x% y%y% w%Thickness% h%h% NA
    Gui 2:Show, x%x% y%y% w%w% h%Thickness% NA
    Gui 3:Show, x%x% y%y% w%Thickness% h%h% NA
    Gui 4:Show, x%x% y%y% w%w% h%Thickness% NA
}

RemoveBox()
{
    Loop 4 {
        Gui %A_Index%:Destroy
    }
}
