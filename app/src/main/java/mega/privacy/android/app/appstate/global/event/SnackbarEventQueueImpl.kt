package mega.privacy.android.app.appstate.global.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.android.core.ui.model.SnackbarAttributes
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import timber.log.Timber
import javax.inject.Inject

class SnackbarEventQueueImpl @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : SnackbarEventQueue {
    private val _events = Channel<SnackbarAttributes>(Channel.UNLIMITED)
    private val isSingleActivityEnabled = MutableStateFlow<Boolean?>(null)

    init {
        applicationScope.launch {
            isSingleActivityEnabled.update {
                getFeatureFlagValueUseCase(AppFeatures.SingleActivity)
            }
        }
    }

    override val eventQueue: ReceiveChannel<SnackbarAttributes> = _events

    override suspend fun queueMessage(message: String) =
        queueMessage(SnackbarAttributes(message))

    override suspend fun queueMessage(attributes: SnackbarAttributes) {
        requireFeatureEnabled {
            _events.send(attributes)
        }
    }

    private suspend inline fun requireFeatureEnabled(send: () -> Unit) {
        // Await for the feature flag to be evaluated
        if (isSingleActivityEnabled.first { it != null } == true) {
            send()
        } else {
            Timber.e("SnackbarEventQueue: Feature SingleActivity is not enabled.")
        }
    }
}