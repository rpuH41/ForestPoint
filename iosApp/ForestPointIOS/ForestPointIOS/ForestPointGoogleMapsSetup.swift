import Foundation
import GoogleMaps

/// Reads the iOS Maps API key from a git-ignored, bundled plist.
/// GMSServices.provideAPIKey must run before a GMSMapView is created.
enum ForestPointGoogleMapsSetup {
    static let enabled: Bool = {
        guard
            let url = Bundle.main.url(
                forResource: "LocalGoogleMaps",
                withExtension: "plist"
            ),
            let data = try? Data(contentsOf: url),
            let values = try? PropertyListSerialization.propertyList(
                from: data,
                format: nil
            ) as? [String: Any],
            let apiKey = values["APIKey"] as? String,
            !apiKey.isEmpty
        else {
            print("Google Maps: LocalGoogleMaps.plist or APIKey not configured")
            return false
        }

        return GMSServices.provideAPIKey(apiKey)
    }()
}
