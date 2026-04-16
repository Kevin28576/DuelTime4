package com.kevin.dueltime4.hook;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.cache.CacheManager;
import com.kevin.dueltime4.data.pojo.PlayerData;
import com.kevin.dueltime4.ranking.RankingManager;
import com.kevin.dueltime4.util.UtilFormat;
import com.kevin.dueltime4.cache.PlayerDataCache;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DuelTimeExpansion extends PlaceholderExpansion {
    private final DuelTimePlugin plugin;

    public DuelTimeExpansion(DuelTimePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "Kevin";
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "dueltime4";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "";
        CacheManager cacheManager = DuelTimePlugin.getInstance().getCacheManager();
        PlayerData playerData = cacheManager.getPlayerDataCache().get(player.getName());
        if (playerData != null) {
            if (identifier.equals("classic_win_number")) {
                return "" + playerData.getArenaClassicWins();
            }
            if (identifier.equals("classic_loss_number")) {
                return "" + playerData.getArenaClassicLoses();
            }
            if (identifier.equals("classic_draw_number")) {
                return "" + playerData.getArenaClassicDraws();
            }
            if (identifier.equals("classic_win_rate")) {
                return UtilFormat.round((playerData.getArenaClassicWins() / (double) (playerData.getArenaClassicLoses() != 0 ? playerData.getArenaClassicLoses() : 1)) * 100, 1) + "%";
            }
            if (identifier.equals("classic_game_number")) {
                return "" + (playerData.getArenaClassicWins() + playerData.getArenaClassicLoses() + playerData.getArenaClassicDraws());
            }
            if (identifier.equals("classic_game_time")) {
                return "" + playerData.getArenaClassicTime();
            }
            if (identifier.equals("total_game_number")) {
                return "" + playerData.getTotalGameNumber();
            }
            if (identifier.equals("total_game_time")) {
                return "" + playerData.getTotalGameTime();
            }
            if (identifier.equals("exp")) {
                return "" + playerData.getExp();
            }
            if (identifier.equals("exp_to_next_level")) {
                return "" + DuelTimePlugin.getInstance().getLevelManager().calculateRemainingExpForLevelUp(playerData.getExp());
            }
            if (identifier.equals("level")) {
                return "" + DuelTimePlugin.getInstance().getLevelManager().getLevel(player.getName());
            }
            if (identifier.equals("tier")) {
                return DuelTimePlugin.getInstance().getLevelManager().getTier(player.getName()).getTitle();
            }
            if (identifier.equals("point")) {
                return "" + playerData.getPoint();
            }
        }
        RankingManager rankingManager = DuelTimePlugin.getInstance().getRankingManager();
        try {
            if (identifier.equals("rank_classic_win_number")) {
                return rankingManager.getRanking(RankingManager.InternalType.CLASSIC_WIN_NUMBER.getId()).getRankString(player);
            }
            if (identifier.equals("rank_classic_win_rate")) {
                return rankingManager.getRanking(RankingManager.InternalType.CLASSIC_WIN_RATE.getId()).getRankString(player);
            }
            if (identifier.equals("rank_classic_game_time")) {
                return rankingManager.getRanking(RankingManager.InternalType.CLASSIC_GAME_TIME.getId()).getRankString(player);
            }
            if (identifier.equals("rank_classic_game_number")) {
                return rankingManager.getRanking(RankingManager.InternalType.CLASSIC_GAME_NUMBER.getId()).getRankString(player);
            }
            if (identifier.equals("rank_total_game_number")) {
                return rankingManager.getRanking(RankingManager.InternalType.TOTAL_GAME_NUMBER.getId()).getRankString(player);
            }
            if (identifier.equals("rank_total_game_time")) {
                return rankingManager.getRanking(RankingManager.InternalType.TOTAL_GAME_TIME.getId()).getRankString(player);
            }
            if (identifier.equals("rank_exp")) {
                return rankingManager.getRanking(RankingManager.InternalType.EXP.getId()).getRankString(player);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }
}
