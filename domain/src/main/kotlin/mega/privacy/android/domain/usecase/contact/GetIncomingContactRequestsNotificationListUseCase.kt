package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.usecase.account.GetIncomingContactRequestsUseCase
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Use case for getting a list of incoming contact request, based on times, for showing a notification.
 *
 * @property getIncomingContactRequestsUseCase [GetIncomingContactRequestsUseCase]
 */
class GetIncomingContactRequestsNotificationListUseCase @Inject constructor(
    private val getIncomingContactRequestsUseCase: GetIncomingContactRequestsUseCase,
) {

    /**
     * Invoke
     *
     * @return List of [ContactRequest]
     */
    suspend operator fun invoke(): List<ContactRequest> {
        val maxDays = 14
        val currentDate = Date(System.currentTimeMillis())

        return getIncomingContactRequestsUseCase().filter { contactRequest ->
            val ts: Long = contactRequest.modificationTime * 1000
            val crDate = Date(ts)
            val diff = currentDate.time - crDate.time
            val diffDays = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
            diffDays < maxDays
        }
    }
}