package mega.privacy.android.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.mapper.ContactRequestMapper
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.entity.ContactRequest
import javax.inject.Inject

/**
 * Default implementation of [ContactsRepository]
 *
 * @property megaApiGateway
 */
class DefaultContactsRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val contactRequestMapper: ContactRequestMapper,
) : ContactsRepository {

    override fun monitorContactRequestUpdates(): Flow<List<ContactRequest>> =
        megaApiGateway.globalUpdates
            .filterIsInstance<GlobalUpdate.OnContactRequestsUpdate>()
            .mapNotNull { it.requests?.map(contactRequestMapper) }
}