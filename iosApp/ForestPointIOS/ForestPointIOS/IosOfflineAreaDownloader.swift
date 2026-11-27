import Foundation
import UIKit
import MapKit
import SQLite3
import CoreLocation

// Stage 11. The preview map uses Apple MapKit. Downloading happens ONLY from an
// independently licensed HTTPS raster XYZ source explicitly supplied by the app owner.
// This code never prefetches tile.openstreetmap.org, Google Maps or Apple Maps tiles.
final class IosOfflineAreaDownloader: UIViewController, MKMapViewDelegate, CLLocationManagerDelegate {
    private let map = MKMapView()
    // STAGE12_LOCATION_FIX: request a fresh fix rather than relying on an empty map.userLocation.
    private let locationManager = CLLocationManager()
    private let locationButton = UIButton(type: .system)
    private var waitingForLocation = false
    // STAGE13_LOCATION_FIX: wait for a usable location, then stop updates.
    private var locationRequestActive = false
    private var locationTimeout: DispatchWorkItem?
    private let selection = UIView()
    private let estimateLabel = UILabel()
    private let statusLabel = UILabel()
    private let progress = UIProgressView(progressViewStyle: .default)
    private let downloadButton = UIButton(type: .system)
    private let closeButton = UIButton(type: .system)
    private var downloadTask: Task<Void, Never>?
    private var isDownloading = false
    private let maxTiles = 4000
    private let minZoom = 12
    private let maxZoom = 16
    private let localeRu = Locale.preferredLanguages.first?.hasPrefix("ru") == true

    private struct Bounds {
        let west: Double
        let south: Double
        let east: Double
        let north: Double
    }

    private struct Tile {
        let z: Int
        let x: Int
        let y: Int
    }

    private struct Source {
        let template: String
        let attribution: String
    }

    private enum DownloadError: LocalizedError {
        case missingSource, forbiddenSource, invalidBounds, tooManyTiles(Int), tooManyRegions
        case invalidResponse(Int), invalidImage, database, cancelled, noNetwork, empty

        var errorDescription: String? {
            switch self {
            case .missingSource:
                return "Источник офлайн-тайлов не настроен. Разработчик должен добавить LocalOfflineTileSource.plist с разрешённым HTTPS-сервером."
            case .forbiddenSource:
                return "Источник не поддерживает безопасную загрузку: требуется HTTPS и разрешение на офлайн-хранение. Нельзя использовать публичные серверы OSM или Google Maps."
            case .invalidBounds: return "Измените масштаб или переместите карту: выбранная область некорректна."
            case .tooManyTiles(let n): return "Слишком большая область: \(n) тайлов. Уменьшите область (максимум 4000)."
            case .tooManyRegions: return "Сохранено уже 5 областей. Удалите ненужную карту."
            case .invalidResponse(let code): return "Сервер карт вернул HTTP \(code). Карта не сохранена."
            case .invalidImage: return "Сервер вернул не PNG/JPEG-тайл. Карта не сохранена."
            case .database: return "Ошибка записи офлайн-карты в SQLite."
            case .cancelled: return "Загрузка отменена; неполный пакет удалён."
            case .noNetwork: return "Нет связи с сервером карт. Проверьте интернет и повторите."
            case .empty: return "В выбранной области отсутствуют тайлы."
            }
        }
    }

    private func tr(_ ru: String, _ en: String) -> String { localeRu ? ru : en }

    static func show() {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        guard let window = scenes.flatMap({ $0.windows }).first(where: { $0.isKeyWindow }) else { return }
        var root = window.rootViewController
        while let next = root?.presentedViewController { root = next }
        let screen = IosOfflineAreaDownloader()
        screen.modalPresentationStyle = .fullScreen
        root?.present(screen, animated: true)
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground

        let title = UILabel()
        title.text = tr("Скачать область карты", "Download map area")
        title.font = .preferredFont(forTextStyle: .title2)
        title.adjustsFontForContentSizeCategory = true
        title.numberOfLines = 0

        let hint = UILabel()
        hint.text = tr("Переместите карту, чтобы выбрать участок в зелёной рамке.",
                       "Move the map to select the area inside the green frame.")
        hint.font = .preferredFont(forTextStyle: .subheadline)
        hint.textColor = .secondaryLabel
        hint.numberOfLines = 0

        closeButton.setTitle(tr("Закрыть", "Close"), for: .normal)
        closeButton.addTarget(self, action: #selector(closeScreen), for: .touchUpInside)

        let header = UIStackView(arrangedSubviews: [title, closeButton])
        header.axis = .horizontal
        header.spacing = 12
        title.setContentHuggingPriority(.defaultLow, for: .horizontal)
        closeButton.setContentHuggingPriority(.required, for: .horizontal)

        let mapBox = UIView()
        mapBox.layer.cornerRadius = 14
        mapBox.clipsToBounds = true
        map.translatesAutoresizingMaskIntoConstraints = false
        mapBox.addSubview(map)
        NSLayoutConstraint.activate([
            map.leadingAnchor.constraint(equalTo: mapBox.leadingAnchor),
            map.trailingAnchor.constraint(equalTo: mapBox.trailingAnchor),
            map.topAnchor.constraint(equalTo: mapBox.topAnchor),
            map.bottomAnchor.constraint(equalTo: mapBox.bottomAnchor)
        ])
        map.delegate = self
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        map.showsUserLocation = true
        map.isRotateEnabled = false
        map.isPitchEnabled = false
        map.setRegion(MKCoordinateRegion(
            center: CLLocationCoordinate2D(latitude: 53.133562, longitude: 25.141006),
            latitudinalMeters: 20000, longitudinalMeters: 20000
        ), animated: false)

        selection.layer.borderWidth = 2.5
        selection.layer.borderColor = UIColor.systemGreen.cgColor
        selection.backgroundColor = UIColor.systemGreen.withAlphaComponent(0.06)
        selection.layer.cornerRadius = 8
        selection.isUserInteractionEnabled = false
        selection.translatesAutoresizingMaskIntoConstraints = false
        mapBox.addSubview(selection)
        NSLayoutConstraint.activate([
            selection.centerXAnchor.constraint(equalTo: mapBox.centerXAnchor),
            selection.centerYAnchor.constraint(equalTo: mapBox.centerYAnchor),
            selection.widthAnchor.constraint(equalToConstant: 245),
            selection.heightAnchor.constraint(equalToConstant: 245)
        ])

        locationButton.setTitle("◎", for: .normal)
        locationButton.titleLabel?.font = .systemFont(ofSize: 26)
        locationButton.backgroundColor = .secondarySystemBackground
        locationButton.layer.cornerRadius = 12
        locationButton.addTarget(self, action: #selector(recenter), for: .touchUpInside)
        locationButton.translatesAutoresizingMaskIntoConstraints = false
        mapBox.addSubview(locationButton)
        NSLayoutConstraint.activate([
            locationButton.trailingAnchor.constraint(equalTo: mapBox.trailingAnchor, constant: -12),
            locationButton.bottomAnchor.constraint(equalTo: mapBox.bottomAnchor, constant: -14),
            locationButton.widthAnchor.constraint(equalToConstant: 44),
            locationButton.heightAnchor.constraint(equalToConstant: 44)
        ])

        estimateLabel.textColor = .secondaryLabel
        estimateLabel.font = .preferredFont(forTextStyle: .footnote)
        estimateLabel.numberOfLines = 2
        estimateLabel.text = tr("Выберите участок карты", "Select a map area")

        statusLabel.textColor = .secondaryLabel
        statusLabel.font = .preferredFont(forTextStyle: .footnote)
        statusLabel.numberOfLines = 3
        statusLabel.text = tr("OSM и Google — онлайн. Скачанная карта откроется через Mapbox.",
                             "OSM and Google stay online. Saved areas open in Mapbox.")

        downloadButton.setTitle(tr("Скачать эту область", "Download this area"), for: .normal)
        downloadButton.backgroundColor = UIColor(red: 0.11, green: 0.37, blue: 0.13, alpha: 1)
        downloadButton.setTitleColor(.white, for: .normal)
        downloadButton.layer.cornerRadius = 12
        downloadButton.titleLabel?.font = .preferredFont(forTextStyle: .headline)
        downloadButton.addTarget(self, action: #selector(askName), for: .touchUpInside)
        downloadButton.heightAnchor.constraint(equalToConstant: 48).isActive = true

        progress.isHidden = true
        let layout = UIStackView(arrangedSubviews: [header, hint, mapBox, estimateLabel,
                                                    progress, statusLabel, downloadButton])
        layout.axis = .vertical
        layout.spacing = 11
        layout.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(layout)
        NSLayoutConstraint.activate([
            layout.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 12),
            layout.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor, constant: 18),
            layout.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor, constant: -18),
            layout.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -12),
            mapBox.heightAnchor.constraint(greaterThanOrEqualToConstant: 280)
        ])
        mapBox.setContentHuggingPriority(.defaultLow, for: .vertical)
        DispatchQueue.main.async { self.updateEstimate() }
    }

    func mapView(_ mapView: MKMapView, regionDidChangeAnimated animated: Bool) {
        updateEstimate()
    }

    private func selectedBounds() -> Bounds? {
        guard selection.bounds.width > 0 else { return nil }
        let rect = selection.convert(selection.bounds, to: map)
        let a = map.convert(CGPoint(x: rect.minX, y: rect.minY), toCoordinateFrom: map)
        let b = map.convert(CGPoint(x: rect.maxX, y: rect.maxY), toCoordinateFrom: map)
        let west = min(a.longitude, b.longitude), east = max(a.longitude, b.longitude)
        let south = max(-85.0511, min(a.latitude, b.latitude))
        let north = min(85.0511, max(a.latitude, b.latitude))
        guard north > south, east > west, east - west < 30, north - south < 30 else { return nil }
        return Bounds(west: west, south: south, east: east, north: north)
    }

    private static func tileX(_ lon: Double, _ z: Int) -> Int {
        let n = 1 << z
        return max(0, min(n - 1, Int(floor((lon + 180) / 360 * Double(n)))))
    }
    private static func tileY(_ lat: Double, _ z: Int) -> Int {
        let n = 1 << z
        let r = lat * .pi / 180
        let v = (1 - log(tan(r) + 1 / cos(r)) / .pi) / 2 * Double(n)
        return max(0, min(n - 1, Int(floor(v))))
    }

    private func tiles(in bounds: Bounds) throws -> [Tile] {
        var result: [Tile] = []
        for z in minZoom...maxZoom {
            let x0 = Self.tileX(bounds.west, z), x1 = Self.tileX(bounds.east, z)
            let y0 = Self.tileY(bounds.north, z), y1 = Self.tileY(bounds.south, z)
            for x in x0...x1 {
                for y in y0...y1 {
                    result.append(Tile(z: z, x: x, y: y))
                    if result.count > maxTiles { throw DownloadError.tooManyTiles(result.count) }
                }
            }
        }
        if result.isEmpty { throw DownloadError.empty }
        return result
    }

    private func updateEstimate() {
        guard !isDownloading else { return }
        guard let bounds = selectedBounds() else {
            estimateLabel.text = tr("Выберите меньшую область", "Select a smaller area")
            return
        }
        do {
            let count = try tiles(in: bounds).count
            let latitude = (bounds.north + bounds.south) / 2
            let area = (bounds.north - bounds.south) * 111.32 *
                (bounds.east - bounds.west) * 111.32 * cos(latitude * .pi / 180)
            estimateLabel.text = String(format: tr("Площадь ~%.1f км² · ~%d XYZ-тайлов · оценка ~%.1f МБ*",
                                                    "Area ~%.1f km² · ~%d XYZ tiles · estimate ~%.1f MB*"),
                                        abs(area), count, Double(count) * 15_000 / 1_048_576)
            downloadButton.isEnabled = true
            downloadButton.alpha = 1
        } catch {
            estimateLabel.text = error.localizedDescription
            downloadButton.isEnabled = false
            downloadButton.alpha = 0.5
        }
    }

    // A locationUnknown (code 0) failure is temporary. Keep observing updates
    // for a short window instead of failing after a single requestLocation().
    @objc private func recenter() {
        guard !waitingForLocation else { return }
        waitingForLocation = true
        locationButton.isEnabled = false
        locationButton.setTitle("…", for: .normal)

        // Do not call CLLocationManager.locationServicesEnabled() on the UI thread.
        // The authorization status and delegate callbacks are sufficient here.
        switch locationManager.authorizationStatus {
        case .notDetermined:
            locationManager.requestWhenInUseAuthorization()
        case .authorizedAlways, .authorizedWhenInUse:
            requestCurrentLocation()
        case .denied, .restricted:
            finishLocationRequest()
            showLocationPermissionAlert()
        @unknown default:
            finishLocationRequest()
            showAlert(tr("Неизвестный статус доступа к геолокации.",
                         "Unknown location permission status."))
        }
    }

    private func requestCurrentLocation() {
        guard waitingForLocation, !locationRequestActive else { return }
        locationRequestActive = true
        // CoreLocation can temporarily report locationUnknown, especially in
        // Simulator. Continuous updates allow the next valid location to arrive.
        locationManager.startUpdatingLocation()
        locationTimeout?.cancel()
        let timeout = DispatchWorkItem { [weak self] in
            guard let self = self, self.waitingForLocation else { return }
            // MapKit can have a valid location even if this manager did not.
            if let last = self.map.userLocation.location,
               last.horizontalAccuracy >= 0,
               CLLocationCoordinate2DIsValid(last.coordinate) {
                self.locationSucceeded(last)
            } else {
                self.locationFailed()
            }
        }
        locationTimeout = timeout
        DispatchQueue.main.asyncAfter(deadline: .now() + 18, execute: timeout)
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        guard waitingForLocation else { return }
        switch manager.authorizationStatus {
        case .authorizedAlways, .authorizedWhenInUse:
            requestCurrentLocation()
        case .denied, .restricted:
            finishLocationRequest()
            showLocationPermissionAlert()
        case .notDetermined:
            break
        @unknown default:
            finishLocationRequest()
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard waitingForLocation else { return }
        guard let location = locations.reversed().first(where: {
            $0.horizontalAccuracy >= 0 && CLLocationCoordinate2DIsValid($0.coordinate)
        }) else { return }
        locationSucceeded(location)
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        guard waitingForLocation else { return }
        let nsError = error as NSError
        if nsError.domain == kCLErrorDomain && nsError.code == 0 {
            // Temporary locationUnknown: keep waiting until the 18s timeout.
            print("ForestPoint offline map: awaiting location (code 0)")
            return
        }
        print("ForestPoint offline map: location error: \(error.localizedDescription)")
        locationFailed()
    }

    private func locationSucceeded(_ location: CLLocation) {
        finishLocationRequest()
        map.setCenter(location.coordinate, animated: true)
        updateEstimate()
    }

    private func finishLocationRequest() {
        locationTimeout?.cancel()
        locationTimeout = nil
        if locationRequestActive {
            locationManager.stopUpdatingLocation()
            locationRequestActive = false
        }
        waitingForLocation = false
        locationButton.isEnabled = true
        locationButton.setTitle("◎", for: .normal)
    }

    private func locationFailed() {
        finishLocationRequest()
        guard view.window != nil else { return }
        showAlert(tr(
            "Координаты пока недоступны. В Simulator выберите Features → " +
            "Location → Custom Location и задайте тестовую позицию.",
            "Location isn't available yet. In Simulator choose Features → " +
            "Location → Custom Location and set a test position."
        ))
    }

    private func showLocationPermissionAlert() {
        let alert = UIAlertController(
            title: tr("Нет доступа к геолокации", "Location access denied"),
            message: tr("Разрешите ForestPoint доступ к местоположению в настройках iPhone.",
                        "Allow ForestPoint to access location in iPhone Settings."),
            preferredStyle: .alert
        )
        alert.addAction(UIAlertAction(title: tr("Отмена", "Cancel"), style: .cancel))
        alert.addAction(UIAlertAction(title: tr("Настройки", "Settings"), style: .default) { _ in
            if let url = URL(string: UIApplication.openSettingsURLString) {
                UIApplication.shared.open(url)
            }
        })
        present(alert, animated: true)
    }

    @objc private func askName() {
        guard selectedBounds() != nil else { return }
        let alert = UIAlertController(title: tr("Название области", "Area name"),
                                      message: tr("Сохранить карту для работы без интернета", "Save map for offline use"),
                                      preferredStyle: .alert)
        alert.addTextField { field in field.placeholder = "Лес / Forest" }
        alert.addAction(UIAlertAction(title: tr("Отмена", "Cancel"), style: .cancel))
        alert.addAction(UIAlertAction(title: tr("Скачать", "Download"), style: .default) { [weak self, weak alert] _ in
            let name = alert?.textFields?.first?.text?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            if !name.isEmpty { self?.startDownload(name: name) }
        })
        present(alert, animated: true)
    }

    // STAGE14_MAPBOX_OFFLINE: official Mapbox offline packs instead of raster MBTiles.
    // Preview may use MapKit; downloaded data is displayed ONLY in a Mapbox MapView.
    private func startDownload(name: String) {
        guard !isDownloading, let bounds = selectedBounds() else { return }
        do { _ = try tiles(in: bounds) } catch { showAlert(error.localizedDescription); return }
        isDownloading = true
        isModalInPresentation = true
        downloadButton.isEnabled = false
        closeButton.setTitle(tr("Отменить", "Cancel"), for: .normal)
        progress.isHidden = false
        progress.progress = 0
        statusLabel.text = tr("Подключаем Mapbox Offline…", "Connecting to Mapbox Offline…")
        IosMapboxOfflineStore.shared.download(
            name: name, west: bounds.west, south: bounds.south,
            east: bounds.east, north: bounds.north,
            progress: { [weak self] fraction, message in
                self?.progress.progress = fraction
                self?.statusLabel.text = message
            },
            completion: { [weak self] result in
                guard let self = self else { return }
                switch result {
                case .success(let saved):
                    self.progress.progress = 1
                    self.statusLabel.text = self.tr(
                        "Сохранено: \(saved). Откройте через «Офлайн-карты → Открыть Mapbox». Выберите авиарежим для проверки.",
                        "Saved: \(saved). Open via Offline maps → Open Mapbox; test in airplane mode.")
                case .failure(let error):
                    self.statusLabel.text = error.localizedDescription
                }
                self.isDownloading = false
                self.isModalInPresentation = false
                self.closeButton.setTitle(self.tr("Закрыть", "Close"), for: .normal)
                self.downloadButton.isEnabled = true
                self.updateEstimate()
            }
        )
    }

    @objc private func closeScreen() {
        if isDownloading {
            IosMapboxOfflineStore.shared.cancelDownload()
            statusLabel.text = tr("Загрузка отменена", "Download cancelled")
        } else {
            finishLocationRequest()
            dismiss(animated: true)
        }
    }

    private func showAlert(_ message: String) {
        let alert = UIAlertController(title: tr("Офлайн-карты", "Offline maps"), message: message,
                                      preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        present(alert, animated: true)
    }
}
