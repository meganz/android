package mega.privacy.android.domain.usecase.meeting.raisehandtospeak

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case to check if the Raise to Hand suggestion has been shown.
 */
class IsRaiseToHandSuggestionShownUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * invocation function
     */
    suspend operator fun invoke() = settingsRepository.isRaiseToHandSuggestionShown() ?: false
}
