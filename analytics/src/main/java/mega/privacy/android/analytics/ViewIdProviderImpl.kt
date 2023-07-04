package mega.privacy.android.analytics

import mega.privacy.android.domain.usecase.analytics.GetViewIdUseCase
import mega.privacy.mobile.analytics.event.api.ViewIdProvider
import javax.inject.Inject

internal class ViewIdProviderImpl @Inject constructor(
    private val getViewIdUseCase: GetViewIdUseCase,
) : ViewIdProvider {
    override suspend fun getViewIdentifier(): String {
        return getViewIdUseCase()
    }
}