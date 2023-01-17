package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.UnTypedNode

/**
 * The use case for getting rubbish node
 */
fun interface GetRubbishNode {

    /**
     * Get rubbish node
     *
     * @return rubbish node
     */
    suspend operator fun invoke(): UnTypedNode?
}