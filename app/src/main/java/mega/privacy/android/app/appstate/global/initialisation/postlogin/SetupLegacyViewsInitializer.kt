package mega.privacy.android.app.appstate.global.initialisation.postlogin

import mega.privacy.android.app.components.legacyfab.LegacyFabButton
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.initialisation.initialisers.PostLoginInitialiserAction
import timber.log.Timber
import javax.inject.Inject

class SetupLegacyViewsInitializer @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : PostLoginInitialiserAction({ _, _ ->
    runCatching {
        getFeatureFlagValueUseCase(AppFeatures.SingleActivity)
    }.onSuccess {
        LegacyFabButton.useNewComponentsForLegacyFabButtons(it)
    }.onFailure { Timber.e(it) }
})