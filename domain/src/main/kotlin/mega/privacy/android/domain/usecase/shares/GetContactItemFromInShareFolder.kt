package mega.privacy.android.domain.usecase.shares

import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.node.TypedFolderNode

/**
 * Get ContactItem of the owner of this Folder if its an inShare Folder
 */
fun interface GetContactItemFromInShareFolder {
    /**
     * Returns the [ContactItem] of the owner of this folder if it's a inShare Folder, null otherwise
     * @param folderNode the folder we want to fetch
     * @param skipCache if true a new fetch will be done to get contact information, if false it may return a cached info
     */
    suspend operator fun invoke(folderNode: TypedFolderNode, skipCache: Boolean): ContactItem?
}