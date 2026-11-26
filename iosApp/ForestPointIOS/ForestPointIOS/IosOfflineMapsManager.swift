import Foundation
import MapKit
import SQLite3
import ForestPointShared

// A raster MBTiles package must be obtained from a source that permits
// offline use. The public tile.openstreetmap.org service forbids bulk download.
// This code NEVER downloads an area or accesses Google Maps' tile cache.
final class IosOfflineMapsManager: NSObject, IosOfflineMapsBridge {
    static let shared = IosOfflineMapsManager()

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
        return packages().compactMap { url in
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
    }

    func deleteRegion(id: String) -> Bool {
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
