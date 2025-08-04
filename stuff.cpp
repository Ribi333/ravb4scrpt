void c_ragebot::scan_players()
{
	int threads_count = 0;

	LISTENER_ENTITY->for_each_player([&](c_cs_player* player)
	{
		if (!player->is_alive() || player->dormant() || player->has_gun_game_immunity())
			return;

		auto rage = &rage_players[player->index()];
		if (!rage || !rage->player || rage->player != player)
			return;

		++threads_count;

		auto dmg = this->get_min_damage(rage->player);
		THREAD_POOL->add_task(pre_cache_centers, dmg, std::ref(hitboxes), std::ref(predicted_eye_pos), rage);
	});

	if (threads_count < 1)
		return;

	THREAD_POOL->wait_all();

	LISTENER_ENTITY->for_each_player([&](c_cs_player* player)
	{
		if (!player->is_alive() || player->dormant() || player->has_gun_game_immunity())
			return;

		auto rage = &rage_players[player->index()];
		if (!rage || !rage->player || rage->player != player)
			return;

		do_hitscan(rage);
	});
}
