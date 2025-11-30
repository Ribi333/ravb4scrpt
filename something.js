const { EmbedBuilder, AttachmentBuilder } = require('discord.js');
const { createCanvas, loadImage } = require('canvas');

// Rank helper function to get player's display rank and color
function getPlayerRank(player) {
    if (!player) return { text: '', color: '#ffffff', isYouTube: false };
    
    // Check for special ranks first (these override purchased ranks)
    if (player.rank) {
        const specialRanks = {
            'ADMIN': { text: '[ADMIN]', color: '#ff5555', isYouTube: false },
            'MODERATOR': { text: '[MOD]', color: '#ff5555', isYouTube: false },
            'HELPER': { text: '[HELPER]', color: '#55ff55', isYouTube: false },
            'JR_HELPER': { text: '[JR HELPER]', color: '#55ff55', isYouTube: false },
            'YOUTUBER': { text: '[YOUTUBE]', color: '#ff5555', isYouTube: true },
            'STAFF': { text: '[ADMIN]', color: '#ff5555', isYouTube: false }
        };
        
        // Only return if it's an actual special rank (not "NORMAL")
        if (player.rank !== 'NORMAL' && specialRanks[player.rank]) {
            return specialRanks[player.rank];
        }
    }
    
    // Check for monthly package (MVP++)
    if (player.monthlyPackageRank === 'SUPERSTAR') {
        return { text: '[MVP++]', color: '#ffaa00', isYouTube: false };
    }
    
    // Check for regular purchased ranks
    const rank = player.newPackageRank || player.packageRank;
    const rankMap = {
        'MVP_PLUS': { text: '[MVP+]', color: '#55ffff', isYouTube: false },
        'MVP': { text: '[MVP]', color: '#55ffff', isYouTube: false },
        'VIP_PLUS': { text: '[VIP+]', color: '#55ff55', isYouTube: false },
        'VIP': { text: '[VIP]', color: '#55ff55', isYouTube: false }
    };
    
    return rankMap[rank] || { text: '', color: '#ffffff', isYouTube: false };
}

module.exports = {
    data: {
        name: "skywars",
        description: "Fetches someone's skywars stats using the Hypixel API.",
        integration_types: [0, 1],
        contexts: [0, 1, 2],
        options: [
            {
                type: 3,
                name: "username",
                description: "Minecraft IGN",
                required: true
            }
        ]
    },
    async execute(interaction) {
        const api = process.env.HYPIXEL_API_KEY;
        const user = interaction.options.getString('username');

        await interaction.deferReply();

        try {
            // Get UUID from Mojang API
            const mojang = await fetch(`https://api.mojang.com/users/profiles/minecraft/${user}`);
            if (!mojang.ok) {
                return interaction.editReply({ 
                    content: `Could not find Minecraft player **${user}**. Please check the username and try again.` 
                });
            }

            const data = await mojang.json();
            const uuid = data.id;
            const username = data.name;

            // Get player data from Hypixel API
            const url = `https://api.hypixel.net/v2/player?key=${api}&uuid=${uuid}`;
            const response = await fetch(url);
            const hypixel = await response.json();

            if (!hypixel.player) {
                return interaction.editReply({ 
                    content: `**${username}** has never joined Hypixel.` 
                });
            }

            // Extract skywars stats
            const swStats = hypixel.player?.stats?.SkyWars || {};
            const achievements = hypixel.player?.achievements || {};
            const level = achievements.skywars_you_re_a_star || 0;
            const experience = swStats.skywars_experience || 0;
            
            // Get player rank
            const playerRank = getPlayerRank(hypixel.player);
            
            const wins = swStats.wins || 0;
            const losses = swStats.losses || 0;
            const wlr = losses > 0 ? (wins / losses).toFixed(2) : wins.toFixed(2);
            
            const kills = swStats.kills || 0;
            const deaths = swStats.deaths || 0;
            const kdr = deaths > 0 ? (kills / deaths).toFixed(2) : kills.toFixed(2);
            
            const assists = swStats.assists || 0;
            const souls = swStats.souls || 0;
            const heads = swStats.heads || 0;
            const tokens = swStats.cosmetic_tokens || 0;
            const opals = swStats.opals || 0;
            
            const winstreak = swStats.winstreak || 0;
            const coins = swStats.coins || 0;
            const gamesPlayed = swStats.games_played || 1;

            // Generate image
            const canvas = createCanvas(1600, 900);
            const ctx = canvas.getContext('2d');

            // Dark background with overlay
            ctx.fillStyle = '#0a0a0a';
            ctx.fillRect(0, 0, 1600, 900);

            // Semi-transparent overlay
            ctx.fillStyle = 'rgba(20, 20, 30, 0.85)';
            ctx.fillRect(0, 0, 1600, 900);

            // Try to load player skin
            try {
                const skinImg = await loadImage(`https://visage.surgeplay.com/bust/256/${uuid}`);
                ctx.drawImage(skinImg, 30, 70, 250, 250);
            } catch (e) {
                // Fallback to head if full skin fails
                try {
                    const headImg = await loadImage(`https://minotar.net/helm/${username}/250`);
                    ctx.drawImage(headImg, 30, 70, 250, 250);
                } catch (err) {
                    console.log('Could not load player image');
                }
            }

            // Format number with commas
            const formatNum = (num) => Math.round(num).toLocaleString('en-US');

            // Title and player info section
            ctx.fillStyle = '#ff5555';
            ctx.font = 'bold 56px Arial';
            ctx.fillText('Overall Skywars Stats', 310, 90);

            // Player name with rank
            ctx.font = 'bold 44px Arial';
            let currentX = 310;
            
            // Draw level with star
            ctx.fillStyle = '#ffaa00';
            const levelText = `[${level}\u2B50] `;
            ctx.fillText(levelText, currentX, 145);
            currentX += ctx.measureText(levelText).width;
            
            // Draw rank with color
            if (playerRank.text) {
                if (playerRank.isYouTube) {
                    // Special handling for YouTube - red brackets, white YOUTUBE text
                    ctx.fillStyle = '#ff5555';
                    ctx.fillText('[', currentX, 145);
                    currentX += ctx.measureText('[').width;
                    
                    ctx.fillStyle = '#ffffff';
                    ctx.fillText('YOUTUBE', currentX, 145);
                    currentX += ctx.measureText('YOUTUBE').width;
                    
                    ctx.fillStyle = '#ff5555';
                    ctx.fillText('] ', currentX, 145);
                    currentX += ctx.measureText('] ').width;
                } else {
                    // Regular colored rank
                    ctx.fillStyle = playerRank.color;
                    const rankText = `${playerRank.text} `;
                    ctx.fillText(rankText, currentX, 145);
                    currentX += ctx.measureText(rankText).width;
                }
            }
            
            // Draw username with same color as rank (or white if no rank)
            ctx.fillStyle = playerRank.color;
            ctx.fillText(username, currentX, 145);

            // Calculate XP progress to next level
            function getSkyWarsExpForLevel(level) {
                const xps = [0, 20, 70, 150, 250, 500, 1000, 2000, 3500, 6000, 10000, 15000];
                if (level >= 12) {
                    return 10000; // All levels 12+ require 10k XP
                }
                if (level < xps.length) {
                    return xps[Math.floor(level)] || 10000;
                }
                return 10000;
            }
            
            // Calculate total XP needed to reach a level
            function getTotalExpToLevel(level) {
                const xps = [0, 20, 70, 150, 250, 500, 1000, 2000, 3500, 6000, 10000, 15000];
                if (level >= 12) {
                    return 15000 + (level - 12) * 10000;
                }
                return xps[Math.floor(level)] || 0;
            }
            
            // Calculate precise level from XP
            function calculatePreciseLevel(xp) {
                const xps = [0, 20, 70, 150, 250, 500, 1000, 2000, 3500, 6000, 10000, 15000];
                if (xp >= 15000) {
                    return (xp - 15000) / 10000 + 12;
                } else {
                    for (let i = 1; i < xps.length; i++) {
                        if (xp < xps[i]) {
                            return (i - 1) + (xp - xps[i-1]) / (xps[i] - xps[i-1]);
                        }
                    }
                }
                return 0;
            }
            
            const preciseLevel = calculatePreciseLevel(experience);
            const currentLevelFloor = Math.floor(preciseLevel);
            const totalExpForCurrentLevel = getTotalExpToLevel(currentLevelFloor);
            const expForNextLevel = getSkyWarsExpForLevel(currentLevelFloor);
            const expInCurrentLevel = experience - totalExpForCurrentLevel;

            // XP Progress bar - OPTIMIZED POSITION
            const barWidth = 780;
            const barHeight = 42;
            const barX = 310;
            const barY = 220;
            const progress = Math.min(Math.max(expInCurrentLevel / expForNextLevel, 0), 1);

            // Bar background
            ctx.fillStyle = '#2a2a2a';
            ctx.fillRect(barX, barY, barWidth, barHeight);

            // Bar fill
            const gradient = ctx.createLinearGradient(barX, 0, barX + barWidth, 0);
            gradient.addColorStop(0, '#55ffff');
            gradient.addColorStop(1, '#5555ff');
            ctx.fillStyle = gradient;
            ctx.fillRect(barX, barY, barWidth * progress, barHeight);

            // Bar border
            ctx.strokeStyle = '#ffffff';
            ctx.lineWidth = 2;
            ctx.strokeRect(barX, barY, barWidth, barHeight);

            // XP text - BIGGER
            ctx.fillStyle = '#ffffff';
            ctx.font = 'bold 30px Arial';
            ctx.fillText(`${formatNum(Math.max(0, expInCurrentLevel))} / ${formatNum(expForNextLevel)} XP`, barX + barWidth + 25, barY + 30);

            // Level up indicator - BIGGER
            ctx.font = 'bold 34px Arial';
            ctx.fillStyle = '#ffffff';
            ctx.fillText(`${formatNum(Math.max(0, expForNextLevel - expInCurrentLevel))} XP to level ${currentLevelFloor + 1}`, barX, barY + 78);

            // Draw boxes/sections
            function drawBox(x, y, width, height, title, stats) {
                // Box background
                ctx.fillStyle = 'rgba(40, 40, 50, 0.7)';
                ctx.fillRect(x, y, width, height);

                // Box border
                ctx.strokeStyle = '#444455';
                ctx.lineWidth = 2;
                ctx.strokeRect(x, y, width, height);

                // Title
                ctx.fillStyle = '#ffaa00';
                ctx.font = 'bold 32px Arial';
                ctx.fillText(title, x + 18, y + 45);

                // Stats
                let yOffset = 92;
                stats.forEach(stat => {
                    ctx.fillStyle = stat.color;
                    ctx.font = 'bold 28px Arial';
                    ctx.fillText(stat.label, x + 18, y + yOffset);

                    ctx.fillStyle = '#ffffff';
                    ctx.font = '30px Arial';
                    ctx.fillText(stat.value, x + width - 25 - ctx.measureText(stat.value).width, y + yOffset);

                    yOffset += 48;
                });
            }

            // Draw box with 2 columns
            function drawDoubleBox(x, y, width, height, title, leftStats, rightStats) {
                // Box background
                ctx.fillStyle = 'rgba(40, 40, 50, 0.7)';
                ctx.fillRect(x, y, width, height);

                // Box border
                ctx.strokeStyle = '#444455';
                ctx.lineWidth = 2;
                ctx.strokeRect(x, y, width, height);

                // Title
                ctx.fillStyle = '#ffaa00';
                ctx.font = 'bold 32px Arial';
                ctx.fillText(title, x + 18, y + 45);

                const columnWidth = width / 2;
                
                // Left column
                let yOffset = 92;
                leftStats.forEach(stat => {
                    ctx.fillStyle = stat.color;
                    ctx.font = 'bold 28px Arial';
                    ctx.fillText(stat.label, x + 18, y + yOffset);

                    ctx.fillStyle = '#ffffff';
                    ctx.font = '30px Arial';
                    const valueWidth = ctx.measureText(stat.value).width;
                    ctx.fillText(stat.value, x + columnWidth - 40 - valueWidth, y + yOffset);

                    yOffset += 48;
                });

                // Right column
                yOffset = 92;
                rightStats.forEach(stat => {
                    ctx.fillStyle = stat.color;
                    ctx.font = 'bold 28px Arial';
                    ctx.fillText(stat.label, x + columnWidth + 18, y + yOffset);

                    ctx.fillStyle = '#ffffff';
                    ctx.font = '30px Arial';
                    ctx.fillText(stat.value, x + width - 25 - ctx.measureText(stat.value).width, y + yOffset);

                    yOffset += 48;
                });
            }

            // Section 1: General Stats (top left)
            drawBox(30, 320, 365, 270, 'General', [
                { label: 'Coins:', value: formatNum(coins), color: '#ffaa00' },
                { label: 'Games Played:', value: formatNum(gamesPlayed), color: '#55ff55' },
                { label: 'Souls:', value: formatNum(souls), color: '#55ffff' },
                { label: 'Winstreak:', value: formatNum(winstreak), color: '#aa55ff' }
            ]);

            // Section 2: Win/Loss (top middle-left)
            drawBox(420, 320, 355, 270, 'Win/Loss', [
                { label: 'Wins:', value: formatNum(wins), color: '#55ff55' },
                { label: 'Losses:', value: formatNum(losses), color: '#ff5555' },
                { label: 'WLR:', value: wlr, color: '#ffaa00' },
                { label: 'Assists:', value: formatNum(assists), color: '#5555ff' }
            ]);

            // Section 3: K/D (top middle-right)
            drawBox(800, 320, 380, 270, 'K/D', [
                { label: 'Kills:', value: formatNum(kills), color: '#55ff55' },
                { label: 'Deaths:', value: formatNum(deaths), color: '#ff5555' },
                { label: 'KDR:', value: kdr, color: '#ffaa00' },
                { label: 'Heads:', value: formatNum(heads), color: '#d45fff' }
            ]);

            // Section 4: Cosmetics (top right)
            drawBox(1205, 320, 380, 270, 'Cosmetics', [
                { label: 'Tokens:', value: formatNum(tokens), color: '#55ff55' },
                { label: 'Opals:', value: formatNum(opals), color: '#55ffff' },
                { label: 'Heads:', value: formatNum(heads), color: '#d45fff' }
            ]);

            // Section 5: Mode Stats (bottom - full width with 2 columns)
            // Get mode-specific stats
            const soloWins = swStats.wins_solo || 0;
            const soloKills = swStats.kills_solo || 0;
            const teamWins = swStats.wins_team || 0;
            const teamKills = swStats.kills_team || 0;
            const rankedWins = swStats.wins_ranked || 0;
            const rankedKills = swStats.kills_ranked || 0;
            const megaWins = swStats.wins_mega || 0;
            const megaKills = swStats.kills_mega || 0;

            drawDoubleBox(30, 605, 1555, 230, 'Mode Statistics',
                [
                    { label: 'Solo Wins:', value: formatNum(soloWins), color: '#55ff55' },
                    { label: 'Solo Kills:', value: formatNum(soloKills), color: '#ffaa00' },
                    { label: 'Team Wins:', value: formatNum(teamWins), color: '#55ff55' },
                    { label: 'Team Kills:', value: formatNum(teamKills), color: '#ffaa00' }
                ],
                [
                    { label: 'Ranked Wins:', value: formatNum(rankedWins), color: '#55ff55' },
                    { label: 'Ranked Kills:', value: formatNum(rankedKills), color: '#ffaa00' },
                    { label: 'Mega Wins:', value: formatNum(megaWins), color: '#55ff55' },
                    { label: 'Mega Kills:', value: formatNum(megaKills), color: '#ffaa00' }
                ]
            );

            // Convert to buffer
            const buffer = canvas.toBuffer('image/png');
            const attachment = new AttachmentBuilder(buffer, { name: `skywars-${username}.png` });

            await interaction.editReply({ files: [attachment] });

        } catch (error) {
            console.error('[ERROR] Skywars command error:', error);
            await interaction.editReply({ 
                content: 'An error occurred while fetching stats. Please try again later.' 
            });
        }
    }
};
