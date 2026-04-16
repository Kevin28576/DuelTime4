package com.kevin.dueltime4.progress;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.event.progress.ProgressFinishedEvent;
import com.kevin.dueltime4.util.UtilFormat;
import com.kevin.dueltime4.viaversion.ViaVersion;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class Progress {
    // 過程的唯一標識
    private final String id;
    // 過程的名稱（可為String也可為Msg）
    private final Object name;
    // 執行該過程的玩家
    private final Player player;
    // 開始過程時攜帶的一些資料
    private final Object data;
    // 步驟集合
    private final Step[] steps;
    // 步驟總數
    private final int totalStep;
    // 當前完成步驟的總數，也可以善用巧合，把這理解為當前所操作的步驟的序號（從0開始）
    private int finishedStep;
    // 是否使用BossBar進度條，若否，則使用純文字提示（1.8及以下沒有直接建立進度條的API）
    private final boolean isBossBarUsed;
    // 用於顯示進度的BossBar（Boss血量條）
    private BossBar progressBar;
    // BossBar原本的顏色，用於恢復暫停時重新恢復顏色（暫定時進度條會變成黃色）
    private BarColor barColor;
    // 是否處於暫停狀態，處於該狀態時，無論做什麼動作都不會觸發上傳資料的事件
    private boolean paused;
    // 定時器，用於實現一些拓展功能
    private BukkitTask timer;

    public Progress(Plugin plugin, String id, Object name, Player player, Object data, boolean isBossBarUsed, Step... steps) {
        // 確定過程的唯一標識，最終ID為“外掛名+冒號+填寫的ID”
        if (plugin == null) {
            throw new NullPointerException("The plugin cannot be null");
        }
        if (!id.contains(":") || !id.split(":")[0].equals(plugin.getDescription().getName().toLowerCase()) || !UtilFormat.isIDStyle(id.split(":")[1])) {
            throw new IllegalArgumentException("The format of the 2nd argument must be 'the lowercase of your plugin' + ':' + 'id',for example,'dueltime4:test',and the id can only consist of English and numbers");
        }
        this.id = id;
        // 確定過程的名稱
        if (!(name instanceof String) && !(name instanceof Msg)) {
            throw new IllegalArgumentException("The type of 3rd argument must be String or Msg");
        }
        this.name = name;
        // 確定執行該過程的玩家
        this.player = player;
        // 傳入開始過程時攜帶的一些資料
        this.data = data;
        // 確定步驟資訊（每個Step包括當前步驟的BossBar提示語、需要的資料型別、完成後的提示語等資訊）
        this.steps = steps;
        this.totalStep = steps.length;
        this.finishedStep = 0;
        this.isBossBarUsed = isBossBarUsed;
        // 設定現在不處於暫停狀態
        this.paused = false;
    }

    public void initBossBar(BarColor barColor, BarStyle barStyle) {
        // 顯示首個步驟的BossBar標題
        String barTitle = MsgBuilder.get(Msg.PROGRESS_BOSSBAR_TIP, player,
                getName(), "0", "" + totalStep, steps[0].getTip());
        // 傳入BossBar原色，用於在恢復暫停時換回來
        this.barColor = barColor;
        // 建立一個BossBar物件
        this.progressBar = Bukkit.createBossBar(barTitle, barColor, barStyle);
        // 設定BossBar進度為0%
        this.progressBar.setProgress(0);
        // 使BossBar向執行玩家展示
        this.progressBar.addPlayer(player);
    }

    public String getName() {
        return (name instanceof String) ?
                (String) name :
                MsgBuilder.get((Msg) name, player);
    }

    /**
     * 確認完成當前步驟並上傳資料，同時進入下一步
     *
     * @param data 上傳的資料
     */
    public void next(Object data) {
        if (finishedStep >= totalStep) {
            // 如果已經是最後一步，則不處理
            return;
        }
        // 獲取當前所完成的步驟
        Step nowStep = steps[finishedStep];
        // 上傳當前步驟所需的資料
        nowStep.setData(data);
        // 傳送當前步驟完成後的Title提示語
        MsgBuilder.sendTitle(nowStep.getFinishTitle(),
                nowStep.getFinishSubTitle().replace("{}", UtilFormat.toString(nowStep.getData(),
                        UtilFormat.StringifyTag.LIST_LIMIT_LINE_LENGTH,
                        UtilFormat.StringifyTag.LIST_LIMIT_LIST_SIZE)),
                0, 30, 5, player, ViaVersion.TitleType.SUBTITLE);
        // 已完成步驟數+1
        finishedStep++;
        // 按照當前步驟更新提示內容
        updateProgressTip();
    }

    /**
     * 返回上一步，同時最近一次上傳的資料會被刪除
     */
    public void reverse() {
        if (finishedStep <= 0) {
            // 如果當前沒有完成任何步驟，則不處理
            return;
        }
        // 刪除資料
        steps[finishedStep].setData(null);
        // 已完成步驟數-1
        finishedStep--;
        // 按照當前步驟更新提示內容
        updateProgressTip();
    }

    private void updateProgressTip() {
        String tip = finishedStep == totalStep ?
                MsgBuilder.get(Msg.PROGRESS_BOSSBAR_MESSAGE_FINISH_STRING, player) : steps[finishedStep].getTip();
        if (isBossBarUsed) {
            progressBar.setTitle(
                    MsgBuilder.get(Msg.PROGRESS_BOSSBAR_TIP, player,
                            getName(), "" + finishedStep, "" + totalStep, tip));
            progressBar.setProgress((double) finishedStep / totalStep);
        } else {
            MsgBuilder.sends(Msg.PROGRESS_BOSSBAR_FREE_TIP, player, false,
                    getName(), "" + finishedStep, "" + totalStep,
                    finishedStep + " " + (totalStep - finishedStep),
                    tip);
        }
        // 如果當前步為最後一步，則再額外執行finish方法
        if (finishedStep == totalStep) {
            finish(true);
        }
    }


    /**
     * 完成該過程
     * 如果所有步驟確實都已經完成，那麼將使用當前完整的資料列表來建立物件
     *
     * @param delayed 是否開啟視覺延遲（有利於讓玩家看到完成提示語和滿值的進度條，最佳化視覺體驗）
     */
    public void finish(boolean delayed) {
        // 若因為一些需求，初始化時Timer被建立，那麼就關閉這個定時器
        if (timer != null) {
            timer.cancel();
        }
        // 根據上傳的資料建立物件。這裡透過發布事件的方式，方便不同的外掛例項監聽
        if (finishedStep >= totalStep) {
            Bukkit.getServer().getPluginManager().callEvent(new ProgressFinishedEvent(player, this));
        }
        // 根據需要實現視覺延時
        if (isBossBarUsed) {
            if (delayed) {
                Bukkit.getScheduler().runTaskLaterAsynchronously(DuelTimePlugin.getInstance(),
                        progressBar::removeAll, 80);
                if (finishedStep >= totalStep) {
                    Bukkit.getScheduler().runTaskLaterAsynchronously(DuelTimePlugin.getInstance(), () -> {
                        MsgBuilder.send(Msg.PROGRESS_FINISH_MESSAGE, player, getName());
                        MsgBuilder.sendTitle(
                                MsgBuilder.get(Msg.PROGRESS_FINISH_TITLE, player),
                                MsgBuilder.get(Msg.PROGRESS_FINISH_SUBTITLE, player, getName()),
                                5, 80, 12, player);
                    }, 30);
                }
            } else {
                progressBar.removeAll();
            }
        } else {
            MsgBuilder.send(Msg.PROGRESS_FINISH_MESSAGE, player, getName());
        }
        // 向管理器傳送請求銷毀自身
        DuelTimePlugin.getInstance().getProgressManager().cancel(player.getName());
    }

    public void exit() {
        if (isBossBarUsed) {
            progressBar.removeAll();
        }
        if (timer != null && !timer.isCancelled()) {
            timer.cancel();
        }
        // 向管理器傳送請求銷毀自身
        DuelTimePlugin.getInstance().getProgressManager().cancel(player.getName());
    }

    public String getId() {
        return id;
    }

    public Player getPlayer() {
        return player;
    }

    public Object getData() {
        return data;
    }

    public int getFinishedStep() {
        return finishedStep;
    }

    public Step getNowStep() {
        return steps[finishedStep];
    }

    public Step[] getSteps() {
        return steps;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        if (isBossBarUsed) {
            if (paused) {
                progressBar.setColor(BarColor.YELLOW);
            } else {
                progressBar.setColor(barColor);
            }
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isBossBarUsed() {
        return isBossBarUsed;
    }

    public BukkitTask getTimer() {
        return timer;
    }

    public void setTimer(BukkitTask timer) {
        this.timer = timer;
    }
}
