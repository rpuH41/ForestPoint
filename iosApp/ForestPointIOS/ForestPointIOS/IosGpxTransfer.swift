import Foundation
import UIKit
import UniformTypeIdentifiers
import ForestPointShared

/// Files / Share Sheet transport for the existing shared Kotlin GPX functions.
/// No direct access to flora.db, and no point deletion or overwriting.
final class IosGpxTransfer: NSObject, IosGpxTransport {
    static let shared = IosGpxTransfer()

    private func topController() -> UIViewController? {
        let windows = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
        guard var controller = windows.first(where: { $0.isKeyWindow })?.rootViewController else {
            return nil
        }
        while let next = controller.presentedViewController {
            controller = next
        }
        return controller
    }

    private func showMessage(_ text: String) {
        // The confirmation dialog in Compose must finish dismissing before UIKit presents.
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) { [weak self] in
            guard let controller = self?.topController() else {
                print("GPX message: no active view controller: \(text)")
                return
            }
            let alert = UIAlertController(title: "ForestPoint · GPX",
                                          message: text, preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: "OK", style: .default))
            controller.present(alert, animated: true)
        }
    }

    private func folders() throws -> (root: URL, backups: URL, imports: URL) {
        guard let root = FileManager.default.urls(
            for: .documentDirectory, in: .userDomainMask
        ).first else {
            throw GpxError.message("Documents folder is unavailable")
        }
        let backups = root.appendingPathComponent("Backups", isDirectory: true)
        let imports = root.appendingPathComponent("Import", isDirectory: true)
        try FileManager.default.createDirectory(
            at: backups, withIntermediateDirectories: true, attributes: nil
        )
        try FileManager.default.createDirectory(
            at: imports, withIntermediateDirectories: true, attributes: nil
        )
        return (root, backups, imports)
    }

    // No remote UIDocumentPicker is needed to save a backup. This prevents
    // the simulator's DocumentManager service from leaving the picker stuck.
    func shareGpx(gpxXml: String) {
        if !Thread.isMainThread {
            DispatchQueue.main.async { [weak self] in
                self?.shareGpx(gpxXml: gpxXml)
            }
            return
        }
        do {
            let (_, backups, _) = try folders()
            let stamp = DateFormatter()
            stamp.locale = Locale(identifier: "en_US_POSIX")
            stamp.dateFormat = "yyyy-MM-dd_HH-mm-ss"
            let name = "ForestPoint_\(stamp.string(from: Date()))_\(UUID().uuidString.prefix(6)).gpx"
            let url = backups.appendingPathComponent(name)
            try Data(gpxXml.utf8).write(to: url, options: .atomic)
            let size = (try? Data(contentsOf: url).count) ?? 0
            print("GPX export: saved to app Documents/Backups/\(name), bytes: \(size)")
            showMessage("GPX сохранён: \(name)\n\nОткройте приложение «Файлы» → «На iPhone» → «ForestPoint» → «Backups». Файл можно переместить или отправить оттуда.")
        } catch {
            print("GPX export error: \(error.localizedDescription)")
            showMessage("Не удалось создать GPX: \(error.localizedDescription)")
        }
    }

    func pickGpx(listener: IosGpxImportListener) {
        if !Thread.isMainThread {
            DispatchQueue.main.async { [weak self] in
                self?.pickGpx(listener: listener)
            }
            return
        }
        do {
            let (root, backups, imports) = try folders()
            let fileManager = FileManager.default
            var unique = Set<String>()
            var files = [URL]()
            for folder in [imports, backups, root] {
                let entries = try fileManager.contentsOfDirectory(
                    at: folder,
                    includingPropertiesForKeys: [.contentModificationDateKey],
                    options: [.skipsHiddenFiles]
                )
                for url in entries where url.pathExtension.lowercased() == "gpx" {
                    if unique.insert(url.path).inserted {
                        files.append(url)
                    }
                }
            }
            files.sort {
                let a = (try? $0.resourceValues(forKeys: [.contentModificationDateKey])
                    .contentModificationDate) ?? .distantPast
                let b = (try? $1.resourceValues(forKeys: [.contentModificationDateKey])
                    .contentModificationDate) ?? .distantPast
                return a > b
            }
            guard !files.isEmpty else {
                print("GPX import: no .gpx files in Documents")
                showMessage("В папке ForestPoint пока нет GPX-файлов.\n\nСкопируйте нужный .gpx через «Файлы» → «На iPhone» → «ForestPoint» → «Import», затем снова нажмите «Восстановить».")
                return
            }
            // Simple local file list — no remote DocumentManager process.
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) { [weak self] in
                guard let controller = self?.topController() else {
                    listener.onGpxImportResult(json: nil, error: "Cannot display GPX choices")
                    return
                }
                let menu = UIAlertController(
                    title: "Восстановить GPX",
                    message: "Выберите файл. Существующие точки не удаляются.",
                    preferredStyle: .actionSheet
                )
                for url in files.prefix(30) {
                    menu.addAction(UIAlertAction(title: url.lastPathComponent,
                                                style: .default) { [weak self] _ in
                        self?.readAndImport(url: url, listener: listener)
                    })
                }
                menu.addAction(UIAlertAction(title: "Отмена", style: .cancel))
                if let popover = menu.popoverPresentationController {
                    popover.sourceView = controller.view
                    popover.sourceRect = CGRect(
                        x: controller.view.bounds.midX,
                        y: controller.view.bounds.midY,
                        width: 1, height: 1
                    )
                }
                controller.present(menu, animated: true)
            }
        } catch {
            print("GPX import listing error: \(error.localizedDescription)")
            listener.onGpxImportResult(json: nil, error: error.localizedDescription)
        }
    }

    private func readAndImport(url: URL, listener: IosGpxImportListener) {
        do {
            let values = try url.resourceValues(forKeys: [.fileSizeKey])
            if let size = values.fileSize, size > 10_000_000 {
                throw GpxError.message("GPX is too large (10 MB maximum)")
            }
            let bytes = try Data(contentsOf: url)
            if bytes.count > 10_000_000 {
                throw GpxError.message("GPX is too large (10 MB maximum)")
            }
            let parser = ForestPointGpxReader()
            let points = try parser.read(bytes)
            let jsonData = try JSONSerialization.data(
                withJSONObject: points, options: []
            )
            guard let text = String(data: jsonData, encoding: .utf8) else {
                throw GpxError.message("Cannot read imported GPX data")
            }
            print("GPX import: selected \(url.lastPathComponent); parsed \(points.count) waypoint(s)")
            listener.onGpxImportResult(json: text, error: nil)
        } catch {
            print("GPX import error: \(error.localizedDescription)")
            listener.onGpxImportResult(json: nil, error: error.localizedDescription)
        }
    }
}

private enum GpxError: LocalizedError {
    case message(String)
    var errorDescription: String? {
        if case let .message(text) = self { return text }
        return "GPX error"
    }
}

private final class ForestPointGpxReader: NSObject, XMLParserDelegate {
    private var foundRoot = false
    private var points: [[String: Any]] = []
    private var current: [String: Any]?
    private var readingTag: String?
    private var text = ""
    private var inExtensions = false
    private var failure: String?

    func read(_ bytes: Data) throws -> [[String: Any]] {
        let parser = XMLParser(data: bytes)
        parser.shouldResolveExternalEntities = false
        parser.delegate = self
        if !parser.parse() {
            throw GpxError.message(failure ?? parser.parserError?.localizedDescription ?? "Invalid GPX XML")
        }
        guard foundRoot else { throw GpxError.message("The file is not GPX") }
        guard !points.isEmpty else { throw GpxError.message("GPX contains no waypoints") }
        return points
    }

    private func localName(_ name: String) -> String {
        String(name.split(separator: ":").last ?? Substring(name))
    }

    func parser(
        _ parser: XMLParser,
        didStartElement elementName: String,
        namespaceURI: String?,
        qualifiedName qName: String?,
        attributes attributeDict: [String: String]
    ) {
        let tag = localName(elementName)
        if tag == "gpx" { foundRoot = true }
        if tag == "wpt" {
            guard foundRoot,
                  let latText = attributeDict["lat"],
                  let lonText = attributeDict["lon"],
                  let lat = Double(latText), lat.isFinite, (-90.0...90.0).contains(lat),
                  let lon = Double(lonText), lon.isFinite, (-180.0...180.0).contains(lon)
            else {
                failure = "Invalid waypoint coordinates"
                parser.abortParsing()
                return
            }
            current = [
                "latitude": lat,
                "longitude": lon,
                "name": "Point",
                "description": "",
                "timestamp": Int64(Date().timeIntervalSince1970),
                "category": "custom"
            ]
            return
        }
        guard current != nil else { return }
        if tag == "extensions" { inExtensions = true }
        if tag == "name" || tag == "desc" || tag == "time" ||
            (tag == "category" && inExtensions) {
            readingTag = tag
            text = ""
        }
    }

    func parser(_ parser: XMLParser, foundCharacters string: String) {
        if readingTag != nil { text += string }
    }

    func parser(
        _ parser: XMLParser,
        didEndElement elementName: String,
        namespaceURI: String?,
        qualifiedName qName: String?
    ) {
        let tag = localName(elementName)
        if var point = current, readingTag == tag {
            let value = text.trimmingCharacters(in: .whitespacesAndNewlines)
            switch tag {
            case "name": if !value.isEmpty { point["name"] = value }
            case "desc": point["description"] = value
            case "category": point["category"] = value.isEmpty ? "custom" : value
            case "time":
                if !value.isEmpty {
                    let iso = ISO8601DateFormatter()
                    if let date = iso.date(from: value) {
                        point["timestamp"] = Int64(date.timeIntervalSince1970)
                    } else {
                        failure = "Invalid waypoint time"
                        parser.abortParsing()
                    }
                }
            default: break
            }
            current = point
            readingTag = nil
            text = ""
        }
        if tag == "extensions" { inExtensions = false }
        if tag == "wpt", let point = current {
            points.append(point)
            current = nil
            if points.count > 5000 {
                failure = "GPX has more than 5000 waypoints"
                parser.abortParsing()
            }
        }
    }
}
