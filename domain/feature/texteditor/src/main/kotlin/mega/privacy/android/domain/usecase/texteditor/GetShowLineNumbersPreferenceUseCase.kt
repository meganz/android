package mega.privacy.android.domain.usecase.texteditor

import kotlinx.coroutines.flow.first
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case to get the persisted "show line numbers" preference for the text editor.
 */
class GetShowLineNumbersPreferenceUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(): Boolean =
        settingsRepository.monitorBooleanPreference(KEY_SHOW_LINE_NUMBERS, false).first()
}
