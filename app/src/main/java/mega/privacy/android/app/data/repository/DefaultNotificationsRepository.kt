package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.gateway.api.MegaLocalStorageGateway
import mega.privacy.android.app.data.mapper.ContactRequestMapper
import mega.privacy.android.app.data.mapper.UserAlertMapper
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.UserAlert
import mega.privacy.android.domain.repository.NotificationsRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaUser
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

/**
 * Default implementation of [NotificationsRepository]
 *
 * @property megaApiGateway
 */
class DefaultNotificationsRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val contactRequestMapper: ContactRequestMapper,
    private val userAlertsMapper: UserAlertMapper,
    private val localStorageGateway: MegaLocalStorageGateway,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : NotificationsRepository {

    override fun monitorContactRequestUpdates() = megaApiGateway.globalUpdates
        .filterIsInstance<GlobalUpdate.OnContactRequestsUpdate>()
        .mapNotNull { it.requests?.map(contactRequestMapper) }

    override fun monitorUserAlerts() = megaApiGateway.globalUpdates
        .filterIsInstance<GlobalUpdate.OnUserAlertsUpdate>()
        .mapNotNull { (userAlerts) ->
            userAlerts?.map {
                withContext(dispatcher) { userAlertsMapper(it, ::provideEmail, ::provideContact) }
            }
        }

    override suspend fun getUserAlerts() = withContext(dispatcher){
        megaApiGateway.getUserAlerts().map {
            userAlertsMapper(it, ::provideEmail, ::provideContact)
        }
    }

    private suspend fun provideEmail(userId: Long): String? =
        getEmailLocally(userId) ?: fetchAndCacheEmail(userId)


    private suspend fun fetchAndCacheEmail(userId: Long): String? =
        suspendCoroutine<String?> { continuation ->
            val callback = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        continuation.resumeWith(Result.success(request.email))
                    } else {
                        Timber.e("Error getting user email: ${error.errorString}")
                        continuation.resumeWith(Result.success(null))
                    }
                }
            )
            megaApiGateway.getUserEmail(userId, callback)
        }.also {
            it?.let { localStorageGateway.setNonContactEmail(userId, it) }
        }


    private suspend fun getEmailLocally(userId: Long) =
        localStorageGateway.getNonContactByHandle(userId)?.email

    private suspend fun provideContact(email: String): Contact? =
        megaApiGateway.getContact(email)?.let {
            Contact(it.visibility == MegaUser.VISIBILITY_VISIBLE)
        }

    override suspend fun acknowledgeUserAlerts() {
        megaApiGateway.acknowledgeUserAlerts()
    }
}