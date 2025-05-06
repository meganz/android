package mega.privacy.android.domain.usecase.setting

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import javax.inject.Inject

/**
 * Monitor auto accept setting for incoming contact requests using contact links setting use case
 */
class MonitorContactLinksOptionUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val monitorUserUpdates: MonitorUserUpdates,
) {
    /**
     * Invoke
     *
     * @return flow of changes to the setting
     */
    operator fun invoke() = flow {
        emit(settingsRepository.getContactLinksOption())

        emitAll(
            monitorUserUpdates()
                .filter { it == UserChanges.ContactLinkVerification }
                .map { settingsRepository.getContactLinksOption() }
        )
    }
}