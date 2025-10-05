import SwiftUI

struct DetailView: View {
    let record: UuidRecord

    var body: some View {
        Form {
            Section("UUID") {
                Text(record.formattedValue)
                    .font(.system(.body, design: .monospaced))
                    .textSelection(.enabled)
            }
            Section("メタ情報") {
                LabeledContent("バージョン", value: record.version.rawValue.uppercased())
                LabeledContent("作成日時", value: record.createdAt.formatted(date: .abbreviated, time: .standard))
                if let namespace = record.namespace {
                    LabeledContent("Namespace", value: namespace)
                }
                if let name = record.name {
                    LabeledContent("Name", value: name)
                }
                if let label = record.label {
                    LabeledContent("ラベル", value: label)
                }
            }
        }
        .navigationTitle("詳細")
    }
}
