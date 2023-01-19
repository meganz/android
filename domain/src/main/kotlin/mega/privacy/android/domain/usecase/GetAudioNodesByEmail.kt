package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.TypedNode

/**
 * The use case for getting audio nodes by email
 */
interface GetAudioNodesByEmail {

    /**
     * Getting audio nodes by email
     *
     * @param email email
     * @return audio nodes
     */
    suspend operator fun invoke(email: String): List<TypedNode>?
}