package mega.privacy.android.domain.usecase.account.contactrequest

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.contacts.ContactRequestLists
import javax.inject.Inject

/**
 * Monitor contact requests
 *
 * @property getIncomingContactRequestsUseCase Get list of incoming contact requests
 * @property getOutgoingContactRequestsUseCase Get list of outgoing contact requests
 * @property monitorContactRequestUpdatesUseCase Monitor global contact request updates
 */
class MonitorContactRequestsUseCase @Inject constructor(
    private val getIncomingContactRequestsUseCase: GetIncomingContactRequestsUseCase,
    private val getOutgoingContactRequestsUseCase: GetOutgoingContactRequestsUseCase,
    private val monitorContactRequestUpdatesUseCase: MonitorContactRequestUpdatesUseCase,
) {

    /**
     * Monitor contact requests
     *
     * @return Flow of [ContactRequestLists]
     */
    operator fun invoke() = flow {
        emit(
            ContactRequestLists(
                getIncomingContactRequestsUseCase(),
                getOutgoingContactRequestsUseCase()
            )
        )

        emitAll(
            monitorContactRequestUpdatesUseCase()
                .map {
                    ContactRequestLists(
                        getIncomingContactRequestsUseCase(),
                        getOutgoingContactRequestsUseCase()
                    )
                }
        )
    }
}