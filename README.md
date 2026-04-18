# DuelTime4

DuelTime4 是一個為新版 Paper 伺服器維護的競技場對戰插件，延續 DuelTime 3 的核心玩法，提供 1v1 對戰、觀戰、段位與排行榜等完整系統。

[![Build](https://github.com/Kevin28576/DuelTime4/actions/workflows/build.yml/badge.svg)](https://github.com/Kevin28576/DuelTime4/actions/workflows/build.yml)
[![License](https://img.shields.io/github/license/Kevin28576/DuelTime4?label=License)](https://github.com/Kevin28576/DuelTime4/blob/main/LICENSE)
[![Last Commit](https://img.shields.io/github/last-commit/Kevin28576/DuelTime4?label=Last%20Commit)](https://github.com/Kevin28576/DuelTime4/commits/main)
[![Issues](https://img.shields.io/github/issues/Kevin28576/DuelTime4?label=Issues)](https://github.com/Kevin28576/DuelTime4/issues)
[![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Paper](https://img.shields.io/badge/Paper-1.21%2B-white)](https://papermc.io/)

## 功能特色

- 經典 1v1 競技場流程（加入、開戰、結算、離場）
- 觀戰系統（可觀看進行中的對戰）
- 玩家經驗 / 段位（Tier）成長
- 積分、戰績查詢與記錄管理
- 排行榜與全息圖顯示
- 多語言訊息系統
- 支援 SQLite 與 MySQL 儲存

## 環境需求

- Java 21+
- Paper 1.21+（`api-version: 1.21`）

## 指令

- 主指令：`/dueltime4`
- 別名：`/dueltime`、`/dt`、`/dt4`

> 實際子指令可在遊戲內使用 `/dt help` 與 `/dt adminhelp` 查看。

## 可選依賴

- PlaceholderAPI
- HolographicDisplays / HolographicDisplay
- DecentHolograms
- CMI
- Multiverse-Core

## 語言檔

預設內建語言（位於 `plugins/DuelTime4/languages`）：

- `zh_tw`（繁體中文）
- `zh_cn`（简体中文）
- `en_us`（English）
- `ja_jp`（日本語）
- `ko_kr`（한국어）
- `fr_fr`（Français）
- `es_es`（Español）
- `de_de`（Deutsch）

可在 `config.yml` 的 `Message.default-language` 設定預設語言。

## 安裝方式

1. 將編譯好的 `dueltime4-*.jar` 放入伺服器 `plugins/` 目錄。
2. 啟動伺服器，讓插件自動生成設定與語言檔。
3. 依需求編輯 `plugins/DuelTime4/config.yml`。
4. 使用 `/dt reload` 重新載入設定，或重啟伺服器。

## 從原始碼編譯

```bash
mvn clean package
```

編譯完成後，輸出檔案位於：

- `target/Dueltime4-Bukkit-4.1.2.jar`

## 更新日誌

- [CHANGELOG.md](CHANGELOG.md)

## 問題回報

- Issues: <https://github.com/Kevin28576/DuelTime4/issues>

## 授權

本專案採用 Apache License 2.0 授權，詳見 [LICENSE](LICENSE)。
