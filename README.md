# DuelTime4

[![Build](https://github.com/Kevin28576/DuelTime4/actions/workflows/build.yml/badge.svg)](https://github.com/Kevin28576/DuelTime4/actions/workflows/build.yml)
[![License](https://img.shields.io/github/license/Kevin28576/DuelTime4?label=License)](https://github.com/Kevin28576/DuelTime4/blob/main/LICENSE)
[![Last Commit](https://img.shields.io/github/last-commit/Kevin28576/DuelTime4?label=Last%20Commit)](https://github.com/Kevin28576/DuelTime4/commits/main)
[![Issues](https://img.shields.io/github/issues/Kevin28576/DuelTime4?label=Issues)](https://github.com/Kevin28576/DuelTime4/issues)
[![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Paper](https://img.shields.io/badge/Paper-1.21_to_26.1.x-white)](https://papermc.io/)

DuelTime4 是一個以 Paper 為目標平台的 PvP 插件，主打可配置的 1v1 競技場、配對隊列、排行系統與可上線運行的管理功能。

## 功能重點

- 經典 1v1 競技場流程與隊列配對
- 可指定競技場的邀請指令
- 連勝/連敗（streak）追蹤與可調整規則
- 離場懲罰（主動離開/斷線）支援扣分與隊列冷卻
- 隊列看門狗（Queue Watchdog）自動清理異常狀態
- 重啟保護（避免重啟時誤判離場懲罰）
- Discord Webhook 回報（Embed 支援）
- 內建更新器（`check` / `download` / `status`）與 SHA-256 驗證
- PlaceholderAPI 整合（資料、排行、競技場狀態、段位進度條分段）

## 環境需求

- Java 21+
- Paper 1.21 +

可選整合：

- PlaceholderAPI
- HolographicDisplays / HolographicDisplay / DecentHolograms / CMI
- Multiverse-Core

## 安裝方式

1. 下載釋出檔：`Dueltime4-Bukkit-<version>.jar`
2. 放入伺服器的 `plugins/` 資料夾
3. 啟動一次伺服器讓設定與語言檔生成
4. 編輯：
   - `plugins/DuelTime4/config.yml`
   - `plugins/DuelTime4/languages/zh_tw.yml`
5. 建議重啟，或執行 `/dt reload`

## 指令

根指令：

- `/dueltime4`
- 別名：`/dueltime`、`/dt`、`/dt4`

內建說明：

- `/dt help`（玩家常用指令）
- `/dt adminhelp`（管理員指令，會依權限顯示）

### 玩家常用指令

- `/dt start`
- `/dt send <玩家> [競技場編輯名]`
- `/dt accept [玩家]`
- `/dt decline [玩家]`
- `/dt queue sound|cooldown`
- `/dt shop`
- `/dt record`
- `/dt stats [玩家]`
- `/dt lang [語言檔名]`
- `/dt quit`

### 管理員常用指令（`dueltime.admin`）

- `/dt arena ...`
- `/dt point ...`
- `/dt level ...`
- `/dt rank ...`
- `/dt balance view|set|config ...`
- `/dt doctor [all|services|database|queue]`
- `/dt queue debug|cooldown ...`
- `/dt record export [player] [json|csv] [limit]`
- `/dt stop <arena_id|all> [reason]`
- `/dt update check|download|status`
- `/dt reload`

## 權限

| 權限 | 用途 |
| --- | --- |
| `dueltime.admin` | 允許管理員子指令與管理功能。 |

## PlaceholderAPI

Expansion 識別字：`%dueltime4_*%`

常見範例：

- `%dueltime4_point%`
- `%dueltime4_level%`
- `%dueltime4_tier%`
- `%dueltime4_rank_exp%`
- `%dueltime4_arena_status_<arenaId>%`（`idle` / `in-game` / `unknown`）
- `%dueltime4_arena_is_in_game_<arenaId>%`（`true` / `false`）

### 段位/升級進度條分段變數

提供「已完成段」與「未完成段」兩段變數，可分別套色。

- `%dueltime4_tier_progress_done%`
- `%dueltime4_tier_progress_remaining%`
- `%dueltime4_tier_progress_percent%`

同義別名（`level_progress_*`）：

- `%dueltime4_level_progress_done%`
- `%dueltime4_level_progress_remaining%`
- `%dueltime4_level_progress_percent%`

可選長度尾碼（預設 `10`，最大 `80`）：

- `%dueltime4_tier_progress_done_20%`
- `%dueltime4_tier_progress_remaining_20%`

範例：

```text
&a%dueltime4_tier_progress_done_20%&7%dueltime4_tier_progress_remaining_20% &f%dueltime4_tier_progress_percent%%
```

## 設定重點

主要設定區塊：

- `Message`
- `System.restart-protection`
- `Database`
- `Network.discord-webhook`
- `Arena.classic.*`
- `Record`
- `Updater`
- `Ranking`
- `Level`

### 比賽中指令白名單

玩家在經典模式比賽中，預設僅允許認輸指令。  
可透過關鍵字白名單放行其他指令（包含比對）。

```yml
Arena:
  classic:
    matchmaking:
      command-whitelist:
        - "test"
        - "/dantiao test_whitelist_command1"
        - "/dantiao test_whitelist_command2"
```

只要玩家輸入的指令文字包含任一關鍵字，就會被允許執行。

## 編譯

```bash
mvn clean package
```

輸出檔案：

- `target/Dueltime4-Bukkit-<version>.jar`

## CI / 發版

- 工作流程：`.github/workflows/build.yml`
- 觸發條件：`push`、`pull_request`、手動 `workflow_dispatch`、`release created`
- 在 release 事件中，CI 會自動把打包 jar 上傳到 GitHub Release

## 更新器 Metadata 自動生成

在 `mvn package` 時會自動依版本、更新日誌與 jar 雜湊生成：

- `src/main/java/com/kevin/dueltime4/network/version`
- `src/main/java/com/kevin/dueltime4/network/update.yml`

更新器會搭配 `config.yml` 的 `Updater.*` URL 設定讀取。

## 文件與支援

- 文件站：<https://cloudxact.com/DuelTime4>
- 問題回報：<https://github.com/Kevin28576/DuelTime4/issues>
- 更新日誌：[CHANGELOG.md](CHANGELOG.md)

## 授權

本專案採用 Apache License 2.0，詳見 [LICENSE](LICENSE)。

## 宣傳圖（點擊展開）

<details>
<summary>點我查看 DuelTime4 宣傳圖</summary>

![DuelTime4](.github/DuelTime4.png)

</details>