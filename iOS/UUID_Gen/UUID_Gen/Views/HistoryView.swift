import SwiftUI

struct HistoryView: View {
    @EnvironmentObject private var store: UuidStore
    @State private var errorMessage: String?

    var body: some View {
        List {
            summarySection
            if store.items.isEmpty {
                ContentUnavailableView(
                    "保存された UUID はありません",
                    systemImage: "tray",
                    description: Text("生成画面から保存できます")
                )
            } else {
                ForEach(store.items) { item in
                    NavigationLink(value: item) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(item.formattedValue)
                                .font(.system(.body, design: .monospaced))
                            HStack(spacing: 12) {
                                Text(item.version.rawValue.uppercased())
                                Text(item.createdAt.formatted(date: .abbreviated, time: .shortened))
                            }
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            if let label = item.label, !label.isEmpty {
                                Text(label)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                    .swipeActions(edge: .trailing) {
                        Button(role: .destructive) {
                            Task { await store.delete(item) }
                        } label: {
                            Label("削除", systemImage: "trash")
                        }
                        ShareLink(item: item.formattedValue) {
                            Label("共有", systemImage: "square.and.arrow.up")
                        }
                        .tint(.blue)
                    }
                }
            }
        }
        .navigationDestination(for: UuidRecord.self) { item in
            DetailView(record: item)
        }
        .navigationTitle("履歴")
        .toolbar {
            ToolbarItemGroup(placement: .navigationBarTrailing) {
                if !store.entitlement.isPro {
                    Button("広告で +1") {
                        Task { await store.addBonusSlot() }
                    }
                }
                Button(role: .destructive) {
                    Task { await store.removeAll() }
                } label: {
                    Image(systemName: "trash")
                }
                .disabled(store.items.isEmpty)
            }
        }
        .alert("エラー", isPresented: Binding(
            get: { errorMessage != nil },
            set: { _ in errorMessage = nil }
        )) {
            Button("OK", role: .cancel) { }
        } message: {
            Text(errorMessage ?? "")
        }
        .refreshable {
            await store.refresh()
        }
    }

    private var summarySection: some View {
        Section {
            VStack(alignment: .leading, spacing: 4) {
                if store.entitlement.isPro {
                    Text("保存数: \(store.items.count)")
                } else {
                    Text("保存数: \(store.items.count) / \(store.limitState.capacity)")
                    Text("ボーナス枠: \(store.bonusSlots.count) / \(LimitConstants.maxBonus)")
                }
            }
        }
    }
}
