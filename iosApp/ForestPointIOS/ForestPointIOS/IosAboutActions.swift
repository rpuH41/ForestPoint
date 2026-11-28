import Foundation
import UIKit
import MessageUI
import StoreKit
import ForestPointShared

/// ForestPoint: iOS equivalents of Android Settings > About the App.
/// The public policy URL and developer email intentionally match Android.
final class IosAboutActions: NSObject, IosAboutActionsBridge, MFMailComposeViewControllerDelegate {
    static let shared = IosAboutActions()

    private let contactEmail = "forestpoint.app@gmail.com"
    private let privacyURL = "https://rpuH41.github.io/forestpoint-privacy"

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

    func openPrivacyPolicy() {
        DispatchQueue.main.async { [weak self] in
            guard let self = self, let url = URL(string: self.privacyURL) else { return }
            UIApplication.shared.open(url, options: [:]) { opened in
                if !opened {
                    print("ForestPoint About: cannot open privacy policy")
                }
            }
        }
    }

    func contactDeveloper() {
        DispatchQueue.main.async { [weak self] in
            self?.presentEmail()
        }
    }

    private func presentEmail() {
        if MFMailComposeViewController.canSendMail(), let presenter = topController() {
            let composer = MFMailComposeViewController()
            composer.mailComposeDelegate = self
            composer.setToRecipients([contactEmail])
            composer.setSubject("ForestPoint — обратная связь")
            presenter.present(composer, animated: true)
            return
        }
        // No Mail account is common on the iPhone Simulator. Try the default email app.
        var components = URLComponents()
        components.scheme = "mailto"
        components.path = contactEmail
        components.queryItems = [URLQueryItem(name: "subject", value: "ForestPoint — обратная связь")]
        if let url = components.url {
            UIApplication.shared.open(url, options: [:]) { [weak self] opened in
                if !opened {
                    DispatchQueue.main.async { self?.offerCopyEmail() }
                }
            }
        } else {
            offerCopyEmail()
        }
    }

    private func offerCopyEmail() {
        guard let presenter = topController() else { return }
        let russian = Locale.preferredLanguages.first?.hasPrefix("ru") == true
        let alert = UIAlertController(
            title: russian ? "Связь с разработчиком" : "Contact developer",
            message: contactEmail,
            preferredStyle: .alert
        )
        alert.addAction(UIAlertAction(
            title: russian ? "Скопировать адрес" : "Copy email",
            style: .default,
            handler: { [weak self] _ in
                UIPasteboard.general.string = self?.contactEmail
            }
        ))
        alert.addAction(UIAlertAction(
            title: russian ? "Закрыть" : "Close",
            style: .cancel,
            handler: nil
        ))
        presenter.present(alert, animated: true)
    }

    func mailComposeController(
        _ controller: MFMailComposeViewController,
        didFinishWith result: MFMailComposeResult,
        error: Error?
    ) {
        controller.dismiss(animated: true)
    }

    func rateApp() {
        DispatchQueue.main.async {
            guard let scene = UIApplication.shared.connectedScenes
                .compactMap({ $0 as? UIWindowScene })
                .first(where: { $0.activationState == .foregroundActive }) else {
                return
            }
            // Apple controls whether/when the review sheet appears. No App Store ID required.
            SKStoreReviewController.requestReview(in: scene)
        }
    }
}
