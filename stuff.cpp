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

	if (rage_config.hitboxes & head)
		hitboxes.emplace_back(HITBOX_HEAD);

	if (rage_config.hitboxes & chest)
		hitboxes.emplace_back(HITBOX_CHEST);

	if (rage_config.hitboxes & stomach)
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
