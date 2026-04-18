# Changelog

## 4.1.2

### Added
- 佇列匹配成功後，改為優先使用 GUI 介面進行接受 / 拒絕確認。
- 新增匹配確認 GUI 點擊事件處理，支援接受、拒絕與無效請求提示。

### Changed
- 低版本或 GUI 發生例外時，會自動回退為指令模式（`/dueltime accept`、`/dueltime decline`）。
- README 新增專案徽章並補上更新日誌入口。
- Build workflow 調整為每次 push 到任意分支都會自動執行。

### Fixed
- 補齊匹配確認相關語系鍵值（`zh_tw`），避免因缺少訊息鍵造成顯示不完整。
