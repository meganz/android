package mega.privacy.android.domain.entity.featureflag

import mega.privacy.android.domain.entity.Feature

/**
 * Feature whose value is driven by a Firebase Remote Config parameter,
 * typically as part of a Firebase A/B Testing experiment.
 *
 * @property remoteConfigKey Key of the Remote Config parameter defined in the Firebase console
 */
interface FirebaseABTestFeature : Feature {
    val remoteConfigKey: String
}
