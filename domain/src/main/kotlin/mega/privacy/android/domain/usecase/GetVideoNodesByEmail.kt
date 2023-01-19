package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.TypedNode

/**
 * The use case for getting video nodes by email
 */
interface GetVideoNodesByEmail {

    /**
     * Getting video nodes by email
     *
     * @param email email
     * @return video nodes
     */
    suspend operator fun invoke(email: String): List<TypedNode>?
}