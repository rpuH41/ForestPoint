import Foundation
import UIKit
import CoreLocation
import MapboxMaps
import Turf
import ForestPointShared

// STAGE14_MAPBOX_OFFLINE
// OSM/Google continue to render the usual ONLINE map. Mapbox is initialized
// ONLY when the user explicitly downloads or opens a saved offline area.
// This TileStore is not an MBTiles archive and must not be used by MKTileOverlay.
struct ForestPointOfflineArea: Codable {
    let id: String
    let name: String
    let west: Double
    let south: Double
    let east: Double
    let north: Double
    let downloadedAt: Date
    let resourceCount: Int64

    var center: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: (north + south) / 2,
                               longitude: (east + west) / 2)
    }
}

final class IosMapboxOfflineStore {
    static let shared = IosMapboxOfflineStore()

    private let recordsKey = "ForestPointMapboxOfflineRegionsV1"
    private var currentId: String?
    private var currentCompletion: ((Result<String, Error>) -> Void)?
    private var styleDownload: Cancelable?
    private var regionDownload: Cancelable?
    private var lastResourceCount: Int64 = 0

    private init() {}

    private func localError(_ message: String) -> Error {
        NSError(domain: "ForestPoint.MapboxOffline", code: 1,
                userInfo: [NSLocalizedDescriptionKey: message])
    }

    // Public pk. token is read from the app bundle, never a secret sk. token.
    // Build-time download tokens belong outside the project in ~/.netrc.
    private func configure() throws {
        guard let url = Bundle.main.url(forResource: "LocalMapboxToken", withExtension: "plist"),
              let dict = NSDictionary(contentsOf: url) as? [String: Any],
              let token = dict["PublicAccessToken"] as? String,
              token.hasPrefix("pk."), !token.contains("YOUR_") else {
            throw localError("Настройте LocalMapboxToken.plist с публичным Mapbox-токеном pk.…")
        }
        MapboxOptions.accessToken = token
    }

    func findArea(rowId: String) -> ForestPointOfflineArea? {
        let areas = records()
        if rowId == "auto" { return areas.last }
        guard rowId.hasPrefix("mapbox:") else { return nil }
        let id = String(rowId.dropFirst("mapbox:".count))
        return areas.first(where: { $0.id == id })
    }

    func prepareSDK() throws { try configure() }

    private func records() -> [ForestPointOfflineArea] {
        guard let bytes = UserDefaults.standard.data(forKey: recordsKey) else { return [] }
        return (try? JSONDecoder().decode([ForestPointOfflineArea].self, from: bytes)) ?? []
    }

    private func writeRecords(_ records: [ForestPointOfflineArea]) {
        guard let data = try? JSONEncoder().encode(records) else { return }
        UserDefaults.standard.set(data, forKey: recordsKey)
    }

    func regionRows() -> [SharedOfflineRegion] {
        let formatter = DateFormatter()
        formatter.dateFormat = "dd.MM.yyyy"
        return records().map { item in
            SharedOfflineRegion(
                id: "mapbox:" + item.id,
                name: item.name,
                tilesCount: Int32(max(0, min(Int64(Int32.max), item.resourceCount))),
                sizeLabel: "Mapbox · офлайн",
                createdAtLabel: formatter.string(from: item.downloadedAt)
            )
        }
    }

    // Deleting a region releases its TileStore reference. Shared style resources
    // are intentionally kept because other offline regions may still need them.
    func deleteRegion(_ rowId: String) -> Bool {
        guard rowId.hasPrefix("mapbox:") else { return false }
        let id = String(rowId.dropFirst("mapbox:".count))
        guard records().contains(where: { $0.id == id }), id != currentId else { return false }
        do { try configure() } catch { return false }
        TileStore.default.removeTileRegion(forId: id)
        writeRecords(records().filter { $0.id != id })
        return true
    }

    func download(name: String,
                  west: Double, south: Double, east: Double, north: Double,
                  progress: @escaping (Float, String) -> Void,
                  completion: @escaping (Result<String, Error>) -> Void) {
        guard Thread.isMainThread else {
            DispatchQueue.main.async { [weak self] in
                self?.download(name: name, west: west, south: south,
                               east: east, north: north,
                               progress: progress, completion: completion)
            }
            return
        }
        guard currentId == nil else {
            completion(.failure(localError("Загрузка другой области уже выполняется.")))
            return
        }
        guard records().count < 5 else {
            completion(.failure(localError("Сохранено 5 областей. Удалите ненужную карту.")))
            return
        }
        guard north > south, east > west,
              (-85...85).contains(south), (-85...85).contains(north),
              (-180...180).contains(west), (-180...180).contains(east) else {
            completion(.failure(localError("Выбранная область некорректна.")))
            return
        }
        do { try configure() } catch { completion(.failure(error)); return }
        let id = "fp-mapbox-" + UUID().uuidString
        currentId = id
        currentCompletion = completion
        lastResourceCount = 0
        progress(0, "Mapbox: загружаем оформление карты…")

        let offlineManager = OfflineManager()
        guard let styleOptions = StylePackLoadOptions(
            glyphsRasterizationMode: .ideographsRasterizedLocally,
            metadata: ["app": "ForestPoint"],
            acceptExpired: true
        ) else {
            finish(id: id, result: .failure(localError("Не удалось подготовить стиль Mapbox.")))
            return
        }
        styleDownload = offlineManager.loadStylePack(for: .outdoors,
                loadOptions: styleOptions) { value in
            DispatchQueue.main.async { [weak self] in
                guard self?.currentId == id else { return }
                let total = max(1, value.requiredResourceCount)
                let fraction = Float(value.completedResourceCount) / Float(total)
                progress(0.15 * fraction, "Mapbox: ресурсы стиля…")
            }
        } completion: { [weak self] result in
            DispatchQueue.main.async {
                guard let self = self, self.currentId == id else { return }
                self.styleDownload = nil
                switch result {
                case .failure(let error):
                    self.finish(id: id, result: .failure(error))
                case .success:
                    self.startRegion(id: id, name: name, west: west, south: south,
                                     east: east, north: north, offlineManager: offlineManager,
                                     progress: progress)
                }
            }
        }
    }

    private func startRegion(id: String, name: String,
                             west: Double, south: Double, east: Double, north: Double,
                             offlineManager: OfflineManager,
                             progress: @escaping (Float, String) -> Void) {
        guard currentId == id else { return }
        // Ring is closed and uses the exact selected rectangle from the preview.
        let ring = [
            CLLocationCoordinate2D(latitude: north, longitude: west),
            CLLocationCoordinate2D(latitude: north, longitude: east),
            CLLocationCoordinate2D(latitude: south, longitude: east),
            CLLocationCoordinate2D(latitude: south, longitude: west),
            CLLocationCoordinate2D(latitude: north, longitude: west)
        ]
        let descriptor = offlineManager.createTilesetDescriptor(for:
            TilesetDescriptorOptions(styleURI: .outdoors, zoomRange: 11...16, tilesets: nil))
        guard let options = TileRegionLoadOptions(
            geometry: .polygon(.init([ring])),
            descriptors: [descriptor],
            metadata: ["name": name, "app": "ForestPoint"],
            acceptExpired: true
        ) else {
            finish(id: id, result: .failure(localError("Не удалось подготовить область Mapbox.")))
            return
        }
        progress(0.15, "Mapbox: загружаем выбранный участок…")
        regionDownload = TileStore.default.loadTileRegion(forId: id,
                loadOptions: options) { value in
            DispatchQueue.main.async { [weak self] in
                guard let self = self, self.currentId == id else { return }
                self.lastResourceCount = Int64(value.completedResourceCount)
                let total = max(1, value.requiredResourceCount)
                let fraction = Float(value.completedResourceCount) / Float(total)
                progress(0.15 + 0.85 * fraction,
                         "Mapbox: \(value.completedResourceCount) / \(value.requiredResourceCount) ресурсов")
            }
        } completion: { [weak self] result in
            DispatchQueue.main.async {
                guard let self = self else { return }
                guard self.currentId == id else {
                    // A late successful callback from a cancelled request may publish
                    // data; make sure its tile region remains unreferenced.
                    TileStore.default.removeTileRegion(forId: id)
                    return
                }
                self.regionDownload = nil
                switch result {
                case .failure(let error):
                    TileStore.default.removeTileRegion(forId: id)
                    self.finish(id: id, result: .failure(error))
                case .success:
                    let item = ForestPointOfflineArea(
                        id: id, name: name, west: west, south: south,
                        east: east, north: north, downloadedAt: Date(),
                        resourceCount: self.lastResourceCount
                    )
                    var existing = self.records()
                    existing.append(item)
                    self.writeRecords(existing)
                    self.finish(id: id, result: .success(name))
                }
            }
        }
    }

    func cancelDownload() {
        guard let id = currentId else { return }
        currentId = nil
        let done = currentCompletion
        currentCompletion = nil
        styleDownload?.cancel()
        regionDownload?.cancel()
        styleDownload = nil
        regionDownload = nil
        TileStore.default.removeTileRegion(forId: id)
        done?(.failure(localError("Загрузка отменена.")))
    }

    private func finish(id: String, result: Result<String, Error>) {
        guard currentId == id else { return }
        currentId = nil
        styleDownload = nil
        regionDownload = nil
        let done = currentCompletion
        currentCompletion = nil
        done?(result)
    }

    private func presenter() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        guard let window = scenes.flatMap({ $0.windows }).first(where: { $0.isKeyWindow }) else {
            return nil
        }
        var controller = window.rootViewController
        while let next = controller?.presentedViewController { controller = next }
        return controller
    }

    func openDownloadedMap() {
        DispatchQueue.main.async {
            guard let presenter = self.presenter() else { return }
            let areas = self.records()
            guard !areas.isEmpty else {
                let alert = UIAlertController(title: "Офлайн-карты Mapbox",
                    message: "Сначала скачайте область через «Скачать область».",
                    preferredStyle: .alert)
                alert.addAction(UIAlertAction(title: "OK", style: .default))
                presenter.present(alert, animated: true)
                return
            }
            let sheet = UIAlertController(title: "Открыть офлайн-карту",
                message: "Выберите сохранённую область Mapbox.", preferredStyle: .actionSheet)
            for item in areas {
                sheet.addAction(UIAlertAction(title: item.name, style: .default) { _ in
                    do { try self.configure() } catch {
                        self.presentError(error.localizedDescription); return
                    }
                    let vc = ForestPointMapboxOfflineViewer(area: item)
                    vc.modalPresentationStyle = .fullScreen
                    self.presenter()?.present(vc, animated: true)
                })
            }
            sheet.addAction(UIAlertAction(title: "Отмена", style: .cancel))
            if let popover = sheet.popoverPresentationController {
                popover.sourceView = presenter.view
                popover.sourceRect = CGRect(x: presenter.view.bounds.midX,
                                            y: presenter.view.bounds.midY, width: 1, height: 1)
            }
            presenter.present(sheet, animated: true)
        }
    }

    private func presentError(_ message: String) {
        guard let presenter = presenter() else { return }
        let alert = UIAlertController(title: "Mapbox Offline", message: message,
                                      preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        presenter.present(alert, animated: true)
    }
}

private final class ForestPointMapboxOfflineViewer: UIViewController {
    private let area: ForestPointOfflineArea
    private var mapView: MapView?

    init(area: ForestPointOfflineArea) {
        self.area = area
        super.init(nibName: nil, bundle: nil)
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground
        // Read-only: never implicitly download new tile packs when the user
        // moves outside the saved region. The saved .outdoors style is used.
        MapboxMapsOptions.tileStoreUsageMode = .readOnly
        let mapOptions = MapInitOptions(
            cameraOptions: CameraOptions(center: area.center, zoom: 13),
            styleURI: .outdoors
        )
        let map = MapView(frame: .zero, mapInitOptions: mapOptions)
        map.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(map)
        NSLayoutConstraint.activate([
            map.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            map.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            map.topAnchor.constraint(equalTo: view.topAnchor),
            map.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
        mapView = map

        let close = UIButton(type: .system)
        close.setTitle("✕  Закрыть", for: .normal)
        close.backgroundColor = .secondarySystemBackground
        close.layer.cornerRadius = 12
        close.contentEdgeInsets = UIEdgeInsets(top: 10, left: 15, bottom: 10, right: 15)
        close.addTarget(self, action: #selector(closeViewer), for: .touchUpInside)
        close.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(close)
        NSLayoutConstraint.activate([
            close.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 12),
            close.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor, constant: 12)
        ])
    }
    @objc private func closeViewer() { dismiss(animated: true) }
}
