package mega.privacy.android.data.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.model.GlobalUpdate
import javax.inject.Inject

/**
 * Default global states repository
 *
 * @property megaApiGateway
 */
internal class DefaultGlobalStatesRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
) : GlobalStatesRepository {

    @Deprecated("See documentation for individual replacements to use instead.")
    override fun monitorGlobalUpdates(): Flow<GlobalUpdate> = megaApiGateway.globalUpdates
}
