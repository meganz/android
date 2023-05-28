package mega.privacy.android.domain.usecase.analytics

import mega.privacy.android.domain.entity.analytics.ScreenViewEvent
import mega.privacy.android.domain.entity.analytics.ScreenViewEventIdentifier
import javax.inject.Inject

/**
 * Track screen view use case
 *
 * @property getViewIdUseCase
 * @property trackEventUseCase
 */
class TrackScreenViewUseCase @Inject constructor(
    private val getViewIdUseCase: GetViewIdUseCase,
    private val trackEventUseCase: TrackEventUseCase,
) {

    /**
     * Invoke
     *
     * @param identifier
     * @return view id
     */
    suspend operator fun invoke(identifier: ScreenViewEventIdentifier): String{
        val viewId = getViewIdUseCase()
        trackEventUseCase(event = ScreenViewEvent(identifier = identifier, viewId = viewId))
        return viewId
    }
}