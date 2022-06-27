package mega.privacy.android.app.data.gateway

import com.google.firebase.appdistribution.UpdateTask

/**
 * Distribution gateway
 *
 * Provides QA app distribution related functionality
 */
interface DistributionGateway {
    /**
     * Auto update if available
     *
     * @return an update task to update the app
     */
    fun autoUpdateIfAvailable(): UpdateTask
}