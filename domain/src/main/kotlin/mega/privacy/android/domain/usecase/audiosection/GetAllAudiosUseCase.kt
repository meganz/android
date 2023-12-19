package mega.privacy.android.domain.usecase.audiosection

import mega.privacy.android.domain.entity.node.TypedAudioNode
import mega.privacy.android.domain.repository.AudioSectionRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import javax.inject.Inject

/**
 * The use case for getting all audio nodes
 */
class GetAllAudiosUseCase @Inject constructor(
    private val getCloudSortOrder: GetCloudSortOrder,
    private val audioSectionRepository: AudioSectionRepository,
) {
    /**
     * Get the all audio nodes
     */
    suspend operator fun invoke(): List<TypedAudioNode> =
        audioSectionRepository.getAllAudios(getCloudSortOrder())
}