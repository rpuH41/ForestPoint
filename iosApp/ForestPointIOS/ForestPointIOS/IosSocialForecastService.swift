
import Foundation
import FirebaseAuth
import FirebaseFunctions
import ForestPointShared

struct IosSocialForecast {
    let message: String
    let species: [String]
    let postsAnalyzed: Int
}

enum IosSocialForecastServiceError: Error {
    case invalidResponse
    case authenticationFailed
}

final class IosSocialForecastService {

    private let functions = Functions.functions(
        region: "us-central1"
    )

    func load(
        latitude: Double,
        longitude: Double,
        language: String,
        completion: @escaping (
            Result<IosSocialForecast, Error>
        ) -> Void
    ) {

        // Как на Android:
        // если пользователь уже авторизован,
        // повторно входить не нужно.
        if Auth.auth().currentUser != nil {
            callForecast(
                latitude: latitude,
                longitude: longitude,
                language: language,
                completion: completion
            )
            return
        }

        // Авторизация без регистрации.
        Auth.auth().signInAnonymously { result, error in

            if let error = error {
                print("FirebaseAuth error: \(error)")
                completion(.failure(error))
                return
            }

            guard result?.user != nil else {
                completion(
                    .failure(
                        IosSocialForecastServiceError
                            .authenticationFailed
                    )
                )
                return
            }

            print("FirebaseAuth: авторизация успешна")

            // Вызываем функцию только после входа.
            self.callForecast(
                latitude: latitude,
                longitude: longitude,
                language: language,
                completion: completion
            )
        }
    }

    private func callForecast(
        latitude: Double,
        longitude: Double,
        language: String,
        completion: @escaping (
            Result<IosSocialForecast, Error>
        ) -> Void
    ) {

        let data: [String: Any] = [
            "lat": latitude,
            "lon": longitude,
            "language": language
        ]

        functions
            .httpsCallable("getForecast")
            .call(data) { result, error in

                if let error = error {
                    print(
                        "SocialForecast Firebase error: \(error)"
                    )
                    completion(.failure(error))
                    return
                }

                guard let response = result?.data
                    as? [String: Any] else {

                    completion(
                        .failure(
                            IosSocialForecastServiceError
                                .invalidResponse
                        )
                    )
                    return
                }

                let message =
                    response["message"] as? String ?? ""

                let species =
                    response["species"] as? [String] ?? []

                let posts =
                    (response["postsAnalyzed"] as? NSNumber)?
                        .intValue ?? 0

                completion(
                    .success(
                        IosSocialForecast(
                            message: message,
                            species: species,
                            postsAnalyzed: posts
                        )
                    )
                )
            }
    }
}

// Существующий мост Swift → Kotlin Multiplatform.
extension IosSocialForecastService:
    SharedSocialForecastGateway {

    func fetch(
        latitude: Double,
        longitude: Double,
        language: String,
        listener: SharedSocialForecastListener
    ) {

        load(
            latitude: latitude,
            longitude: longitude,
            language: language
        ) { result in

            DispatchQueue.main.async {

                switch result {

                case .success(let forecast):
                    listener.onSuccess(
                        message: forecast.message,
                        species: forecast.species,
                        postsAnalyzed: Int32(
                            forecast.postsAnalyzed
                        )
                    )

                case .failure(let error):
                    let nsError = error as NSError

                    let noInternet =
                        nsError.domain == NSURLErrorDomain &&
                        nsError.code ==
                            NSURLErrorNotConnectedToInternet

                    listener.onFailure(
                        noInternet: noInternet
                    )
                }
            }
        }
    }
}
