import SwiftUI

#if canImport(UIKit)
import UIKit
#endif

struct GeneratorView: View {
    @EnvironmentObject private var store: UuidStore

    @AppStorage("defaultVersion") private var defaultVersionRaw: String = UUIDVersion.v7.rawValue
    @AppStorage("formatHyphen") private var defaultHyphen: Bool = true
    @AppStorage("formatUppercase") private var defaultUppercase: Bool = false
    @AppStorage("formatBraces") private var defaultBraces: Bool = false

    @State private var selectedVersion: UUIDVersion = .v7
    @State private var namespace: String = ""
    @State private var name: String = ""
    @State private var label: String = ""
    @State private var generated: GeneratedUuid?
    @State private var formattedValue: String = ""
    @State private var isGenerating: Bool = false
    @State private var errorMessage: String?
    @State private var showResultAlert: Bool = false

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            Form {
                Section("バージョン") {
                    Picker("UUID バージョン", selection: $selectedVersion) {
                        ForEach(UUIDVersion.allCases) { version in
                            VStack(alignment: .leading) {
                                Text(version.title)
                                Text(version.description)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            .tag(version)
                        }
                    }
                    .pickerStyle(.inline)
                }

                if selectedVersion == .v5 {
                    Section("名前空間 v5") {
                        TextField("Namespace UUID", text: $namespace)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                        TextField("Name", text: $name)
                            .autocorrectionDisabled()
                    }
                }

                Section("整形オプション") {
                    Toggle("ハイフンを含める", isOn: Binding(
                        get: { formatOptions.contains(.hyphen) },
                        set: { updateOption($0, option: .hyphen) }
                    ))
                    Toggle("大文字にする", isOn: Binding(
                        get: { formatOptions.contains(.uppercase) },
                        set: { updateOption($0, option: .uppercase) }
                    ))
                    Toggle("{} を付与", isOn: Binding(
                        get: { formatOptions.contains(.braces) },
                        set: { updateOption($0, option: .braces) }
                    ))
                }

                Section("ラベル (任意)") {
                    TextField("メモ", text: $label)
                }

                if let generated {
                    Section("結果") {
                        Text(formattedValue)
                            .font(.system(.title3, design: .monospaced))
                            .textSelection(.enabled)
                            .contextMenu {
                                Button("コピー") { copyResult() }
                            }
                        resultInfoView(generated: generated)
                        HStack {
                            Button {
                                copyResult()
                            } label: {
                                Label("コピー", systemImage: "doc.on.doc")
                            }
                            Spacer()
                            ShareLink(item: formattedValue, preview: SharePreview("UUID", image: Image(systemName: "square.on.square"))) {
                                Label("共有", systemImage: "square.and.arrow.up")
                            }
                        }
                        Button {
                            Task { await saveCurrent() }
                        } label: {
                            Label("保存", systemImage: "tray.and.arrow.down")
                        }
                        .disabled(!store.limitState.canAdd(currentCount: store.items.count))
                    }
                }

                Section("保存可能数") {
                    if store.entitlement.isPro {
                        Text("保存上限: 無制限")
                    } else {
                        Text("保存数: \(store.items.count) / \(store.limitState.capacity)")
                        if store.bonusSlots.count > 0 {
                            Text("ボーナス枠: \(store.bonusSlots.count) / \(LimitConstants.maxBonus)")
                        }
                    }
                    if !store.bonusSlots.isEmpty {
                        ForEach(store.bonusSlots) { slot in
                            Text("ボーナス枠 有効期限: \(slot.expiresAt.formatted(date: .omitted, time: .shortened))")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }
            .padding(.bottom, 96)

            Button {
                Task { await generate() }
            } label: {
                Image(systemName: "plus")
                    .font(.system(size: 22, weight: .bold))
                    .padding(20)
                    .foregroundStyle(.white)
                    .background(Circle().fill(Color.accentColor))
            }
            .accessibilityLabel("UUIDを生成")
            .disabled(isGenerating)
            .opacity(isGenerating ? 0.6 : 1.0)
            .padding(.trailing, 24)
            .padding(.bottom, 24)
            .shadow(radius: 4, x: 0, y: 2)
        }
        .navigationTitle("UUID 生成")
        .task {
            selectedVersion = UUIDVersion(rawValue: defaultVersionRaw) ?? .v7
        }
        .alert("生成したUUID", isPresented: $showResultAlert, actions: {
            Button("コピー") { copyResult() }
            Button("閉じる", role: .cancel) { }
        }, message: {
            Text(formattedValue)
        })
        .alert("警告", isPresented: Binding(
            get: { errorMessage != nil },
            set: { _ in errorMessage = nil }
        ), actions: {
            Button("OK", role: .cancel) { }
        }, message: {
            Text(errorMessage ?? "")
        })
    }

    private var formatOptions: UuidFormatOptions {
        var options = UuidFormatOptions()
        if defaultHyphen { options.insert(.hyphen) }
        if defaultUppercase { options.insert(.uppercase) }
        if defaultBraces { options.insert(.braces) }
        return options
    }

    private func updateOption(_ newValue: Bool, option: UuidFormatOptions) {
        switch option {
        case .hyphen: defaultHyphen = newValue
        case .uppercase: defaultUppercase = newValue
        case .braces: defaultBraces = newValue
        default: break
        }
    }

    @MainActor
    private func generate() async {
        isGenerating = true
        defer { isGenerating = false }
        do {
            let result = try UUIDGenerator.generate(version: selectedVersion, namespaceString: namespace.isEmpty ? nil : namespace, name: name.isEmpty ? nil : name)
            generated = result
            formattedValue = format(uuid: result.value, options: formatOptions)
            defaultVersionRaw = selectedVersion.rawValue
            showResultAlert = true
        } catch UUIDGenerationError.invalidNamespace {
            errorMessage = "名前空間 UUID が不正です"
        } catch UUIDGenerationError.invalidName {
            errorMessage = "Name を入力してください"
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func saveCurrent() async {
        guard let generated else { return }
        do {
            try await store.save(generated: generated, label: label.isEmpty ? nil : label, formatOptions: formatOptions)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func copyResult() {
        guard !formattedValue.isEmpty else { return }
        #if canImport(UIKit)
        UIPasteboard.general.string = formattedValue
        #endif
    }

    @ViewBuilder
    private func resultInfoView(generated: GeneratedUuid) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("バージョン: \(generated.version.rawValue.uppercased())")
            Text("生成時刻: \(generated.createdAt.formatted(date: .abbreviated, time: .standard))")
            if let namespace = generated.namespace?.uuidString {
                Text("Namespace: \(namespace)")
            }
            if let name = generated.name {
                Text("Name: \(name)")
            }
        }
        .font(.caption)
        .foregroundStyle(.secondary)
    }
}
