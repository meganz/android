package mega.privacy.android.domain.usecase.texteditor

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case to persist the "show line numbers" preference for the text editor.
 */
class SetShowLineNumbersPreferenceUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(show: Boolean) {
        settingsRepository.setBooleanPreference(KEY_SHOW_LINE_NUMBERS, show)
    }
}
