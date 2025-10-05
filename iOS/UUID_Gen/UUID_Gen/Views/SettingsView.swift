import SwiftUI

struct SettingsView: View {
    @AppStorage("defaultVersion") private var defaultVersionRaw: String = UUIDVersion.v7.rawValue
    @AppStorage("formatHyphen") private var defaultHyphen: Bool = true
    @AppStorage("formatUppercase") private var defaultUppercase: Bool = false
    @AppStorage("formatBraces") private var defaultBraces: Bool = false

    var body: some View {
        Form {
            Section("既定の生成設定") {
                Picker("デフォルトバージョン", selection: $defaultVersionRaw) {
                    ForEach(UUIDVersion.allCases) { version in
                        Text(version.title).tag(version.rawValue)
                    }
                }
                Toggle("ハイフンを含める", isOn: $defaultHyphen)
                Toggle("大文字にする", isOn: $defaultUppercase)
                Toggle("{} を付与", isOn: $defaultBraces)
            }

            Section("情報") {
                LabeledContent("アプリバージョン", value: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0")
                LabeledContent("ビルド", value: Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1")
            }
        }
        .navigationTitle("設定")
    }
}
