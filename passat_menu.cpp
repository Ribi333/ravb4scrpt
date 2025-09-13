#include <condition_variable>
#include <Windows.h>
#include <shellapi.h>

#include "globals.hpp"
#include "imgui/imconfig.h"
#include "imgui/imgui.h"
#include "imgui/imgui_internal.h"
#include "passat_menu.h"
#include "fonts.hpp"
#include "font_awesome_defines.h"
#include "legacy ui/menu/menu.h"
#include "legacy ui/legacy_str.h"
#include "skins.hpp"

bool MenuShit::toggle(const char* name, bool* ceva) noexcept
{
    ImGuiWindow* window = ImGui::GetCurrentWindow();
    ImGuiContext& g = *GImGui;
    const auto id = window->GetID(name);
    const float w = ImGui::CalcItemWidth();
    float height = ImGui::GetFrameHeight();
    const float width = height * 1.55f;
    const float radius = height * 0.5f;
    const ImRect total_bb(window->DC.CursorPos, window->DC.CursorPos + ImVec2(245, ImGui::GetFrameHeight()));
    const ImRect frame_bb(window->DC.CursorPos + ImVec2(245 - (ImGui::GetFrameHeight() * 1.55f), 0), window->DC.CursorPos + ImVec2(245, ImGui::GetFrameHeight()));
    ImGui::ItemSize(total_bb);
    if (!ImGui::ItemAdd(total_bb, id, &frame_bb))
        return false;

    window->DrawList->AddText({ total_bb.Min.x, total_bb.Min.y + ImGui::CalcTextSize(name).y / 2 }, IM_COL32(255, 255, 255, 255 * alpha), name);

    float last_active_id_timer = g.LastActiveIdTimer;

    bool hovered, held;
    bool pressed = ImGui::ButtonBehavior(frame_bb, id, &hovered, &held);
    if (pressed)
    {
        *ceva = !*ceva;
        ImGui::MarkItemEdited(id);
        g.LastActiveIdTimer = 0.f;
    }
    if (g.LastActiveIdTimer == 0.f && g.LastActiveId == id && !pressed)
        g.LastActiveIdTimer = last_active_id_timer;

    float t = *ceva ? 1.0f : 0.0f;

    if (g.LastActiveId == id)
    {
        float t_anim = ImSaturate(g.LastActiveIdTimer / 0.09f);
        t = *ceva ? (t_anim) : (1.0f - t_anim);
    }

    ImU32 col_bg;
    col_bg = IM_COL32((int)(g_cfg.misc.ui_color.base().r() * t)
        , (int)(g_cfg.misc.ui_color.base().g() * t)
        , (int)(g_cfg.misc.ui_color.base().b() * t)
        , (int)(g_cfg.misc.ui_color.base().a() / 255.f * 203 * t * alpha));

    window->DrawList->AddRectFilled({ frame_bb.Min.x, frame_bb.Min.y + 6 }, ImVec2(frame_bb.Max.x, frame_bb.Max.y - 4), col_bg, 9.f);
    window->DrawList->AddRectFilled({ frame_bb.Min.x, frame_bb.Min.y + 6 }, ImVec2(frame_bb.Max.x, frame_bb.Max.y - 4), g_cfg.misc.ui_color.base().new_alpha(0.1643137f * 255 * alpha).as_imcolor(), 9.f);
    window->DrawList->AddRect({ frame_bb.Min.x, frame_bb.Min.y + 6 }, ImVec2(frame_bb.Max.x, frame_bb.Max.y - 4), g_cfg.misc.ui_color.base().new_alpha(0.25f * -(t - 1) * 255 * alpha).as_imcolor(), 9.f);
    window->DrawList->AddCircleFilled(ImVec2(frame_bb.Min.x + t * (width - radius) + 1.5f * t + (radius - 2.f) * -(t - 1), frame_bb.Max.y - (radius - 2) - 1), radius - 2.f, IM_COL32(255, 255, 255, 255 * alpha));
    return ceva;
}

void MenuShit::inputScalar(const char* label, ImGuiDataType data_type, void* p_data, const void* p_step, const void* p_step_fast, const char* format, ImGuiInputTextFlags flags)
{
    ImGuiWindow* window = ImGui::GetCurrentWindow();
    ImGuiContext& g = *GImGui;
    const auto id = window->GetID(label);
    const float w = ImGui::CalcItemWidth();
    float height = ImGui::GetFrameHeight();
    const float width = height * 1.55f;
    const float radius = height * 0.5f;
    const ImRect total_bb(window->DC.CursorPos, window->DC.CursorPos + ImVec2(245, ImGui::GetFrameHeight()));
    const ImRect frame_bb(total_bb.Min + ImVec2(160, 0), total_bb.Max);
    ImGui::ItemSize(total_bb);
    if (!ImGui::ItemAdd(total_bb, id, &frame_bb))
        return;

    window->DrawList->AddText({ total_bb.Min.x, total_bb.Min.y + ImGui::CalcTextSize(label).y / 2 }, IM_COL32(255, 255, 255, 255 * alpha), label);
    
    if (format == NULL)
        format = ImGui::DataTypeGetInfo(data_type)->PrintFmt;

    char buf[64];
    ImGui::DataTypeFormatString(buf, IM_ARRAYSIZE(buf), data_type, p_data, format);

    if ((flags & (ImGuiInputTextFlags_CharsHexadecimal | ImGuiInputTextFlags_CharsScientific)) == 0)
        flags |= ImGuiInputTextFlags_CharsDecimal;
    flags |= ImGuiInputTextFlags_AutoSelectAll;
    flags |= ImGuiInputTextFlags_NoMarkEdited;  // We call MarkItemEdited() ourselve by comparing the actual data rather than the string.
    
    bool value_changed = false;

    ImGui::PushID(id);
    ImGui::SameLine(160.f);
    ImGui::SetNextItemWidth(85.f);
    if (ImGui::InputText("", buf, IM_ARRAYSIZE(buf), flags))
        value_changed = ImGui::DataTypeApplyOpFromText(buf, g.InputTextState.InitialTextA.Data, data_type, p_data, format);

    if (value_changed)
        ImGui::MarkItemEdited(window->DC.LastItemId);
    ImGui::PopID();
}

bool MenuShit::toggleDisabled(const char* name, bool* ceva, const char* hint) noexcept
{
    ImGuiWindow* window = ImGui::GetCurrentWindow();
    ImGuiContext& g = *GImGui;
    const auto id = window->GetID(name);
    const float w = ImGui::CalcItemWidth();
    float height = ImGui::GetFrameHeight();
    const float width = height * 1.55f;
    const float radius = height * 0.5f;
    const ImRect total_bb(window->DC.CursorPos, window->DC.CursorPos + ImVec2(245, ImGui::GetFrameHeight()));
    const ImRect frame_bb(window->DC.CursorPos + ImVec2(245 - (ImGui::GetFrameHeight() * 1.55f), 0), window->DC.CursorPos + ImVec2(245, ImGui::GetFrameHeight()));
    ImGui::ItemSize(total_bb);
    if (!ImGui::ItemAdd(total_bb, id, &frame_bb))
        return false;

    window->DrawList->AddText({ total_bb.Min.x, total_bb.Min.y + ImGui::CalcTextSize(name).y / 2 }, g_cfg.misc.ui_color.base().new_alpha(0.65f * 255 * alpha).as_imcolor(), name);

    ImGui::PushFont(g_fonts->font_awesome);
    window->DrawList->AddText({ total_bb.Min.x + ImGui::CalcTextSize(name).x + 3, total_bb.Min.y + ImGui::CalcTextSize(ICON_FA_EXCLAMATION_TRIANGLE).y / 2 + 2 }, g_cfg.misc.ui_color.base().new_alpha(0.85f * 255).as_imcolor(), ICON_FA_EXCLAMATION_TRIANGLE);
    ImGui::PopFont();

    float last_active_id_timer = g.LastActiveIdTimer;
    bool hovered, held;
    bool pressed = ImGui::ButtonBehavior(frame_bb, id, &hovered, &held);
    if (pressed)
    {
        *ceva = !*ceva;
        ImGui::MarkItemEdited(id);
        g.LastActiveIdTimer = 0.f;
    }
    if (g.LastActiveIdTimer == 0.f && g.LastActiveId == id && !pressed)
        g.LastActiveIdTimer = last_active_id_timer;

    float t = *ceva ? 1.0f : 0.0f;

    if (g.LastActiveId == id)
    {
        float t_anim = ImSaturate(g.LastActiveIdTimer / 0.09f);
        t = *ceva ? (t_anim) : (1.0f - t_anim);
    }

    ImU32 col_bg;
    col_bg = IM_COL32((int)(g_cfg.misc.ui_color.base().r() * t)
        , (int)(g_cfg.misc.ui_color.base().g() * t)
        , (int)(g_cfg.misc.ui_color.base().b() * t)
        , (int)(g_cfg.misc.ui_color.base().a() / 255.f * 203 * t * alpha));
    /*if (hovered)
        col_bg = !*ceva ? IM_COL32(20, 20, 20, 255) : Helpers::calculateColor(config->misc.accentColor, 0.7943137f * t);
    else
        col_bg = !*ceva ? IM_COL32(0, 0, 0, 255) : Helpers::calculateColor(config->misc.accentColor, 0.7043137f);*/

    window->DrawList->AddRectFilled({ frame_bb.Min.x, frame_bb.Min.y + 6 }, ImVec2(frame_bb.Max.x, frame_bb.Max.y - 4), col_bg, 9.f);
    window->DrawList->AddRectFilled({ frame_bb.Min.x, frame_bb.Min.y + 6 }, ImVec2(frame_bb.Max.x, frame_bb.Max.y - 4), g_cfg.misc.ui_color.base().new_alpha(0.0643137f * 255 * alpha).as_imcolor(), 9.f);
    window->DrawList->AddRect({ frame_bb.Min.x, frame_bb.Min.y + 6 }, ImVec2(frame_bb.Max.x, frame_bb.Max.y - 4), g_cfg.misc.ui_color.base().new_alpha(0.15f * -(t - 1) * 255 * alpha).as_imcolor(), 9.f);
    window->DrawList->AddCircleFilled(ImVec2(frame_bb.Min.x + t * (width - radius) + 1.5f * t + (radius - 2.f) * -(t - 1), frame_bb.Max.y - (radius - 2) - 1), radius - 2.f, IM_COL32(170, 170, 170, 255 * alpha));
    if (ImGui::IsItemHovered())
        ImGui::SetTooltip(hint);
    return false;
}

bool MenuShit::toggleWithExtra(const char* name, bool* ceva, const char* popupName) noexcept
{
    ImGuiWindow* window = ImGui::GetCurrentWindow();
    ImGuiContext& g = *GImGui;
    const auto id = window->GetID(name);
    const float w = ImGui::CalcItemWidth();
    const float height = ImGui::GetFrameHeight();
    const float width = height * 1.55f;
    const float radius = height * 0.5f;
    const ImRect total_bb(window->DC.CursorPos, window->DC.CursorPos + ImVec2(245, ImGui::GetFrameHeight()));
    const ImRect frame_bb(window->DC.CursorPos + ImVec2(245 - (ImGui::GetFrameHeight() * 1.55f), 0), window->DC.CursorPos + ImVec2(245, ImGui::GetFrameHeight()));
    const ImRect frame_bb1(window->DC.CursorPos + ImVec2(245 - 29 - (ImGui::GetFrameHeight() * 1.55f), 0), window->DC.CursorPos + ImVec2(245 - 10 - (ImGui::GetFrameHeight() * 1.55f), ImGui::GetFrameHeight()));
    ImVec2 p = ImGui::GetCursorScreenPos();
    ImGui::ItemSize(total_bb);
    if (!ImGui::ItemAdd(total_bb, id, &frame_bb))
        return false;

    if (!ImGui::ItemAdd(total_bb, id + 1, &frame_bb1))
        return false;
    float last_active_id_timer = g.LastActiveIdTimer;
    //ImGui::GetWindowDrawList()->AddRectFilled(frame_bb1.Min, frame_bb1.Max, IM_COL32(255, 255, 255, 255), 12.f);
    ImGui::PushFont(g_fonts->icomenus);
    window->DrawList->AddText({ frame_bb1.Max.x - ImGui::CalcTextSize("M").x / 2 - 9 , p.y }, g_cfg.misc.ui_color.base().new_alpha(0.75f * 255 * alpha).as_imcolor(), CXOR("M"));
    //ImGui::GetWindowDrawList()->AddCircleFilled({ frame_bb1.Min.x + ImGui::GetFrameHeight() * 0.5f - 2.f, frame_bb1.Min.y + ImGui::GetFrameHeight() * 0.5f + 1 }, ImGui::GetFrameHeight() * 0.5f - 2.f, Helpers::calculateColor(color.color[0] * 255, color.color[1] * 255, color.color[2] * 255, 255), 64);
    ImGui::PopFont();
    window->DrawList->AddText({ total_bb.Min.x, total_bb.Min.y + ImGui::CalcTextSize(name).y / 2 }, IM_COL32(255, 255, 255, 255 * alpha), name);
    bool hovered, held;
    bool hovered1, held1;
    bool pressed = ImGui::ButtonBehavior(frame_bb, id, &hovered, &held);
    bool pressed1 = ImGui::ButtonBehavior(frame_bb1, id + 1, &hovered1, &held1);
    if (pressed1 || held1)
    {
        ImGui::OpenPopup(popupName);
    }

    if (pressed)
    {
        *ceva = !*ceva;
        ImGui::MarkItemEdited(id);
        g.LastActiveIdTimer = 0.f;
    }
    if (g.LastActiveIdTimer == 0.f && g.LastActiveId == id && !pressed)
        g.LastActiveIdTimer = last_active_id_timer;

    float t = *ceva ? 1.0f : 0.0f;

    if (g.LastActiveId == id)
    {
        float t_anim = ImSaturate(g.LastActiveIdTimer / 0.09f);
        t = *ceva ? (t_anim) : (1.0f - t_anim);
    }

    ImU32 col_bg;
    col_bg = IM_COL32((int)(g_cfg.misc.ui_color.base().r() * t)
        , (int)(g_cfg.misc.ui_color.base().g() * t)
        , (int)(g_cfg.misc.ui_color.base().b() * t)
        , (int)(g_cfg.misc.ui_color.base().a() / 255.f * 203 * t * alpha));

    window->DrawList->AddRectFilled({ frame_bb.Min.x, frame_bb.Min.y + 6 }, ImVec2(frame_bb.Max.x, frame_bb.Max.y - 4), col_bg, 9.f);
    window->DrawList->AddRectFilled({ frame_bb.Min.x, frame_bb.Min.y + 6 }, ImVec2(frame_bb.Max.x, frame_bb.Max.y - 4), g_cfg.misc.ui_color.base().new_alpha(0.1643137f * 255 * alpha).as_imcolor(), 9.f);
    window->DrawList->AddRect({ frame_bb.Min.x, frame_bb.Min.y + 6 }, ImVec2(frame_bb.Max.x, frame_bb.Max.y - 4), g_cfg.misc.ui_color.base().new_alpha(0.25f * -(t - 1) * 255 * alpha).as_imcolor(), 9.f);
    window->DrawList->AddCircleFilled(ImVec2(frame_bb.Min.x + t * (width - radius) + 1.5f * t + (radius - 2.f) * -(t - 1), frame_bb.Max.y - (radius - 2) - 1), radius - 2.f, IM_COL32(255, 255, 255, 255 * alpha));
    return *ceva;
}

bool MenuShit::buttonToggle(const char* title, bool* ceva) noexcept
{
    ImGuiWindow* window = ImGui::GetCurrentWindow();
    if (window->SkipItems)
        return false;

    ImGuiContext& g = *GImGui;
    const ImGuiStyle& style = g.Style;
    const ImGuiID id = window->GetID(title);
    const ImVec2 label_size = ImGui::CalcTextSize(title, NULL, true);

    ImVec2 pos = window->DC.CursorPos;
    ImVec2 size = ImGui::CalcItemSize({}, label_size.x + style.FramePadding.x * 2.0f, label_size.y + style.FramePadding.y * 2.0f);

    const ImRect bb(pos, pos + size);
    ImGui::ItemSize(size, style.FramePadding.y);
    if (!ImGui::ItemAdd(bb, id))
        return false;

    ImGui::PushID(id);
    bool hovered, held;
    bool pressed = ImGui::ButtonBehavior(bb, id, &hovered, &held, 0);

    if (pressed)
        *ceva = !*ceva;

    ImU32 col_bg = IM_COL32(20, 20, 20, 255 * alpha);

    window->DrawList->AddRectFilled(ImVec2(bb.Min.x, bb.Min.y + 3.f), ImVec2(bb.Max.x, bb.Max.y - 1.f), col_bg, 3.f);
    window->DrawList->AddRectFilled(ImVec2(bb.Min.x, bb.Min.y + 3.f), ImVec2(bb.Max.x, bb.Max.y - 1.f), g_cfg.misc.ui_color.base().new_alpha(*ceva ? 0.1643137f * 255 * alpha : 0.05f * 255 * alpha).as_imcolor(), 3.f);
    window->DrawList->AddRect(ImVec2(bb.Min.x, bb.Min.y + 3.f), ImVec2(bb.Max.x, bb.Max.y - 1.f), g_cfg.misc.ui_color.base().new_alpha(*ceva ? 0.25f * 255 * alpha : 0.15f * 255 * alpha).as_imcolor(), 3.f);

    window->DrawList->AddText(ImVec2(pos + size / 2) - ImVec2(label_size.x / 2, label_size.y / 2), IM_COL32(255, 255, 255, 255 * alpha), title, NULL);

    ImGui::PopID();
    return *ceva;
}

static bool hotkey3(const char* label, key_binds_t& var) noexcept
{
    ImGuiWindow* window = ImGui::GetCurrentWindow();
    if (window->SkipItems)
        return false;

    ImGuiContext& g = *GImGui;
    ImGuiIO& io = g.IO;
    const ImGuiStyle& style = g.Style;

    const float w = ImGui::GetWindowWidth();

    const ImGuiID id = window->GetID(label);
    const ImVec2 label_size = ImGui::CalcTextSize(label);

    const ImRect frame_bb(window->DC.CursorPos + ImVec2(160, 0), window->DC.CursorPos + ImVec2(245, ImGui::GetFrameHeight()));
    const ImRect total_bb(window->DC.CursorPos, window->DC.CursorPos + ImVec2(245, ImGui::GetFrameHeight()));

    ImGui::ItemSize(total_bb, style.FramePadding.y);
    if (!ImGui::ItemAdd(frame_bb, id))
        return false;

    const bool hovered = ImGui::IsItemHovered();
    const bool user_clicked = hovered && io.MouseClicked[0];

    if (user_clicked) {
        if (g.ActiveId != id) {
            // Start edition
            memset(io.MouseDown, 0, sizeof(io.MouseDown));
            memset(io.KeysDown, 0, sizeof(io.KeysDown));
            var.key = 0;
        }
        ImGui::SetActiveID(id, window);
        ImGui::FocusWindow(window);
    }
    else if (io.MouseClicked[0]) {
        // Release focus when we click outside
        if (g.ActiveId == id)
            ImGui::ClearActiveID();
    }

    bool value_changed = false;
    int key = var.key;

    if (g.ActiveId == id) {
        for (auto i = 0; i < 5; i++) {
            if (io.MouseDown[i]) {
                switch (i) {
                case 0:
                    key = VK_LBUTTON;
                    break;
                case 1:
                    key = VK_RBUTTON;
                    break;
                case 2:
                    key = VK_MBUTTON;
                    break;
                case 3:
                    key = VK_XBUTTON1;
                    break;
                case 4:
                    key = VK_XBUTTON2;
                    break;
                }
                value_changed = true;
                ImGui::ClearActiveID();
            }
        }
        if (!value_changed) {
            for (auto i = VK_BACK; i <= VK_RMENU; i++) {
                if (io.KeysDown[i]) {
                    key = i;
                    value_changed = true;
                    ImGui::ClearActiveID();
                }
            }
        }

        if (ImGui::IsKeyPressedMap(ImGuiKey_Escape)) {
            var.key = -1;
            ImGui::ClearActiveID();
        }
        else {
            var.key = key;
        }
    }

    // Render
    std::string buf_display = CXOR("None");

    if (var.key != -1 && g.ActiveId != id) {

        buf_display = KeyNames[var.key];
    }
    else if (g.ActiveId == id) {

        buf_display = CXOR("...");
    }

    window->DrawList->AddText({ frame_bb.Max.x - (85.f / 2.f) - (ImGui::CalcTextSize(buf_display.c_str()).x / 2.f), frame_bb.Min.y + ImGui::CalcTextSize(buf_display.c_str()).y / 2 }, IM_COL32(255, 255, 255, 255), buf_display.c_str());

    if (label_size.x > 0)
        window->DrawList->AddText({ total_bb.Min.x, total_bb.Min.y + ImGui::CalcTextSize(label).y / 2 }, IM_COL32(255, 255, 255, 255), label);

    return value_changed;
}

bool MenuShit::toggleWithBind(const char* name, bool* ceva, key_binds_t& var) noexcept
{
    ImGuiWindow* window = ImGui::GetCurrentWindow();
    ImGuiContext& g = *GImGui;
    const auto id = window->GetID(name);
    const float w = ImGui::CalcItemWidth();
    ImGui::PushID(id);
    const float height = ImGui::GetFrameHeight();
    const float width = height * 1.55f;
    const float radius = height * 0.5f;
    const ImRect total_bb(window->DC.CursorPos, window->DC.CursorPos + ImVec2(245, ImGui::GetFrameHeight()));
    const ImRect frame_bb(window->DC.CursorPos + ImVec2(245 - (ImGui::GetFrameHeight() * 1.55f), 0), window->DC.CursorPos + ImVec2(245, ImGui::GetFrameHeight()));
    ImGui::PushFont(g_fonts->icomenus1);
    auto text_size = ImGui::CalcTextSize(CXOR("k")).x;
    ImGui::PopFont();
    const ImRect bind_bb(window->DC.CursorPos + ImVec2(245 + 15 - text_size / 2 - (ImGui::GetFrameHeight() * 1.55f) * 2, 0), window->DC.CursorPos + ImVec2(245 - (ImGui::GetFrameHeight() * 1.55f) - text_size / 2, ImGui::GetFrameHeight()));
    ImGui::ItemSize(total_bb);
    if (!ImGui::ItemAdd(total_bb, id, &frame_bb))
        return false;
    if (!ImGui::ItemAdd(total_bb, id + 1, &bind_bb))
        return false;
    window->DrawList->AddText({ total_bb.Min.x, total_bb.Min.y + ImGui::CalcTextSize(name).y / 2 }, IM_COL32(255, 255, 255, 255 * alpha), name);
    float last_active_id_timer = g.LastActiveIdTimer;
    bool hovered, held;
    bool pressed = ImGui::ButtonBehavior(frame_bb, id, &hovered, &held);
    if (pressed)
    {
        *ceva = !*ceva;
        ImGui::MarkItemEdited(id);
        g.LastActiveIdTimer = 0.f;
    }
    if (g.LastActiveIdTimer == 0.f && g.LastActiveId == id && !pressed)
        g.LastActiveIdTimer = last_active_id_timer;

    float t = *ceva ? 1.0f : 0.0f;

    if (g.LastActiveId == id)
    {
        float t_anim = ImSaturate(g.LastActiveIdTimer / 0.09f);
        t = *ceva ? (t_anim) : (1.0f - t_anim);
    }

    ImU32 col_bg;
    col_bg = IM_COL32((int)(g_cfg.misc.ui_color.base().r() * t)
        , (int)(g_cfg.misc.ui_color.base().g() * t)
        , (int)(g_cfg.misc.ui_color.base().b() * t)
        , (int)(g_cfg.misc.ui_color.base().a() / 255.f * 203 * t * alpha));
    /*if (hovered)
        col_bg = !*ceva ? IM_COL32(20, 20, 20, 255) : Helpers::calculateColor(config->misc.accentColor, 0.7943137f * t);
    else
        col_bg = !*ceva ? IM_COL32(0, 0, 0, 255) : Helpers::calculateColor(config->misc.accentColor, 0.7043137f);*/

    bool hoveredb, heldb;
    bool pressedb = ImGui::ButtonBehavior(bind_bb, id + 1, &hoveredb, &heldb);

    if (pressed)
    {
        ImGui::OpenPopupEx(id);
    }
    if (ImGui::BeginPopupEx(id, ImGuiWindowFlags_AlwaysAutoResize | ImGuiWindowFlags_NoTitleBar | ImGuiWindowFlags_NoSavedSettings))
    {
        hotkey3(CXOR("Bind"), var);
        MenuShit::combo(CXOR("Key mode"), &var.type, "Always\0Hold\0Toggle\0");
        //MenuShit::toggle(CXOR("Show in list"), &k->showInList);
        ImGui::EndPopup();
    }

    ImGui::PushFont(g_fonts->icomenus1);
    window->DrawList->AddText(ImVec2{ bind_bb.Max.x - ImGui::CalcTextSize(CXOR("k")).x, bind_bb.Max.y - ImGui::CalcTextSize(CXOR("k")).y }, g_cfg.misc.ui_color.base().as_imcolor(), CXOR("k"));
    ImGui::PopFont();
    window->DrawList->AddRectFilled({ frame_bb.Min.x, frame_bb.Min.y + 6 }, ImVec2(frame_bb.Max.x, frame_bb.Max.y - 4), col_bg, 9.f);
    window->DrawList->AddRectFilled({ frame_bb.Min.x, frame_bb.Min.y + 6 }, ImVec2(frame_bb.Max.x, frame_bb.Max.y - 4), g_cfg.misc.ui_color.base().new_alpha(0.0643137f * 255 * alpha).as_imcolor(), 9.f);
    window->DrawList->AddRect({ frame_bb.Min.x, frame_bb.Min.y + 6 }, ImVec2(frame_bb.Max.x, frame_bb.Max.y - 4), g_cfg.misc.ui_color.base().new_alpha(0.15f * -(t - 1) * 255 * alpha).as_imcolor(), 9.f);
    window->DrawList->AddCircleFilled(ImVec2(frame_bb.Min.x + t * (width - radius) + 1.5f * t + (radius - 2.f) * -(t - 1), frame_bb.Max.y - (radius - 2) - 1), radius - 2.f, IM_COL32(255, 255, 255, 255 * alpha));
    ImGui::PopID();
    return ceva;
}

bool MenuShit::bind(const char* label, key_binds_t& var) noexcept {

    ImGuiWindow* window = ImGui::GetCurrentWindow();
    ImGuiContext& g = *GImGui;
    const auto id = window->GetID(label);
    ImGui::PushID(id);
    const float w = ImGui::CalcItemWidth();
    const ImVec2 label_size = ImGui::CalcTextSize(label);
    const ImRect total_bb(window->DC.CursorPos, window->DC.CursorPos + ImVec2(245, ImGui::GetFrameHeight()));
    const ImRect frame_bb(window->DC.CursorPos + ImVec2(245 + 13 - (ImGui::GetFrameHeight() * 1.55f), 0), window->DC.CursorPos + ImVec2(245, ImGui::GetFrameHeight()));
    ImGui::ItemSize(total_bb);
    if (!ImGui::ItemAdd(total_bb, id, &frame_bb))
        return false;
    window->DrawList->AddText({ total_bb.Min.x, total_bb.Min.y + ImGui::CalcTextSize(label).y / 2 }, IM_COL32(255, 255, 255, 255), label);
    bool hovered, held;
    bool pressed = ImGui::ButtonBehavior(frame_bb, id, &hovered, &held);
    //window->DrawList->AddRect(frame_bb.Min, frame_bb.Max, IM_COL32(255, 255, 255, 255));
    ImGui::PushFont(g_fonts->icomenus1);
    window->DrawList->AddText(ImVec2{ frame_bb.Max.x - ImGui::CalcTextSize(CXOR("k")).x, total_bb.Max.y - ImGui::CalcTextSize(CXOR("k")).y }, g_cfg.misc.ui_color.base().new_alpha(255 * alpha).as_imcolor(), CXOR("k"));
    ImGui::PopFont();
    if (pressed)
    {
        ImGui::OpenPopupEx(id);
    }
    if (ImGui::BeginPopupEx(id, ImGuiWindowFlags_AlwaysAutoResize | ImGuiWindowFlags_NoTitleBar | ImGuiWindowFlags_NoSavedSettings))
    {
        hotkey3(CXOR("Bind"), var);
        MenuShit::combo(CXOR("Key mode"), &var.type, "Always\0Hold\0Toggle\0");
        MenuShit::toggle(CXOR("Show in list"), &var.show_in_list);
        ImGui::EndPopup();
    }
    ImGui::PopID();
}

static const char* PatchFormatStringFloatToInt(const char* fmt)
{
    if (fmt[0] == '%' && fmt[1] == '.' && fmt[2] == '0' && fmt[3] == 'f' && fmt[4] == 0) // Fast legacy path for "%.0f" which is expected to be the most common case.
        return "%d";
    const char* fmt_start = ImParseFormatFindStart(fmt);    // Find % (if any, and ignore %%)
    const char* fmt_end = ImParseFormatFindEnd(fmt_start);  // Find end of format specifier, which itself is an exercise of confidence/recklessness (because snprintf is dependent on libc or user).
    if (fmt_end > fmt_start && fmt_end[-1] == 'f')
    {
#ifndef IMGUI_DISABLE_OBSOLETE_FUNCTIONS
        if (fmt_start == fmt && fmt_end[0] == 0)
            return "%d";
        ImGuiContext& g = *GImGui;
        ImFormatString(g.TempBuffer, IM_ARRAYSIZE(g.TempBuffer), "%.*s%%d%s", (int)(fmt_start - fmt), fmt, fmt_end); // Honor leading and trailing decorations, but lose alignment/precision.
        return g.TempBuffer;
#else
        IM_ASSERT(0 && "DragInt(): Invalid format string!"); // Old versions used a default parameter of "%.0f", please replace with e.g. "%d"
#endif
    }
    return fmt;
}

void MenuShit::sliderInt(const char* name, int* value, int min, int max, const char* format, ImGuiSliderFlags flags)
{
    ImGuiWindow* window = ImGui::GetCurrentWindow();
    ImGuiContext& g = *GImGui;
    const auto id = window->GetID(name);
    const float w = ImGui::CalcItemWidth();
    const ImVec2 label_size = ImGui::CalcTextSize(name);
    ImRect total_bb(window->DC.CursorPos, window->DC.CursorPos + ImVec2(w, ImGui::GetFrameHeight()));
    ImRect frame_bb(total_bb.Min + ImVec2(160, 0), total_bb.Max);
    ImRect frame_bb_extra(total_bb.Min + ImVec2(126, 0), total_bb.Min + ImVec2(160, ImGui::GetFrameHeight()));
    ImGui::ItemSize(total_bb);
    if (!ImGui::ItemAdd(total_bb, id, &frame_bb))
        return;

    window->DrawList->AddText({ total_bb.Min.x, total_bb.Min.y + label_size.y / 2 }, IM_COL32(255, 255, 255, 255), name);

    format = PatchFormatStringFloatToInt(format);

    const bool hovered = ImGui::ItemHoverable(frame_bb, id);
    const bool clicked = (hovered && g.IO.MouseClicked[0]);

    bool temp_input_is_active = ImGui::TempInputTextIsActive(id);
    bool temp_input_start = false;

    if (!temp_input_is_active)
    {
        const bool focus_requested = ImGui::FocusableItemRegister(window, id);
        if (focus_requested || clicked || g.NavActivateId == id || g.NavInputId == id)
        {
            ImGui::SetActiveID(id, window);
            ImGui::SetFocusID(id, window);
            ImGui::FocusWindow(window);
            g.ActiveIdUsingNavDirMask |= (1 << ImGuiDir_Left) | (1 << ImGuiDir_Right);
            if (focus_requested || (clicked && g.IO.KeyCtrl) || g.NavInputId == id)
            {
                temp_input_start = true;
                ImGui::FocusableItemUnregister(window);
            }
        }
    }

    if (temp_input_is_active || temp_input_start)
    {
        ImGui::SameLine(160.f);
        ImGui::TempInputTextScalar(frame_bb, id, name, ImGuiDataType_S32, value, format);
        return;
    }

    ImRect grab_bb;
    const bool value_changed = ImGui::SliderBehavior(frame_bb, id, ImGuiDataType_S32, value, &min, &max, format, 1.f, flags, &grab_bb);
    if (value_changed)
        ImGui::MarkItemEdited(id);

    ImU32 col_bg;
    if (hovered)
        col_bg = IM_COL32(20, 20, 20, 255 * alpha);
    else
        col_bg = IM_COL32(0, 0, 0, 255 * alpha);
    char value_buf[64];
    const char* value_buf_end = value_buf + ImGui::DataTypeFormatString(value_buf, IM_ARRAYSIZE(value_buf), ImGuiDataType_S32, value, format);
    ImGui::RenderNavHighlight(frame_bb, id);
    window->DrawList->AddRectFilled({ frame_bb.Min.x, frame_bb.Min.y + 5.f }, { frame_bb.Max.x, frame_bb.Max.y - 3.f }, col_bg, 12.0f);
    window->DrawList->AddRectFilled({ frame_bb.Min.x, frame_bb.Min.y + 5.f }, { frame_bb.Max.x, frame_bb.Max.y - 3.f }, g_cfg.misc.ui_color.base().new_alpha(0.1643137f * 255 * alpha).as_imcolor(), 12.f);
    window->DrawList->AddRect({ frame_bb.Min.x, frame_bb.Min.y + 5.f }, { frame_bb.Max.x, frame_bb.Max.y - 3.f }, g_cfg.misc.ui_color.base().new_alpha(0.25f * 255 * alpha).as_imcolor(), 12.f, 15, 1.f);
    window->DrawList->AddRectFilled({ frame_bb.Min.x, frame_bb.Min.y + 5.5f }, ImVec2(grab_bb.Max.x + 1.f, frame_bb.Max.y - 3.5f), g_cfg.misc.ui_color.base().new_alpha(255 * alpha).as_imcolor(), 12.f);
    ImGui::RenderTextClipped(total_bb.Min, total_bb.Min + ImVec2(154, ImGui::GetFrameHeight()), value_buf, value_buf_end, NULL, ImVec2(1.0f, 0.5f));
    return;
}

float MenuShit::sliderFloat(const char* name, float* value, float min, float max, const char* format)
{
    ImGuiWindow* window = ImGui::GetCurrentWindow();
    ImGuiContext& g = *GImGui;
    const auto id = window->GetID(name);
    const float w = ImGui::CalcItemWidth();
    const ImVec2 label_size = ImGui::CalcTextSize(name);
    const ImRect total_bb(window->DC.CursorPos, window->DC.CursorPos + ImVec2(245, ImGui::GetFrameHeight()));
    ImGui::ItemSize(total_bb);
    const ImRect frame_bb(total_bb.Min + ImVec2(160, 0), total_bb.Max);
    if (!ImGui::ItemAdd(total_bb, id, &frame_bb))
        return 0.f;
    if (format == NULL)
        format = ImGui::DataTypeGetInfo(ImGuiDataType_Float)->PrintFmt;
    window->DrawList->AddText({ total_bb.Min.x, total_bb.Min.y + label_size.y / 2 }, IM_COL32(255, 255, 255, 255), name);
    const bool hovered = ImGui::ItemHoverable(frame_bb, id);
    const bool clicked = (hovered && g.IO.MouseClicked[0]);
    if (clicked)
    {
        ImGui::SetActiveID(id, window);
        ImGui::SetFocusID(id, window);
        ImGui::FocusWindow(window);
        g.ActiveIdUsingNavDirMask |= (1 << ImGuiDir_Left) | (1 << ImGuiDir_Right);
    }
    ImRect grab_bb;
    const bool value_changed = ImGui::SliderBehavior(frame_bb, id, ImGuiDataType_Float, value, &min, &max, format, 1.f, NULL, &grab_bb);
    if (value_changed)
        ImGui::MarkItemEdited(id);
    ImU32 col_bg;
    if (hovered)
        col_bg = IM_COL32(40, 28, 0, alpha * 255);  // Dark orange tint
    else
        col_bg = IM_COL32(25, 17, 0, alpha * 255);  // Very dark orange
    char value_buf[64];
    const char* value_buf_end = value_buf + ImGui::DataTypeFormatString(value_buf, IM_ARRAYSIZE(value_buf), ImGuiDataType_Float, value, format);
    ImGui::RenderNavHighlight(frame_bb, id);

    window->DrawList->AddRectFilled({ frame_bb.Min.x, frame_bb.Min.y + 5.f }, { frame_bb.Max.x, frame_bb.Max.y - 3.f }, col_bg, 12.0f);
    window->DrawList->AddRectFilled({ frame_bb.Min.x, frame_bb.Min.y + 5.f }, { frame_bb.Max.x, frame_bb.Max.y - 3.f }, g_cfg.misc.ui_color.base().new_alpha(alpha * 0.1643137f * 255).as_imcolor(), 12.f);
    window->DrawList->AddRect({ frame_bb.Min.x, frame_bb.Min.y + 5.f }, { frame_bb.Max.x, frame_bb.Max.y - 3.f }, g_cfg.misc.ui_color.base().new_alpha(alpha * 0.25f * 255).as_imcolor(), 12.f, 15, 1.f);
    window->DrawList->AddRectFilled({ frame_bb.Min.x, frame_bb.Min.y + 5.5f }, ImVec2(grab_bb.Max.x + 1.f, frame_bb.Max.y - 3.5f), g_cfg.misc.ui_color.base().new_alpha(255 * alpha).as_imcolor(), 12.f);
    ImGui::RenderTextClipped(total_bb.Min, total_bb.Min + ImVec2(154, ImGui::GetFrameHeight()), value_buf, value_buf_end, NULL, ImVec2(1.0f, 0.5f));
    return *value;
}

bool MenuShit::toggleWithColor(const char* name, bool* enable, c_float_color& col, bool use_alpha) noexcept
{
    if (use_alpha)
        return MenuShit::toggleWithColor(name, enable, col.float_base(), &col[3]);
    else
        return MenuShit::toggleWithColor(name, enable, col.float_base());
}

bool MenuShit::toggleWithColor(const char* name, bool* enable, c_color& col, bool use_alpha) noexcept
{
    return MenuShit::toggleWithColor(name, enable, col, use_alpha);
}

bool MenuShit::toggleWithColor(const char* name, bool* enable, float color[3], float* alpha) noexcept
{
    ImGuiWindow* window = ImGui::GetCurrentWindow();
    ImGuiContext& g = *GImGui;
    const auto id = window->GetID(name);
    const float w = ImGui::CalcItemWidth();
    float height = ImGui::GetFrameHeight();
    const float width = height * 1.55f;
    const float radius = height * 0.5f;
    const ImRect total_bb(window->DC.CursorPos, window->DC.CursorPos + ImVec2(245, ImGui::GetFrameHeight()));
    const ImRect frame_bb(window->DC.CursorPos + ImVec2(245 - (ImGui::GetFrameHeight() * 1.55f), 0), window->DC.CursorPos + ImVec2(245, ImGui::GetFrameHeight()));
    const ImRect frame_bb1(window->DC.CursorPos + ImVec2(245 - 29 - (ImGui::GetFrameHeight() * 1.55f), 0), window->DC.CursorPos + ImVec2(245 - 10 - (ImGui::GetFrameHeight() * 1.55f), ImGui::GetFrameHeight()));
    ImGui::ItemSize(total_bb);
    if (!ImGui::ItemAdd(total_bb, id, &frame_bb))
        return false;

    if (!ImGui::ItemAdd(total_bb, id + 1, &frame_bb1))
        return false;

    //ImGui::GetWindowDrawList()->AddRectFilled(frame_bb1.Min, frame_bb1.Max, IM_COL32(255, 255, 255, 255), 12.f);
    ImGui::GetWindowDrawList()->AddCircleFilled({ frame_bb1.Min.x + ImGui::GetFrameHeight() * 0.5f - 2.f, frame_bb1.Min.y + ImGui::GetFrameHeight() * 0.5f + 1 }, ImGui::GetFrameHeight() * 0.5f - 2.f, ImColor(color[0], color[1], color[2], *alpha), 64);
    window->DrawList->AddText({ total_bb.Min.x, total_bb.Min.y + ImGui::CalcTextSize(name).y / 2 }, IM_COL32(255, 255, 255, 255), name);
    bool hovered, held;
    bool hovered1, held1;
    bool pressed = ImGui::ButtonBehavior(frame_bb, id, &hovered, &held);
    bool pressed1 = ImGui::ButtonBehavior(frame_bb1, id + 1, &hovered1, &held1);

    std::string popup_name = name + XOR("##popup");
    if (pressed1 || held1)
    {
        ImGui::OpenPopup(popup_name.c_str());
    }
    if (ImGui::BeginPopup(popup_name.c_str())) {
        if (alpha) {
            float col[]{ color[0], color[1], color[2], *alpha };
            ImGui::ColorPicker4(CXOR("##picker"), col, ImGuiColorEditFlags_NoSmallPreview | ImGuiColorEditFlags_NoSidePreview);
            color[0] = col[0];
            color[1] = col[1];
            color[2] = col[2];
            *alpha = col[3];
        }
        else {
            ImGui::ColorPicker3(CXOR("##picker"), color, ImGuiColorEditFlags_NoSmallPreview | ImGuiColorEditFlags_NoSidePreview);
        }
        
        // Convert to the formats we need
        float f[4] = { color[0], color[1], color[2], alpha ? *alpha : 1.0f };
        int i[4] = { IM_F32_TO_INT8_UNBOUND(f[0]), IM_F32_TO_INT8_UNBOUND(f[1]), IM_F32_TO_INT8_UNBOUND(f[2]), IM_F32_TO_INT8_UNBOUND(f[3]) };
        
        bool value_changed = false;
        bool value_changed_as_float = false;

        // RGB Hexadecimal Input
        char buf[64];
        if (alpha)
            ImFormatString(buf, IM_ARRAYSIZE(buf), "#%02X%02X%02X%02X", ImClamp(i[0], 0, 255), ImClamp(i[1], 0, 255), ImClamp(i[2], 0, 255), ImClamp(i[3], 0, 255));
        else
            ImFormatString(buf, IM_ARRAYSIZE(buf), "#%02X%02X%02X", ImClamp(i[0], 0, 255), ImClamp(i[1], 0, 255), ImClamp(i[2], 0, 255));
        ImGui::SetNextItemWidth(ImGui::CalcItemWidth());
        if (ImGui::InputText("##Text", buf, IM_ARRAYSIZE(buf), ImGuiInputTextFlags_CharsHexadecimal | ImGuiInputTextFlags_CharsUppercase))
        {
            value_changed = true;
            char* p = buf;
            while (*p == '#' || ImCharIsBlankW(*p))
                p++;
            i[0] = i[1] = i[2] = i[3] = 0;
            if (alpha)
                sscanf(p, "%02X%02X%02X%02X", (unsigned int*)&i[0], (unsigned int*)&i[1], (unsigned int*)&i[2], (unsigned int*)&i[3]); // Treat at unsigned (%X is unsigned)
            else
                sscanf(p, "%02X%02X%02X", (unsigned int*)&i[0], (unsigned int*)&i[1], (unsigned int*)&i[2]);
        }

        if (!value_changed_as_float)
            for (int n = 0; n < 4; n++)
                f[n] = i[n] / 255.0f;

        if (value_changed)
        {
            color[0] = f[0];
            color[1] = f[1];
            color[2] = f[2];
            if (alpha)
                *alpha = f[3];
        }

        window = ImGui::GetCurrentWindow();
        window->Size = { 120,120 };

        for (int i = 0; i < pastedColors.size(); ++i)
        {
            auto& col_paste = pastedColors[i];

            if (ImGui::InvisibleButton(std::to_string(i + 1).c_str(), { ImGui::GetFrameHeight(), ImGui::GetFrameHeight() }))
            {
                color[0] = col_paste[0];
                color[1] = col_paste[1];
                color[2] = col_paste[2];
                if (alpha)
                    *alpha = col_paste[3];
            }

            if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
                ImGui::OpenPopup(CXOR("##option magic") + i);

            if (ImGui::BeginPopup(CXOR("##option magic") + i))
            {
                if (ImGui::Button(CXOR("Delete"), { 45.0f, 0.0f }))
                {
                    pastedColors.erase(pastedColors.begin() + i);
                    ImGui::CloseCurrentPopup();
                }
                ImGui::EndPopup();
            }

            window->DrawList->AddRectFilled(window->DC.CursorPos - ImVec2{ -ImGui::GetFrameHeight() * i - g.Style.ItemSpacing.x * i, ImGui::GetFrameHeight() + 1 }, window->DC.CursorPos + ImVec2{ ImGui::GetFrameHeight() * (i + 1) + g.Style.ItemSpacing.x * i, -5 }, IM_COL32(col_paste[0] * 255, col_paste[1] * 255, col_paste[2] * 255, 255), 3.f);

            ImGui::SameLine();
        }

        const std::array<float, 4> ar_col = { color[0], color[1],color[2], alpha ? *alpha : 1.f };

        if (ImGui::Button(CXOR("+"), { ImGui::GetFrameHeight() - 1.f, ImGui::GetFrameHeight() }))
            pastedColors.push_back(ar_col);

        ImGui::EndPopup();
    }

    window = ImGui::GetCurrentWindow();

    auto& mod = g_menu.item_animations[CONST_HASH(name)];
    if (pressed)
    {
        *enable = !*enable;
        ImGui::MarkItemEdited(id);
    }

    g_menu.create_animation(mod.alpha, *enable, 0.4f, lerp_animation);
    float t = mod.alpha;

    ImU32 col_bg;
    col_bg = IM_COL32((int)(255 * t)  // Orange R
        , (int)(140 * t)              // Orange G  
        , (int)(0 * t)                // Orange B
        , (int)(203 * t * MenuShit::alpha));

    window->DrawList->AddRectFilled({ frame_bb.Min.x, frame_bb.Min.y + 6 }, ImVec2(frame_bb.Max.x, frame_bb.Max.y - 4), col_bg, 9.f);
    window->DrawList->AddRectFilled({ frame_bb.Min.x, frame_bb.Min.y + 6 }, ImVec2(frame_bb.Max.x, frame_bb.Max.y - 4), g_cfg.misc.ui_color.base().new_alpha(0.1643137f * 255 * MenuShit::alpha).as_imcolor(), 9.f);
    window->DrawList->AddRect({ frame_bb.Min.x, frame_bb.Min.y + 6 }, ImVec2(frame_bb.Max.x, frame_bb.Max.y - 4), g_cfg.misc.ui_color.base().new_alpha(0.25f * -(t - 1) * 255 * MenuShit::alpha).as_imcolor(), 9.f);
    window->DrawList->AddCircleFilled(ImVec2(frame_bb.Min.x + t * (width - radius) + 1.5f * t + (radius - 2.f) * -(t - 1), frame_bb.Max.y - (radius - 2) - 1), radius - 2.f, IM_COL32(255, 255, 255, 255 * MenuShit::alpha));
    return enable;
}

void MenuShit::colorPicker(const char* name, float color[3], float* alpha) noexcept
{
    ImGuiWindow* window = ImGui::GetCurrentWindow();
    ImGuiContext& g = *GImGui;
    const auto id = window->GetID(name);
    const ImRect total_bb(window->DC.CursorPos, window->DC.CursorPos + ImVec2(245, ImGui::GetFrameHeight()));
    const ImRect frame_bb(window->DC.CursorPos + ImVec2(245 - (ImGui::GetFrameHeight() * 1.55f), 0), window->DC.CursorPos + ImVec2(245, ImGui::GetFrameHeight()));
    ImGui::PushID(id);
    ImGui::ItemSize(total_bb);
    if (!ImGui::ItemAdd(total_bb, id, &frame_bb))
        return;
    window->DrawList->AddText({ total_bb.Min.x, total_bb.Min.y + ImGui::CalcTextSize(name).y / 2 }, IM_COL32(255, 255, 255, MenuShit::alpha * 255), name);
    bool hovered, held;
    bool pressed = ImGui::ButtonBehavior(frame_bb, id, &hovered, &held);
    if (pressed || held)
        ImGui::OpenPopup(CXOR("##popup"));
    if (ImGui::BeginPopup(CXOR("##popup"))) {
        if (alpha) {
            float col[]{ color[0], color[1], color[2], *alpha };
            ImGui::ColorPicker4(CXOR("##picker"), col, ImGuiColorEditFlags_NoSmallPreview | ImGuiColorEditFlags_NoSidePreview);
            color[0] = col[0];
            color[1] = col[1];
            color[2] = col[2];
            *alpha = col[3];
        }
        else {
            ImGui::ColorPicker3(CXOR("##picker"), color, ImGuiColorEditFlags_NoSmallPreview | ImGuiColorEditFlags_NoSidePreview);
        }

        // Convert to the formats we need
        float f[4] = { color[0], color[1], color[2], alpha ? *alpha : 1.0f };
        int i[4] = { IM_F32_TO_INT8_UNBOUND(f[0]), IM_F32_TO_INT8_UNBOUND(f[1]), IM_F32_TO_INT8_UNBOUND(f[2]), IM_F32_TO_INT8_UNBOUND(f[3]) };

        bool value_changed = false;
        bool value_changed_as_float = false;

        // RGB Hexadecimal Input
        char buf[64];
        if (alpha)
            ImFormatString(buf, IM_ARRAYSIZE(buf), "#%02X%02X%02X%02X", ImClamp(i[0], 0, 255), ImClamp(i[1], 0, 255), ImClamp(i[2], 0, 255), ImClamp(i[3], 0, 255));
        else
            ImFormatString(buf, IM_ARRAYSIZE(buf), "#%02X%02X%02X", ImClamp(i[0], 0, 255), ImClamp(i[1], 0, 255), ImClamp(i[2], 0, 255));
        ImGui::SetNextItemWidth(ImGui::CalcItemWidth());
        if (ImGui::InputText("##Text", buf, IM_ARRAYSIZE(buf), ImGuiInputTextFlags_CharsHexadecimal | ImGuiInputTextFlags_CharsUppercase))
        {
            value_changed = true;
            char* p = buf;
            while (*p == '#' || ImCharIsBlankW(*p))
                p++;
            i[0] = i[1] = i[2] = i[3] = 0;
            if (alpha)
                sscanf(p, "%02X%02X%02X%02X", (unsigned int*)&i[0], (unsigned int*)&i[1], (unsigned int*)&i[2], (unsigned int*)&i[3]); // Treat at unsigned (%X is unsigned)
            else
                sscanf(p, "%02X%02X%02X", (unsigned int*)&i[0], (unsigned int*)&i[1], (unsigned int*)&i[2]);
        }

        if (!value_changed_as_float)
            for (int n = 0; n < 4; n++)
                f[n] = i[n] / 255.0f;

        if (value_changed)
        {
            color[0] = f[0];
            color[1] = f[1];
            color[2] = f[2];
            if (alpha)
                *alpha = f[3];
        }

        window = ImGui::GetCurrentWindow();
        window->Size = { 120,120 };

        for (int i = 0; i < pastedColors.size(); ++i)
        {
            auto& col_paste = pastedColors[i];

            if (ImGui::InvisibleButton(std::to_string(i + 1).c_str(), { ImGui::GetFrameHeight(), ImGui::GetFrameHeight() }))
            {
                color[0] = col_paste[0];
                color[1] = col_paste[1];
                color[2] = col_paste[2];
                if (alpha)
                    *alpha = col_paste[3];
            }

            if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
                ImGui::OpenPopup(CXOR("##option magic") + i);

            if (ImGui::BeginPopup(CXOR("##option magic") + i))
            {
                if (ImGui::Button(CXOR("Delete"), { 45.0f, 0.0f }))
                {
                    pastedColors.erase(pastedColors.begin() + i);
                    ImGui::CloseCurrentPopup();
                }
                ImGui::EndPopup();
            }

            window->DrawList->AddRectFilled(window->DC.CursorPos - ImVec2{ -ImGui::GetFrameHeight() * i - g.Style.ItemSpacing.x * i, ImGui::GetFrameHeight() + 1 }, window->DC.CursorPos + ImVec2{ ImGui::GetFrameHeight() * (i + 1) + g.Style.ItemSpacing.x * i, -5 }, IM_COL32(col_paste[0] * 255, col_paste[1] * 255, col_paste[2] * 255, 255), 3.f);

            ImGui::SameLine();
        }

        const std::array<float, 4> ar_col = { color[0], color[1],color[2], alpha ? *alpha : 1.f };

        if (ImGui::Button(CXOR("+"), { ImGui::GetFrameHeight() - 1.f, ImGui::GetFrameHeight() }))
            pastedColors.push_back(ar_col);

        ImGui::EndPopup();
    }
    window = ImGui::GetCurrentWindow();

    window->DrawList->AddCircleFilled({ total_bb.Max.x - ImGui::GetFrameHeight() * 0.5f + 2.f, total_bb.Min.y + ImGui::GetFrameHeight() * 0.5f }, ImGui::GetFrameHeight() * 0.5f - 2.f, IM_COL32(color[0] * 255, color[1] * 255, color[2] * 255, MenuShit::alpha * 255), 64);
    ImGui::PopID();
}

void MenuShit::colorPicker(const char* name, c_float_color& col, bool use_alpha) noexcept
{
    if (use_alpha)
        MenuShit::colorPicker(name, col.float_base(), &col[3]);
    else
        MenuShit::colorPicker(name, col.float_base());
}

void MenuShit::colorPicker(const char* name, c_color& col, bool use_alpha) noexcept
{
    MenuShit::colorPicker(name, col, use_alpha);
}

static float CalcMaxPopupHeightFromItemCount(int items_count)
{
    ImGuiContext& g = *GImGui;
    if (items_count <= 0)
        return FLT_MAX;
    return (g.FontSize + g.Style.ItemSpacing.y) * items_count - g.Style.ItemSpacing.y + (g.Style.WindowPadding.y * 2);
}

ImRect GetWindowAllowedExtentRect(ImGuiWindow* window)
{
    /*ImGuiContext& g = *GImGui;
    IM_UNUSED(window);
    ImRect r_screen = ((ImGuiViewportP*)(void*)ImGui::GetMainViewport())->GetMainRect();
    ImVec2 padding = g.Style.DisplaySafeAreaPadding;
    r_screen.Expand(ImVec2((r_screen.GetWidth() > padding.x * 2) ? -padding.x : 0.0f, (r_screen.GetHeight() > padding.y * 2) ? -padding.y : 0.0f));
    return r_screen;*/
    return ImRect{ {}, {} };
}

static float PopupMaxHeight(int items_count)
{
    ImGuiContext& g = *GImGui;
    if (items_count <= 0)
        return FLT_MAX;
    return (g.FontSize + g.Style.ItemSpacing.y) * items_count - g.Style.ItemSpacing.y + (g.Style.WindowPadding.y * 2);
}

bool MenuShit::beginCombo(const char* label, const char* preview_value, ImGuiComboFlags flags, int items, std::string extra)
{
    // Always consume the SetNextWindowSizeConstraint() call in our early return paths
    ImGuiContext& g = *GImGui;
    bool has_window_size_constraint = (g.NextWindowData.Flags & ImGuiNextWindowDataFlags_HasSizeConstraint) != 0;
    g.NextWindowData.Flags &= ~ImGuiNextWindowDataFlags_HasSizeConstraint;

    ImGuiWindow* window = ImGui::GetCurrentWindow();
    if (window->SkipItems)
        return false;

    IM_ASSERT((flags & (ImGuiComboFlags_NoArrowButton | ImGuiComboFlags_NoPreview)) != (ImGuiComboFlags_NoArrowButton | ImGuiComboFlags_NoPreview)); // Can't use both flags together

    const ImGuiStyle& style = g.Style;
    const ImGuiID id = window->GetID(label);

    const float arrow_size = (flags & ImGuiComboFlags_NoArrowButton) ? 0.0f : ImGui::GetFrameHeight();
    const ImVec2 label_size = ImGui::CalcTextSize(label, NULL, true);
    const float expected_w = ImGui::CalcItemWidth();
    const float w = (flags & ImGuiComboFlags_NoPreview) ? arrow_size : expected_w;
    const ImRect total_bb(window->DC.CursorPos, window->DC.CursorPos + ImVec2(245, ImGui::GetFrameHeight()));
    ImGui::ItemSize(total_bb);
    const ImRect frame_bb(total_bb.Min + ImVec2(160, 0), total_bb.Max);
    const ImRect frame_bb_extra(total_bb.Min + ImVec2(126, 0), total_bb.Min + ImVec2(160, ImGui::GetFrameHeight()));
    if (!ImGui::ItemAdd(total_bb, id, &frame_bb))
        return false;
    if (extra.length() > 0)
    {
        if (!ImGui::ItemAdd(total_bb, id + 1, &frame_bb_extra))
            return false;
        bool hovered1, held1;
        bool pressed1 = ImGui::ButtonBehavior(frame_bb_extra, id + 1, &hovered1, &held1);
        if (pressed1 || held1)
        {
            ImGui::OpenPopup(extra.c_str());
        }
        ImGui::PushFont(g_fonts->icomenus);
        window->DrawList->AddText({ frame_bb_extra.Max.x - ImGui::CalcTextSize("M").x / 2 - 19, total_bb.Min.y + 1 }, g_cfg.misc.ui_color.base().new_alpha(alpha * 0.75f * 255).as_imcolor(), CXOR("M"));
        ImGui::PopFont();
    }

    bool hovered, held;
    bool pressed = ImGui::ButtonBehavior(frame_bb, id, &hovered, &held);
    bool popup_open = ImGui::IsPopupOpen(id);
    window->DrawList->AddText({ total_bb.Min.x, total_bb.Min.y + label_size.y / 2 }, IM_COL32(255, 255, 255, alpha * 255), label);
    ImU32 col_bg;
    if (hovered)
        col_bg = IM_COL32(40, 28, 0, alpha * 255);  // Dark orange tint
    else
        col_bg = IM_COL32(25, 17, 0, alpha * 255);  // Very dark orange
    const float value_x2 = ImMax(frame_bb.Min.x, frame_bb.Max.x - arrow_size);
    window->DrawList->AddRectFilled(ImVec2(frame_bb.Min.x, frame_bb.Min.y + 3.f), ImVec2(frame_bb.Max.x, frame_bb.Max.y - 1.f), col_bg, 3.f);
    window->DrawList->AddRectFilled(ImVec2(frame_bb.Min.x, frame_bb.Min.y + 3.f), ImVec2(frame_bb.Max.x, frame_bb.Max.y - 1.f), g_cfg.misc.ui_color.base().new_alpha(alpha * 0.1643137f * 255).as_imcolor(), 3.f);
    window->DrawList->AddRect(ImVec2(frame_bb.Min.x, frame_bb.Min.y + 3.f), ImVec2(frame_bb.Max.x, frame_bb.Max.y - 1.f), g_cfg.misc.ui_color.base().new_alpha(alpha * 0.25f * 255).as_imcolor(), 3.f);
    ImGui::RenderArrow(window->DrawList, ImVec2(value_x2 + style.FramePadding.y, frame_bb.Min.y + style.FramePadding.y + 1.5f), ImColor(1.f, 1.f, 1.f, 1.f), !popup_open ? ImGuiDir_Down : ImGuiDir_Up, 0.9f);
    if (preview_value != NULL && !(flags & ImGuiComboFlags_NoPreview))
    {
        ImVec2 preview_pos = frame_bb.Min + style.FramePadding;
        ImGui::RenderTextClipped(preview_pos, ImVec2(value_x2 + 2, frame_bb.Max.y), preview_value, NULL, NULL, ImVec2(0.0f, 0.205f));
    }
    if ((pressed || g.NavActivateId == id) && !popup_open)
    {
        if (window->DC.NavLayerCurrent == 0)
            window->NavLastIds[0] = id;
        ImGui::OpenPopupEx(id);
        popup_open = true;
    }

    if (!popup_open)
        return false;

    if (has_window_size_constraint)
    {
        g.NextWindowData.Flags |= ImGuiNextWindowDataFlags_HasSizeConstraint;
        g.NextWindowData.SizeConstraintRect.Min.x = ImMax(g.NextWindowData.SizeConstraintRect.Min.x, w);
    }
    else
    {
        if ((flags & ImGuiComboFlags_HeightMask_) == 0)
            flags |= ImGuiComboFlags_HeightRegular;
        IM_ASSERT(ImIsPowerOfTwo(flags & ImGuiComboFlags_HeightMask_));    // Only one
        int popup_max_height_in_items = -1;
        if (flags & ImGuiComboFlags_HeightRegular)     popup_max_height_in_items = 8;
        else if (flags & ImGuiComboFlags_HeightSmall)  popup_max_height_in_items = 4;
        else if (flags & ImGuiComboFlags_HeightLarge)  popup_max_height_in_items = 20;
        ImGui::SetNextWindowSizeConstraints(ImVec2(w, 0.0f), ImVec2(FLT_MAX, CalcMaxPopupHeightFromItemCount(popup_max_height_in_items)));
    }

    char name[16];
    ImFormatString(name, IM_ARRAYSIZE(name), CXOR("##Combo_%02d"), g.BeginPopupStack.Size); // Recycle windows based on depth

    // Position the window given a custom constraint (peak into expected window size so we can position it)
    // This might be easier to express with an hypothetical SetNextWindowPosConstraints() function.
    if (ImGuiWindow* popup_window = ImGui::FindWindowByName(name))
        if (popup_window->WasActive)
        {
            // Always override 'AutoPosLastDirection' to not leave a chance for a past value to affect us.
            ImVec2 size_expected = { 245.f, PopupMaxHeight(items) };
            ImVec2 pos = total_bb.Min + ImVec2{ 0, ImGui::GetFrameHeight() };
            ImGui::SetNextWindowPos(pos);
        }

    // We don't use BeginPopupEx() solely because we have a custom name string, which we could make an argument to BeginPopupEx()
    ImGuiWindowFlags window_flags = ImGuiWindowFlags_AlwaysAutoResize | ImGuiWindowFlags_Popup | ImGuiWindowFlags_NoTitleBar | ImGuiWindowFlags_NoScrollbar | ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_NoMove;

    // Horizontally align ourselves with the framed text
    ImGui::PushStyleVar(ImGuiStyleVar_WindowPadding, ImVec2(6.f, 7.5f));
    ImGui::PushStyleVar(ImGuiStyleVar_FrameRounding, 3.5f);
    bool ret = ImGui::Begin(name, NULL, window_flags);
    window = ImGui::GetCurrentWindow();
    auto sizeOfRet = ImGui::GetWindowSize();
    //ImGui::GetBackgroundDrawList()->AddShadowRect({window->Pos}, {window->Pos + sizeOfRet}, Helpers::calculateColor(config->misc.accentColor), 48.f, {0,0}, NULL);
    if (!ret)
    {
        ImGui::EndPopup();
        IM_ASSERT(0);   // This should never happen as we tested for IsPopupOpen() above
        return false;
    }
    ImGui::PopStyleVar(2);
    return true;
}

// Getter for the old Combo() API: const char*[]
static bool Items_ArrayGetter(void* data, int idx, const char** out_text)
{
    const char* const* items = (const char* const*)data;
    if (out_text)
        *out_text = items[idx];
    return true;
}

// Getter for the old Combo() API: "item1\0item2\0item3\0"
static bool Items_SingleStringGetter(void* data, int idx, const char** out_text)
{
    // FIXME-OPT: we could pre-compute the indices to fasten this. But only 1 active combo means the waste is limited.
    const char* items_separated_by_zeros = (const char*)data;
    int items_count = 0;
    const char* p = items_separated_by_zeros;
    while (*p)
    {
        if (idx == items_count)
            break;
        p += strlen(p) + 1;
        items_count++;
    }
    if (!*p)
        return false;
    if (out_text)
        *out_text = p;
    return true;
}

// Old API, prefer using BeginCombo() nowadays if you can.
bool MenuShit::combo(const char* label, int* current_item, bool (*items_getter)(void*, int, const char**), void* data, int items_count, int popup_max_height_in_items, std::string extra)
{
    ImGuiContext& g = *GImGui;

    // Call the getter to obtain the preview string which is a parameter to BeginCombo()
    const char* preview_value = NULL;
    if (*current_item >= 0 && *current_item < items_count)
        items_getter(data, *current_item, &preview_value);

    // The old Combo() API exposed "popup_max_height_in_items". The new more general BeginCombo() API doesn't have/need it, but we emulate it here.
    if (popup_max_height_in_items != -1 && !(g.NextWindowData.Flags & ImGuiNextWindowDataFlags_HasSizeConstraint))
        ImGui::SetNextWindowSizeConstraints(ImVec2(0, 0), ImVec2(FLT_MAX, CalcMaxPopupHeightFromItemCount(popup_max_height_in_items)));

    if (!MenuShit::beginCombo(label, preview_value, ImGuiComboFlags_None, items_count, extra))
        return false;

    // Display items
    // FIXME-OPT: Use clipper (but we need to disable it on the appearing frame to make sure our call to SetItemDefaultFocus() is processed)
    bool value_changed = false;
    for (int i = 0; i < items_count; i++)
    {
        ImGui::PushID((void*)(intptr_t)i);
        const bool item_selected = (i == *current_item);
        const char* item_text;
        if (!items_getter(data, i, &item_text))
            item_text = CXOR("#UNKNOWN");
        if (ImGui::Selectable(item_text, item_selected))
        {
            value_changed = true;
            *current_item = i;
        }
        if (item_selected)
            ImGui::SetItemDefaultFocus();
        ImGui::PopID();
    }

    ImGui::EndCombo();
    //if (value_changed)
        //mGui::MarkItemEdited(g.CurrentWindow->DC.LastItemId);

    return value_changed;
}

// Combo box helper allowing to pass an array of strings.
bool MenuShit::combo(const char* label, int* current_item, const char* const items[], int items_count, int height_in_items, std::string extra)
{
    const bool value_changed = combo(label, current_item, Items_ArrayGetter, (void*)items, items_count, height_in_items, extra);
    return value_changed;
}

// Combo box helper allowing to pass all items in a single string literal holding multiple zero-terminated items "item1\0item2\0"
bool MenuShit::combo(const char* label, int* current_item, const char* items_separated_by_zeros, int height_in_items, std::string extra)
{
    int items_count = 0;
    const char* p = items_separated_by_zeros;       // FIXME-OPT: Avoid computing this, or at least only when combo is open
    while (*p)
    {
        p += strlen(p) + 1;
        items_count++;
    }
    bool value_changed = combo(label, current_item, Items_SingleStringGetter, (void*)items_separated_by_zeros, items_count, height_in_items, extra);
    return value_changed;
}

void MenuShit::endCombo()
{
    ImGui::EndPopup();
}

std::vector< std::string > passat_config_list{};
std::vector< std::string > passat_empty_list = { XOR("Config folder is empty!") };

bool passat_update_configs = false;

typedef void (*LPSEARCHFUNC)(LPCTSTR lpszFileName);

static BOOL search_files(LPCTSTR lpszFileName, LPSEARCHFUNC lpSearchFunc, BOOL bInnerFolders)
{
    LPTSTR part;
    char tmp[MAX_PATH];
    char name[MAX_PATH];

    HANDLE hSearch = NULL;
    WIN32_FIND_DATA wfd;
    memset(&wfd, 0, sizeof(WIN32_FIND_DATA));

    if (bInnerFolders)
    {
        if (GetFullPathNameA(lpszFileName, MAX_PATH, tmp, &part) == 0)
            return FALSE;
        strcpy(name, part);
        strcpy(part, CXOR("*.*"));
        wfd.dwFileAttributes = FILE_ATTRIBUTE_DIRECTORY;
        if (!((hSearch = FindFirstFileA(tmp, &wfd)) == INVALID_HANDLE_VALUE))
            do
            {
                if (!strncmp(wfd.cFileName, CXOR("."), 1) || !strncmp(wfd.cFileName, CXOR(".."), 2))
                    continue;

                if (wfd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY)
                {
                    char next[MAX_PATH];
                    if (GetFullPathNameA(lpszFileName, MAX_PATH, next, &part) == 0)
                        return FALSE;
                    strcpy(part, wfd.cFileName);
                    strcat(next, CXOR("\\"));
                    strcat(next, name);

                    search_files(next, lpSearchFunc, TRUE);
                }
            } while (FindNextFileA(hSearch, &wfd));
        FindClose(hSearch);
    }

    if ((hSearch = FindFirstFileA(lpszFileName, &wfd)) == INVALID_HANDLE_VALUE)
        return TRUE;
    do
        if (!(wfd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY))
        {
            char file[MAX_PATH];
            if (GetFullPathNameA(lpszFileName, MAX_PATH, file, &part) == 0)
                return FALSE;
            strcpy(part, wfd.cFileName);

            lpSearchFunc(wfd.cFileName);
        }
    while (FindNextFileA(hSearch, &wfd));
    FindClose(hSearch);
    return TRUE;
}

static void read_configs(LPCTSTR lpszFileName)
{
    passat_config_list.push_back(lpszFileName);
}

static void refresh_configs()
{
    std::string folder, file;

    passat_config_list.clear();
    std::string config_dir = CXOR("PassatHook\\*");
    search_files(config_dir.c_str(), read_configs, FALSE);
}

static int activeTab = 0;
std::pair<ImVec2, ImVec2> buttonPos;

static void rage_bot_tab() noexcept
{
    static int weapon_cfg = 0;
    ImGui::BeginChild(CXOR("#MAIN"), { 245, 390 });
    ImGui::PushItemWidth(245.0f);
    ImGui::Text(CXOR("Main"));
    ImGui::Separator();
    MenuShit::toggleWithExtra(CXOR("Enable ragebot"), &g_cfg.rage.enable, CXOR("TIT"));
    if (ImGui::BeginPopup(CXOR("TIT")))
    {
        MenuShit::toggle(CXOR("Auto fire"), &g_cfg.rage.auto_fire);
        MenuShit::toggle(CXOR("Anti aim correction"), &g_cfg.rage.resolver);
        ImGui::EndPopup();
    }
    MenuShit::combo(CXOR("Weapon config"), &weapon_cfg, "Global\0Auto snipers\0R8 Revolver\0Deagle\0Pistols\0SSG-08\0AWP\0");

    ImGui::Separator();

    auto& cfg = g_cfg.rage.weapon[weapon_cfg];

    if (weapon_cfg > 0)
        MenuShit::toggle(CXOR("Override global"), &cfg.enable);

    MenuShit::bind(CXOR("Double tap"), g_cfg.binds[dt_b]);
    MenuShit::bind(CXOR("Hide shots"), g_cfg.binds[hs_b]);
    MenuShit::bind(CXOR("Force body"), g_cfg.binds[force_body_b]);
    MenuShit::bind(CXOR("Force safe point"), g_cfg.binds[force_sp_b]);
    MenuShit::bind(CXOR("Override damage"), g_cfg.binds[override_dmg_b]);
    MenuShit::bind(CXOR("Ping spike"), g_cfg.binds[spike_b]);
    MenuShit::sliderInt(CXOR("Ping spike amount"), &g_cfg.rage.spike_amt, 0, 200, "%d ms");

    ImGui::PopItemWidth();
    ImGui::EndChild(false);

    ImGui::SameLine();

    ImGui::BeginChild(CXOR("#MAIN2"), { 245, 390 });
    ImGui::PushItemWidth(245.f);
    ImGui::Text(CXOR("Accuracy"));
    ImGui::Separator();
    MenuShit::toggle(CXOR("Auto scope"), &cfg.auto_scope);
    MenuShit::sliderInt(CXOR("Hitchance"), &cfg.hitchance, 0, 100, "%d%%");
    auto dmg_str = cfg.mindamage == 100 ? CXOR("HP") : cfg.mindamage > 100 ? CXOR("HP + ") + std::to_string(cfg.mindamage - 100) : CXOR("%dHP");
    auto override_str = cfg.damage_override == 100 ? CXOR("HP") : cfg.damage_override > 100 ? CXOR("HP + ") + std::to_string(cfg.damage_override - 100) : CXOR("%dHP");
    MenuShit::sliderInt(CXOR("Minimum damage"), &cfg.mindamage, 1, 110, dmg_str.c_str());
    MenuShit::sliderInt(CXOR("Damage override"), &cfg.damage_override, 1, 110, override_str.c_str());
    MenuShit::toggleWithExtra(CXOR("Quick stop"), &cfg.quick_stop, CXOR("QS"));
    if (ImGui::BeginPopup(CXOR("QS")))
    {
        ImGui::PushItemWidth(245.f);
        static bool multi[4] = { false, false, false, false };
        const char* multicombo_items[] = { "Early", "Between shots", "Force accuracy", "In air" };
        static std::string previewvalue1 = "";
        bool once1 = false;
        for (size_t i = 0; i < ARRAYSIZE(multi); i++)
        {
            multi[i] = (cfg.quick_stop_options & 1 << i) == 1 << i;
        }
        if (MenuShit::beginCombo(CXOR("Modifiers"), previewvalue1.c_str(), 0, 4))
        {
            previewvalue1 = "";
            for (size_t i = 0; i < ARRAYSIZE(multicombo_items); i++)
            {
                ImGui::Selectable(multicombo_items[i], &multi[i], ImGuiSelectableFlags_::ImGuiSelectableFlags_DontClosePopups);
            }
            MenuShit::endCombo();
        }
        for (size_t i = 0; i < ARRAYSIZE(multicombo_items); i++)
        {
            if (!once1)
            {
                previewvalue1 = "";
                once1 = true;
            }
            if (multi[i])
            {
                previewvalue1 += previewvalue1.size() ? std::string(", ") + multicombo_items[i] : multicombo_items[i];
                cfg.quick_stop_options |= 1 << i;
            }
            else
            {
                cfg.quick_stop_options &= ~(1 << i);
            }
        }
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }
    ImGui::Text("");
    ImGui::Text(CXOR("Hitscan"));
    ImGui::Separator();
    static auto percent_xored = XOR("%d");

    auto scale_head_str = cfg.scale_head == -1 ? CXOR("Auto") : percent_xored;
    auto scale_body_str = cfg.scale_body == -1 ? CXOR("Auto") : percent_xored;

    static bool multi[5] = { false, false, false, false, false };
    const char* multicombo_items[] = { "Head", "Chest", "Stomach", "Pelvis", "Arms", "Legs" };
    static std::string previewvalue1 = "";
    bool once1 = false;
    for (size_t i = 0; i < ARRAYSIZE(multi); i++)
    {
        multi[i] = (cfg.hitboxes & 1 << i) == 1 << i;
    }
    if (MenuShit::beginCombo(CXOR("Hitboxes"), previewvalue1.c_str(), 0, 5, "ses"))
    {
        previewvalue1 = "";
        for (size_t i = 0; i < ARRAYSIZE(multicombo_items); i++)
        {
            ImGui::Selectable(multicombo_items[i], &multi[i], ImGuiSelectableFlags_::ImGuiSelectableFlags_DontClosePopups);
        }
        MenuShit::endCombo();
    }
    for (size_t i = 0; i < ARRAYSIZE(multicombo_items); i++)
    {
        if (!once1)
        {
            previewvalue1 = "";
            once1 = true;
        }
        if (multi[i])
        {
            previewvalue1 += previewvalue1.size() ? std::string(", ") + multicombo_items[i] : multicombo_items[i];
            cfg.hitboxes |= 1 << i;
        }
        else
        {
            cfg.hitboxes &= ~(1 << i);
        }
    }
    if (ImGui::BeginPopup("ses"))
    {
        ImGui::PushItemWidth(245.f);
        MenuShit::sliderInt(CXOR("Head scale"), &cfg.scale_head, -1, 100, scale_head_str.c_str());
        MenuShit::sliderInt(CXOR("Body scale"), &cfg.scale_body, -1, 100, scale_body_str.c_str());
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }
    MenuShit::toggle(CXOR("Prefer body"), &cfg.prefer_body);
    MenuShit::toggle(CXOR("Prefer safe point"), &cfg.prefer_safe);
    ImGui::PopItemWidth();
    ImGui::EndChild(false);
}

static void anti_aim_tab() noexcept
{
    static int condition = 0;
    ImGui::BeginChild(CXOR("#MAIN"), { 245, 470 - (560 - 480) });
    ImGui::PushItemWidth(245.0f);
    ImGui::Text(CXOR("Main"));
    ImGui::Separator();
    ImGui::PushID(2541);
    MenuShit::combo(CXOR("Condition"), &condition, "Global\0Standing\0Moving\0Slow walk\0Duck\0In air\0In air duck\0");
    auto& cfg = g_cfg.antihit[condition];
    MenuShit::toggle(CXOR("Enable"), &cfg.enable);
    ImGui::Separator();
    MenuShit::combo(CXOR("Pitch"), &cfg.pitch, CXOR("Off\0Down\0Up\0Custom\0Switch\0"), -1, cfg.pitch >= 3 ? CXOR("PC") : "");
    if (ImGui::BeginPopup(CXOR("PC")))
    {
        ImGui::PushItemWidth(245.f);
        if (cfg.pitch == 3)
        {
            MenuShit::sliderInt(CXOR("Custom pitch amount"), &cfg.custom_pitch, -89, 89, "%d");
        }
        else if (cfg.pitch == 4)
        {
            MenuShit::sliderInt(CXOR("Range"), &cfg.pitch_switch_range, -89, 89);
            MenuShit::sliderInt(CXOR("Switch ticks"), &cfg.pitch_ticks, 1, 16);
        }
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }
    MenuShit::combo(CXOR("Yaw"), &cfg.yaw, CXOR("Forward\0Backward\0Forward spin\0Backward spin\0"), -1, CXOR("YAW"));
    if (ImGui::BeginPopup(CXOR("YAW")))
    {
        ImGui::PushItemWidth(245.f);

        MenuShit::toggle(CXOR("At targets"), &cfg.at_targets);
        MenuShit::toggle(CXOR("Anti backstab"), &g_cfg.anti_backstab);

        if (cfg.yaw == 2 || cfg.yaw == 3)
        {
            MenuShit::sliderInt(CXOR("Spin range"), &cfg.spin_range, 0, 360);
            MenuShit::sliderInt(CXOR("Spin speed"), &cfg.spin_speed, 1, 100, "%d%%");
        }

        MenuShit::sliderInt(CXOR("Yaw offset"), &cfg.yaw_add, -90, 90);
        MenuShit::bind(CXOR("Freestand"), g_cfg.binds[freestand_b]);
        MenuShit::bind(CXOR("Manual left"), g_cfg.binds[left_b]);
        MenuShit::bind(CXOR("Manual right"), g_cfg.binds[right_b]);
        MenuShit::bind(CXOR("Manual back"), g_cfg.binds[back_b]);
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }
    MenuShit::combo(CXOR("Yaw modifier"), &cfg.jitter_mode, CXOR("None\0Centered\0Offset\0Random\0Three way\0X way\0Sway\0"), -1, CXOR("YMF"));
    if (ImGui::BeginPopup(CXOR("YMF")))
    {
        ImGui::PushItemWidth(245.f);
        if (cfg.jitter_mode == 5)
        {
            MenuShit::sliderInt(CXOR("Ways"), &cfg.x_ways, 3, 25);
            for (int i = 0; i < cfg.x_ways; ++i)
            {
                std::string label = CXOR("Way #") + std::to_string(i + 1);
                MenuShit::sliderInt(label.c_str(), &cfg.x_way_range[i], -180, 180);
            }
        }
        else
            MenuShit::sliderInt(CXOR("Range"), &cfg.jitter_range, -90, 90, "%d");
        if (cfg.jitter_mode == 1)
        {
            MenuShit::sliderInt(CXOR("Delay ticks"), &cfg.jitter_delay_ticks, 1, 16, "%d");
        }
        else if (cfg.jitter_mode == 6)
        {
            MenuShit::sliderInt(CXOR("Sway speed"), &cfg.sway_speed, 1, 100, "%d");
        }
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }
    ImGui::PopID();
    ImGui::Text(CXOR(""));

    ImGui::Text(CXOR("Defensive AA"));
    ImGui::Separator();
    ImGui::PushID(125);
    MenuShit::toggle(CXOR("Enable defensive"), &cfg.defensive);
    MenuShit::sliderInt(CXOR("Pause ticks"), &cfg.pause_ticks, 1, 12, "%d ticks");
    MenuShit::combo(CXOR("Pitch"), &cfg.def_pitch, CXOR("Off\0Down\0Up\0Custom\0Switch\0"), -1, cfg.def_pitch >= 3 ? CXOR("PCD") : "");
    if (ImGui::BeginPopup(CXOR("PCD")))
    {
        ImGui::PushItemWidth(245.f);
        if (cfg.def_pitch == 3)
        {
            MenuShit::sliderInt(CXOR("Custom pitch amount"), &cfg.def_custom_pitch, -89, 89, "%d");
        }
        else if (cfg.def_pitch == 4)
        {
            MenuShit::sliderInt(CXOR("Range"), &cfg.def_pitch_switch_range, -89, 89);
            MenuShit::sliderInt(CXOR("Switch ticks"), &cfg.def_pitch_ticks, 1, 16);
        }
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }
    MenuShit::combo(CXOR("Yaw"), &cfg.def_yaw, CXOR("Forward\0Backward\0Forward spin\0Backward spin\0"), -1, CXOR("YD"));
    if (ImGui::BeginPopup(CXOR("YD")))
    {
        ImGui::PushItemWidth(245.f);
        MenuShit::sliderInt(CXOR("Yaw offset"), &cfg.def_yaw_add, -90, 90);

        if (cfg.def_yaw == 2 || cfg.def_yaw == 3)
        {
            MenuShit::sliderInt(CXOR("Spin range"), &cfg.def_spin_range, 0, 360);
            MenuShit::sliderInt(CXOR("Spin speed"), &cfg.def_spin_speed, 1, 100, "%d%%");
        }

        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }
    MenuShit::combo(CXOR("Yaw modifier"), &cfg.def_jitter_mode, CXOR("None\0Centered\0Offset\0Random\0Three way\0X way\0Sway\0"), -1, CXOR("YMFD"));
    if (ImGui::BeginPopup(CXOR("YMFD")))
    {
        ImGui::PushItemWidth(245.f);
        if (cfg.def_jitter_mode == 5)
        {
            MenuShit::sliderInt(CXOR("Ways"), &cfg.def_x_ways, 3, 25);
            for (int i = 0; i < cfg.def_x_ways; ++i)
            {
                std::string label = CXOR("Way #") + std::to_string(i + 1);
                MenuShit::sliderInt(label.c_str(), &cfg.def_x_way_range[i], -180, 180);
            }
        }
        else
            MenuShit::sliderInt(CXOR("Range"), &cfg.def_jitter_range, -90, 90, "%d");
        if (cfg.def_jitter_mode == 1)
        {
            MenuShit::sliderInt(CXOR("Delay ticks"), &cfg.def_jitter_delay_ticks, 1, 16, "%d");
        }
        else if (cfg.def_jitter_mode == 6)
        {
            MenuShit::sliderInt(CXOR("Sway speed"), &cfg.def_sway_speed, 1, 100, "%d");
        }
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }

    ImGui::PopID();
    ImGui::PopItemWidth();
    ImGui::EndChild(false);

    ImGui::SameLine();

    ImGui::BeginChild(CXOR("#MAIN2"), { 245, 390 });
    ImGui::PushItemWidth(245.f);
    ImGui::Text(CXOR("Fake lag"));
    ImGui::Separator();
    MenuShit::toggle(CXOR("Enable"), &cfg.fakelag);
    MenuShit::sliderInt(CXOR("Limit"), &cfg.fakelag_limit, 1, HACKS->max_choke ? HACKS->max_choke + 1 : 15, CXOR("%d ticks"));

    ImGui::Text(CXOR(""));

    MenuShit::toggleWithExtra(CXOR("Desync"), &cfg.desync, CXOR("DES"));
    if (ImGui::BeginPopup(CXOR("DES")))
    {
        ImGui::PushItemWidth(245.f);
        MenuShit::bind(CXOR("Inverter"), g_cfg.binds[inv_b]);
        MenuShit::toggle(CXOR("Randomize fake amount"), &cfg.random_dsy);
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }
    ImGui::Separator();

    MenuShit::combo(CXOR("Fake type"), &cfg.desync_mode, "Default\0Jitter\0");
    MenuShit::sliderInt(CXOR("Left amount"), &cfg.desync_left, 0, 60, CXOR("%d"));
    MenuShit::sliderInt(CXOR("Right amount"), &cfg.desync_right, 0, 60, CXOR("%d"));
    MenuShit::toggleWithExtra(CXOR("Extend fake"), &cfg.distortion, CXOR("ROLL"));
    if (ImGui::BeginPopup(CXOR("ROLL")))
    {
        ImGui::PushItemWidth(245.f);
        MenuShit::bind(CXOR("Ensure lean"), g_cfg.binds[ens_lean_b]);
        MenuShit::sliderInt(CXOR("Amount"), &cfg.distortion_range, 0, 100, CXOR("%d%%"));
        MenuShit::sliderInt(CXOR("Height"), &cfg.distortion_pitch, 0, 50);
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }

    ImGui::Text(CXOR(""));
    ImGui::Text(CXOR("Other"));
    ImGui::Separator();
    MenuShit::bind(CXOR("Fake duck"), g_cfg.binds[fd_b]);
    MenuShit::bind(CXOR("Slow walk"), g_cfg.binds[sw_b]);
    ImGui::PopItemWidth();
    ImGui::EndChild(false);
}

static int esp_tab = 0;
static void players_tab() noexcept
{
    auto& esp = g_cfg.visuals.player_esp[esp_tab];

    ImGui::BeginChild(CXOR("#MAIN"), { 245, 390 });
    ImGui::PushItemWidth(245.f);
    ImGui::Text(CXOR("ESP Options"));
    ImGui::Separator();

    MenuShit::toggle(CXOR("Enable"), &esp.enable);
    MenuShit::toggleWithColor(CXOR("Glow"), &esp.glow, esp.glow_color, true); 
    if (esp_tab != 1)
        MenuShit::toggleWithExtra(CXOR("Chams"), &esp.chams, CXOR("CHMS"));
    MenuShit::toggleWithExtra(CXOR("Bullet tracers"), &esp.bullet_tracers, CXOR("BT"));
    if (ImGui::BeginPopup(CXOR("BT")))
    {
        ImGui::PushItemWidth(245.f);
        MenuShit::colorPicker(CXOR("Color"), esp.bullet_tracers_col, true);
        MenuShit::combo(CXOR("Type"), &esp.bullet_tracers_type, CXOR("Beam\0Line\0Glow\0"));
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }

    if (esp_tab != 2)
        MenuShit::toggleWithExtra(CXOR("Offscreen arrows"), &esp.offscreen, CXOR("OFF"));
    if (ImGui::BeginPopup(CXOR("OFF")))
    {
        ImGui::PushItemWidth(245.f);
        MenuShit::colorPicker(CXOR("Color"), esp.offscreen_color, true);
        MenuShit::sliderInt(CXOR("Size"), &esp.offscreen_size, 10, 50, "%dpx");
        MenuShit::sliderInt(CXOR("Offset"), &esp.offscreen_offset, 10, RENDER->screen.y / 2 - 90, "%dpx");
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }

    if (ImGui::BeginPopup(CXOR("CHMS")))
    {
        ImGui::PushItemWidth(245.f);
        if (esp_tab == 0)
        {
            static int chams_model = 0;
            const char* enemy_types[] { "Visible", "Invisible", "History", "On-shot", "Ragdolls", };
            const char* vis_chams_type[] = { "Textured", "Metallic", "Flat", "Glass", "Glow", "Bubble", };

            MenuShit::combo(CXOR("Type"), &chams_model, enemy_types, IM_ARRAYSIZE(enemy_types));

            auto& enemy_config = g_cfg.visuals.chams[chams_model];

            MenuShit::toggle(CXOR("Enable"), &enemy_config.enable);
            if (chams_model == 2)
                MenuShit::toggle(CXOR("Full history"), &g_cfg.visuals.show_all_history);

            MenuShit::combo(CXOR("Material"), &enemy_config.material, vis_chams_type, IM_ARRAYSIZE(vis_chams_type));
            MenuShit::colorPicker(CXOR("Material color"), enemy_config.main_color, true);
            if (enemy_config.material == 4)
            {
                MenuShit::sliderInt(CXOR("Glow fill"), &enemy_config.glow_fill, 0, 100, CXOR("%d%%"));
                MenuShit::colorPicker(CXOR("Glow color"), enemy_config.glow_color, true);
            }

            if (chams_model == 3)
                MenuShit::sliderInt(CXOR("Shot duration"), &enemy_config.shot_duration, 1, 10, CXOR("%ds"));
        }
        else if (esp_tab == 2)
        {
            static int chams_local_model = 0;
            const char* local_models[]{ "Visible", "Viewmodel", "Weapon", "Attachments", "Fake", };
            const char* vis_chams_type[] = { "Textured", "Metallic", "Flat", "Glass", "Glow", "Bubble", };

            MenuShit::combo(CXOR("Type"), &chams_local_model, local_models, IM_ARRAYSIZE(local_models));

            auto& local_config = g_cfg.visuals.chams[chams_local_model + 5];

            MenuShit::toggle(CXOR("Enable"), &local_config.enable);

            MenuShit::combo(CXOR("Material"), &local_config.material, vis_chams_type, IM_ARRAYSIZE(vis_chams_type));
            MenuShit::colorPicker(CXOR("Material color"), local_config.main_color, true);
            if (local_config.material == 4)
            {
                MenuShit::sliderInt(CXOR("Fill"), &local_config.glow_fill, 0, 100, CXOR("%d%%"));
                MenuShit::colorPicker(CXOR("Glow color"), local_config.glow_color, true);
            }

            if (chams_local_model == 3)
                MenuShit::sliderInt(CXOR("Attachments blend"), &g_cfg.misc.attachments_amt, 0, 100, CXOR("%d%%"));
        }
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }
    ImGui::PopItemWidth();
    ImGui::EndChild(false);

    ImGui::SameLine();

    ImGui::BeginChild(CXOR("#MAIN2"), { 245, 390 });
    ImGui::PushItemWidth(245.f);
    ImGui::Text(CXOR("ESP Preview"));
    ImGui::Separator();

    auto pos = ImGui::GetWindowPos();
    ImVec4 Rect{ pos.x + 67, pos.y + 92, 106, 176 };
    auto drawList = ImGui::GetWindowDrawList();

    if (esp.box)
    {
        drawList->AddRect(pos + ImVec2{ 65, 90 }, pos + ImVec2{ 175, 270 }, c_color(0, 0, 0, esp.box_color.base().a()).as_imcolor());
        drawList->AddRect(pos + ImVec2{ 67, 92 }, pos + ImVec2{ 173, 268 }, c_color(0,0,0, esp.box_color.base().a()).as_imcolor());
        drawList->AddRect(pos + ImVec2{ 66, 91 }, pos + ImVec2{ 174, 269 }, esp.box_color.base().as_imcolor());
    }

    if (esp.name_esp)
    {
        std::string name = CXOR("Mosqui");

        ImGui::PushFont(RENDER->fonts.esp.get());
        auto name_size = ImGui::CalcTextSize(name.c_str());
        drawList->AddText(pos + ImVec2{ 245 / 2.f - name_size.x / 2 + 1, 90.f - name_size.y - 1 }, c_color(0, 0, 0, esp.name_esp_color.base().a()).as_imcolor(), name.c_str());
        drawList->AddText(pos + ImVec2{ 245 / 2.f - name_size.x / 2, 90.f - name_size.y - 2 }, esp.name_esp_color.base().as_imcolor(), name.c_str());
        ImGui::SetCursorPos(ImVec2{ 245 / 2.f - name_size.x / 2, 90 - name_size.y - 2 });
        ImGui::PopFont();

        ImGui::InvisibleButton(CXOR("NAME EDIT"), { name_size.x,name_size.y });
        if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
            ImGui::OpenPopup(CXOR("name__"));

        if (ImGui::BeginPopup(CXOR("name__")))
        {
            MenuShit::colorPicker(CXOR("Color"), esp.name_esp_color, true);
            ImGui::EndPopup();
        }
    }

    const auto offset_ammo = esp.ammo_bar ? 5.f : 0.f;
    float offste_weapon = esp.weapon_esp ? 8.f : 0.f;

    if (esp.weapon_esp)
    {
        std::string name = CXOR("Knife");

        ImGui::PushFont(RENDER->fonts.pixel.get());
        auto name_size = ImGui::CalcTextSize(name.c_str());
        drawList->AddText(pos + ImVec2{ 245 / 2.f - name_size.x / 2 + 1, 272 + offset_ammo + 1 }, c_color(0, 0, 0, esp.weapon_esp_color.base().a()).as_imcolor(), name.c_str());
        drawList->AddText(pos + ImVec2{ 245 / 2.f - name_size.x / 2 + 1, 272 + offset_ammo - 1 }, c_color(0, 0, 0, esp.weapon_esp_color.base().a()).as_imcolor(), name.c_str());
        drawList->AddText(pos + ImVec2{ 245 / 2.f - name_size.x / 2 - 1, 272 + offset_ammo - 1 }, c_color(0, 0, 0, esp.weapon_esp_color.base().a()).as_imcolor(), name.c_str());
        drawList->AddText(pos + ImVec2{ 245 / 2.f - name_size.x / 2 - 1, 272 + offset_ammo + 1 }, c_color(0, 0, 0, esp.weapon_esp_color.base().a()).as_imcolor(), name.c_str());
        drawList->AddText(pos + ImVec2{ 245 / 2.f - name_size.x / 2 - 1, 272 + offset_ammo }, c_color(0, 0, 0, esp.weapon_esp_color.base().a()).as_imcolor(), name.c_str());
        drawList->AddText(pos + ImVec2{ 245 / 2.f - name_size.x / 2, 272 + offset_ammo - 1 }, c_color(0, 0, 0, esp.weapon_esp_color.base().a()).as_imcolor(), name.c_str());
        drawList->AddText(pos + ImVec2{ 245 / 2.f - name_size.x / 2 + 1, 272 + offset_ammo }, c_color(0, 0, 0, esp.weapon_esp_color.base().a()).as_imcolor(), name.c_str());
        drawList->AddText(pos + ImVec2{ 245 / 2.f - name_size.x / 2, 272 + offset_ammo + 1 }, c_color(0, 0, 0, esp.weapon_esp_color.base().a()).as_imcolor(), name.c_str());
        drawList->AddText(pos + ImVec2{ 245 / 2.f - name_size.x / 2, 272 + offset_ammo }, esp.weapon_esp_color.base().as_imcolor(), name.c_str());
        ImGui::SetCursorPos(ImVec2{ 245 / 2.f - name_size.x / 2, 272 + offset_ammo });
        ImGui::PopFont();

        ImGui::InvisibleButton(CXOR("WEAPON EDIT"), { name_size.x,name_size.y });
        if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
            ImGui::OpenPopup(CXOR("weapon__"));

        if (ImGui::BeginPopup(CXOR("weapon__")))
        {
            MenuShit::colorPicker(CXOR("Color"), esp.weapon_esp_color, true);
            ImGui::EndPopup();
        }
    }

    if (esp.weapon_icon_esp)
    {
        const char8_t* name = u8"\uE03B";

        ImGui::PushFont(RENDER->fonts.weapon_icons.get());
        auto name_size = ImGui::CalcTextSize((const char*)name);
        drawList->AddText(pos + ImVec2{ 245 / 2.f - name_size.x / 2 + 1, 272 + offset_ammo + offste_weapon + 1 }, c_color(0, 0, 0, esp.weapon_icon_esp_color.base().a()).as_imcolor(), (const char*)name);
        drawList->AddText(pos + ImVec2{ 245 / 2.f - name_size.x / 2, 272 + offset_ammo + offste_weapon }, esp.weapon_icon_esp_color.base().as_imcolor(), (const char*)name);
        ImGui::SetCursorPos(ImVec2{ 245 / 2.f - name_size.x / 2, 272 + offset_ammo + offste_weapon });
        ImGui::PopFont();

        ImGui::InvisibleButton(CXOR("ICON EDIT"), { name_size.x,name_size.y });
        if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
            ImGui::OpenPopup(CXOR("weaponi__"));

        if (ImGui::BeginPopup(CXOR("weaponi__")))
        {
            MenuShit::colorPicker(CXOR("Color"), esp.weapon_icon_esp_color, true);
            ImGui::EndPopup();
        }
    }

    float offset = 0.f;
    if (esp.armor_flag)
    {
        std::string text = CXOR("HK");
        ImGui::PushFont(RENDER->fonts.pixel.get());
        auto name_size = ImGui::CalcTextSize(text.c_str());
        ImVec2 poss{ Rect.x + Rect.z + 4, Rect.y - 3 + offset };
        drawList->AddText( ImVec2{ poss.x + 1, poss.y + 1 }, c_color(0, 0, 0, esp.armor_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x + 1, poss.y - 1 }, c_color(0, 0, 0, esp.armor_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x - 1, poss.y - 1 }, c_color(0, 0, 0, esp.armor_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x - 1, poss.y + 1 }, c_color(0, 0, 0, esp.armor_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x - 1, poss.y }, c_color(0, 0, 0, esp.armor_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x, poss.y - 1 }, c_color(0, 0, 0, esp.armor_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x + 1, poss.y }, c_color(0, 0, 0, esp.armor_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x, poss.y + 1 }, c_color(0, 0, 0, esp.armor_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x, poss.y }, esp.armor_flag_color.base().as_imcolor(), text.c_str());
        ImGui::SetCursorPos(ImVec2{ Rect.x + Rect.z + 4, Rect.y - 3 + offset } - pos);
        offset += name_size.y;
        ImGui::PopFont();

        ImGui::InvisibleButton(std::string(text + CXOR(" EDIT")).c_str(), { name_size.x,name_size.y });
        if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
            ImGui::OpenPopup(std::string(text + CXOR("EDIT")).c_str());

        if (ImGui::BeginPopup(std::string(text + CXOR("EDIT")).c_str()))
        {
            MenuShit::colorPicker(CXOR("Color"), esp.armor_flag_color, true);
            ImGui::EndPopup();
        }
    }
    
    if (esp.bomb_flag)
    {
        std::string text = CXOR("C4");
        ImGui::PushFont(RENDER->fonts.pixel.get());
        auto name_size = ImGui::CalcTextSize(text.c_str());
        ImVec2 poss{ Rect.x + Rect.z + 4, Rect.y - 3 + offset };
        drawList->AddText( ImVec2{ poss.x + 1, poss.y + 1 }, c_color(0, 0, 0, esp.bomb_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x + 1, poss.y - 1 }, c_color(0, 0, 0, esp.bomb_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x - 1, poss.y - 1 }, c_color(0, 0, 0, esp.bomb_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x - 1, poss.y + 1 }, c_color(0, 0, 0, esp.bomb_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x - 1, poss.y }, c_color(0, 0, 0, esp.bomb_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x, poss.y - 1 }, c_color(0, 0, 0, esp.bomb_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x + 1, poss.y }, c_color(0, 0, 0, esp.bomb_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x, poss.y + 1 }, c_color(0, 0, 0, esp.bomb_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x, poss.y }, esp.bomb_flag_color.base().as_imcolor(), text.c_str());
        ImGui::SetCursorPos(ImVec2{ Rect.x + Rect.z + 4, Rect.y - 3 + offset } - pos);
        offset += name_size.y;
        ImGui::PopFont();

        ImGui::InvisibleButton(std::string(text + CXOR(" EDIT")).c_str(), { name_size.x,name_size.y });
        if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
            ImGui::OpenPopup(std::string(text + CXOR("EDIT")).c_str());

        if (ImGui::BeginPopup(std::string(text + CXOR("EDIT")).c_str()))
        {
            MenuShit::colorPicker(CXOR("Color"), esp.bomb_flag_color, true);
            ImGui::EndPopup();
        }
    }

    if (esp.zoom_flag)
    {
        std::string text = CXOR("ZOOM");
        ImGui::PushFont(RENDER->fonts.pixel.get());
        auto name_size = ImGui::CalcTextSize(text.c_str());
        ImVec2 poss{ Rect.x + Rect.z + 4, Rect.y - 3 + offset };
        drawList->AddText(ImVec2{ poss.x + 1, poss.y + 1 }, c_color(0, 0, 0, esp.zoom_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText(ImVec2{ poss.x + 1, poss.y - 1 }, c_color(0, 0, 0, esp.zoom_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText(ImVec2{ poss.x - 1, poss.y - 1 }, c_color(0, 0, 0, esp.zoom_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText(ImVec2{ poss.x - 1, poss.y + 1 }, c_color(0, 0, 0, esp.zoom_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText(ImVec2{ poss.x - 1, poss.y }, c_color(0, 0, 0, esp.zoom_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText(ImVec2{ poss.x, poss.y - 1 }, c_color(0, 0, 0, esp.zoom_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText(ImVec2{ poss.x + 1, poss.y }, c_color(0, 0, 0, esp.zoom_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText(ImVec2{ poss.x, poss.y + 1 }, c_color(0, 0, 0, esp.zoom_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText(ImVec2{ poss.x, poss.y }, esp.zoom_flag_color.base().as_imcolor(), text.c_str());
        ImGui::SetCursorPos(ImVec2{ Rect.x + Rect.z + 4, Rect.y - 3 + offset } - pos);
        offset += name_size.y;
        ImGui::PopFont();

        ImGui::InvisibleButton(std::string(text + CXOR(" EDIT")).c_str(), { name_size.x,name_size.y });
        if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
            ImGui::OpenPopup(std::string(text + CXOR("EDIT")).c_str());

        if (ImGui::BeginPopup(std::string(text + CXOR("EDIT")).c_str()))
        {
            MenuShit::colorPicker(CXOR("Color"), esp.zoom_flag_color, true);
            ImGui::EndPopup();
        }
    }

    if (esp.planting_flag)
    {
        std::string text = CXOR("PLANT");
        ImGui::PushFont(RENDER->fonts.pixel.get());
        auto name_size = ImGui::CalcTextSize(text.c_str());
        ImVec2 poss{ Rect.x + Rect.z + 4, Rect.y - 3 + offset };
        drawList->AddText( ImVec2{ poss.x + 1, poss.y + 1 }, c_color(0, 0, 0, esp.planting_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x + 1, poss.y - 1 }, c_color(0, 0, 0, esp.planting_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x - 1, poss.y - 1 }, c_color(0, 0, 0, esp.planting_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x - 1, poss.y + 1 }, c_color(0, 0, 0, esp.planting_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x - 1, poss.y }, c_color(0, 0, 0, esp.planting_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x, poss.y - 1 }, c_color(0, 0, 0, esp.planting_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x + 1, poss.y }, c_color(0, 0, 0, esp.planting_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x, poss.y + 1 }, c_color(0, 0, 0, esp.planting_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x, poss.y }, esp.planting_flag_color.base().as_imcolor(), text.c_str());
        ImGui::SetCursorPos(ImVec2{ Rect.x + Rect.z + 4, Rect.y - 3 + offset } - pos);
        offset += name_size.y;
        ImGui::PopFont();

        ImGui::InvisibleButton(std::string(text + CXOR(" EDIT")).c_str(), { name_size.x,name_size.y });
        if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
            ImGui::OpenPopup(std::string(text + CXOR("EDIT")).c_str());

        if (ImGui::BeginPopup(std::string(text + CXOR("EDIT")).c_str()))
        {
            MenuShit::colorPicker(CXOR("Color"), esp.planting_flag_color, true);
            ImGui::EndPopup();
        }
    }

    if (esp.kit_flag)
    {
        std::string text = CXOR("KIT");
        ImGui::PushFont(RENDER->fonts.pixel.get());
        auto name_size = ImGui::CalcTextSize(text.c_str());
        ImVec2 poss{ Rect.x + Rect.z + 4, Rect.y - 3 + offset };
        drawList->AddText( ImVec2{ poss.x + 1, poss.y + 1 }, c_color(0, 0, 0, esp.kit_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x + 1, poss.y - 1 }, c_color(0, 0, 0, esp.kit_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x - 1, poss.y - 1 }, c_color(0, 0, 0, esp.kit_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x - 1, poss.y + 1 }, c_color(0, 0, 0, esp.kit_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x - 1, poss.y }, c_color(0, 0, 0, esp.kit_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x, poss.y - 1 }, c_color(0, 0, 0, esp.kit_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x + 1, poss.y }, c_color(0, 0, 0, esp.kit_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x, poss.y + 1 }, c_color(0, 0, 0, esp.kit_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText( ImVec2{ poss.x, poss.y }, esp.kit_flag_color.base().as_imcolor(), text.c_str());
        ImGui::SetCursorPos(ImVec2{ Rect.x + Rect.z + 4, Rect.y - 3 + offset } - pos);
        offset += name_size.y;
        ImGui::PopFont();

        ImGui::InvisibleButton(std::string(text + CXOR(" EDIT")).c_str(), { name_size.x,name_size.y });
        if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
            ImGui::OpenPopup(std::string(text + CXOR("EDIT")).c_str());

        if (ImGui::BeginPopup(std::string(text + CXOR("EDIT")).c_str()))
        {
            MenuShit::colorPicker(CXOR("Color"), esp.kit_flag_color, true);
            ImGui::EndPopup();
        }
    }

    const float hp = std::sin(GetTickCount64() / 900.f * 1.5f) * 0.5f + 0.5f;

    if (esp.exploit_flag)
    {
        std::string text = CXOR("X");
        ImGui::PushFont(RENDER->fonts.pixel.get());
        auto name_size = ImGui::CalcTextSize(text.c_str());
        ImVec2 poss{ Rect.x + Rect.z + 4, Rect.y - 3 + offset };
        drawList->AddText(ImVec2{ poss.x + 1, poss.y + 1 }, c_color(0, 0, 0, esp.exploit_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText(ImVec2{ poss.x + 1, poss.y - 1 }, c_color(0, 0, 0, esp.exploit_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText(ImVec2{ poss.x - 1, poss.y - 1 }, c_color(0, 0, 0, esp.exploit_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText(ImVec2{ poss.x - 1, poss.y + 1 }, c_color(0, 0, 0, esp.exploit_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText(ImVec2{ poss.x - 1, poss.y }, c_color(0, 0, 0, esp.exploit_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText(ImVec2{ poss.x, poss.y - 1 }, c_color(0, 0, 0, esp.exploit_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText(ImVec2{ poss.x + 1, poss.y }, c_color(0, 0, 0, esp.exploit_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText(ImVec2{ poss.x, poss.y + 1 }, c_color(0, 0, 0, esp.exploit_flag_color.base().a()).as_imcolor(), text.c_str());
        drawList->AddText(ImVec2{ poss.x, poss.y }, (hp > 0.30f ? esp.exploit_flag_color.base().as_imcolor() : esp.exploit_flag_color_shift.base().as_imcolor()), text.c_str());
        ImGui::SetCursorPos(ImVec2{ Rect.x + Rect.z + 4, Rect.y - 3 + offset } - pos);
        offset += name_size.y;
        ImGui::PopFont();

        ImGui::InvisibleButton(std::string(text + CXOR(" EDIT")).c_str(), { name_size.x,name_size.y });
        if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
            ImGui::OpenPopup(std::string(text + CXOR("EDIT")).c_str());

        if (ImGui::BeginPopup(std::string(text + CXOR("EDIT")).c_str()))
        {
            MenuShit::colorPicker(CXOR("Color"), esp.exploit_flag_color, true);
            MenuShit::colorPicker(CXOR("Shifting color"), esp.exploit_flag_color_shift, true);
            ImGui::EndPopup();
        }
    }

    if (esp.health_bar)
    {
        ImGui::SetCursorPos(ImVec2{ Rect.x - 7, Rect.y - 2 } - pos);
        ImGui::InvisibleButton(CXOR("HB EDIT"), { 4.f, Rect.w + 2 });
        if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
            ImGui::OpenPopup(CXOR("hb_"));

        if (ImGui::BeginPopup(CXOR("hb_")))
        {
            MenuShit::combo(CXOR("Type"), &esp.health_bar_type, CXOR("Solid\0Gradient\0"));
            MenuShit::colorPicker(esp.health_bar_type == 0 ? CXOR("High HP color") : CXOR("Top color"), esp.health_bar_solid_1, true);
            MenuShit::colorPicker(esp.health_bar_type == 0 ? CXOR("Low HP color") : CXOR("Bottom color"), esp.health_bar_solid_2, true);
            ImGui::EndPopup();
        }

        auto height = Rect.w * (1.f - hp);

        auto col = esp.health_bar_solid_1.base().multiply(esp.health_bar_solid_2.base(), hp);
        
        switch (esp.health_bar_type)
        {
        case 0:
            drawList->AddRectFilled({ Rect.x - 7, Rect.y - 2 }, { Rect.x - 3, Rect.y + Rect.w + 2 }, IM_COL32(0,0,0, col.a()));
            drawList->AddRectFilled({ Rect.x - 6, Rect.y + Rect.w - height - 1 }, { Rect.x - 4, Rect.y + Rect.w + 1 }, col.as_imcolor());
            break;
        case 1:
            drawList->AddRectFilled({ Rect.x - 7, Rect.y - 2 }, { Rect.x - 3, Rect.y + Rect.w + 2 }, IM_COL32(0, 0, 0, col.a()));
            drawList->AddRectFilledMultiColor({ Rect.x - 6, Rect.y + Rect.w - height - 1 }, { Rect.x - 4, Rect.y + Rect.w + 1 }, col.as_imcolor(), col.as_imcolor(), esp.health_bar_solid_2.base().as_imcolor(), esp.health_bar_solid_2.base().as_imcolor());
            break;
        }

        //text
        ImGui::PushFont(RENDER->fonts.pixel.get());
        int health = 100 * (1.f - hp);
        auto t_size = ImGui::CalcTextSize(std::to_string(health).c_str());
        ImVec2 poss{ Rect.x - 5 - t_size.x / 2, Rect.y + Rect.w - height - 4 };
        
        drawList->AddText(ImVec2{ poss.x + 1, poss.y + 1 }, c_color(0, 0, 0, 255).as_imcolor(), std::to_string(health).c_str());
        drawList->AddText(ImVec2{ poss.x + 1, poss.y - 1 }, c_color(0, 0, 0, 255).as_imcolor(), std::to_string(health).c_str());
        drawList->AddText(ImVec2{ poss.x - 1, poss.y - 1 }, c_color(0, 0, 0, 255).as_imcolor(), std::to_string(health).c_str());
        drawList->AddText(ImVec2{ poss.x - 1, poss.y + 1 }, c_color(0, 0, 0, 255).as_imcolor(), std::to_string(health).c_str());
        drawList->AddText(ImVec2{ poss.x - 1, poss.y }, c_color(0, 0, 0, 255).as_imcolor(), std::to_string(health).c_str());
        drawList->AddText(ImVec2{ poss.x, poss.y - 1 }, c_color(0, 0, 0, 255).as_imcolor(), std::to_string(health).c_str());
        drawList->AddText(ImVec2{ poss.x + 1, poss.y }, c_color(0, 0, 0, 255).as_imcolor(), std::to_string(health).c_str());
        drawList->AddText(ImVec2{ poss.x, poss.y + 1 }, c_color(0, 0, 0, 255).as_imcolor(), std::to_string(health).c_str());
        drawList->AddText(ImVec2{ poss.x, poss.y }, c_color(255, 255, 255, 255).as_imcolor(), std::to_string(health).c_str());
        ImGui::PopFont();
    }

    if (esp.ammo_bar)
    {
        ImGui::SetCursorPos(ImVec2{ Rect.x - 2, Rect.y + Rect.w + 3 } - pos);
        ImGui::InvisibleButton(CXOR("AB EDIT"), { Rect.z + 2, 4. });
        if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
            ImGui::OpenPopup(CXOR("ab_"));

        if (ImGui::BeginPopup(CXOR("ab_")))
        {
            MenuShit::combo(CXOR("Type"), &esp.ammo_bar_type, CXOR("Solid\0Gradient\0"));
            MenuShit::colorPicker(esp.ammo_bar_type == 0 ? CXOR("High ammo color") : CXOR("Right color"), esp.ammo_bar_solid_full, true);
            MenuShit::colorPicker(esp.ammo_bar_type == 0 ? CXOR("Low ammo color") : CXOR("Left color"), esp.ammo_bar_solid_empty, true);
            ImGui::EndPopup();
        }

        auto width = abs(Rect.z) * (1.f - hp);

        auto col = esp.ammo_bar_solid_full.base().multiply(esp.ammo_bar_solid_empty.base(), hp);

        switch (esp.ammo_bar_type)
        {
        case 0:
            drawList->AddRectFilled({ Rect.x - 2, Rect.y + Rect.w + 3 }, { Rect.x + Rect.z + 2, Rect.y + Rect.w + 7 }, IM_COL32(0, 0, 0, col.a()));
            drawList->AddRectFilled({ Rect.x - 1, Rect.y + Rect.w + 4 }, { Rect.x + width + 1, Rect.y + Rect.w + 6 }, col.as_imcolor());
            break;
        case 1:
            drawList->AddRectFilled({ Rect.x - 2, Rect.y + Rect.w + 3 }, { Rect.x + Rect.z + 2, Rect.y + Rect.w + 7 }, IM_COL32(0, 0, 0, col.a()));
            drawList->AddRectFilledMultiColor({ Rect.x - 1, Rect.y + Rect.w + 4 }, { Rect.x + width + 1, Rect.y + Rect.w + 6 }, esp.ammo_bar_solid_empty.base().as_imcolor(), col.as_imcolor(), col.as_imcolor(), esp.ammo_bar_solid_empty.base().as_imcolor());
            break;
        }

        //text
        ImGui::PushFont(RENDER->fonts.pixel.get());
        int health = 20 * (1.f - hp);
        auto t_size = ImGui::CalcTextSize(std::to_string(health).c_str());
        ImVec2 poss{ Rect.x + width + 1, Rect.y + Rect.w + 4 - t_size.y / 2 };

        drawList->AddText(ImVec2{ poss.x + 1, poss.y + 1 }, c_color(0, 0, 0, 255).as_imcolor(), std::to_string(health).c_str());
        drawList->AddText(ImVec2{ poss.x + 1, poss.y - 1 }, c_color(0, 0, 0, 255).as_imcolor(), std::to_string(health).c_str());
        drawList->AddText(ImVec2{ poss.x - 1, poss.y - 1 }, c_color(0, 0, 0, 255).as_imcolor(), std::to_string(health).c_str());
        drawList->AddText(ImVec2{ poss.x - 1, poss.y + 1 }, c_color(0, 0, 0, 255).as_imcolor(), std::to_string(health).c_str());
        drawList->AddText(ImVec2{ poss.x - 1, poss.y }, c_color(0, 0, 0, 255).as_imcolor(), std::to_string(health).c_str());
        drawList->AddText(ImVec2{ poss.x, poss.y - 1 }, c_color(0, 0, 0, 255).as_imcolor(), std::to_string(health).c_str());
        drawList->AddText(ImVec2{ poss.x + 1, poss.y }, c_color(0, 0, 0, 255).as_imcolor(), std::to_string(health).c_str());
        drawList->AddText(ImVec2{ poss.x, poss.y + 1 }, c_color(0, 0, 0, 255).as_imcolor(), std::to_string(health).c_str());
        drawList->AddText(ImVec2{ poss.x, poss.y }, c_color(255, 255, 255, 255).as_imcolor(), std::to_string(health).c_str());
        ImGui::PopFont();
    }

    ImGui::SetCursorPos({ 0, 358 });
    if (ImGui::InvisibleButton(CXOR("Manage elements"), { 245, 32 }))
        ImGui::OpenPopup(CXOR("ELEMENTS"));

    auto sz_elem = ImGui::CalcTextSize(CXOR("Manage elements"));
    ImGui::PushFont(g_fonts->font_awesome);
    auto sz_cog = ImGui::CalcTextSize(ICON_FA_COG);
    ImGui::GetWindowDrawList()->AddText(pos + ImVec2{ 127.f - sz_elem.x / 2 - sz_cog.x + 1.f, 376.f - sz_elem.y / 2 }, IM_COL32(255, 255, 255, 255), ICON_FA_COG);
    ImGui::PopFont();
    ImGui::GetWindowDrawList()->AddText(pos + ImVec2{ 127.f - sz_elem.x / 2 + sz_cog.x / 2 - 1.f, 374.f - sz_elem.y / 2 }, IM_COL32(255, 255, 255, 255), CXOR("Manage elements"));
    if (ImGui::BeginPopup(CXOR("ELEMENTS")))
    {
        ImGui::PushItemWidth(245.f);
        ImGui::Text(CXOR("Text"));
        ImGui::Separator();

        MenuShit::buttonToggle(CXOR("Player name"), &esp.name_esp);
        if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
            ImGui::OpenPopup(CXOR("name_"));

        if (ImGui::BeginPopup(CXOR("name_")))
        {
            MenuShit::colorPicker(CXOR("Color"), esp.name_esp_color, true);
            ImGui::EndPopup();
        }

        ImGui::SameLine();

        MenuShit::buttonToggle(CXOR("Weapon name"), &esp.weapon_esp);
        if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
            ImGui::OpenPopup(CXOR("weapon_"));

        if (ImGui::BeginPopup(CXOR("weapon_")))
        {
            MenuShit::colorPicker(CXOR("Color"), esp.weapon_esp_color, true);
            ImGui::EndPopup();
        }

        ImGui::SameLine();

        MenuShit::buttonToggle(CXOR("Weapon icon"), &esp.weapon_icon_esp);
        if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
            ImGui::OpenPopup(CXOR("weapon_icon_"));

        if (ImGui::BeginPopup(CXOR("weapon_icon_")))
        {
            MenuShit::colorPicker(CXOR("Color"), esp.weapon_icon_esp_color, true);
            ImGui::EndPopup();
        }

        ImGui::Text(CXOR("Flags"));
        ImGui::Separator();

        MenuShit::buttonToggle(CXOR("Armor"), &esp.armor_flag);
        if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
            ImGui::OpenPopup(CXOR("armor_flag_"));

        if (ImGui::BeginPopup(CXOR("armor_flag_")))
        {
            MenuShit::colorPicker(CXOR("Color"), esp.armor_flag_color, true);
            ImGui::EndPopup();
        }

        ImGui::SameLine();

        MenuShit::buttonToggle(CXOR("Bomb"), &esp.bomb_flag);
        if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
            ImGui::OpenPopup(CXOR("bomb_flag_"));

        if (ImGui::BeginPopup(CXOR("bomb_flag_")))
        {
            MenuShit::colorPicker(CXOR("Color"), esp.bomb_flag_color, true);
            ImGui::EndPopup();
        }

        ImGui::SameLine();

        MenuShit::buttonToggle(CXOR("Scoped"), &esp.zoom_flag);
        if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
            ImGui::OpenPopup(CXOR("zoom_flag_"));

        if (ImGui::BeginPopup(CXOR("zoom_flag_")))
        {
            MenuShit::colorPicker(CXOR("Color"), esp.zoom_flag_color, true);
            ImGui::EndPopup();
        }

        ImGui::SameLine();

        MenuShit::buttonToggle(CXOR("Is planting"), &esp.planting_flag);
        if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
            ImGui::OpenPopup(CXOR("planting_flag_"));

        if (ImGui::BeginPopup(CXOR("planting_flag_")))
        {
            MenuShit::colorPicker(CXOR("Color"), esp.planting_flag_color, true);
            ImGui::EndPopup();
        }

        MenuShit::buttonToggle(CXOR("Has kit"), &esp.kit_flag);
        if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
            ImGui::OpenPopup(CXOR("kit_flag_"));

        if (ImGui::BeginPopup(CXOR("kit_flag_")))
        {
            MenuShit::colorPicker(CXOR("Color"), esp.kit_flag_color, true);
            ImGui::EndPopup();
        }

        ImGui::SameLine();

        MenuShit::buttonToggle(CXOR("Exploit"), &esp.exploit_flag);
        if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
            ImGui::OpenPopup(CXOR("exploit_flag_"));

        if (ImGui::BeginPopup(CXOR("exploit_flag_")))
        {
            MenuShit::colorPicker(CXOR("Color"), esp.exploit_flag_color, true);
            MenuShit::colorPicker(CXOR("Shifting color"), esp.exploit_flag_color_shift, true);
            ImGui::EndPopup();
        }

        ImGui::SameLine();

        MenuShit::buttonToggle(CXOR("Resolver"), &esp.resolver_flag);
        if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
            ImGui::OpenPopup(CXOR("resolver_flag_"));

        if (ImGui::BeginPopup(CXOR("resolver_flag_")))
        {
            MenuShit::colorPicker(CXOR("Color"), esp.resolver_flag_color, true);
            ImGui::EndPopup();
        }

        ImGui::Text(CXOR("Bars"));
        ImGui::Separator();

        MenuShit::buttonToggle(CXOR("Health bar"), &esp.health_bar);
        if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
            ImGui::OpenPopup(CXOR("health_"));

        if (ImGui::BeginPopup(CXOR("health_")))
        {
            MenuShit::combo(CXOR("Type"), &esp.health_bar_type, CXOR("Solid\0Gradient\0"));
            MenuShit::colorPicker(esp.health_bar_type == 0 ? CXOR("High HP color") : CXOR("Top color"), esp.health_bar_solid_1, true);
            MenuShit::colorPicker(esp.health_bar_type == 0 ? CXOR("Low HP color") : CXOR("Bottom color"), esp.health_bar_solid_2, true);
            ImGui::EndPopup();
        }

        ImGui::SameLine();

        MenuShit::buttonToggle(CXOR("Ammo bar"), &esp.ammo_bar);
        if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
            ImGui::OpenPopup(CXOR("ammo_"));

        if (ImGui::BeginPopup(CXOR("ammo_")))
        {
            MenuShit::combo(CXOR("Type"), &esp.ammo_bar_type, CXOR("Solid\0Gradient\0"));
            MenuShit::colorPicker(esp.ammo_bar_type == 0 ? CXOR("High ammo color") : CXOR("Right color"), esp.ammo_bar_solid_full, true);
            MenuShit::colorPicker(esp.ammo_bar_type == 0 ? CXOR("Low ammo color") : CXOR("Left color"), esp.ammo_bar_solid_empty, true);
            ImGui::EndPopup();
        }

        ImGui::Text(CXOR("Other"));
        ImGui::Separator();
        MenuShit::buttonToggle(CXOR("Bounding box"), &esp.box);
        if (ImGui::IsItemHovered() && ImGui::IsMouseClicked(1))
            ImGui::OpenPopup(CXOR("BBOX"));

        if (ImGui::BeginPopup(CXOR("BBOX")))
        {
            MenuShit::colorPicker(CXOR("Color"), esp.box_color, true);
            ImGui::EndPopup();
        }
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }
    ImGui::PopItemWidth();
    ImGui::EndChild(false);
}

static void world_tab() noexcept
{
    ImGui::BeginChild(CXOR("#MAIN"), { 245, 390 });
    ImGui::PushItemWidth(245.f);
    ImGui::Text(CXOR("View modifiers"));
    ImGui::Separator();
    MenuShit::sliderInt(CXOR("FOV"), &g_cfg.misc.fovs[world], -60, 60);
    MenuShit::sliderInt(CXOR("Zoom amount"), &g_cfg.misc.fovs[zoom], 0, 100, CXOR("%d%%"));
    MenuShit::sliderInt(CXOR("Aspect ratio"), &g_cfg.misc.aspect_ratio, 0, 200);
    static bool multi[7] = { false, false, false, false, false, false, false };
    const char* multicombo_items[] = { ("Scope"), ("Visual recoil"), ("Post processing"), ("Smoke"), ("Flash"), ("Fog"), ("Shadows") };
    static std::string previewvalue1 = "";
    bool once1 = false;
    for (size_t i = 0; i < ARRAYSIZE(multi); i++)
    {
        multi[i] = (g_cfg.misc.removals & 1 << i) == 1 << i;
    }
    if (MenuShit::beginCombo(CXOR("Removals"), previewvalue1.c_str(), 0, 7))
    {
        previewvalue1 = "";
        for (size_t i = 0; i < ARRAYSIZE(multicombo_items); i++)
        {
            ImGui::Selectable(multicombo_items[i], &multi[i], ImGuiSelectableFlags_::ImGuiSelectableFlags_DontClosePopups);
        }
        MenuShit::endCombo();
    }
    for (size_t i = 0; i < ARRAYSIZE(multicombo_items); i++)
    {
        if (!once1)
        {
            previewvalue1 = "";
            once1 = true;
        }
        if (multi[i])
        {
            previewvalue1 += previewvalue1.size() ? std::string(", ") + multicombo_items[i] : multicombo_items[i];
            g_cfg.misc.removals |= 1 << i;
        }
        else
        {
            g_cfg.misc.removals &= ~(1 << i);
        }
    }
    MenuShit::toggleWithExtra(CXOR("Viewmodel"), &g_cfg.misc.viewmodel_custom, CXOR("VM"));
    if (ImGui::BeginPopup(CXOR("VM")))
    {
        ImGui::PushItemWidth(245.f);
        MenuShit::toggle(CXOR("Show viewmodel in scope"), &g_cfg.misc.viewmodel_scope);
        MenuShit::sliderInt(CXOR("Viewmodel FOV"), &g_cfg.misc.fovs[arms], -30, 30);
        MenuShit::sliderInt(CXOR("Offset X"), &g_cfg.misc.viewmodel_pos[0], -20, 20);
        MenuShit::sliderInt(CXOR("Offset Y"), &g_cfg.misc.viewmodel_pos[1], -20, 20);
        MenuShit::sliderInt(CXOR("Offset Z"), &g_cfg.misc.viewmodel_pos[2], -20, 20);
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }
    MenuShit::toggleWithExtra(CXOR("Thirdperson"), &g_cfg.misc.thirdperson, CXOR("TP"));
    if (ImGui::BeginPopup(CXOR("TP")))
    {
        ImGui::PushItemWidth(245.f);
        MenuShit::bind(CXOR("Bind"), g_cfg.binds[tp_b]);
        MenuShit::toggle(CXOR("While dead"), &g_cfg.misc.thirdperson_dead);
        MenuShit::sliderInt(CXOR("Distance"), &g_cfg.misc.thirdperson_dist, 10, 300);
        MenuShit::sliderInt(CXOR("Scope/Nade blend"), &g_cfg.misc.scope_amt, 0, 100, CXOR("%d%%"));
        MenuShit::sliderInt(CXOR("Attachments blend"), &g_cfg.misc.attachments_amt, 0, 100, CXOR("%d%%"));
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }
    ImGui::Text(CXOR(""));
    ImGui::Text(CXOR("World modulation"));
    ImGui::Separator();
    MenuShit::colorPicker(CXOR("World color"), g_cfg.misc.world_clr[world], false);
    MenuShit::colorPicker(CXOR("Props color"), g_cfg.misc.world_clr[props], true);
    MenuShit::toggleWithExtra(CXOR("Skybox controller"), &g_cfg.misc.skybox_enable, CXOR("SB"));
    if (ImGui::BeginPopup(CXOR("SB")))
    {
        ImGui::PushItemWidth(245.f);
        const char* skyboxes[]
        {
            xor_strs::aa_default.c_str(),
            xor_strs::sky_tibet.c_str(),
            xor_strs::sky_bagage.c_str(),
            xor_strs::sky_italy.c_str(),
            xor_strs::sky_jungle.c_str(),
            xor_strs::sky_office.c_str(),
            xor_strs::sky_daylight.c_str(),
            xor_strs::sky_daylight2.c_str(),
            xor_strs::sky_vertigo_blue.c_str(),
            xor_strs::sky_vertigo.c_str(),
            xor_strs::sky_day.c_str(),
            xor_strs::sky_nuke_bank.c_str(),
            xor_strs::sky_venice.c_str(),
            xor_strs::sky_daylight3.c_str(),
            xor_strs::sky_daylight4.c_str(),
            xor_strs::sky_cloudy.c_str(),
            xor_strs::sky_night.c_str(),
            xor_strs::sky_nightb.c_str(),
            xor_strs::sky_night_flat.c_str(),
            xor_strs::sky_dust.c_str(),
            xor_strs::sky_vietnam.c_str(),
            xor_strs::sky_lunacy.c_str(),
            xor_strs::sky_embassy.c_str(),
            xor_strs::sky_custom.c_str(),
        };
        MenuShit::combo(CXOR("Skybox"), &g_cfg.misc.skybox, skyboxes, IM_ARRAYSIZE(skyboxes), -1, g_cfg.misc.skybox == 23 ? "" : "");
        if (g_cfg.misc.sound == 23)
            ImGui::InputTextWithHint(CXOR(""), CXOR("Skybox name"), g_cfg.misc.skybox_name, 256);
        MenuShit::colorPicker(CXOR("Color"), g_cfg.misc.world_clr[sky], false);
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }
    MenuShit::toggleWithExtra(CXOR("Fog controller"), &g_cfg.misc.custom_fog, CXOR("FOG"));
    if (ImGui::BeginPopup(CXOR("FOG")))
    {
        ImGui::PushItemWidth(245.f);
        MenuShit::sliderInt(CXOR("Fog start"), &g_cfg.misc.fog_start, 0, 10000);
        MenuShit::sliderInt(CXOR("Fog end"), &g_cfg.misc.fog_end, 0, 10000);
        MenuShit::sliderInt(CXOR("Density"), &g_cfg.misc.fog_density, 0, 100, CXOR("%d%%"));
        MenuShit::colorPicker(CXOR("Fog color"), g_cfg.misc.world_clr[fog], false);
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }
    MenuShit::toggleWithExtra(CXOR("Shadow modulation"), &g_cfg.misc.sunset_mode, CXOR("SS"));
    if (ImGui::BeginPopup(CXOR("SS")))
    {
        ImGui::PushItemWidth(245.f);
        MenuShit::sliderInt(CXOR("Sun pitch"), &g_cfg.misc.sunset_angle.x, -180, 180, CXOR("%d degrees"));
        MenuShit::sliderInt(CXOR("Sun yaw"), &g_cfg.misc.sunset_angle.y, -180, 180, CXOR("%d degrees"));
        MenuShit::sliderInt(CXOR("Sun roll"), &g_cfg.misc.sunset_angle.z, -180, 180, CXOR("%d degrees"));
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }
    ImGui::PopItemWidth();
    ImGui::EndChild(false);

    ImGui::SameLine();

    ImGui::BeginChild(CXOR("#MAIN2"), { 245, 390 });
    ImGui::PushItemWidth(245.f);
    ImGui::Text(CXOR("World entities"));
    ImGui::Separator();
    MenuShit::toggleWithExtra(CXOR("Weapons"), &g_cfg.visuals.world_esp[0].enabled, CXOR("DROP"));
    if (ImGui::BeginPopup(CXOR("DROP")))
    {
        ImGui::PushItemWidth(245.f);
        MenuShit::toggleWithColor(CXOR("Bounding box"), &g_cfg.visuals.world_esp[0].box, g_cfg.visuals.world_esp[0].box_color, true);
        MenuShit::toggleWithExtra(CXOR("Ammo bar"), &g_cfg.visuals.world_esp[0].ammo_bar, CXOR("ammo_"));
        if (ImGui::BeginPopup(CXOR("ammo_")))
        {
            MenuShit::combo(CXOR("Type"), &g_cfg.visuals.world_esp[0].ammo_bar_type, CXOR("Solid\0Gradient\0"));
            MenuShit::colorPicker(g_cfg.visuals.world_esp[0].ammo_bar_type == 0 ? CXOR("High ammo color") : CXOR("Right color"), g_cfg.visuals.world_esp[0].ammo_bar_solid_full, true);
            MenuShit::colorPicker(g_cfg.visuals.world_esp[0].ammo_bar_type == 0 ? CXOR("Low ammo color") : CXOR("Left color"), g_cfg.visuals.world_esp[0].ammo_bar_solid_empty, true);
            ImGui::EndPopup();
        }
        MenuShit::toggleWithColor(CXOR("Name"), &g_cfg.visuals.world_esp[0].name_esp, g_cfg.visuals.world_esp[0].name_esp_color, true);
        MenuShit::toggleWithColor(CXOR("Icon"), &g_cfg.visuals.world_esp[0].icon_esp, g_cfg.visuals.world_esp[0].icon_esp_color, true);
        MenuShit::toggleWithColor(CXOR("Glow"), &g_cfg.visuals.world_esp[0].glow, g_cfg.visuals.world_esp[0].glow_color, true);
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }
    MenuShit::toggleWithExtra(CXOR("Projectiles"), &g_cfg.visuals.world_esp[1].enabled, CXOR("PROJ"));
    if (ImGui::BeginPopup(CXOR("PROJ")))
    {
        ImGui::PushItemWidth(245.f);
        MenuShit::toggleWithColor(CXOR("Bounding box"), &g_cfg.visuals.world_esp[1].box, g_cfg.visuals.world_esp[1].box_color, true);
        MenuShit::toggleWithExtra(CXOR("Timer"), &g_cfg.visuals.world_esp[1].timer_bar, CXOR("TIM"));
        if (ImGui::BeginPopup(CXOR("TIM")))
        {
            ImGui::PushItemWidth(245.f);
            MenuShit::combo(CXOR("Type"), &g_cfg.visuals.world_esp[1].timer_bar_type, CXOR("Solid\0Gradient\0"));
            MenuShit::colorPicker(g_cfg.visuals.world_esp[1].timer_bar_type == 0 ? CXOR("Full color") : CXOR("Right color"), g_cfg.visuals.world_esp[1].timer_bar_solid_full, true);
            MenuShit::colorPicker(g_cfg.visuals.world_esp[1].timer_bar_type == 0 ? CXOR("Empty color") : CXOR("Left color"), g_cfg.visuals.world_esp[1].timer_bar_solid_empty, true);
            ImGui::PopItemWidth();
            ImGui::EndPopup();
        }
        MenuShit::toggleWithColor(CXOR("Name"), &g_cfg.visuals.world_esp[1].name_esp, g_cfg.visuals.world_esp[1].name_esp_color, true);
        MenuShit::toggleWithColor(CXOR("Icon"), &g_cfg.visuals.world_esp[1].icon_esp, g_cfg.visuals.world_esp[1].icon_esp_color, true);
        MenuShit::toggleWithColor(CXOR("Glow"), &g_cfg.visuals.world_esp[1].glow, g_cfg.visuals.world_esp[1].glow_color, true);
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }
    MenuShit::toggleWithExtra(CXOR("Proximity warning"), &g_cfg.visuals.grenade_warning, CXOR("WARNING"));
    if (ImGui::BeginPopup(CXOR("WARNING")))
    {
        ImGui::PushItemWidth(245.f);
        MenuShit::colorPicker(CXOR("Color"), g_cfg.visuals.warning_clr, true);
        MenuShit::toggle(CXOR("Offscreen"), &g_cfg.visuals.grenade_warning_oof);
        MenuShit::sliderInt(CXOR("Offscreen offset"), &g_cfg.visuals.grenade_offset, 20, RENDER->screen.y / 2 - 90);
        MenuShit::toggle(CXOR("Trajectory line"), &g_cfg.visuals.grenade_warning_line);
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }
    ImGui::Text(CXOR(""));
    ImGui::Text(CXOR("Other"));
    ImGui::Separator();
    MenuShit::toggleWithColor(CXOR("Grenade prediction"), &g_cfg.visuals.grenade_predict, g_cfg.visuals.predict_clr, true);
    MenuShit::toggleWithExtra(CXOR("Animation breakers"), &g_cfg.misc.animation_breakers, CXOR("ANIM"));
    if (ImGui::BeginPopup(CXOR("ANIM")))
    {
        ImGui::PushItemWidth(245.f);
        static bool multi[1] = { false };
        const char* multicombo_items[] = { "Zero pitch on land" };
        static std::string previewvalue1 = "";
        bool once1 = false;
        for (size_t i = 0; i < ARRAYSIZE(multi); i++)
        {
            multi[i] = (g_cfg.misc.other_animation_changes & 1 << i) == 1 << i;
        }
        if (MenuShit::beginCombo(CXOR("Main breakers"), previewvalue1.c_str(), 0, 1))
        {
            previewvalue1 = "";
            for (size_t i = 0; i < ARRAYSIZE(multicombo_items); i++)
            {
                ImGui::Selectable(multicombo_items[i], &multi[i], ImGuiSelectableFlags_::ImGuiSelectableFlags_DontClosePopups);
            }
            MenuShit::endCombo();
        }
        for (size_t i = 0; i < ARRAYSIZE(multicombo_items); i++)
        {
            if (!once1)
            {
                previewvalue1 = "";
                once1 = true;
            }
            if (multi[i])
            {
                previewvalue1 += previewvalue1.size() ? std::string(", ") + multicombo_items[i] : multicombo_items[i];
                g_cfg.misc.other_animation_changes |= 1 << i;
            }
            else
            {
                g_cfg.misc.other_animation_changes &= ~(1 << i);
            }
        }
        //if (g_cfg.misc.other_animation_changes & 2)
            //MenuShit::sliderInt(CXOR("Lean amount"), &g_cfg.misc.lean_amount, 0, 100, "%d%%");
        MenuShit::combo(CXOR("Leg anim breaker"), &g_cfg.misc.leg_animation_changes, "None\0Backward slide\0Break\0Moonwalk\0");
        MenuShit::combo(CXOR("Air anim breaker"), &g_cfg.misc.air_animation_changes, "None\0Static legs\0Moonwalk\0");
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }
    MenuShit::toggleWithExtra(CXOR("Hit indicators"), &g_cfg.misc.hit_indicators, CXOR("HIT"));
    if (ImGui::BeginPopup(CXOR("HIT")))
    {
        ImGui::PushItemWidth(245.f);
        MenuShit::toggle(CXOR("3D hitmarker"), &g_cfg.misc.hitmarker_world);
        MenuShit::toggle(CXOR("2D hitmarker"), &g_cfg.misc.hitmarker_screen);
        MenuShit::colorPicker(CXOR("Hitmarker color"), g_cfg.misc.hitmarker_clr, true);

        const char* hitsound[]
        {
            xor_strs::aa_disabled.c_str(),
            xor_strs::sound_metallic.c_str(),
            xor_strs::sound_wood_plank.c_str(),
            "Osu!",
            xor_strs::sound_tap.c_str(),
            xor_strs::sound_custom.c_str(),
        };

        MenuShit::combo(CXOR("Hitsound"), &g_cfg.misc.sound, hitsound, IM_ARRAYSIZE(hitsound));
        MenuShit::sliderInt(CXOR("Volume"), &g_cfg.misc.sound_volume, 0, 100, CXOR("%d%%"));

        if (g_cfg.misc.sound == 5)
            ImGui::InputTextWithHint(CXOR(""), CXOR("Sound name"), g_cfg.misc.sound_name, 128);

        MenuShit::toggle(CXOR("Damage indicator"), &g_cfg.misc.damage);
        MenuShit::colorPicker(CXOR("Indicator color"), g_cfg.misc.damage_clr, true);
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }
    MenuShit::toggleWithExtra(CXOR("Custom scope"), &g_cfg.misc.scope.enabled, CXOR("SCOPE"));
    if (ImGui::BeginPopup(CXOR("SCOPE")))
    {
        ImGui::PushItemWidth(245.f);
        MenuShit::sliderInt(CXOR("Length"), &g_cfg.misc.scope.length, 0, 500);
        MenuShit::sliderInt(CXOR("Offset"), &g_cfg.misc.scope.offset, 0, 100);
        MenuShit::colorPicker(CXOR("Color in"), g_cfg.misc.scope.in, true);
        MenuShit::colorPicker(CXOR("Color out"), g_cfg.misc.scope.out, true);
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }
    MenuShit::toggle(CXOR("Force crosshair"), &g_cfg.misc.snip_crosshair);
    ImGui::PopItemWidth();
    ImGui::EndChild(false);
}

static void misc_tab() noexcept
{
    ImGui::BeginChild(CXOR("#MAIN"), { 245, 390 });
    ImGui::PushItemWidth(245.f);
    ImGui::Text(CXOR("Movement"));
    ImGui::Separator();
    MenuShit::toggle(CXOR("Bunny hop"), &g_cfg.misc.auto_jump);
    MenuShit::toggleWithExtra(CXOR("Air strafe"), &g_cfg.misc.auto_strafe, CXOR("AS"));
    if (ImGui::BeginPopup(CXOR("AS")))
    {
        ImGui::PushItemWidth(245.f);
        MenuShit::sliderInt(CXOR("Smoothness"), &g_cfg.misc.strafe_smooth, 0, 100, CXOR("%d%%"));
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }
    MenuShit::toggleWithExtra(CXOR("Auto peek"), &g_cfg.misc.auto_peek, CXOR("PEEK"));
    if (ImGui::BeginPopup(CXOR("PEEK")))
    {
        ImGui::PushItemWidth(245.f);
        MenuShit::bind(CXOR("Bind"), g_cfg.binds[ap_b]);
        MenuShit::colorPicker(CXOR("Standby color"), g_cfg.misc.autopeek_clr, true);
        MenuShit::colorPicker(CXOR("Retreat color"), g_cfg.misc.autopeek_clr_back, true);
        MenuShit::toggle(CXOR("Retreat on key release"), &g_cfg.misc.retrack_peek);
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }
    MenuShit::toggle(CXOR("Fast stop"), &g_cfg.misc.fast_stop);
    MenuShit::toggle(CXOR("Fast ladder"), &g_cfg.misc.fast_ladder);
    //MenuShit::toggle(CXOR("Fast duck"), &g_cfg.misc.fast_duck); // why add when its already done in createmove???
    MenuShit::toggle(CXOR("Slide walk"), &g_cfg.misc.slide_walk);
    MenuShit::bind(CXOR("Edge jump"), g_cfg.binds[ej_b]);
    ImGui::Text(CXOR(""));
    ImGui::Text(CXOR("Interface"));
    ImGui::Separator();
    {
        static bool multi[5] = { false, false, false, false, false };
        const char* multicombo_items[] = { ("Keybinds list"), ("Bomb window"), ("Watermark"), ("Spectators list"), ("Skeet indicators") };
        static std::string previewvalue1 = "";
        bool once1 = false;
        for (size_t i = 0; i < ARRAYSIZE(multi); i++)
        {
            multi[i] = (g_cfg.misc.menu_indicators & 1 << i) == 1 << i;
        }
        if (MenuShit::beginCombo(CXOR("UI elements"), previewvalue1.c_str(), 0, 5))
        {
            previewvalue1 = "";
            for (size_t i = 0; i < ARRAYSIZE(multicombo_items); i++)
            {
                ImGui::Selectable(multicombo_items[i], &multi[i], ImGuiSelectableFlags_::ImGuiSelectableFlags_DontClosePopups);
            }
            MenuShit::endCombo();
        }
        for (size_t i = 0; i < ARRAYSIZE(multicombo_items); i++)
        {
            if (!once1)
            {
                previewvalue1 = "";
                once1 = true;
            }
            if (multi[i])
            {
                previewvalue1 += previewvalue1.size() ? std::string(", ") + multicombo_items[i] : multicombo_items[i];
                g_cfg.misc.menu_indicators |= 1 << i;
            }
            else
            {
                g_cfg.misc.menu_indicators &= ~(1 << i);
            }
        }
    }
    MenuShit::toggleWithExtra(CXOR("Event logger"), &g_cfg.visuals.eventlog.enable, CXOR("EL"));
    if (ImGui::BeginPopup(CXOR("EL")))
    {
        ImGui::PushItemWidth(245.f);
        static bool multi[5] = { false, false, false, false, false };
        const char* multicombo_items[] = { ("Hits"), ("Misses"), ("Debug"), ("Purchases"), ("Bomb plant") };
        static std::string previewvalue1 = "";
        bool once1 = false;
        for (size_t i = 0; i < ARRAYSIZE(multi); i++)
        {
            multi[i] = (g_cfg.visuals.eventlog.logs & 1 << i) == 1 << i;
        }
        if (MenuShit::beginCombo(CXOR("Events"), previewvalue1.c_str(), 0, 5))
        {
            previewvalue1 = "";
            for (size_t i = 0; i < ARRAYSIZE(multicombo_items); i++)
            {
                ImGui::Selectable(multicombo_items[i], &multi[i], ImGuiSelectableFlags_::ImGuiSelectableFlags_DontClosePopups);
            }
            MenuShit::endCombo();
        }
        for (size_t i = 0; i < ARRAYSIZE(multicombo_items); i++)
        {
            if (!once1)
            {
                previewvalue1 = "";
                once1 = true;
            }
            if (multi[i])
            {
                previewvalue1 += previewvalue1.size() ? std::string(", ") + multicombo_items[i] : multicombo_items[i];
                g_cfg.visuals.eventlog.logs |= 1 << i;
            }
            else
            {
                g_cfg.visuals.eventlog.logs &= ~(1 << i);
            }
        }
        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }
    MenuShit::toggle(CXOR("Gamesense mode"), &g_cfg.misc.skeet_mode);

    ImGui::PopItemWidth();
    ImGui::EndChild(false);

    ImGui::SameLine();

    ImGui::BeginChild(CXOR("#MAIN2"), { 245, 390 });
    ImGui::PushItemWidth(245.f);
    ImGui::Text(CXOR("Main"));
    ImGui::Separator();
    MenuShit::colorPicker(CXOR("Accent color"), g_cfg.misc.ui_color);
    MenuShit::toggle(CXOR("Preserve killfeed"), &g_cfg.misc.preverse_killfeed);
    MenuShit::toggle(CXOR("Unlock inventory"), &g_cfg.misc.unlock_inventory);
    MenuShit::toggle(CXOR("Bypass sv_pure"), &g_cfg.misc.bypass_sv_pure);
    MenuShit::toggle(CXOR("Filter console"), &g_cfg.visuals.eventlog.filter_console);
    MenuShit::toggleWithExtra(CXOR("Buy bot"), &g_cfg.misc.buybot.enable, CXOR("BB"));
    if (ImGui::BeginPopup(CXOR("BB")))
    {
        ImGui::PushItemWidth(245.f);
        const char* primary_items[] = {("None"), ("SCAR-20/G3SG1"), ("SSG-08"), ("AWP"), ("Negev"), ("M249"), ("AK47/M4A1"), ("AUG/SG 553")};
        MenuShit::combo(CXOR("Primary"), &g_cfg.misc.buybot.main_weapon, primary_items, IM_ARRAYSIZE(primary_items));
        const char* secondary_items[] = { ("None"), ("Dual berettas"), ("P250"), ("Tec-9/Five-SeveN"), ("R8/Deagle") };
        MenuShit::combo(CXOR("Secondary"), &g_cfg.misc.buybot.second_weapon, secondary_items, IM_ARRAYSIZE(secondary_items));
        
        static bool multi[7] = { false, false, false, false, false, false, false };
        const char* multicombo_items[] = { ("Helmet"), ("Armor"), ("HE grenade"), ("Molotov"), ("Smoke"), ("Taser"), ("Defuse kit") };
        static std::string previewvalue1 = "";
        bool once1 = false;
        for (size_t i = 0; i < ARRAYSIZE(multi); i++)
        {
            multi[i] = (g_cfg.misc.buybot.other_items & 1 << i) == 1 << i;
        }
        if (MenuShit::beginCombo(CXOR("Equipment"), previewvalue1.c_str(), 0, 7))
        {
            previewvalue1 = "";
            for (size_t i = 0; i < ARRAYSIZE(multicombo_items); i++)
            {
                ImGui::Selectable(multicombo_items[i], &multi[i], ImGuiSelectableFlags_::ImGuiSelectableFlags_DontClosePopups);
            }
            MenuShit::endCombo();
        }
        for (size_t i = 0; i < ARRAYSIZE(multicombo_items); i++)
        {
            if (!once1)
            {
                previewvalue1 = "";
                once1 = true;
            }
            if (multi[i])
            {
                previewvalue1 += previewvalue1.size() ? std::string(", ") + multicombo_items[i] : multicombo_items[i];
                g_cfg.misc.buybot.other_items |= 1 << i;
            }
            else
            {
                g_cfg.misc.buybot.other_items &= ~(1 << i);
            }
        }

        ImGui::PopItemWidth();
        ImGui::EndPopup();
    }
    ImGui::Text(CXOR(""));
    ImGui::Text(CXOR("Other"));
    ImGui::Separator();
    MenuShit::toggle(CXOR("Clan tag"), &g_cfg.misc.clantag);
    MenuShit::combo(CXOR("Cheat spoofer"), &g_cfg.misc.cheat_spoofer, "None\0Fatality\0Plaguecheat\0Pandora\0Rifk7\0Arctic.tech\0Airflow\0");
    ImGui::PopItemWidth();
    ImGui::EndChild(false);
}

static void skins_tab()
{
    ImGui::BeginChild(CXOR("#MAIN"), { 245, 390 });
    ImGui::PushItemWidth(245.f);
    ImGui::Text(CXOR("Main"));
    ImGui::Separator();
    const char* glove_models[]
    {
        ("Default"),
        ("Bloodhound"),
        ("Broken Fang"),
        ("Driver"),
        ("Hand Wraps"),
        ("Hydra"),
        ("Moto"),
        ("Specialist"),
        ("Sport"),
    };

    MenuShit::combo(CXOR("Glove model"), &g_cfg.skins.model_glove, glove_models, IM_ARRAYSIZE(glove_models));

    auto& glove_skin = g_cfg.skins.glove_skin[g_cfg.skins.model_glove];

    switch (g_cfg.skins.model_glove)
    {
    case 0:
        MenuShit::combo(CXOR("Glove skin"), &glove_skin, CXOR("None\0"));
        break;
    case 1: // Bloodhound
        MenuShit::combo(CXOR("Glove skin"), &glove_skin, CXOR("Snakebite\0Guerrilla\0Charred\0Bronzed\0"));
        break;
    case 2: // Broken fang
        MenuShit::combo(CXOR("Glove skin"), &glove_skin, CXOR("Jade\0Needle point\0Unhinged\0Yellow-banded\0"));
        break;
    case 3: // Driver
        MenuShit::combo(CXOR("Glove skin"), &glove_skin, CXOR("Black Tie\0Convoy\0Crimson Weave\0Diamondback\0Imperial Plaid\0King snake\0Lunar weave\0Overtake\0Queen jaguar\0Racing green\0Rezan the red\0Snow leopard\0"));
        break;
    case 4: // Hand wraps
        MenuShit::combo(CXOR("Glove skin"), &glove_skin, CXOR("Arboreal\0Badlands\0CAUTION!\0Cobalt Skulls\0Constrictor\0Desert Shamagh\0Duct tape\0Giraffe\0Leather\0Overprint\0Slaughter\0Spruce DDPAT\0"));
        break;
    case 5: // Hydra
        MenuShit::combo(CXOR("Glove skin"), &glove_skin, CXOR("Case Hardened\0Emerald\0Mangrove\0Rattler\0"));
        break;
    case 6: // Moto
        MenuShit::combo(CXOR("Glove skin"), &glove_skin, CXOR("3rd Commando Company\0Blood Pressure\0Boom!\0Cool Mint\0Eclipse\0Finish Line\0Polygon\0POW!\0Smoke Out!\0Spearmint\0Transport\0Turtle\0"));
        break;
    case 7: // Specialist
        MenuShit::combo(CXOR("Glove skin"), &glove_skin, CXOR("Buckshot\0Crimson Kimono\0Crimson Web\0Emerald Web\0Fade\0Field Agent\0Forest DDPAT\0Foundation\0Lt. Commander\0Marble Fade\0Mogul\0Tiger Strike\0"));
        break;
    case 8: // Sport
        MenuShit::combo(CXOR("Glove skin"), &glove_skin, CXOR("Amphibious\0Arid\0Big Game\0Bronze Morph\0Hedge Maze\0Nocts\0Omega\0Pandora's box\0Scarlet Shamagh\0Slingshot\0Superconductor\0Vice\0"));
        break;
    }

    ImGui::Text(CXOR(""));
    ImGui::Text(CXOR("Player model"));
    ImGui::Separator();
    const char* agents[]
    {
        xor_strs::agent_default.c_str(),
        xor_strs::agent_danger_a.c_str(),
        xor_strs::agent_danger_b.c_str(),
        xor_strs::agent_danger_c.c_str(),
        xor_strs::agent_cmdr_davida.c_str(),
        xor_strs::agent_cmdr_frank.c_str(),
        xor_strs::agent_cmdr_lieutenant.c_str(),
        xor_strs::agent_cmdr_michael.c_str(),
        xor_strs::agent_cmdr_operator.c_str(),
        xor_strs::agent_cmdr_spec_agent_ava.c_str(),
        xor_strs::agent_cmdr_markus.c_str(),
        xor_strs::agent_cmdr_sous.c_str(),
        xor_strs::agent_cmdr_chem_haz.c_str(),
        xor_strs::agent_cmdr_chef_d.c_str(),
        xor_strs::agent_cmdr_aspirant.c_str(),
        xor_strs::agent_cmdr_officer.c_str(),
        xor_strs::agent_cmdr_d_sq.c_str(),
        xor_strs::agent_cmdr_b_sq.c_str(),
        xor_strs::agent_cmdr_seal_team6.c_str(),
        xor_strs::agent_cmdr_bunkshot.c_str(),
        xor_strs::agent_cmdr_lt_commander.c_str(),
        xor_strs::agent_cmdr_bunkshot2.c_str(),
        xor_strs::agent_cmdr_3rd_commando.c_str(),
        xor_strs::agent_cmdr_two_times_.c_str(),
        xor_strs::agent_cmdr_two_times_2.c_str(),
        xor_strs::agent_cmdr_premeiro.c_str(),
        xor_strs::agent_cmdr_cmdr.c_str(),
        xor_strs::agent_cmdr_1st_le.c_str(),
        xor_strs::agent_cmdr_john_van.c_str(),
        xor_strs::agent_cmdr_bio_haz.c_str(),
        xor_strs::agent_cmdr_sergeant.c_str(),
        xor_strs::agent_cmdr_chem_haz__.c_str(),
        xor_strs::agent_cmdr_farwlo.c_str(),
        xor_strs::agent_cmdr_getaway_sally.c_str(),
        xor_strs::agent_cmdr_getaway_number_k.c_str(),
        xor_strs::agent_cmdr_getaway_little_kev.c_str(),
        xor_strs::agent_cmdr_safecracker.c_str(),
        xor_strs::agent_cmdr_bloody_darryl.c_str(),
        xor_strs::agent_cmdr_bloody_loud.c_str(),
        xor_strs::agent_cmdr_bloody_royale.c_str(),
        xor_strs::agent_cmdr_bloody_skullhead.c_str(),
        xor_strs::agent_cmdr_bloody_silent.c_str(),
        xor_strs::agent_cmdr_bloody_miami.c_str(),
        xor_strs::agent_street_solider.c_str(),
        xor_strs::agent_solider.c_str(),
        xor_strs::agent_slingshot.c_str(),
        xor_strs::agent_enforcer.c_str(),
        xor_strs::agent_mr_muhlik.c_str(),
        xor_strs::agent_prof_shahmat.c_str(),
        xor_strs::agent_prof_osiris.c_str(),
        xor_strs::agent_prof_ground_rebek.c_str(),
        xor_strs::agent_prof_elite_muhlik.c_str(),
        xor_strs::agent_prof_trapper.c_str(),
        xor_strs::agent_prof_trapper_aggressor.c_str(),
        xor_strs::agent_prof_vypa_sista.c_str(),
        xor_strs::agent_prof_col_magnos.c_str(),
        xor_strs::agent_prof_crasswater.c_str(),
        xor_strs::agent_prof_crasswater_forgotten.c_str(),
        xor_strs::agent_prof_solman.c_str(),
        xor_strs::agent_prof_romanov.c_str(),
        xor_strs::agent_prof_blackwolf.c_str(),
        xor_strs::agent_prof_maximus.c_str(),
        xor_strs::agent_prof_dragomir.c_str(),
        xor_strs::agent_prof_rezan.c_str(),
        xor_strs::agent_prof_rezan_red.c_str(),
        xor_strs::agent_prof_dragomir2.c_str(),
        xor_strs::agent_gign.c_str(),
        xor_strs::agent_gign_a.c_str(),
        xor_strs::agent_gign_b.c_str(),
        xor_strs::agent_gign_c.c_str(),
        xor_strs::agent_gign_d.c_str(),
        xor_strs::agent_pirate.c_str(),
        xor_strs::agent_pirate_a.c_str(),
        xor_strs::agent_pirate_b.c_str(),
        xor_strs::agent_pirate_c.c_str(),
        xor_strs::agent_pirate_d.c_str(),
        xor_strs::sky_custom.c_str(),
    };
    const char* masks[]
    {
        xor_strs::mask_none.c_str(),
        xor_strs::mask_battle.c_str(),
        xor_strs::mask_hoxton.c_str(),
        xor_strs::mask_doll.c_str(),
        xor_strs::mask_skull.c_str(),
        xor_strs::mask_samurai.c_str(),
        xor_strs::mask_evil_clown.c_str(),
        xor_strs::mask_wolf.c_str(),
        xor_strs::mask_sheep.c_str(),
        xor_strs::mask_bunny_gold.c_str(),
        xor_strs::mask_anaglyph.c_str(),
        xor_strs::mask_kabuki_doll.c_str(),
        xor_strs::mask_dallas.c_str(),
        xor_strs::mask_pumpkin.c_str(),
        xor_strs::mask_sheep_bloody.c_str(),
        xor_strs::mask_devil_plastic.c_str(),
        xor_strs::mask_boar.c_str(),
        xor_strs::mask_chains.c_str(),
        xor_strs::mask_tiki.c_str(),
        xor_strs::mask_bunny.c_str(),
        xor_strs::mask_sheep_gold.c_str(),
        xor_strs::mask_zombie_plastic.c_str(),
        xor_strs::mask_chicken.c_str(),
        xor_strs::mask_skull_gold.c_str(),
        xor_strs::mask_demon_man.c_str(),
        xor_strs::mask_engineer.c_str(),
        xor_strs::mask_heavy.c_str(),
        xor_strs::mask_medic.c_str(),
        xor_strs::mask_pyro.c_str(),
        xor_strs::mask_scout.c_str(),
        xor_strs::mask_sniper.c_str(),
        xor_strs::mask_solider.c_str(),
        xor_strs::mask_spy.c_str(),
        xor_strs::mask_holiday_light.c_str(),
    };
    MenuShit::combo(CXOR("Mask"), &g_cfg.skins.masks, masks, IM_ARRAYSIZE(masks));

    MenuShit::combo(CXOR("Agent CT"), &g_cfg.skins.model_ct, agents, IM_ARRAYSIZE(agents));

    if (g_cfg.skins.model_ct == 76)
        ImGui::InputTextWithHint(CXOR(""), CXOR("CT Model path"), g_cfg.skins.custom_model_ct, 128);

    MenuShit::combo(CXOR("Agent T"), &g_cfg.skins.model_t, agents, IM_ARRAYSIZE(agents));

    if (g_cfg.skins.model_t == 76)
        ImGui::InputTextWithHint(CXOR(""), CXOR("T Model path"), g_cfg.skins.custom_model_ct, 128);

    ImGui::PopItemWidth();
    ImGui::EndChild(false);

    ImGui::SameLine();

    ImGui::BeginChild(CXOR("#SKINS"), { 245, 390 });
    ImGui::PushItemWidth(245.f);
    ImGui::Text(CXOR("Skins"));
    ImGui::Separator();

    const char* skin_weapon_configs[] = {
    xor_strs::weapon_cfg_deagle.c_str(),
    xor_strs::weapon_cfg_duals.c_str(),
    xor_strs::weapon_cfg_fiveseven.c_str(),
    xor_strs::weapon_cfg_glock.c_str(),
    xor_strs::weapon_cfg_ak.c_str(),
    xor_strs::weapon_cfg_aug.c_str(),
    xor_strs::weapon_cfg_awp.c_str(),
    xor_strs::weapon_cfg_famas.c_str(),
    xor_strs::weapon_cfg_g3sg1.c_str(),
    xor_strs::weapon_cfg_galil.c_str(),
    xor_strs::weapon_cfg_m249.c_str(),
    xor_strs::weapon_cfg_m4a1.c_str(),
    xor_strs::weapon_cfg_m4a1s.c_str(),
    xor_strs::weapon_cfg_mac10.c_str(),
    xor_strs::weapon_cfg_p90.c_str(),
    xor_strs::weapon_cfg_mp5.c_str(),
    xor_strs::weapon_cfg_ump45.c_str(),
    xor_strs::weapon_cfg_xm1014.c_str(),
    xor_strs::weapon_cfg_bizon.c_str(),
    xor_strs::weapon_cfg_mag7.c_str(),
    xor_strs::weapon_cfg_negev.c_str(),
    xor_strs::weapon_cfg_sawed_off.c_str(),
    xor_strs::weapon_cfg_tec9.c_str(),
    xor_strs::weapon_cfg_p2000.c_str(),
    xor_strs::weapon_cfg_mp7.c_str(),
    xor_strs::weapon_cfg_mp9.c_str(),
    xor_strs::weapon_cfg_nova.c_str(),
    xor_strs::weapon_cfg_p250.c_str(),
    xor_strs::weapon_cfg_scar20.c_str(),
    xor_strs::weapon_cfg_sg553.c_str(),
    xor_strs::weapon_cfg_scout.c_str(),
    xor_strs::weapon_cfg_usps.c_str(),
    xor_strs::weapon_cfg_cz75.c_str(),
    xor_strs::weapon_cfg_revolver.c_str(),
    xor_strs::weapon_cfg_knife.c_str(),
    };
    const char* knife_models[]{
    xor_strs::knife_default.c_str(),
    xor_strs::knife_bayonet.c_str(),
    xor_strs::knife_css.c_str(),
    xor_strs::knife_skeleton.c_str(),
    xor_strs::knife_nomad.c_str(),
    xor_strs::knife_paracord.c_str(),
    xor_strs::knife_survival.c_str(),
    xor_strs::knife_flip.c_str(),
    xor_strs::knife_gut.c_str(),
    xor_strs::knife_karambit.c_str(),
    xor_strs::knife_m9.c_str(),
    xor_strs::knife_huntsman.c_str(),
    xor_strs::knife_falchion.c_str(),
    xor_strs::knife_bowie.c_str(),
    xor_strs::knife_butterfly.c_str(),
    xor_strs::knife_shadow.c_str(),
    xor_strs::knife_ursus.c_str(),
    xor_strs::knife_navaga.c_str(),
    xor_strs::knife_stiletto.c_str(),
    xor_strs::knife_talon.c_str(),
    };

    MenuShit::combo(CXOR("Weapon"), &g_cfg.skins.group_type, skin_weapon_configs, IM_ARRAYSIZE(skin_weapon_configs));
    if (g_cfg.skins.group_type == weapon_cfg_knife)
        MenuShit::combo(CXOR("Knife model"), &g_cfg.skins.skin_weapon[g_cfg.skins.group_type].knife_model, knife_models, IM_ARRAYSIZE(knife_models));

    MenuShit::toggle(CXOR("Enable"), &g_cfg.skins.skin_weapon[g_cfg.skins.group_type].enable);

    if (PassatHookMenu::skin_names.size() < skin_changer::paint_kits.size())
    {
        for (auto& s : skin_changer::paint_kits)
            PassatHookMenu::skin_names.emplace_back(s.first.c_str());
    }

    auto& skin_list = skin_changer::paint_kits.size() <= 0 ? std::vector<const char* >{ "Empty" } : PassatHookMenu::skin_names;
    MenuShit::combo(CXOR("Paint kit"), &g_cfg.skins.skin_weapon[g_cfg.skins.group_type].skin, skin_list.data(), skin_list.size());

    int wear = g_cfg.skins.skin_weapon[g_cfg.skins.group_type].wear * 100.f;
    MenuShit::sliderInt(CXOR("Wear"), &wear, 0, 100, "%d%%", ImGuiSliderFlags_IgnoreGrabCalc);
    //MenuShit::sliderInt(CXOR("Seed"), &g_cfg.skins.skin_weapon[g_cfg.skins.group_type].seed, 0, 999, "%d%", ImGuiSliderFlags_IgnoreGrabCalc);
    MenuShit::inputScalar(CXOR("Seed"), ImGuiDataType_S32, (void*)&g_cfg.skins.skin_weapon[g_cfg.skins.group_type].seed, 0, 0, "%d", 0);
    g_cfg.skins.skin_weapon[g_cfg.skins.group_type].wear = wear / 100.f;
    ImGui::PopItemWidth();
    ImGui::EndChild(false);
}

static bool update_config = false;

static void config_tab() noexcept
{
    static int config_select = -1;
    static char config_name[256]{};
    bool wrong_config = config_select == -1 || passat_config_list.size() <= 0 || config_select > passat_config_list.size() - 1;

    if (!update_config)
    {
        refresh_configs();
        update_config = true;
    }

    ImGui::BeginChild(CXOR("#MAIN"), { 245, 390 });
    ImGui::PushItemWidth(245.f);
    ImGui::PushItemWidth(185.f);
    ImGui::InputTextWithHint(CXOR(""), CXOR("Config name"), config_name, 256);
    ImGui::PopItemWidth();
    int len = std::strlen(config_name);
    ImGui::SameLine(0.f, 2.f);
    if (ImGui::Button(CXOR("Create"), ImVec2{58.f, 0.f}))
    {
        if (len > 0)
        {
            config::create(config_name);
        }
        refresh_configs();
    }
    if (ImGui::Button(CXOR("Open configs folder")))
    {
        std::string folder = CXOR("PassatHook\\");
        ShellExecuteA(NULL, NULL, folder.c_str(), NULL, NULL, SW_SHOWNORMAL);
    }
    if (ImGui::Button(CXOR("Refresh list")))
    {
        refresh_configs();
    }
    if (config_select != -1)
    {
        if (ImGui::Button(CXOR("Load")))
        {
            config::load(passat_config_list[config_select]);
            HACKS->loading_config = true;
        }
        if (ImGui::Button(CXOR("Save")))
        {
            config::save(passat_config_list[config_select]);
        }
        if (ImGui::Button(CXOR("Delete")))
        {
            config::erase(passat_config_list[config_select]);
            refresh_configs();
        }
    }
    ImGui::PopItemWidth();
    ImGui::EndChild(false);

    ImGui::SameLine();
    ImGui::BeginChild(CXOR("#MAIN2"), { 245, 390 });
    ImGui::PushItemWidth(245.f);
    static char config_search[256];

    auto& list = passat_config_list.size() <= 0 ? passat_empty_list : passat_config_list;
    ImGui::InputTextWithHint(CXOR(""), CXOR("Config search"), config_search, 256);
    ImGui::ListBox(CXOR("Configs"), &config_select, [](void* data, int idx, const char** out_text) {
        auto& vector = *static_cast<std::vector<std::string>*>(data);
        *out_text = vector[idx].c_str();
        return true;
        }, &list, list.size(), 19);
    ImGui::PopItemWidth();
    ImGui::EndChild(false);
}

void PassatHookMenu::draw(bool setWindowFocus) {
    auto& io = ImGui::GetIO();
    ImGuiStyle* style = &ImGui::GetStyle();
    style->Colors[ImGuiCol_Header] = ImColor(g_cfg.misc.ui_color.base().new_alpha(0.35f * 255).as_imcolor());
    style->Colors[ImGuiCol_HeaderHovered] = ImColor(g_cfg.misc.ui_color.base().new_alpha(0.4f * 255).as_imcolor());
    style->Colors[ImGuiCol_PopupBg] = ImColor(0, 0, 0, 250);
    style->Colors[ImGuiCol_WindowBg] = ImColor(0, 0, 0, 0);
    style->Colors[ImGuiCol_ChildBg] = ImColor(31, 31, 31);
    style->Colors[ImGuiCol_CheckMark] = ImColor(255, 255, 255, 255);
    style->Colors[ImGuiCol_TextSelectedBg] = ImColor(g_cfg.misc.ui_color.base().new_alpha(0.45f * 255).as_imcolor());
    style->Colors[ImGuiCol_FrameBg] = ImColor(g_cfg.misc.ui_color.base().new_alpha(0.0643137f * 255).as_imcolor());
    style->Colors[ImGuiCol_Button] = ImColor(g_cfg.misc.ui_color.base().new_alpha(0.0643137f * 255).as_imcolor());
    style->Colors[ImGuiCol_ButtonActive] = ImColor(g_cfg.misc.ui_color.base().new_alpha(0.3f * 255).as_imcolor());
    style->Colors[ImGuiCol_ButtonHovered] = ImColor(g_cfg.misc.ui_color.base().new_alpha(0.5f * 255).as_imcolor());
    style->Colors[ImGuiCol_ScrollbarBg] = ImColor(14, 14, 14);
    style->Colors[ImGuiCol_SliderGrabActive] = ImColor(g_cfg.misc.ui_color.base().new_alpha(0.4f * 255).as_imcolor());
    style->Colors[ImGuiCol_FrameBgHovered] = ImColor(g_cfg.misc.ui_color.base().new_alpha(0.4f * 255).as_imcolor());
    style->Colors[ImGuiCol_FrameBgActive] = ImColor(g_cfg.misc.ui_color.base().new_alpha(0.2f * 255).as_imcolor());
    style->Colors[ImGuiCol_HeaderActive] = ImColor(g_cfg.misc.ui_color.base().new_alpha(0.2f * 255).as_imcolor());
    style->Colors[ImGuiCol_TitleBg] = ImColor(0, 0, 0);
    style->Colors[ImGuiCol_TitleBgActive] = ImColor(0, 0, 0);
    style->Colors[ImGuiCol_Separator] = ImColor(g_cfg.misc.ui_color.base().new_alpha(0.6f * 255).as_imcolor());
    style->Colors[ImGuiCol_Border] = ImColor(255, 255, 255, 0);
    static auto backupWindowPadd = style->WindowPadding;
    style->WindowPadding = { 0.f, 0.f };
    style->WindowRounding = 16.f;
    style->PopupRounding = 6.f;
    //style->WindowRounding = 5.f;
    style->Colors[ImGuiCol_ScrollbarGrab] = ImColor(g_cfg.misc.ui_color.base().new_alpha(0.6f * 255).as_imcolor());
    style->Colors[ImGuiCol_ScrollbarGrabHovered] = ImColor(g_cfg.misc.ui_color.base().new_alpha(0.7f * 255).as_imcolor());
    style->Colors[ImGuiCol_Text] = ImColor(1.f, 1.f, 1.f);
    style->Colors[ImGuiCol_TextDisabled] = ImColor(g_cfg.misc.ui_color.base().new_alpha(0.6f * 255).as_imcolor());
    ImGui::Begin(CXOR("g32t8h8o9t23h92gt4fhiog24rhio24ghiog24h82g4h9g412h81g4h9831"), NULL, ImGuiWindowFlags_NoNavInputs | ImGuiWindowFlags_NoNav | ImGuiWindowFlags_NoCollapse | ImGuiWindowFlags_NoTitleBar | ImGuiWindowFlags_NoBackground | ImGuiWindowFlags_NoResize | ImGuiWindowFlags_NoScrollbar | ImGuiWindowFlags_NoScrollWithMouse);
    ImGui::SetWindowSize({ 720, 480 });
    style->WindowPadding = backupWindowPadd;
    auto pos = ImGui::GetWindowPos();

    //background
    ImGui::GetWindowDrawList()->AddRectFilled({ pos.x + 0, pos.y + 0 }, { pos.x + 720, pos.y + 480 }, IM_COL32(15, 15, 15, 255 * MenuShit::alpha), 16.5f);
    ImGui::GetWindowDrawList()->AddRectFilled({ pos.x + 0, pos.y + 0 }, { pos.x + 720, pos.y + 480 }, g_cfg.misc.ui_color.base().new_alpha(0.01f * 255 * MenuShit::alpha).as_imcolor(), 16.5f);
    ImGui::GetWindowDrawList()->AddRect({ pos.x + 1, pos.y + 1 }, { pos.x + 719, pos.y + 479 }, g_cfg.misc.ui_color.base().new_alpha(255 * MenuShit::alpha).as_imcolor(), 14.f, 15, 2.f);

    //title
    ImGui::PushFont(g_fonts->title);
    ImGui::GetWindowDrawList()->AddText({ pos.x + 27.f, pos.y + 27 }, IM_COL32(255, 255, 255, 255 * MenuShit::alpha), CXOR("Passat"));
    ImGui::GetWindowDrawList()->AddText({ pos.x + 27.f + ImGui::CalcTextSize(CXOR("Passat")).x, pos.y + 27 }, g_cfg.misc.ui_color.base().new_alpha(255 * MenuShit::alpha).as_imcolor(), CXOR("Hook"));
    ImGui::PopFont();

    ImGui::SetCursorPos({ 27, 70 });
    ImGui::TextDisabled(CXOR("Aimbot"));

    //sidebar buttons
    ImGui::PushFont(g_fonts->icomenus1);
    ImGui::SetCursorPos({ 27, 92 });
    switch (activeTab)
    {
    case 0: //ragebot
        buttonPos.first = ImVec2{ 27, 91 };
        buttonPos.second = ImVec2{ 208 - (27 * 2), buttonPos.first.y + 26 };
        break;
    case 1: //anti-aim
        buttonPos.first = ImVec2{ 27, 96 + ImGui::CalcTextSize(CXOR("R")).y * 1.5f };
        buttonPos.second = ImVec2{ 208 - (27 * 2), buttonPos.first.y + 26 };
        break;
    case 2: //esp
        buttonPos.first = ImVec2{ 27, 122 + ImGui::CalcTextSize(CXOR("R")).y * 3.f };
        buttonPos.second = ImVec2{ 208 - (27 * 2), buttonPos.first.y + 26 };
        break;
    case 3: //world
        buttonPos.first = ImVec2{ 27, 127 + ImGui::CalcTextSize(CXOR("R")).y * 4.5f };
        buttonPos.second = ImVec2{ 208 - (27 * 2), buttonPos.first.y + 26 };
        break;
    case 4: //skin changer
        buttonPos.first = ImVec2{ 27, 132 + ImGui::CalcTextSize(CXOR("R")).y * 6.f };
        buttonPos.second = ImVec2{ 208 - (27 * 2), buttonPos.first.y + 26 };
        break;
    case 5: //misc
        buttonPos.first = ImVec2{ 27, 159 + ImGui::CalcTextSize(CXOR("R")).y * 7.5f };
        buttonPos.second = ImVec2{ 208 - (27 * 2), buttonPos.first.y + 26 };
        break;
    case 6: //configs
        buttonPos.first = ImVec2{ 27, 164 + ImGui::CalcTextSize(CXOR("R")).y * 9.f };
        buttonPos.second = ImVec2{ 208 - (27 * 2), buttonPos.first.y + 26 };
        break;
    }

    //aimbot section
    ImGui::GetWindowDrawList()->AddRectFilled({ pos.x + buttonPos.first.x - 1, pos.y + buttonPos.first.y - 1 }, { pos.x + 208 - 29, pos.y + buttonPos.second.y + 1 }, g_cfg.misc.ui_color.base().new_alpha(0.075f * 255 * MenuShit::alpha).as_imcolor(), 4.f);
    //ImGui::GetWindowDrawList()->AddShadowRect({ pos.x + buttonPos.first.x, pos.y + buttonPos.first.y }, { pos.x + 208 - 27, pos.y + buttonPos.second.y }, g_cfg.misc.ui_color.base().new_alpha(255 * this->get_alpha()).as_imcolor(), 32.f, { 0.f,0.f }, NULL, 4.f);
    if (ImGui::InvisibleButton(CXOR("LGB"), { 208 - (27 * 2), 24 }))
        activeTab = 0;
    ImGui::SetCursorPos({ 27, 96 + ImGui::CalcTextSize(CXOR("R")).y * 1.5f });
    if (ImGui::InvisibleButton(CXOR("GHP"), { 208 - (27 * 2), 24 }))
        activeTab = 1;

    ImGui::GetWindowDrawList()->AddText({ pos.x + 29, pos.y + 94 }, g_cfg.misc.ui_color.base().as_imcolor(), CXOR("R"));
    ImGui::GetWindowDrawList()->AddText({ pos.x + 29, pos.y + 99 + ImGui::CalcTextSize(CXOR("R")).y * 1.5f }, g_cfg.misc.ui_color.base().as_imcolor(), CXOR("A"));

    ImGui::PushFont(g_fonts->sidebar);
    ImGui::GetWindowDrawList()->AddText({ pos.x + 54, pos.y + 96 }, IM_COL32(255, 255, 255, 255), CXOR("Ragebot"));
    ImGui::GetWindowDrawList()->AddText({ pos.x + 54, pos.y + 109 + ImGui::CalcTextSize(CXOR("R")).y * 1.5f }, IM_COL32(255, 255, 255, 255), CXOR("Anti-Aim"));

    ImGui::SetCursorPos({ 27, 117 + ImGui::CalcTextSize(CXOR("R")).y * 3.f });
    ImGui::PopFont(); //pop 107
    ImGui::PopFont(); //pop 68
    ImGui::TextDisabled(CXOR("Visuals"));
    ImGui::PushFont(g_fonts->icomenus1);
    ImGui::SetCursorPos({ 27, 122 + ImGui::CalcTextSize(CXOR("R")).y * 3.f });
    if (ImGui::InvisibleButton(CXOR("PL"), { 208 - (27 * 2), 24 }))
        activeTab = 2;
    ImGui::GetWindowDrawList()->AddText({ pos.x + 29, pos.y + 125 + ImGui::CalcTextSize(CXOR("R")).y * 3.f }, g_cfg.misc.ui_color.base().as_imcolor(), CXOR("P"));
    ImGui::GetWindowDrawList()->AddText({ pos.x + 29, pos.y + 130 + ImGui::CalcTextSize(CXOR("R")).y * 4.5f }, g_cfg.misc.ui_color.base().as_imcolor(), CXOR("W"));
    ImGui::GetWindowDrawList()->AddText({ pos.x + 29, pos.y + 135 + ImGui::CalcTextSize(CXOR("R")).y * 6.f }, g_cfg.misc.ui_color.base().as_imcolor(), CXOR("W"));
    ImGui::SetCursorPos({ 27, 127 + ImGui::CalcTextSize(CXOR("R")).y * 4.5f });
    if (ImGui::InvisibleButton(CXOR("WRL"), { 208 - (27 * 2), 24 }))
        activeTab = 3;
    ImGui::SetCursorPos({ 27, 132 + ImGui::CalcTextSize(CXOR("R")).y * 6.f });
    if (ImGui::InvisibleButton(CXOR("SKC"), { 208 - (27 * 2), 24 }))
        activeTab = 4;
    ImGui::PopFont(); //pop 113

    ImGui::PushFont(g_fonts->sidebar);
    ImGui::GetWindowDrawList()->AddText({ pos.x + 54, pos.y + 142 + ImGui::CalcTextSize(CXOR("R")).y * 3.f }, IM_COL32(255, 255, 255, 255), CXOR("Players"));
    ImGui::GetWindowDrawList()->AddText({ pos.x + 54, pos.y + 155 + ImGui::CalcTextSize(CXOR("R")).y * 4.5f }, IM_COL32(255, 255, 255, 255), CXOR("World"));
    ImGui::GetWindowDrawList()->AddText({ pos.x + 54, pos.y + 167 + ImGui::CalcTextSize(CXOR("R")).y * 6.f }, IM_COL32(255, 255, 255, 255), CXOR("Skin changer"));
    ImGui::PopFont(); //pop 124

    ImGui::SetCursorPos({ 27, 191 + ImGui::CalcTextSize(CXOR("R")).y * 7.5f });
    ImGui::TextDisabled(CXOR("Miscellaneous"));
    ImGui::PushFont(g_fonts->tab_ico);
    ImGui::SetCursorPos({ 27, 159 + ImGui::CalcTextSize(CXOR("R")).y * 7.5f });
    if (ImGui::InvisibleButton(CXOR("MSC"), { 208 - (27 * 2), 24 }))
        activeTab = 5;
    ImGui::GetWindowDrawList()->AddText({ pos.x + 29, pos.y + 162 + ImGui::CalcTextSize("E").y * 7.5f }, g_cfg.misc.ui_color.base().as_imcolor(), CXOR("E"));
    ImGui::SetCursorPos({ 27, 164 + ImGui::CalcTextSize(CXOR("R")).y * 9.f });
    if (ImGui::InvisibleButton(CXOR("CFG"), { 208 - (27 * 2), 24 }))
        activeTab = 6;
    ImGui::GetWindowDrawList()->AddText({ pos.x + 29, pos.y + 167 + ImGui::CalcTextSize("D").y * 9.f }, g_cfg.misc.ui_color.base().as_imcolor(), CXOR("D"));
    ImGui::PopFont(); //pop 131
    ImGui::PushFont(g_fonts->sidebar);
    ImGui::GetWindowDrawList()->AddText({ pos.x + 54, pos.y + 201 + ImGui::CalcTextSize("E").y * 7.5f }, IM_COL32(255, 255, 255, 255), CXOR("Main"));
    ImGui::GetWindowDrawList()->AddText({ pos.x + 54, pos.y + 214 + ImGui::CalcTextSize("E").y * 9.f }, IM_COL32(255, 255, 255, 255), CXOR("Configs"));
    ImGui::PopFont();
    style->Colors[ImGuiCol_ChildBg] = ImColor(0.1f, 0.1f, 0.1f, 0.f);
    ImGui::SetNextWindowPos({ pos.x + 200, pos.y + 32 });
    ImGui::BeginChild(CXOR("##Up"), { 501, 26 }, false, ImGuiWindowFlags_NoNavInputs | ImGuiWindowFlags_NoNav | ImGuiWindowFlags_NoCollapse | ImGuiWindowFlags_NoTitleBar | ImGuiWindowFlags_NoBackground | ImGuiWindowFlags_NoResize);
    ImGui::PushFont(g_fonts->main_font);
    ImGui::AlignTextToFramePadding();

    if (activeTab == 2)
    {
        ImGui::SetCursorPos({ 130, 0 });
        if (ImGui::InvisibleButton("why u tryna debug me | ida64", { 80, 26 }))
            esp_tab = 0;

        bool hoverede = ImGui::IsItemHovered();

        ImGui::GetWindowDrawList()->AddRectFilled({ ImGui::GetWindowPos().x + 130, ImGui::GetWindowPos().y }, { ImGui::GetWindowPos().x + 210, ImGui::GetWindowPos().y + 22 }, IM_COL32(20, 20, 20, 255), 5.f, ImDrawCornerFlags_BotLeft | ImDrawCornerFlags_TopLeft);
        ImGui::GetWindowDrawList()->AddRectFilled({ ImGui::GetWindowPos().x + 130, ImGui::GetWindowPos().y }, { ImGui::GetWindowPos().x + 210, ImGui::GetWindowPos().y + 22 }, g_cfg.misc.ui_color.base().new_alpha(esp_tab == 0 ? 0.1643137f * 255 : 0.05f * 255).as_imcolor(), 5.f, ImDrawCornerFlags_BotLeft | ImDrawCornerFlags_TopLeft);
        ImGui::GetWindowDrawList()->AddText({ ImGui::GetWindowPos().x + 170 - ImGui::CalcTextSize(CXOR("Enemies")).x / 2, ImGui::GetWindowPos().y + 11 - ImGui::CalcTextSize(CXOR("Enemies")).y / 2 }, IM_COL32(255, 255, 255, 255), CXOR("Enemies"));
        ImGui::GetWindowDrawList()->AddRect({ ImGui::GetWindowPos().x + 130, ImGui::GetWindowPos().y }, { ImGui::GetWindowPos().x + 210, ImGui::GetWindowPos().y + 22 }, g_cfg.misc.ui_color.base().new_alpha(esp_tab == 0 ? 0.25f * 255 : 0.15f * 255).as_imcolor(), 5.f, ImDrawCornerFlags_BotLeft | ImDrawCornerFlags_TopLeft);
        /*    window->DrawList->AddRectFilled(ImVec2(bb.Min.x, bb.Min.y + 3 .f), ImVec2(bb.Max.x, bb.Max.y - 1.f), Helpers::calculateColor(config->misc.accentColor, 0.1643137f), 3.f);
    window->DrawList->AddRect(ImVec2(bb.Min.x, bb.Min.y + 3.f), ImVec2(bb.Max.x, bb.Max.y - 1.f), Helpers::calculateColor(config->misc.accentColor, 0.25f), 3.f);*/
        ImGui::SetCursorPos({ 210, 0 });
        if (ImGui::InvisibleButton("fuck off | x64dbg", { 80, 26 }))
            esp_tab = 1;

        bool hoveredt = ImGui::IsItemHovered();

        ImGui::GetWindowDrawList()->AddRectFilled({ ImGui::GetWindowPos().x + 210, ImGui::GetWindowPos().y }, { ImGui::GetWindowPos().x + 290, ImGui::GetWindowPos().y + 22 }, IM_COL32(20, 20, 20, 255), 0.f);
        ImGui::GetWindowDrawList()->AddRectFilled({ ImGui::GetWindowPos().x + 210, ImGui::GetWindowPos().y }, { ImGui::GetWindowPos().x + 290, ImGui::GetWindowPos().y + 22 }, g_cfg.misc.ui_color.base().new_alpha(esp_tab == 1 ? 0.1643137f * 255 : 0.05f * 255).as_imcolor(), 0.f);
        ImGui::GetWindowDrawList()->AddText({ ImGui::GetWindowPos().x + 250 - ImGui::CalcTextSize(CXOR("Allies")).x / 2, ImGui::GetWindowPos().y + 11 - ImGui::CalcTextSize(CXOR("Allies")).y / 2 }, IM_COL32(255, 255, 255, 255), CXOR("Allies"));
        ImGui::GetWindowDrawList()->AddRect({ ImGui::GetWindowPos().x + 210, ImGui::GetWindowPos().y }, { ImGui::GetWindowPos().x + 290, ImGui::GetWindowPos().y + 22 }, g_cfg.misc.ui_color.base().new_alpha(esp_tab == 1 ? 0.25f * 255 : 0.15f * 255).as_imcolor(), 0.f);

        ImGui::SetCursorPos({ 290, 0 });
        if (ImGui::InvisibleButton("stop debugging me!!! | mosqui", { 80, 26 }))
            esp_tab = 2;

        bool hoveredte = ImGui::IsItemHovered();

        ImGui::GetWindowDrawList()->AddRectFilled({ ImGui::GetWindowPos().x + 290, ImGui::GetWindowPos().y }, { ImGui::GetWindowPos().x + 370, ImGui::GetWindowPos().y + 22 }, IM_COL32(20, 20, 20, 255), 5.f, ImDrawCornerFlags_BotRight | ImDrawCornerFlags_TopRight);
        ImGui::GetWindowDrawList()->AddRectFilled({ ImGui::GetWindowPos().x + 290, ImGui::GetWindowPos().y }, { ImGui::GetWindowPos().x + 370, ImGui::GetWindowPos().y + 22 }, g_cfg.misc.ui_color.base().new_alpha(esp_tab == 2 ? 0.1643137f * 255 : 0.05f * 255).as_imcolor(), 5.f, ImDrawCornerFlags_BotRight | ImDrawCornerFlags_TopRight);
        ImGui::GetWindowDrawList()->AddText({ ImGui::GetWindowPos().x + 330 - ImGui::CalcTextSize(CXOR("Local")).x / 2, ImGui::GetWindowPos().y + 11 - ImGui::CalcTextSize(CXOR("Local")).y / 2 }, IM_COL32(255, 255, 255, 255), CXOR("Local"));
        ImGui::GetWindowDrawList()->AddRect({ ImGui::GetWindowPos().x + 290, ImGui::GetWindowPos().y }, { ImGui::GetWindowPos().x + 370, ImGui::GetWindowPos().y + 22 }, g_cfg.misc.ui_color.base().new_alpha(esp_tab == 2 ? 0.25f * 255 : 0.15f * 255).as_imcolor(), 5.f, ImDrawCornerFlags_BotRight | ImDrawCornerFlags_TopRight);
    }

    ImGui::SetCursorPos({ 477, 0 });
    ImGui::PushFont(g_fonts->font_awesome);
    //ImGui::GetWindowDrawList()->AddText(fonts.font_awesome, 11, { ImGui::GetWindowPos().x + 477 + 9, ImGui::GetWindowPos().y + ImGui::CalcTextSize(ICON_FA_QUESTION).y / 2 }, IM_COL32(255, 255, 255, 255), ICON_FA_QUESTION);
    if (ImGui::InvisibleButton(CXOR("INFO"), { 25, 26 }))
        ImGui::OpenPopup(CXOR("ABT"));

    ImGui::PopFont();
    if (ImGui::BeginPopup(CXOR("ABT")))
    {
        auto winSize = ImGui::GetWindowSize();
        auto winPos = ImGui::GetWindowPos();

        ImGui::PushFont(g_fonts->title);
        ImGui::TextColored(ImVec4{ 0.f, 0.f, 0.f, 0.f }, CXOR("Mosqui"));
        ImGui::Separator();
        ImGui::PopFont();

        ImGui::PushFont(g_fonts->ui);
        ImGui::Text(CXOR("Mosqui CSGO HvH Cheat.\nHello!"));
        ImGui::Separator();
        ImGui::AlignTextToFramePadding();
        ImGui::Text(CXOR("made by ribi"));
        ImGui::SameLine();
        if (ImGui::Button(CXOR("YouTube")))
            ShellExecuteA(NULL, CXOR("open"), CXOR(""), NULL, NULL, SW_SHOWNORMAL);
        ImGui::SameLine();
        if (ImGui::Button(CXOR("Github")))
            ShellExecuteA(NULL, CXOR("open"), CXOR(""), NULL, NULL, SW_SHOWNORMAL);
        ImGui::SameLine();
        if (ImGui::Button(CXOR("Discord")))
            ShellExecuteA(NULL, CXOR("open"), CXOR(""), NULL, NULL, SW_SHOWNORMAL);
        ImGui::PopFont();
        ImGui::Separator();

        ImGui::PushFont(g_fonts->title);
        ImGui::GetWindowDrawList()->AddText(winPos + ImVec2{ winSize.x / 2 - ImGui::CalcTextSize(CXOR("Mosqui")).x / 2, 9 }, IM_COL32(255, 140, 0, 255), CXOR("Mos"));
        ImGui::GetWindowDrawList()->AddText(winPos + ImVec2{ winSize.x / 2 - ImGui::CalcTextSize(CXOR("Mosqui")).x / 2 + ImGui::CalcTextSize(CXOR("Mos")).x, 9 }, g_cfg.misc.ui_color.base().as_imcolor(), CXOR("qui"));
        ImGui::PopFont();

        ImGui::EndPopup();
    }

    ImGui::EndChild(false);
    ImGui::SetNextWindowPos({ pos.x + 200, pos.y + 68 });

    ImGui::BeginChild(CXOR("##Main"), { 501, 480 - (560 - 480) }, false, ImGuiWindowFlags_NoNavInputs | ImGuiWindowFlags_NoNav | ImGuiWindowFlags_NoCollapse | ImGuiWindowFlags_NoTitleBar | ImGuiWindowFlags_NoBackground | ImGuiWindowFlags_NoResize);

    if (activeTab == 0)
        rage_bot_tab();
    else if (activeTab == 1)
        anti_aim_tab();
    else if (activeTab == 2)
        players_tab();
    else if (activeTab == 3)
        world_tab();
    else if (activeTab == 4)
        skins_tab();
    else if (activeTab == 5)
        misc_tab();
    else if (activeTab == 6)
    {
        if (update_config)
            update_config = false;
        config_tab();
    }

    ImGui::PopFont();
    ImGui::EndChild(false);

    ImGui::End(false);
}