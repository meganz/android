package mega.privacy.android.domain.usecase.home

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Remove all items pinned to the Home screen.
 */
class ClearPinnedHomeItemsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke() {
        settingsRepository.clearPinnedHomeItems()
    }
}
