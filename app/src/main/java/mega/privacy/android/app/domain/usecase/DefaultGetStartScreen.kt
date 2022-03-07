package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import mega.privacy.android.app.domain.repository.SettingsRepository
import javax.inject.Inject

class DefaultGetStartScreen @Inject constructor(private val settingsRepository: SettingsRepository) : GetStartScreen {
    override fun invoke(): Flow<Int> {
        return flow {
            emit(settingsRepository.getStartScreen())
            emitAll(settingsRepository.monitorStartScreen())
        }
    }
}