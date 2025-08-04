void c_ragebot::scan_players()
{
    // Collect valid players first to avoid unnecessary thread creation
    std::vector<c_cs_player*> valid_players;
    
    LISTENER_ENTITY->for_each_player([&](c_cs_player* player)
    {
        if (!player->is_alive() || player->dormant() || player->has_gun_game_immunity())
            return;

        auto rage = &rage_players[player->index()];
        if (!rage || !rage->player || rage->player != player)
            return;

        valid_players.push_back(player);
    });

    if (valid_players.empty())
        return;

    // Process in smaller batches or sequentially for better performance
    for (auto* player : valid_players)
    {
        auto rage = &rage_players[player->index()];
        auto dmg = this->get_min_damage(rage->player);
        
        // Try processing without threading first to see if it's faster
        pre_cache_centers(dmg, hitboxes, predicted_eye_pos, rage);
    }

    // Process hitscan
    for (auto* player : valid_players)
    {
        auto rage = &rage_players[player->index()];
        do_hitscan(rage);
    }
}
