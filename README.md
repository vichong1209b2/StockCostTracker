# StockCostTracker

這是一個使用 Kotlin + Jetpack Compose 撰寫的 Android App 範例，功能已升級為：

- 使用 `Room` 儲存交易資料與報價快取
- 支援買進與賣出交易
- 計算平均成本
- 計算未實現損益
- 計算已實現損益
- 串接台股報價 API 更新現價
- 提供較完整的 Android Studio 專案結構

## 目前功能

- 新增買進紀錄
- 新增賣出紀錄
- 驗證賣出張數不可超過現有持股
- 依股票代號彙總投資組合
- 可設定手續費、證交稅（用於更精準損益）
- 顯示：
  - 持有張數
  - 持有股數
  - 剩餘成本
  - 平均成本
  - 目前股價
  - 未實現損益（含估算賣出費稅）
  - 已實現損益
  - 最近報價時間
- 手動更新台股現價
- 總資產統計卡片（總成本、淨市值、總損益）
- 報酬率趨勢圖（更新現價時會自動寫入資產快照）
- 刪除交易紀錄

## 專案架構

- `app/src/main/java/com/example/stockcosttracker/MainActivity.kt`
- `app/src/main/java/com/example/stockcosttracker/ui/PortfolioScreen.kt`
- `app/src/main/java/com/example/stockcosttracker/ui/PortfolioViewModel.kt`
- `app/src/main/java/com/example/stockcosttracker/data/local`
- `app/src/main/java/com/example/stockcosttracker/data/remote`
- `app/src/main/java/com/example/stockcosttracker/data/repository`
- `app/src/main/java/com/example/stockcosttracker/domain`

## 損益計算邏輯

### 買進

- 買進金額 = 成交價 × 張數 × 1000
- 買進後增加持有股數與剩餘成本

### 賣出

- 以目前平均成本計算賣出成本
- 已實現損益 = 賣出成交金額 - 賣出對應成本
- 賣出後同步降低持有股數與剩餘成本

### 平均成本

- 平均成本 = 剩餘成本 ÷ 持有股數

### 未實現損益

- 未實現損益（淨）= (目前股價 × 持有股數 - 估算賣出手續費 - 估算證交稅) - 剩餘成本

## API 說明

目前使用 FinMind 的台股技術面 API 文件作為現價查詢依據，查詢資料集為 `TaiwanStockPriceMinute`。

- 文件：[FinMind Taiwan Market Technical](https://finmind.github.io/v3/tutor/TaiwanMarket/Technical/)

說明：

- App 會依股票代號查詢最近一筆 `deal_price`
- 這筆價格用來作為目前股價
- 若網路或 API 暫時失敗，App 會保留原有交易資料

## 使用方式

1. 下載並解壓縮 `StockCostTracker.zip`
2. 用 Android Studio 開啟 `StockCostTracker` 資料夾
3. 等待 Gradle Sync
4. 執行到模擬器或 Android 手機
5. 先新增一筆買進交易
6. 若要更新現價，點擊「更新現價」
7. 若有賣出，新增賣出交易後即可看到已實現損益

## 注意事項

- 目前專案已加入 `gradlew`、`gradlew.bat` 與 `gradle/wrapper`
- 我這邊無法完整跑完 Gradle，同步測試卡在下載 Gradle 發行包時的網路逾時
- 在你本機用 Android Studio 開啟時，通常會自動完成 Gradle 下載與同步
- 若你要上架：需要自行建立 keystore 並在 `app/build.gradle.kts` 配置 signingConfigs（此範例未包含私鑰檔）

## 下一步可繼續擴充

- 加入手續費、證交稅與融資成本
- 支援多帳戶或多投資組合
- 新增圖表、月報酬與資產分布
- 串接更穩定或需授權的即時報價來源
- 加入單元測試與 UI 測試
