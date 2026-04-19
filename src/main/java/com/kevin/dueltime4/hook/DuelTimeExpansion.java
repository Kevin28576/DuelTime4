package com.kevin.dueltime4.hook;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.ArenaManager;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.cache.CacheManager;
import com.kevin.dueltime4.data.pojo.PlayerData;
import com.kevin.dueltime4.ranking.RankingManager;
import com.kevin.dueltime4.util.UtilFormat;
import com.kevin.dueltime4.yaml.message.DynamicLang;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DuelTimeExpansion extends PlaceholderExpansion {
    private static final int DEFAULT_PROGRESS_BAR_LENGTH = 10;
    private static final int MAX_PROGRESS_BAR_LENGTH = 80;
    private static final char DEFAULT_PROGRESS_BAR_CHAR = '|';
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
        ArenaManager arenaManager = DuelTimePlugin.getInstance().getArenaManager();
        if (identifier.startsWith("arena_status_")) {
            String arenaId = identifier.substring("arena_status_".length());
            return resolveArenaStatus(arenaManager, arenaId);
        }
        if (identifier.startsWith("arena_is_in_game_")) {
            String arenaId = identifier.substring("arena_is_in_game_".length());
            return String.valueOf(isArenaInGame(arenaManager, arenaId));
        }

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
            if (identifier.startsWith("tier_progress_done") || identifier.startsWith("level_progress_done")) {
                return buildLevelProgressSegment(identifier, playerData.getExp(), true);
            }
            if (identifier.startsWith("tier_progress_remaining") || identifier.startsWith("level_progress_remaining")) {
                return buildLevelProgressSegment(identifier, playerData.getExp(), false);
            }
            if (identifier.equals("tier_progress_percent") || identifier.equals("level_progress_percent")) {
                double progress = plugin.getLevelManager().calculateLevelUpProgress(playerData.getExp());
                return String.valueOf(Math.round(progress * 100D));
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

    private String buildLevelProgressSegment(String identifier, double exp, boolean completed) {
        double progress = plugin.getLevelManager().calculateLevelUpProgress(exp);
        int barLength = parseProgressBarLength(identifier);
        int completedLength = (int) Math.round(progress * barLength);
        if (completedLength < 0) {
            completedLength = 0;
        } else if (completedLength > barLength) {
            completedLength = barLength;
        }
        int segmentLength = completed ? completedLength : barLength - completedLength;
        if (segmentLength <= 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(segmentLength);
        for (int i = 0; i < segmentLength; i++) {
            builder.append(DEFAULT_PROGRESS_BAR_CHAR);
        }
        return builder.toString();
    }

    private int parseProgressBarLength(String identifier) {
        int lastUnderline = identifier.lastIndexOf('_');
        if (lastUnderline < 0 || lastUnderline == identifier.length() - 1) {
            return DEFAULT_PROGRESS_BAR_LENGTH;
        }
        String suffix = identifier.substring(lastUnderline + 1);
        int length;
        try {
            length = Integer.parseInt(suffix);
        } catch (NumberFormatException ignored) {
            return DEFAULT_PROGRESS_BAR_LENGTH;
        }
        if (length < 1) {
            return 1;
        }
        return Math.min(length, MAX_PROGRESS_BAR_LENGTH);
    }

    private String resolveArenaStatus(ArenaManager arenaManager, String arenaId) {
        BaseArena arena = arenaManager == null ? null : arenaManager.get(arenaId);
        if (arena == null) {
            return DynamicLang.get(null,
                    "Dynamic.placeholder.arena-status.unknown",
                    "未知");
        }
        if (isInProgress(arena.getState())) {
            return DynamicLang.get(null,
                    "Dynamic.placeholder.arena-status.in-game",
                    "對戰中");
        }
        return DynamicLang.get(null,
                "Dynamic.placeholder.arena-status.idle",
                "空閒");
    }

    private boolean isArenaInGame(ArenaManager arenaManager, String arenaId) {
        BaseArena arena = arenaManager == null ? null : arenaManager.get(arenaId);
        if (arena == null) {
            return false;
        }
        return isInProgress(arena.getState());
    }

    private boolean isInProgress(BaseArena.State state) {
        return state == BaseArena.State.IN_PROGRESS_OPENED || state == BaseArena.State.IN_PROGRESS_CLOSED;
    }
}
