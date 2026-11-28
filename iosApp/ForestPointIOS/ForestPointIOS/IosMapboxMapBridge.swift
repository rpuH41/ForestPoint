import Foundation
import UIKit
import CoreLocation
import MapboxMaps
import Turf
import ForestPointShared

// STAGE16_MAIN_MAPBOX
// Mapbox is instantiated only for users who open a saved offline region.
// Online OpenStreetMap / Google Maps views are unchanged.
final class IosMapboxMapBridge: NSObject, IosOfflineMapBridge {
    static let shared = IosMapboxMapBridge()

    private var mapView: MapView?
    private var pendingPoints: [SharedMapPoint] = []
    private var annotationManager: PointAnnotationManager?
    private var tapObserver: AnyCancelable?
    private var tapListener: IosMapTapListener?
    private var chosenArea: ForestPointOfflineArea?
    private var pendingCenter: CLLocationCoordinate2D?

    func selectRegion(rowId: String) -> Bool {
        guard let area = IosMapboxOfflineStore.shared.findArea(rowId: rowId) else {
            return false
        }
        do {
            try IosMapboxOfflineStore.shared.prepareSDK()
        } catch {
            print("ForestPoint Mapbox: \(error.localizedDescription)")
            return false
        }
        chosenArea = area
        pendingCenter = nil
        if let map = mapView {
            map.mapboxMap.setCamera(to: CameraOptions(center: area.center, zoom: 13))
        }
        return true
    }

    func makeView() -> UIView {
        if let map = mapView { return map }
        if chosenArea == nil { _ = selectRegion(rowId: "auto") }
        guard let area = chosenArea else {
            let placeholder = UIView(frame: .zero)
            placeholder.backgroundColor = .secondarySystemBackground
            let label = UILabel()
            label.text = "Выберите скачанную офлайн-карту в меню слоёв"
            label.numberOfLines = 0
            label.textAlignment = .center
            label.translatesAutoresizingMaskIntoConstraints = false
            placeholder.addSubview(label)
            NSLayoutConstraint.activate([
                label.centerXAnchor.constraint(equalTo: placeholder.centerXAnchor),
                label.centerYAnchor.constraint(equalTo: placeholder.centerYAnchor),
                label.leadingAnchor.constraint(greaterThanOrEqualTo: placeholder.leadingAnchor, constant: 16),
                label.trailingAnchor.constraint(lessThanOrEqualTo: placeholder.trailingAnchor, constant: -16)
            ])
            return placeholder
        }
        // Never request missing tiles outside downloaded packs in this map mode.
        MapboxMapsOptions.tileStoreUsageMode = .readOnly
        let options = MapInitOptions(
            cameraOptions: CameraOptions(center: area.center, zoom: 13),
            styleURI: .outdoors
        )
        let map = MapView(frame: .zero, mapInitOptions: options)
        mapView = map
        annotationManager = map.annotations.makePointAnnotationManager()
        tapObserver = map.gestures.onMapTap.observe { [weak self] context in
            self?.tapListener?.onMapTap(
                latitude: context.coordinate.latitude,
                longitude: context.coordinate.longitude
            )
        }
        renderMarkers()
        if let center = pendingCenter { centerOn(latitude: center.latitude, longitude: center.longitude) }
        return map
    }

    func setMapTapListener(listener: IosMapTapListener?) {
        tapListener = listener
    }

    func updatePoints(points: [SharedMapPoint]) {
        pendingPoints = points
        renderMarkers()
    }

    private func renderMarkers() {
        guard let manager = annotationManager else { return }
        var annotations: [PointAnnotation] = []
        for point in pendingPoints {
            let coordinate = CLLocationCoordinate2D(latitude: point.latitude, longitude: point.longitude)
            guard CLLocationCoordinate2DIsValid(coordinate) else { continue }
            var annotation = PointAnnotation(coordinate: coordinate)
            annotation.image = .init(image: icon(for: point.category), name: "fp-\(point.key)")
            annotation.textField = point.displayName
            annotation.tapHandler = { [weak self] _ in
                self?.showPointTitle(point.displayName)
                return true
            }
            annotations.append(annotation)
        }
        manager.annotations = annotations
    }

    private func icon(for category: String) -> UIImage {
        let (symbol, color): (String, UIColor)
        switch category.lowercased() {
        case "mushroom": (symbol, color) = ("🍄", .brown)
        case "berry": (symbol, color) = ("🫐", .systemBlue)
        case "plant": (symbol, color) = ("🌿", .systemGreen)
        case "nut": (symbol, color) = ("🌰", .systemOrange)
        default: (symbol, color) = ("📍", .systemGray)
        }
        let size = CGSize(width: 42, height: 42)
        return UIGraphicsImageRenderer(size: size).image { _ in
            let rect = CGRect(origin: .zero, size: size)
            let path = UIBezierPath(ovalIn: rect.insetBy(dx: 1, dy: 1))
            color.setFill(); path.fill()
            UIColor.white.setStroke(); path.lineWidth = 2; path.stroke()
            let paragraph = NSMutableParagraphStyle()
            paragraph.alignment = .center
            (symbol as NSString).draw(
                in: CGRect(x: 0, y: 7, width: 42, height: 32),
                withAttributes: [.font: UIFont.systemFont(ofSize: 24), .paragraphStyle: paragraph]
            )
        }
    }

    private func showPointTitle(_ title: String) {
        guard let map = mapView, let root = map.window?.rootViewController else { return }
        var presenter = root
        while let child = presenter.presentedViewController { presenter = child }
        let alert = UIAlertController(title: title, message: nil, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        presenter.present(alert, animated: true)
    }

    func centerOn(latitude: Double, longitude: Double) {
        let coordinate = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
        guard CLLocationCoordinate2DIsValid(coordinate) else { return }
        // In offline mode, avoid centering on places outside the downloaded map.
        if let area = chosenArea {
            guard longitude >= area.west, longitude <= area.east,
                  latitude >= area.south, latitude <= area.north else { return }
        }
        pendingCenter = coordinate
        mapView?.mapboxMap.setCamera(to: CameraOptions(center: coordinate))
    }

    func zoomIn() { changeZoom(by: 1) }
    func zoomOut() { changeZoom(by: -1) }

    private func changeZoom(by change: Double) {
        guard let map = mapView else { return }
        let z = max(0, min(22, map.mapboxMap.cameraState.zoom + change))
        map.mapboxMap.setCamera(to: CameraOptions(zoom: z))
    }

    func sharePoint(latitude: Double, longitude: Double, title: String) {
        guard let map = mapView, let root = map.window?.rootViewController,
              let url = URL(string: "https://www.openstreetmap.org/?mlat=\(latitude)&mlon=\(longitude)") else { return }
        var presenter = root
        while let next = presenter.presentedViewController { presenter = next }
        let share = UIActivityViewController(activityItems: [title, url], applicationActivities: nil)
        if let popover = share.popoverPresentationController {
            popover.sourceView = map
            popover.sourceRect = CGRect(x: map.bounds.midX, y: map.bounds.midY, width: 1, height: 1)
        }
        presenter.present(share, animated: true)
    }
}
