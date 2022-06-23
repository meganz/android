package mega.privacy.android.app.data.gateway

import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.google.firebase.appdistribution.UpdateTask

/**
 * Firebase distribution gateway implementation of [DistributionGateway]
 *
 * @constructor Create empty Firebase distribution gateway
 */
class FirebaseDistributionGateway : DistributionGateway{

    override fun autoUpdateIfAvailable(): UpdateTask {
        return FirebaseAppDistribution.getInstance().updateIfNewReleaseAvailable()
    }

}