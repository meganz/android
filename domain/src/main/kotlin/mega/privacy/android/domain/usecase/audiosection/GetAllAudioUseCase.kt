package mega.privacy.android.domain.usecase.audiosection

import mega.privacy.android.domain.entity.node.TypedAudioNode
import javax.inject.Inject

/**
 * The use case for getting all audio nodes
 */
class GetAllAudioUseCase @Inject constructor() {
    /**
     * Get the all audio nodes
     */
    operator fun invoke(): List<TypedAudioNode> = emptyList()
}