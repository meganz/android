package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.RemotePreferencesRepository
import javax.inject.Inject

/**
 * Get note to self chat preference use case
 *
 * @property remotePreferencesRepository [RemotePreferencesRepository]
 */
class GetNoteToSelfChatNewLabelPreferenceUseCase @Inject constructor(
    private val remotePreferencesRepository: RemotePreferencesRepository,
) {

    /**
     * Get note to self chat to be shown
     *
     * @return counter
     */
    suspend operator fun invoke(): Int =
        remotePreferencesRepository.getNoteToSelfChatNewLabelPreference().toInt()
}
