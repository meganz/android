package mega.privacy.android.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.mapper.ContactRequestMapper
import mega.privacy.android.app.data.mapper.UserUpdateMapper
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.repository.ContactsRepository
import nz.mega.sdk.MegaUser
import javax.inject.Inject


/**
 * Default implementation of [ContactsRepository]
 *
 * @property megaApiGateway         [MegaApiGateway]
 * @property contactRequestMapper   [ContactRequestMapper]
 */
class DefaultContactsRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val contactRequestMapper: ContactRequestMapper,
    private val userUpdateMapper: UserUpdateMapper,
) : ContactsRepository {

    override fun monitorContactRequestUpdates(): Flow<List<ContactRequest>> =
        megaApiGateway.globalUpdates
            .filterIsInstance<GlobalUpdate.OnContactRequestsUpdate>()
            .mapNotNull { it.requests?.map(contactRequestMapper) }

    override fun monitorContactUpdates() =
        megaApiGateway.globalUpdates
            .filterIsInstance<GlobalUpdate.OnUsersUpdate>()
            .mapNotNull { it.users }
            .map { usersList ->
                userUpdateMapper(usersList.filter { user ->
                    user.handle != megaApiGateway.myUserHandle
                            && (user.changes == 0
                            || (user.hasChanged(MegaUser.CHANGE_TYPE_AVATAR) && user.isOwnChange == 0)
                            || user.hasChanged(MegaUser.CHANGE_TYPE_FIRSTNAME)
                            || user.hasChanged(MegaUser.CHANGE_TYPE_LASTNAME)
                            || user.hasChanged(MegaUser.CHANGE_TYPE_EMAIL)
                            || user.hasChanged(MegaUser.CHANGE_TYPE_ALIAS))
                })
            }.filter { it.changes.isNotEmpty() }
}