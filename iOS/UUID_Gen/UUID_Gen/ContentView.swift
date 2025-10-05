import SwiftUI

struct ContentView: View {
    @StateObject private var store = UuidStore()

    var body: some View {
        TabView {
            NavigationStack {
                GeneratorView()
                    .environmentObject(store)
            }
            .tabItem {
                Label("生成", systemImage: "wand.and.stars")
            }

            NavigationStack {
                HistoryView()
                    .environmentObject(store)
            }
            .tabItem {
                Label("履歴", systemImage: "clock")
            }

            NavigationStack {
                StoreView()
                    .environmentObject(store)
            }
            .tabItem {
                Label("Pro", systemImage: "crown")
            }

            NavigationStack {
                SettingsView()
            }
            .tabItem {
                Label("設定", systemImage: "gearshape")
            }
        }
    }
}

#Preview {
    ContentView()
}
