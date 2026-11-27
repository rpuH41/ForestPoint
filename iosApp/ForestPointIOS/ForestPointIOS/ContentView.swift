import SwiftUI
import UIKit
import ForestPointShared

struct ComposeView: UIViewControllerRepresentable {

    func makeUIViewController(
        context: Context
    ) -> UIViewController {

        return MainViewControllerKt.MainViewController(
            socialGateway: IosSocialForecastService(),
            mapBridge: IosOsmMapView(),
            googleMapBridge: IosGoogleMapView(),
            googleMapsEnabled: ForestPointGoogleMapsSetup.enabled,
            gpxTransport: IosGpxTransfer.shared,
            offlineMapsBridge: IosOfflineMapsManager.shared,
            offlineMapBridge: IosMapboxMapBridge.shared,
            aboutActions: IosAboutActions.shared
        )
    }

    func updateUIViewController(
        _ uiViewController: UIViewController,
        context: Context
    ) {
    }
}

struct ContentView: View {

    var body: some View {

        ComposeView()
            .background(
                Color(
                    red: 0.075,
                    green: 0.067,
                    blue: 0.082
                )
                .ignoresSafeArea()
            )
            .preferredColorScheme(.dark)
    }
}
