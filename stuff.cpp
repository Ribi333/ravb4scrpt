void c_ragebot::update_hitboxes()
{
    if (HACKS->weapon->is_taser())
    {
        hitboxes.emplace_back(HITBOX_STOMACH);
        hitboxes.emplace_back(HITBOX_PELVIS);
        return;
    }

    if (HACKS->convars.mp_damage_headshot_only->get_int() == 1)
    {
        hitboxes.emplace_back(HITBOX_HEAD);
        return;
    }

    // NEW: Check if AI resolver suggests a different hitbox priority
    if (g_cfg.rage.ai_resolver_enabled && best_rage_player.player) {
        int ai_suggested_hitbox = ENHANCED_AI_RESOLVER->get_best_hitbox(best_rage_player.player);
        
        // Prioritize AI suggested hitbox
        if (ai_suggested_hitbox == HITGROUP_HEAD && (rage_config.hitboxes & head)) {
            hitboxes.emplace_back(HITBOX_HEAD);
        } else if (ai_suggested_hitbox == HITGROUP_CHEST && (rage_config.hitboxes & chest)) {
            hitboxes.emplace_back(HITBOX_CHEST);
        } else if (ai_suggested_hitbox == HITGROUP_STOMACH && (rage_config.hitboxes & stomach)) {
            hitboxes.emplace_back(HITBOX_STOMACH);
        }
    }

    // Original hitbox logic (but check if already added)
    if (rage_config.hitboxes & head && std::find(hitboxes.begin(), hitboxes.end(), HITBOX_HEAD) == hitboxes.end())
        hitboxes.emplace_back(HITBOX_HEAD);

    if (rage_config.hitboxes & chest && std::find(hitboxes.begin(), hitboxes.end(), HITBOX_CHEST) == hitboxes.end())
        hitboxes.emplace_back(HITBOX_CHEST);

    if (rage_config.hitboxes & stomach && std::find(hitboxes.begin(), hitboxes.end(), HITBOX_STOMACH) == hitboxes.end())
        hitboxes.emplace_back(HITBOX_STOMACH);

    if (rage_config.hitboxes & pelvis)
        hitboxes.emplace_back(HITBOX_PELVIS);

    if (rage_config.hitboxes & arms_)
    {
        hitboxes.emplace_back(HITBOX_LEFT_UPPER_ARM);
        hitboxes.emplace_back(HITBOX_RIGHT_UPPER_ARM);
    }

    if (rage_config.hitboxes & legs)
    {
        hitboxes.emplace_back(HITBOX_LEFT_FOOT);
        hitboxes.emplace_back(HITBOX_RIGHT_FOOT);
    }
}
