import Foundation
import UIKit
import UniformTypeIdentifiers
import MapKit
import SQLite3
import ForestPointShared
import Network

// A raster MBTiles package must be obtained from a source that permits
// offline use. The public tile.openstreetmap.org service forbids bulk download.
// This code NEVER downloads an area or accesses Google Maps' tile cache.
final class IosOfflineMapsManager: NSObject, IosOfflineMapsBridge {
    func selectAndDownloadRegion() {
        DispatchQueue.main.async { IosOfflineAreaDownloader.show() }
    }

    static let shared = IosOfflineMapsManager()

    // NWPathMonitor does not require CoreLocation permissions or GPS.
    private let networkMonitor = NWPathMonitor()
    private let networkLock = NSLock()
    private var hasNetworkStatus = false
    private var networkIsOffline = false

    override init() {
        super.init()
        networkMonitor.pathUpdateHandler = { [weak self] path in
            guard let self = self else { return }
            self.networkLock.lock()
            self.hasNetworkStatus = true
            self.networkIsOffline = path.status != .satisfied
            self.networkLock.unlock()
        }
        networkMonitor.start(queue: DispatchQueue(label: "ForestPoint.network-monitor"))
    }

    func isNetworkUnavailable() -> Bool {
        networkLock.lock()
        defer { networkLock.unlock() }
        return hasNetworkStatus && networkIsOffline
    }

    private func folder() -> URL? {
        guard let documents = FileManager.default.urls(
            for: .documentDirectory, in: .userDomainMask
        ).first else { return nil }
        let folder = documents.appendingPathComponent("OfflineMaps", isDirectory: true)
        do {
            try FileManager.default.createDirectory(
                at: folder, withIntermediateDirectories: true, attributes: nil
            )
            return folder
        } catch {
            print("OfflineMaps folder error: \(error)")
            return nil
        }
    }

    private func packages() -> [URL] {
        guard let folder = folder(),
              let files = try? FileManager.default.contentsOfDirectory(
                at: folder, includingPropertiesForKeys: nil,
                options: [.skipsHiddenFiles]
              ) else { return [] }
        return files
            .filter { $0.pathExtension.lowercased() == "mbtiles" }
            .sorted { $0.lastPathComponent < $1.lastPathComponent }
    }

    private func withDatabase<T>(_ url: URL, _ body: (OpaquePointer) -> T?) -> T? {
        var database: OpaquePointer?
        let result = sqlite3_open_v2(url.path, &database, SQLITE_OPEN_READONLY, nil)
        guard result == SQLITE_OK, let db = database else {
            if database != nil { sqlite3_close(database) }
            return nil
        }
        defer { sqlite3_close(db) }
        return body(db)
    }

    private func metadata(_ key: String, db: OpaquePointer) -> String? {
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(
            db, "SELECT value FROM metadata WHERE name = ? LIMIT 1",
            -1, &statement, nil
        ) == SQLITE_OK, let stmt = statement else {
            if statement != nil { sqlite3_finalize(statement) }
            return nil
        }
        defer { sqlite3_finalize(stmt) }
        // Keep the bound UTF-8 bytes alive through sqlite3_step().
        return key.withCString { keyBytes in
            sqlite3_bind_text(stmt, 1, keyBytes, -1, nil)
            guard sqlite3_step(stmt) == SQLITE_ROW,
                  let value = sqlite3_column_text(stmt, 0) else { return nil }
            return String(cString: UnsafeRawPointer(value)
                .assumingMemoryBound(to: CChar.self))
        }
    }

    private func isRaster(_ db: OpaquePointer) -> Bool {
        guard let format = metadata("format", db: db)?.lowercased() else {
            // Some raster packages omit metadata.format; tile signatures
            // are checked below before returning anything to MapKit.
            return true
        }
        return ["png", "jpg", "jpeg"].contains(format)
    }

    private func countTiles(_ db: OpaquePointer) -> Int32 {
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(db, "SELECT COUNT(*) FROM tiles", -1,
                                 &statement, nil) == SQLITE_OK,
              let stmt = statement else {
            if statement != nil { sqlite3_finalize(statement) }
            return 0
        }
        defer { sqlite3_finalize(stmt) }
        guard sqlite3_step(stmt) == SQLITE_ROW else { return 0 }
        let count = sqlite3_column_int64(stmt, 0)
        return Int32(min(Int64(Int32.max), max(0, count)))
    }

    func listRegions() -> [SharedOfflineRegion] {
        let dateFormat = DateFormatter()
        dateFormat.dateFormat = "dd.MM.yyyy"
        let imported = packages().compactMap { url in
            withDatabase(url) { db -> SharedOfflineRegion? in
                guard isRaster(db) else { return nil }
                let size = (try? url.resourceValues(forKeys: [.fileSizeKey]).fileSize) ?? 0
                let modified = (try? url.resourceValues(
                    forKeys: [.contentModificationDateKey]
                ).contentModificationDate) ?? Date()
                let name = metadata("name", db: db) ?? url.deletingPathExtension().lastPathComponent
                return SharedOfflineRegion(
                    id: url.lastPathComponent,
                    name: name,
                    tilesCount: countTiles(db),
                    sizeLabel: String(format: "%.1f MB", Double(size) / 1048576.0),
                    createdAtLabel: dateFormat.string(from: modified)
                )
            }
        }
        return imported + IosMapboxOfflineStore.shared.regionRows()
    }

    func openDownloadedMap() {
        IosMapboxOfflineStore.shared.openDownloadedMap()
    }

    func deleteRegion(id: String) -> Bool {
        if id.hasPrefix("mapbox:") {
            return IosMapboxOfflineStore.shared.deleteRegion(id)
        }
        // Never allow deleting outside the dedicated offline map folder.
        guard id == (id as NSString).lastPathComponent,
              id.lowercased().hasSuffix(".mbtiles"),
              let directory = folder() else { return false }
        let url = directory.appendingPathComponent(id)
        do {
            try FileManager.default.removeItem(at: url)
            return true
        } catch {
            print("OfflineMaps removal error: \(error)")
            return false
        }
    }

    // Called by MKTileOverlay. The MBTiles specification uses TMS Y indexing;
    // some external archives declare XYZ in metadata.scheme.
    func tile(for path: MKTileOverlayPath) -> Data? {
        guard (0...22).contains(path.z), path.x >= 0, path.y >= 0 else { return nil }
        let maxIndex = (Int64(1) << path.z) - 1
        guard Int64(path.x) <= maxIndex, Int64(path.y) <= maxIndex else { return nil }
        for url in packages() {
            let data: Data? = withDatabase(url) { db in
                guard isRaster(db) else { return nil }
                let scheme = metadata("scheme", db: db)?.lowercased()
                let tileY = scheme == "xyz" ? Int64(path.y) : maxIndex - Int64(path.y)
                var statement: OpaquePointer?
                guard sqlite3_prepare_v2(db,
                    "SELECT tile_data FROM tiles WHERE zoom_level=? AND tile_column=? AND tile_row=? LIMIT 1",
                    -1, &statement, nil
                ) == SQLITE_OK, let stmt = statement else {
                    if statement != nil { sqlite3_finalize(statement) }
                    return nil
                }
                defer { sqlite3_finalize(stmt) }
                sqlite3_bind_int64(stmt, 1, Int64(path.z))
                sqlite3_bind_int64(stmt, 2, Int64(path.x))
                sqlite3_bind_int64(stmt, 3, tileY)
                guard sqlite3_step(stmt) == SQLITE_ROW,
                      let bytes = sqlite3_column_blob(stmt, 0) else { return nil }
                let count = Int(sqlite3_column_bytes(stmt, 0))
                guard count > 4, count < 10_000_000 else { return nil }
                let data = Data(bytes: bytes, count: count)
                // Currently support PNG and JPEG raster packages.
                let png = data.starts(with: [0x89, 0x50, 0x4E, 0x47])
                let jpeg = data.starts(with: [0xFF, 0xD8, 0xFF])
                return (png || jpeg) ? data : nil
            }
            if let data { return data }
        }
        return nil
    }
}


// MARK: - Stage 10: import raster MBTiles directly from Files
extension IosOfflineMapsManager: UIDocumentPickerDelegate {
    // Kotlin's IosOfflineMapsBridge calls this on the Settings/OfflineImport screen.
    func pickRegion() {
        DispatchQueue.main.async {
            guard let presenter = self.activeViewController() else {
                print("OfflineMaps: no available view controller for file picker")
                return
            }
            // File providers do not consistently register the .mbtiles UTType.
            // Permit browsing and validate the extension and SQLite content below.
            let picker = UIDocumentPickerViewController(
                forOpeningContentTypes: [.item], asCopy: false
            )
            picker.delegate = self
            picker.allowsMultipleSelection = false
            presenter.present(picker, animated: true)
        }
    }

    private func activeViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        guard let window = scenes.flatMap({ $0.windows }).first(where: { $0.isKeyWindow })
        else { return nil }
        var controller = window.rootViewController
        while let presented = controller?.presentedViewController {
            controller = presented
        }
        return controller
    }

    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        guard let source = urls.first else { return }
        DispatchQueue.global(qos: .userInitiated).async {
            let outcome = self.importPickedFile(source)
            DispatchQueue.main.async {
                self.showImportResult(outcome)
            }
        }
    }

    private enum ImportProblem: LocalizedError {
        case wrongExtension
        case invalidRaster
        case missingFolder
        case failedToCopy

        var errorDescription: String? {
            switch self {
            case .wrongExtension:
                return "Выбранный файл должен иметь расширение .mbtiles."
            case .invalidRaster:
                return "Архив не содержит пригодных PNG/JPEG-тайлов MBTiles. Векторные PBF не поддерживаются."
            case .missingFolder:
                return "Не удалось открыть хранилище офлайн-карт."
            case .failedToCopy:
                return "Не удалось скопировать выбранный файл."
            }
        }
    }

    private func importPickedFile(_ source: URL) -> Result<String, Error> {
        do {
            guard source.pathExtension.lowercased() == "mbtiles" else {
                throw ImportProblem.wrongExtension
            }
            guard let directory = folder() else { throw ImportProblem.missingFolder }
            let scoped = source.startAccessingSecurityScopedResource()
            defer { if scoped { source.stopAccessingSecurityScopedResource() } }

            let files = FileManager.default
            let temp = directory.appendingPathComponent(
                "incoming-\(UUID().uuidString).stage10tmp", isDirectory: false
            )
            defer { try? files.removeItem(at: temp) }

            // File-provider URLs must be read with coordination.
            let coordinator = NSFileCoordinator(filePresenter: nil)
            var coordinationError: NSError?
            var copyError: Error?
            var wasCopied = false
            coordinator.coordinate(readingItemAt: source, options: [], error: &coordinationError) { url in
                do {
                    try files.copyItem(at: url, to: temp)
                    wasCopied = true
                } catch {
                    copyError = error
                }
            }
            if let error = coordinationError { throw error }
            if let error = copyError { throw error }
            guard wasCopied else { throw ImportProblem.failedToCopy }
            guard isValidRasterMBTiles(temp) else { throw ImportProblem.invalidRaster }

            // Never replace an existing map on import. Use a distinct filename.
            let rawBase = source.deletingPathExtension().lastPathComponent
            let base = rawBase.isEmpty ? "OfflineMap" : rawBase
            var destination = directory.appendingPathComponent(base).appendingPathExtension("mbtiles")
            var suffix = 2
            while files.fileExists(atPath: destination.path) {
                destination = directory.appendingPathComponent("\(base)-\(suffix)")
                    .appendingPathExtension("mbtiles")
                suffix += 1
            }
            try files.moveItem(at: temp, to: destination)
            return .success(destination.lastPathComponent)
        } catch {
            return .failure(error)
        }
    }

    private func isValidRasterMBTiles(_ url: URL) -> Bool {
        withDatabase(url) { db -> Bool? in
            guard isRaster(db) else { return false }
            var statement: OpaquePointer?
            let sql = "SELECT tile_data FROM tiles LIMIT 1"
            guard sqlite3_prepare_v2(db, sql, -1, &statement, nil) == SQLITE_OK,
                  let stmt = statement else {
                if statement != nil { sqlite3_finalize(statement) }
                return false
            }
            defer { sqlite3_finalize(stmt) }
            guard sqlite3_step(stmt) == SQLITE_ROW,
                  let bytes = sqlite3_column_blob(stmt, 0) else { return false }
            let count = Int(sqlite3_column_bytes(stmt, 0))
            guard count >= 4 else { return false }
            let header = Data(bytes: bytes, count: min(8, count))
            return header.starts(with: [0x89, 0x50, 0x4E, 0x47]) ||
                   header.starts(with: [0xFF, 0xD8, 0xFF])
        } ?? false
    }

    private func showImportResult(_ outcome: Result<String, Error>) {
        let russian = Locale.preferredLanguages.first?.hasPrefix("ru") == true
        let title: String
        let message: String
        switch outcome {
        case .success(let filename):
            title = russian ? "Офлайн-карта добавлена" : "Offline map imported"
            message = russian
                ? "\(filename)\nВернитесь в список карт и нажмите «Обновить»."
                : "\(filename)\nReturn to the offline map list and tap Refresh."
        case .failure(let error):
            title = russian ? "Ошибка импорта" : "Import failed"
            message = error.localizedDescription
        }
        let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        activeViewController()?.present(alert, animated: true)
    }
}
