package mega.privacy.android.shared.sync.domain

import javax.inject.Inject

/**
 * Use case to check if the Sync feature is enabled
 *
 * The use case is temporary and will be removed once the feature is confirmed to be stable
 * on all device manufacturers
 */
class IsSyncFeatureEnabledUseCase @Inject constructor() {

    /**
     * Invoke
     */
    operator fun invoke(): Boolean {
        val isNotOppoManufacturer =
            !android.os.Build.MANUFACTURER.contains("OPPO", ignoreCase = true)
        val isNotAndroid14 =
            android.os.Build.VERSION.SDK_INT != android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE

        return isNotOppoManufacturer || isNotAndroid14
    }
}