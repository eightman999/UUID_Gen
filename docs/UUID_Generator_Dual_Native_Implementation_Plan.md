# UUID生成アプリ 同時実装計画書（Kotlin/Swift ネイティブ）

## 0. 目的 & スコープ
- v4 / v5 / v7 の UUID を高速・簡単に生成・保存できるシンプルなユーティリティアプリを Android / iOS 両プラットフォームで提供する。
- 無料ユーザーは最大 10 件まで保存可能。買い切り課金で保存数無制限。リワード広告視聴で 24 時間有効な一時保存枠を +1（同時最大 +5）。

## 1. プラットフォーム & 最低 OS バージョン
| プラットフォーム | 言語 / UI | ストレージ | 課金 | 広告 | 最低 OS |
| --- | --- | --- | --- | --- | --- |
| Android | Kotlin / Jetpack Compose | Room + DataStore | BillingClient v6+ | AdMob Rewarded | Android 8.0 (API 26) |
| iOS | Swift / SwiftUI | Core Data | StoreKit 2 | Google Mobile Ads Rewarded | iOS 15 |

## 2. 機能一覧
### MVP
- UUID 生成: v4, v5（namespace + name）, v7（RFC 9562 時刻ベース）
- 表記整形: ハイフン有無 / 大文字小文字 / `{}` 付与
- 保存・履歴表示・コピー・共有・削除
- 保存上限: 無料 10 件 / Pro 無制限 / 広告ボーナス（+1/24h）
- 課金（非消耗型）: `pro_unlimited`
- 広告: Rewarded 視聴成功 → ボーナス枠 +1
- 設定: 既定の生成バージョン・整形オプション
- ローカライズ: 日本語（初期）、英語（将来追加）

### 次期拡張候補
- まとめ生成 N 件（重複回避）
- 検索・並べ替え（作成時刻 / バージョン）
- CSV / JSON エクスポート
- 自動コピー ON/OFF
- v1 / v3 追加、QR 出力、ウィジェット、Siri / ショートカット

## 3. 共通仕様詳細
### 3.1 UUID サポート
- **v4:** 暗号学的擬似乱数ベース。
- **v5:** SHA-1(namespace + name) から生成。version/variant ビット補正必須。
- **v7:** UNIX ms + 乱数を RFC 9562 に準拠して組み立て、version/variant ビット補正。

### 3.2 保存上限ロジック
```kotlin
baseLimit = 10
bonusSlots = 有効なボーナス枠数（24h 有効）
isPro = 課金アンロック
capacity = isPro ? ∞ : baseLimit + bonusSlots
canAdd = currentCount < capacity
```
- ボーナス枠は付与から 24 時間有効。期限切れは起動 / 復帰 / 保存時に GC。
- 同時有効ボーナスの上限は `MAX_BONUS = 5`（定数）。

## 4. アーキテクチャ
### Android
- レイヤ: `ui (Compose)` → `domain (UseCase)` → `data (Room/DataStore)` → `platform (Billing/Ads)`
- DI: Hilt（推奨）
- DB: Room、設定: DataStore Preferences
- 課金: BillingClient v6+、広告: Google Mobile Ads SDK Rewarded

### iOS
- レイヤ: `SwiftUI` → `Domain (UseCase)` → `Repository` → `Core Data / StoreKit2 / GMA`
- DB: Core Data、設定は `UserDefaults` もしくは `AppStorage`
- 課金: StoreKit 2、広告: Google Mobile Ads SDK Rewarded

## 5. データモデル
| エンティティ | フィールド例 | 役割 |
| --- | --- | --- |
| `UuidItem` | `id`, `value`, `version`, `createdAt`, `label?`, `namespace?`, `name?`, `styleFlags` | 保存した UUID レコード |
| `BonusSlot` | `id`, `expiresAt` | 広告で獲得した一時保存枠 |
| `Entitlement` | `isPro`, `proSince`, `lastSyncAt` | 課金状態（単一レコード） |
| `Prefs` | `defaultVersion`, `hyphen`, `uppercase`, `braces` | 生成時の既定設定 |

- **Room（Android）**
  - `uuid_items(id TEXT PRIMARY KEY, value TEXT, version TEXT, created_at LONG, label TEXT?, namespace TEXT?, name TEXT?, style_flags INT)`
  - `bonus_slots(id TEXT PRIMARY KEY, expires_at LONG)`
  - `entitlement(id INT PRIMARY KEY=1, is_pro INT, pro_since LONG, last_sync LONG)`
- **Core Data（iOS）**
  - `UuidItemEntity`, `BonusSlotEntity`, `EntitlementEntity` を上記フィールド相当で定義。

## 6. 画面 & ユーザーフロー
1. **Generator（メイン）**
   - v4 / v5 / v7 生成ボタン
   - v5 用 namespace / name 入力フィールド
   - 整形オプション: ハイフン、大小文字、ブラケット
   - 生成結果の保存 or コピー
2. **History**
   - 保存数 / 上限表示（例: `8 / 10 (+2)`）
   - Pro 未購入時の CTA: 「無制限にする」「広告で +1」
   - 各行でコピー / 共有 / 削除 / 詳細遷移
3. **Detail**
   - UUID 値、生成時刻、バージョン、v5 入力値、整形オプション表示
4. **Store**
   - Pro の説明、価格、購入 / 復元ボタン
5. **Settings**
   - 既定の生成バージョン、整形オプション
   - 将来的にエクスポート / 自動コピーなど追加想定

## 7. 課金 & 広告
### 7.1 プロダクト構成
- **IAP（非消耗型）:** `pro_unlimited`（両 OS 共通 ID）
- **Rewarded 広告:** 視聴成功 → `BonusSlot` を +1、`expiresAt = now + 24h`
- 有効ボーナスの同時最大は `MAX_BONUS = 5`

### 7.2 Android 実装要点
- BillingClient で起動時に購入状態を照会し `Entitlement` を更新。
- RewardedAd の `onUserEarnedReward` で `BonusSlot` を追加。失敗時はメッセージ表示。

### 7.3 iOS 実装要点
- StoreKit 2 の `Product.products(for:)` で商品取得 → 購入 → `Transaction.updates` 監視で永続化。
- Google Mobile Ads の Rewarded 広告で報酬受領時に `BonusSlot` を追加。
- MVP はオンデバイス検証で運用し、必要に応じてサーバ検証を検討。

## 8. 実装スニペット（共通ロジック）
### 8.1 上限制御（Kotlin）
```kotlin
data class LimitState(val isPro: Boolean, val baseLimit: Int, val bonusActive: Int)

fun capacity(s: LimitState) = if (s.isPro) Int.MAX_VALUE else s.baseLimit + s.bonusActive
fun canAddItem(currentCount: Int, s: LimitState) = currentCount < capacity(s)
```

### 8.2 上限制御（Swift）
```swift
struct LimitState { let isPro: Bool; let baseLimit: Int; let bonusActive: Int }
func capacity(_ s: LimitState) -> Int { s.isPro ? .max : s.baseLimit + s.bonusActive }
func canAddItem(currentCount: Int, state: LimitState) -> Bool { currentCount < capacity(state) }
```

### 8.3 UUID 生成（Kotlin）
```kotlin
fun uuidV5(namespace: UUID, name: String): UUID { /* SHA-1 + version/variant 補正 */ }
fun uuidV7(nowMs: Long = System.currentTimeMillis(), rng: SecureRandom = SecureRandom()): UUID { /* RFC 9562 */ }
```

### 8.4 UUID 生成（Swift）
```swift
func uuidV5(namespace: UUID, name: String) -> UUID { /* SHA-1 + version/variant 補正 */ }
func uuidV7(now: UInt64 = UInt64(Date().timeIntervalSince1970 * 1000)) -> UUID { /* RFC 9562 */ }
```

## 9. データ整合 & 例外処理
- ボーナス期限切れは `expiresAt < now` のものを削除。タイミング: アプリ起動 / 復帰 / 保存処理時。
- 端末時計の巻き戻りを検出した場合は警告表示 & ボーナス無効化（任意）。
- オフライン時は生成・保存は可能だが IAP / 広告は無効。UI で明示。
- 広告在庫なし: トースト等で通知し、リトライ導線を用意。
- 購入 / 復元失敗: 状態表示とサポート導線を提供。

## 10. QA / テスト計画
- UUID バージョンごとの version / variant ビット確認、v7 の時刻境界テスト。
- 保存上限 UX: 9→10→11 件目の挙動（Pro / 非 Pro、ボーナス有無）。
- ボーナス: 付与→24h 経過→失効、最大 +5、連続視聴。
- IAP: 購入→再起動→復元、別端末同一アカウントでの確認。
- 広告: 成功 / 失敗 / 中断 / 在庫なしケース。
- パフォーマンス: 履歴 1,000 件時のスクロールを検証。

## 11. リリース & 運用
- プロダクト ID: `pro_unlimited`（両 OS）
- 想定価格帯: ¥300〜¥700（市場調査後確定）
- プライバシー: 個人情報収集なし。広告 SDK のポリシー遵守（iOS: ATT / IDFA 対応）。
- 任意: アナリティクスで生成 / 保存 / 広告視聴 / 購入イベントを記録（PII 非保持）。

## 12. マイルストーン
1. **M0 設計固定**: 仕様凍結（+枠期間 / 最大値 / 価格）
2. **M1 生成器**: v4/v5/v7 実装、整形オプション
3. **M2 ストレージ & 上限制御**: Android(Room/DataStore)、iOS(Core Data) 実装
4. **M3 UI**: Generator / History / Detail / Store / Settings（両 OS）+ コピー/共有/削除
5. **M4 課金 & 広告**: Android(Billing/Rewarded)、iOS(StoreKit2/Rewarded)
6. **M5 QA & リリース**: テスト、ストアメタデータ、プライバシー、内部テスト/TestFlight

## 13. UI 最小構成サンプル
### Android (Jetpack Compose)
```kotlin
@Composable
fun HomeScreen(vm: HomeViewModel) {
    val s by vm.uiState.collectAsState()
    Column {
        Text("${'$'}{s.count} / ${'$'}{s.capacity}" + if (!s.isPro) " (+${'$'}{s.bonus})" else "")
        Row {
            Button({ vm.genV4() }) { Text("v4") }
            Button({ vm.genV7() }) { Text("v7") }
            Button({ vm.openV5Dialog() }) { Text("v5") }
        }
        if (!s.isPro) {
            Row {
                Button({ vm.buyPro() }) { Text("無制限にする") }
                Button({ vm.watchAd() }) { Text("広告で+1") }
            }
        }
        LazyColumn { items(s.items) { ItemRow(it, vm) } }
    }
}
```

### iOS (SwiftUI)
```swift
struct HomeView: View {
    @StateObject var vm: HomeVM
    var body: some View {
        VStack {
            Text("\(vm.count) / \(vm.capacity)" + (vm.isPro ? "" : " (+\(vm.bonus))"))
            HStack {
                Button("v4") { vm.genV4() }
                Button("v7") { vm.genV7() }
                Button("v5") { vm.showV5Sheet = true }
            }
            if !vm.isPro {
                HStack {
                    Button("無制限にする") { vm.buyPro() }
                    Button("広告で+1") { vm.watchAd() }
                }
            }
            List(vm.items) { RowView(item: $0) }
        }
    }
}
```

## 14. 変更可能パラメータ
- `BASE_LIMIT` 初期値（10 件）
- `MAX_BONUS`（デフォルト 5）
- ボーナス期限（24 時間）
- 価格（¥300〜¥700）
- 既定の UUID バージョン / 整形オプション

## 15. 先に決めておくと良い点
- ボーナス枠のユーザー通知方法（期限表示、プッシュの有無）
- 広告 SDK の導入時期とテスト用端末 / アカウント
- 課金価格の最終決定とローカライズ（通貨 / 説明文）
- オフライン時の UI（ボタン無効化 or ダイアログ）
- プライバシーポリシー / 利用規約ページの用意

