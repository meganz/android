package mega.privacy.android.analytics.event.content

import mega.privacy.android.analytics.event.ButtonInfo

/**
 * DeleteAlbumCancelButtonInfo
 */
object DeleteAlbumCancelButtonInfo : ButtonInfo{
    override val name = "button_delete_album_cancel"
    override val screen = PhotosScreenInfo
    override val dialog = DeleteAlbumsConfirmationDialogInfo
    override val uniqueIdentifier = 200

}

/**
 * DeleteAlbumConfirmButtonInfo
 */
object DeleteAlbumConfirmButtonInfo : ButtonInfo{
    override val name = "button_delete_album_confirm"
    override val screen = PhotosScreenInfo
    override val dialog = DeleteAlbumsConfirmationDialogInfo
    override val uniqueIdentifier = 201

}