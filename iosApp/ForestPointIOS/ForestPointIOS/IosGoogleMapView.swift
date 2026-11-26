import Foundation
import UIKit
import CoreLocation
import GoogleMaps
import ForestPointShared

/// Google Maps implementation of the shared map bridge.
/// Keep IosOsmMapView.swift unchanged: switching providers happens in Compose.
final class IosGoogleMapView: NSObject, IosGoogleMapBridge, GMSMapViewDelegate {

    private var mapView: GMSMapView?
    private var markers: [GMSMarker] = []
    private var pendingPoints: [SharedMapPoint] = []
    private var pendingCenter: CLLocationCoordinate2D?
    private var layerKey = "NORMAL"
    private var mapTapListener: IosMapTapListener?

    func makeView() -> UIView {
        if let mapView = mapView {
            return mapView
        }

        let options = GMSMapViewOptions()
        options.camera = GMSCameraPosition.camera(
            withLatitude: 0,
            longitude: 0,
            zoom: 2
        )

        let map = GMSMapView(options: options)
        map.delegate = self
        map.settings.compassButton = false
        map.settings.myLocationButton = false
        map.isMyLocationEnabled = true
        mapView = map

        setGoogleLayer(layerKey: layerKey)
        updatePoints(points: pendingPoints)

        if let center = pendingCenter {
            moveTo(center, initialZoom: true)
        }
        return map
    }

    func setGoogleLayer(layerKey: String) {
        self.layerKey = layerKey
        guard let mapView = mapView else { return }

        switch layerKey {
        case "SATELLITE": mapView.mapType = .satellite
        case "HYBRID": mapView.mapType = .hybrid
        case "TERRAIN": mapView.mapType = .terrain
        default: mapView.mapType = .normal
        }
    }

    func centerOn(latitude: Double, longitude: Double) {
        let coordinate = CLLocationCoordinate2D(
            latitude: latitude,
            longitude: longitude
        )
        guard CLLocationCoordinate2DIsValid(coordinate) else { return }

        pendingCenter = coordinate
        moveTo(coordinate, initialZoom: false)
    }

    private func moveTo(
        _ coordinate: CLLocationCoordinate2D,
        initialZoom: Bool
    ) {
        guard let mapView = mapView else { return }
        if initialZoom || mapView.camera.zoom <= 2 {
            mapView.camera = GMSCameraPosition.camera(
                withTarget: coordinate,
                zoom: 11
            )
        } else {
            mapView.animate(toLocation: coordinate)
        }
    }

    func zoomIn() {
        guard let mapView = mapView else { return }
        mapView.animate(toZoom: min(21, mapView.camera.zoom + 1))
    }

    func zoomOut() {
        guard let mapView = mapView else { return }
        mapView.animate(toZoom: max(2, mapView.camera.zoom - 1))
    }
    
    
    func updatePoints(points: [SharedMapPoint]) {
        print("GOOGLE UPDATE POINTS: \(points.count)")

        for point in points {
            print(
                "GOOGLE POINT: \(point.displayName), CATEGORY: \(point.category)"
            )
        }

        pendingPoints = points


        guard let mapView = mapView else { return }

        for marker in markers {
            marker.map = nil
        }
        markers.removeAll()
        
        markers = points.map { point in
            let marker = GMSMarker(
                position: CLLocationCoordinate2D(
                    latitude: point.latitude,
                    longitude: point.longitude
                )
            )

            marker.title = point.displayName

            marker.iconView = categoryMarkerView(point.category)

            marker.groundAnchor = CGPoint(x: 0.5, y: 0.5)

            print(
                "Google marker: \(point.displayName), category: \(point.category)"
            )

            marker.map = mapView

            return marker
        }
    }


    func setMapTapListener(listener: IosMapTapListener?) {
        mapTapListener = listener
    }

    func mapView(
        _ mapView: GMSMapView,
        didTapAt coordinate: CLLocationCoordinate2D
    ) {
        guard CLLocationCoordinate2DIsValid(coordinate) else { return }
        mapTapListener?.onMapTap(
            latitude: coordinate.latitude,
            longitude: coordinate.longitude
        )
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
        guard CLLocationCoordinate2DIsValid(coordinate) else { return }
        guard let mapView = mapView,
              let root = mapView.window?.rootViewController else { return }

        var components = URLComponents(string: "https://www.google.com/maps/search/")
        components?.queryItems = [
            URLQueryItem(name: "api", value: "1"),
            URLQueryItem(name: "query", value: "\(latitude),\(longitude)")
        ]
        guard let url = components?.url else { return }

        let activity = UIActivityViewController(
            activityItems: [title, url],
            applicationActivities: nil
        )
        if let popover = activity.popoverPresentationController {
            popover.sourceView = mapView
            popover.sourceRect = CGRect(
                x: mapView.bounds.midX,
                y: mapView.bounds.midY,
                width: 1,
                height: 1
            )
        }
        var presenter = root
        while let next = presenter.presentedViewController {
            presenter = next
        }
        presenter.present(activity, animated: true)
    }
    
    
    private func categoryMarkerView(_ category: String) -> UIView {
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

        let view = UIView(
            frame: CGRect(x: 0, y: 0, width: 42, height: 42)
        )
        view.backgroundColor = color
        view.layer.cornerRadius = 21
        view.layer.borderWidth = 2
        view.layer.borderColor = UIColor.white.cgColor

        let label = UILabel(frame: view.bounds)
        label.text = emoji
        label.font = .systemFont(ofSize: 24)
        label.textAlignment = .center
        view.addSubview(label)

        return view
    }

}
