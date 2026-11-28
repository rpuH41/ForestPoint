import Foundation

import UIKit

import MapKit

import ForestPointShared

private final class ForestPointOSMTileOverlay: MKTileOverlay {

    override func loadTile(

        at path: MKTileOverlayPath,

        result: @escaping (Data?, Error?) -> Void

    ) {

        
        // Load licensed offline raster MBTiles data before trying the network.
        if let offlineTile = IosOfflineMapsManager.shared.tile(for: path) {
            result(offlineTile, nil)
            return
        }
        let url = url(forTilePath: path)

        var request = URLRequest(url: url)

        request.cachePolicy = .useProtocolCachePolicy

        request.setValue(

            "ForestPoint-iOS/1.0 (+https://github.com/rpuH41/ForestPoint)",

            forHTTPHeaderField: "User-Agent"

        )

        URLSession.shared.dataTask(with: request) {

            data, response, error in

            if let error = error {

                result(nil, error)

                return

            }

            guard

                let response = response as? HTTPURLResponse,

                response.statusCode == 200,

                let data = data

            else {

                result(nil, URLError(.badServerResponse))

                return

            }

            result(data, nil)

        }.resume()

    }

}

private final class ForestPointCategoryAnnotation: MKPointAnnotation {
    let category: String

    init(category: String) {
        self.category = category
        super.init()
    }
}

final class IosOsmMapView: NSObject,

                           IosMapBridge,

                           MKMapViewDelegate {

    private var mapView: MKMapView?

    private var pendingPoints: [SharedMapPoint] = []

    private var pointAnnotations: [ForestPointCategoryAnnotation] = []

    private var pendingCenter: CLLocationCoordinate2D?

    private var hasCenteredOnLocation = false

    func makeView() -> UIView {

        if let mapView = mapView {

            return mapView

        }

        let map = MKMapView(frame: .zero)

        let tap = UITapGestureRecognizer(

            target: self,

            action: #selector(handleMapTap(_:))

        )

        tap.cancelsTouchesInView = false

        map.addGestureRecognizer(tap)

        map.delegate = self

        map.showsUserLocation = true

        map.showsCompass = false

        map.showsScale = false

        map.isPitchEnabled = false

        let tiles = ForestPointOSMTileOverlay(

            urlTemplate:

                "https://tile.openstreetmap.org/{z}/{x}/{y}.png"

        )

        tiles.canReplaceMapContent = true

        map.addOverlay(tiles, level: .aboveLabels)

        mapView = map

        updatePoints(points: pendingPoints)

        if let center = pendingCenter {

            centerMap(on: center)

        }

        return map

    }

    func mapView(
        _ mapView: MKMapView,
        viewFor annotation: MKAnnotation
    ) -> MKAnnotationView? {
        guard let point = annotation as? ForestPointCategoryAnnotation else {
            // Keep the standard location dot and other system annotations.
            return nil
        }

        let identifier = "ForestPointCategoryMarker"
        let view = mapView.dequeueReusableAnnotationView(
            withIdentifier: identifier
        ) ?? MKAnnotationView(annotation: annotation, reuseIdentifier: identifier)

        view.annotation = annotation
        view.canShowCallout = true
        view.image = categoryMarkerImage(point.category)
        view.centerOffset = .zero
        return view
    }

    func updatePoints(points: [SharedMapPoint]) {

        pendingPoints = points

        guard let mapView = mapView else {

            return

        }

        for annotation in pointAnnotations {

            mapView.removeAnnotation(annotation)

        }

        pointAnnotations = points.map { point in

            let annotation = ForestPointCategoryAnnotation(category: point.category)

            annotation.coordinate = CLLocationCoordinate2D(

                latitude: point.latitude,

                longitude: point.longitude

            )

            annotation.title = point.displayName

            return annotation

        }

        for annotation in pointAnnotations {

            mapView.addAnnotation(annotation)

        }

    }

    func centerOn(

        latitude: Double,

        longitude: Double

    ) {

        let coordinate = CLLocationCoordinate2D(

            latitude: latitude,

            longitude: longitude

        )

        guard CLLocationCoordinate2DIsValid(coordinate) else {

            return

        }

        pendingCenter = coordinate

        centerMap(on: coordinate)

    }

    private func centerMap(

        on coordinate: CLLocationCoordinate2D

    ) {

        guard let mapView = mapView else {

            return

        }

        if hasCenteredOnLocation {

            mapView.setCenter(

                coordinate,

                animated: true

            )

        } else {

            let region = MKCoordinateRegion(

                center: coordinate,

                latitudinalMeters: 20_000,

                longitudinalMeters: 20_000

            )

            mapView.setRegion(region, animated: false)

            hasCenteredOnLocation = true

        }

    }

    func zoomIn() {

        changeZoom(by: 0.5)

    }

    func zoomOut() {

        changeZoom(by: 2.0)

    }

    private func changeZoom(by factor: Double) {

        guard let mapView = mapView else {

            return

        }

        let region = mapView.region

        let span = MKCoordinateSpan(

            latitudeDelta: min(

                180,

                max(0.001, region.span.latitudeDelta * factor)

            ),

            longitudeDelta: min(

                360,

                max(0.001, region.span.longitudeDelta * factor)

            )

        )

        mapView.setRegion(

            MKCoordinateRegion(

                center: region.center,

                span: span

            ),

            animated: true

        )

    }

    func mapView(

        _ mapView: MKMapView,

        rendererFor overlay: MKOverlay

    ) -> MKOverlayRenderer {

        if let tiles = overlay as? MKTileOverlay {

            return MKTileOverlayRenderer(

                tileOverlay: tiles

            )

        }

        return MKOverlayRenderer(overlay: overlay)

    }

    private var mapTapListener: IosMapTapListener?

    func setMapTapListener(listener: IosMapTapListener?) {

        mapTapListener = listener

    }

    @objc private func handleMapTap(

        _ recognizer: UITapGestureRecognizer

    ) {

        if recognizer.state != .ended {

            return

        }

        if let map = mapView {

            let touchPoint = recognizer.location(in: map)

            let coordinate = map.convert(

                touchPoint,

                toCoordinateFrom: map

            )

            if CLLocationCoordinate2DIsValid(coordinate) {

                mapTapListener?.onMapTap(

                    latitude: coordinate.latitude,

                    longitude: coordinate.longitude

                )

            }

        }

    }

    func sharePoint(

        latitude: Double,

        longitude: Double,

        title: String

    ) {

        let coordinate = CLLocationCoordinate2D(

            latitude: latitude,

            longitude: longitude

        )

        guard CLLocationCoordinate2DIsValid(coordinate) else {

            return

        }

        let address =

            "https://www.openstreetmap.org/" +

            "?mlat=\(latitude)&mlon=\(longitude)" +

            "#map=16/\(latitude)/\(longitude)"

        guard let url = URL(string: address),

              let mapView = mapView,

              let root = mapView.window?.rootViewController

        else {

            return

        }

        let shareController = UIActivityViewController(

            activityItems: [title, url],

            applicationActivities: nil

        )

        var presenter = root

        while let next = presenter.presentedViewController {

            presenter = next

        }

        if let popover =

            shareController.popoverPresentationController {

            popover.sourceView = mapView

            popover.sourceRect = CGRect(

                x: mapView.bounds.midX,

                y: mapView.bounds.midY,

                width: 1,

                height: 1

            )

        }

        presenter.present(

            shareController,

            animated: true

        )

    }

    private func categoryMarkerImage(_ category: String) -> UIImage? {

        let emoji: String

        let color: UIColor

        switch category.lowercased() {

        case "mushroom":

            emoji = "🍄"

            color = UIColor(red: 0.55, green: 0.38, blue: 0.24, alpha: 1.0)

        case "berry":

            emoji = "🫐"

            color = .systemBlue

        case "plant":

            emoji = "🌿"

            color = .systemGreen

        case "nut":

            emoji = "🌰"

            color = .systemOrange

        default:

            emoji = "📍"

            color = .systemGray

        }

        let size = CGSize(width: 42, height: 42)

        let renderer = UIGraphicsImageRenderer(size: size)

        return renderer.image { _ in

            let rect = CGRect(origin: .zero, size: size)

            let circlePath = UIBezierPath(ovalIn: rect)

            color.setFill()

            circlePath.fill()

            UIColor.white.setStroke()

            circlePath.lineWidth = 2

            circlePath.stroke()

            let paragraph = NSMutableParagraphStyle()

            paragraph.alignment = .center

            let attributes: [NSAttributedString.Key: Any] = [

                .font: UIFont.systemFont(ofSize: 24),

                .paragraphStyle: paragraph

            ]

            let textRect = CGRect(x: 0, y: 7, width: size.width, height: size.height)

            (emoji as NSString).draw(in: textRect, withAttributes: attributes)

        }

    }

}
