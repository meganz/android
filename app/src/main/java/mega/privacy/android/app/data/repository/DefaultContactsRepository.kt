package mega.privacy.android.app.data.repository

import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Default implementation of [ContactsRepository]
 *
 * @property megaApiGateway
 */
class DefaultContactsRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway
) : ContactsRepository {

    override fun monitorContactRequestUpdates() =
        megaApiGateway.globalUpdates
            .filterIsInstance<GlobalUpdate.OnContactRequestsUpdate>()
            .mapNotNull { it.requests?.toList() }
}