package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.user.UserChanges
import javax.inject.Inject

/**
 * Default monitor auto accept QR links
 *
 * @property fetchAutoAcceptQRLinks
 * @property monitorUserUpdates
 */
class DefaultMonitorAutoAcceptQRLinks @Inject constructor(
    private val fetchAutoAcceptQRLinks: FetchAutoAcceptQRLinks,
    private val monitorUserUpdates: MonitorUserUpdates,
) : MonitorAutoAcceptQRLinks {
    override fun invoke(): Flow<Boolean> {
        return flow {
            emit(fetchAutoAcceptQRLinks())

            emitAll(
                monitorUserUpdates()
                    .filter { it == UserChanges.ContactLinkVerification }
                    .map { fetchAutoAcceptQRLinks() }
            )
        }
    }

}
