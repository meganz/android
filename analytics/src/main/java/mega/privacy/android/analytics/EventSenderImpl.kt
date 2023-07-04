package mega.privacy.android.analytics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.analytics.SendUserJourneyEventUseCase
import mega.privacy.mobile.analytics.event.api.EventSender
import javax.inject.Inject

internal class EventSenderImpl @Inject constructor(
    private val sendUserJourneyEventUseCase: SendUserJourneyEventUseCase,
    @ApplicationScope private val scope: CoroutineScope,
) : EventSender {
    override fun sendEvent(eventId: Int, message: String, viewId: String?) {
        scope.launch {
            sendUserJourneyEventUseCase(eventId = eventId, message = message, viewId = viewId)
        }
    }
}