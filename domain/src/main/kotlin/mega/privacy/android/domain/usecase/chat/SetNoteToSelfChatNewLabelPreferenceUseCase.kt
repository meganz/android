package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.RemotePreferencesRepository
import javax.inject.Inject

/**
 * Set note to self chat new label use case
 *
 * @property remotePreferencesRepository [RemotePreferencesRepository]
 */
class SetNoteToSelfChatNewLabelPreferenceUseCase @Inject constructor(
    private val remotePreferencesRepository: RemotePreferencesRepository,
) {

    /**
     * Set note to self chat new label to be shown
     *
     * @param counter   [Int]
     */
    suspend operator fun invoke(counter: Int) =
        remotePreferencesRepository.setNoteToSelfChatNewLabelPreference(counter.toString())
}