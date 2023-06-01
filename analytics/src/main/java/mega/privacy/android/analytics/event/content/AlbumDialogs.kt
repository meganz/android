package mega.privacy.android.analytics.event.content

import mega.privacy.android.analytics.event.DialogInfo

/**
 * DeleteAlbumsConfirmationDialogInfo
 */
object DeleteAlbumsConfirmationDialogInfo : DialogInfo {
    override val name = "dialog_delete_albums_confirmation"
    override val uniqueIdentifier = 200
}

/**
 * RemoveLinksConfirmationDialogInfo
 */
object RemoveLinksConfirmationDialogInfo : DialogInfo {
    override val name = "dialog_remove_links_confirmation"
    override val uniqueIdentifier = 201
}

/**
 * CreateNewAlbumDialogInfo
 */
object CreateNewAlbumDialogInfo : DialogInfo {
    override val name = "dialog_create_new_album"
    override val uniqueIdentifier = 202
}