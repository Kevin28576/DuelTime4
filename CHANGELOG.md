# Changelog

## 4.1.3

### Added
- 無

### Changed
- 統一全專案色碼轉換流程，改為使用 Bukkit 標準 `ChatColor.translateAlternateColorCodes('&', text)`。
- 移除等待 ActionBar 與語言訊息中對特殊 Symbol 的相容轉換，避免因符號異常造成顯示或解析錯誤。
- 調整更新提示訊息的預設分隔線與前綴處理，改為穩定可讀的色碼格式。

### Fixed
- 全面移除 `\u79AE`（`禮`）相關替換邏輯，修正由非標準色碼符號引發的潛在 bug。
- 修正 Discord Webhook 訊息去色碼流程，避免因非標準符號造成內容處理異常。
- 修正主程式語言名稱顯示去色碼流程，避免啟動資訊因符號替換而出現不一致結果。

## 4.1.2

### Added
- 佇列匹配成功後，改為優先使用 GUI 介面進行接受 / 拒絕確認。
- 新增匹配確認 GUI 點擊事件處理，支援接受、拒絕與無效請求提示。
- 新增自動更新管理器（UpdateManager），支援遠端版本檢查、下載更新檔與下次重啟自動套用。
- 新增管理指令：`/dueltime update check|download|status`。
- 新增遠端更新清單檔：`network/update.yml`。
- 新增 Discord webhook 通知訊息與可選 `content` 文字（語言檔可配置，全域與事件可分開設定）。

### Changed
- 低版本或 GUI 發生例外時，會自動回退為指令模式（`/dueltime accept`、`/dueltime decline`）。
- README 新增專案徽章並補上更新日誌入口。
- Build workflow 調整為每次 push 到任意分支都會自動執行。
- `config.yml` 新增 `Updater` 區塊，可控制自動檢查、下載、SHA-256 驗證與連線逾時。
- Discord webhook 通知改為 Embed 格式，戰報與離場懲罰改用標題、欄位、顏色與時間戳顯示。
- `Dynamic.webhook` 語言結構重整為 `embed.title/description/fields`，內容更易維護。

### Fixed
- 補齊匹配確認相關語系鍵值（`zh_tw`），避免因缺少訊息鍵造成顯示不完整。
- 補齊 webhook embed 相關語系鍵值（`zh_tw`），避免缺少鍵值時顯示 fallback 文案。
- webhook `content` 預設為空字串，未設定時不會送出多餘文字訊息。
