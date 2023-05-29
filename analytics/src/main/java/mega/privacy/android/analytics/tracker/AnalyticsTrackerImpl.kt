package mega.privacy.android.analytics.tracker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.event.ScreenView
import mega.privacy.android.domain.entity.analytics.ScreenViewEventIdentifier
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.analytics.TrackScreenViewUseCase
import javax.inject.Inject

/**
 * Analytics tracker impl
 *
 * @property appScope
 * @property trackScreenViewUseCase
 */
class AnalyticsTrackerImpl @Inject constructor(
    @ApplicationScope private val appScope: CoroutineScope,
    private val trackScreenViewUseCase: TrackScreenViewUseCase,
) : AnalyticsTracker {
    private var currentViewId: String? = null

    override fun trackScreenView(screen: ScreenView) {
        appScope.launch {
            val identifier = ScreenViewEventIdentifier(
                name = screen.name,
                uniqueIdentifier = screen.uniqueIdentifier
            )
            trackScreenViewUseCase(identifier).also { currentViewId = it }
        }
    }

}