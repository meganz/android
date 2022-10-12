package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Default is hide recent activity enabled
 *
 * @property settingsRepository
 */
class DefaultIsHideRecentActivityEnabled @Inject constructor(private val settingsRepository: SettingsRepository) :
    IsHideRecentActivityEnabled {
    override fun invoke(): Flow<Boolean> {
        return flow {
            emit(settingsRepository.shouldHideRecentActivity())
            emitAll(settingsRepository.monitorHideRecentActivityEvent())
        }
    }
}