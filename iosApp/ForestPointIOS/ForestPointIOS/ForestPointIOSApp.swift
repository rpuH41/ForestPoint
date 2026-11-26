import SwiftUI
import ForestPointShared
import FirebaseCore

@main
struct ForestPointIOSApp: App {

    init() {
        FirebaseApp.configure()

        let googleMapsEnabled = ForestPointGoogleMapsSetup.enabled
        print("Google Maps SDK enabled: \(googleMapsEnabled)")

        prepareFloraDatabase()

        let speciesCount = IosFloraDatabaseKt.countFloraSpecies()
        print("✅ ForestPoint iOS: \(speciesCount) видов")
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

private func prepareFloraDatabase() {
    let fileManager = FileManager.default

    guard let source = Bundle.main.url(
        forResource: "flora",
        withExtension: "db"
    ) else {
        print("❌ ForestPoint: flora.db не найдена в ресурсах")
        return
    }

    do {
        let directory = try fileManager.url(
            for: .applicationSupportDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: true
        )

        let destination = directory.appendingPathComponent("flora.db")

        if !fileManager.fileExists(atPath: destination.path) {
            try fileManager.copyItem(at: source, to: destination)
            print("✅ ForestPoint: база скопирована")
        } else {
            print("✅ ForestPoint: база уже существует")
        }

        print("Путь к базе: \(destination.path)")

    } catch {
        print("❌ ForestPoint: ошибка базы — \(error)")
    }
}
