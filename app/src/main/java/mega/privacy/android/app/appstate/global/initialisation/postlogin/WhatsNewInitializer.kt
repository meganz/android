package mega.privacy.android.app.appstate.global.initialisation.postlogin

import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.home.ShouldDisplayNewFeatureUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.PostLoginInitialiserAction
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogsEventQueue
import mega.privacy.android.navigation.destination.WhatsNewNavKey
import timber.log.Timber
import javax.inject.Inject

class WhatsNewInitializer @Inject constructor(
    private val appDialogsEventQueue: AppDialogsEventQueue,
    private val shouldDisplayNewFeatureUseCase: ShouldDisplayNewFeatureUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : PostLoginInitialiserAction(
    action = { _, _ ->
        val shouldDisplay = runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.WhatsNewFeatureDialog) && shouldDisplayNewFeatureUseCase()
        }.onFailure {
            Timber.e(it, "Failed to check new feature display")
        }.getOrDefault(false)

        if (shouldDisplay) {
            appDialogsEventQueue.emit(AppDialogEvent(WhatsNewNavKey))
        }
    }
)
