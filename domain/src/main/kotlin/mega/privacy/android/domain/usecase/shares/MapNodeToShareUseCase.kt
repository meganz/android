package mega.privacy.android.domain.usecase.shares

import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.shares.ShareFileNode
import mega.privacy.android.domain.entity.node.shares.ShareFolderNode
import mega.privacy.android.domain.entity.node.shares.ShareNode
import mega.privacy.android.domain.usecase.contact.GetContactUserNameFromDatabaseUseCase
import javax.inject.Inject

/**
 * Map node to share node use case
 */
class MapNodeToShareUseCase @Inject constructor(
    private val getContactUserNameFromDatabaseUseCase: GetContactUserNameFromDatabaseUseCase,
) {
    /**
     * Invoke
     *
     * @param node [TypedNode]
     * @param shareData [ShareData]
     * @return
     */
    suspend operator fun invoke(node: TypedNode, shareData: ShareData? = null): ShareNode =
        when (node) {
            is TypedFileNode -> ShareFileNode(node, shareData)
            is TypedFolderNode -> {
                ShareFolderNode(node, shareData?.let {
                    // If verified and count is 1, get the user full name from database
                    if (it.count == 1 && it.isVerified) {
                        it.copy(
                            userFullName = getContactUserNameFromDatabaseUseCase(it.user)
                        )
                    } else {
                        it
                    }
                })
            }

            else -> throw IllegalStateException("Invalid type")
        }
}