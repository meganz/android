package mega.privacy.android.app.main.ads.initialiser

import android.content.Context
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.filter
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.setting.MonitorMiscLoadedUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.AppStartInitialiserAction
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * App start initialiser that initialises MobileAds once misc flags are ready and the
 * GoogleAds feature flag is enabled. Runs on every app foreground via
 * [mega.privacy.android.app.appstate.global.initialisation.GlobalInitialiser.onAppStart],
 * and skips re-initialisation after the first successful call within the process.
 *
 * @param appContext Application context used by [MobileAds.initialize].
 * @param monitorMiscLoadedUseCase Emits true once misc flags are ready.
 * @param getFeatureFlagValueUseCase Reads the [ApiFeatures.GoogleAdsFeatureFlag] feature flag.
 */
class MobileAdsInitialiser @Inject constructor(
    @ApplicationContext appContext: Context,
    monitorMiscLoadedUseCase: MonitorMiscLoadedUseCase,
    getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : AppStartInitialiserAction(
    action = {
        monitorMiscLoadedUseCase()
            .filter { it && !isInitialised.get() }
            .collect {
                runCatching {
                    val isAdsFeatureEnabled =
                        getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag)
                    if (!isAdsFeatureEnabled) return@runCatching

                    if (isInitialised.compareAndSet(false, true)) {
                        Timber.d("Initialising MobileAds")
                        MobileAds.initialize(
                            appContext,
                            InitializationConfig.Builder("ca-app-pub-2135147798858967~2157690671")
                                .build()
                        )
                    }
                }.onFailure {
                    Timber.e(it, "MobileAds initialization failed")
                }
            }
    }
) {
    companion object {
        private val isInitialised = AtomicBoolean(false)
    }
}
