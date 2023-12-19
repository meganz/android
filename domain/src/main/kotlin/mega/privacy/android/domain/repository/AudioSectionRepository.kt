package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedAudioNode

/**
 * Repository related to audios
 */
interface AudioSectionRepository {

    /**
     * Get all audios
     *
     * @param order the list order
     * @return audio node list
     */
    suspend fun getAllAudios(order: SortOrder): List<TypedAudioNode>
}