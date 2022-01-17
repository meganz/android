package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import mega.privacy.android.app.domain.repository.SettingsRepository
import javax.inject.Inject

class DefaultShouldHideRecentActivity @Inject constructor(private val settingsRepository: SettingsRepository) : ShouldHideRecentActivity {
    override fun invoke(): Flow<Boolean> {
        return flow {
            emit(settingsRepository.shouldHideRecentActivity())
            emitAll(settingsRepository.monitorHideRecentActivity())
        }
    }
}