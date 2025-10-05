import SwiftUI

struct StoreView: View {
    @EnvironmentObject private var store: UuidStore
    @State private var isProcessing = false
    @State private var message: String?

    var body: some View {
        Form {
            Section("UUID Pro") {
                Text("保存数無制限、広告非表示（予定）")
                Button {
                    Task { await togglePro() }
                } label: {
                    if isProcessing {
                        ProgressView()
                    } else {
                        Text(store.entitlement.isPro ? "Pro を解除 (開発用)" : "Pro を購入 (テスト)")
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(isProcessing)
                Text("StoreKit 2 の実装まではローカルで状態を切り替えます")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            if let proSince = store.entitlement.proSince {
                Section("購入情報") {
                    Text("Pro since: \(proSince.formatted(date: .abbreviated, time: .shortened))")
                }
            }

            Section("ボーナス枠") {
                if store.bonusSlots.isEmpty {
                    Text("ボーナス枠はありません")
                } else {
                    ForEach(store.bonusSlots) { slot in
                        Text("有効期限: \(slot.expiresAt.formatted(date: .omitted, time: .shortened))")
                    }
                }
                Button {
                    Task { await store.addBonusSlot() }
                } label: {
                    Label("広告視聴をシミュレート", systemImage: "play.rectangle")
                }
                .disabled(store.bonusSlots.count >= LimitConstants.maxBonus)
            }
        }
        .navigationTitle("ストア")
        .alert("メッセージ", isPresented: Binding(
            get: { message != nil },
            set: { _ in message = nil }
        )) {
            Button("OK", role: .cancel) { }
        } message: {
            Text(message ?? "")
        }
    }

    private func togglePro() async {
        guard !isProcessing else { return }
        isProcessing = true
        await store.togglePro()
        isProcessing = false
        message = store.entitlement.isPro ? "Pro を有効化しました" : "Pro を解除しました"
    }
}
