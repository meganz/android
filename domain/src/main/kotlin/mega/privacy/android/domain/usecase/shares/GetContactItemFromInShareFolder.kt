package mega.privacy.android.domain.usecase.shares

import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.node.TypedFolderNode

/**
 * Get ContactItem of the owner of this Folder if its an inShare Folder
 */
fun interface GetContactItemFromInShareFolder {
    /**
     * Returns the [ContactItem] of the owner of this folder if it's a inShare Folder, null otherwise
     */
    suspend operator fun invoke(folderNode: TypedFolderNode): ContactItem?
}