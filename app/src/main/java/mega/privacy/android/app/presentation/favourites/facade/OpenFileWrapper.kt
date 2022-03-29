package mega.privacy.android.app.presentation.favourites.facade

import android.content.Context
import android.content.Intent
import mega.privacy.android.app.interfaces.SnackbarShower
import nz.mega.sdk.MegaNode

/**
 * The interface for OpenFileHelper
 */
interface OpenFileWrapper {

    /**
     * Get Intent to open file
     * @param context Context
     * @param node mega node
     * @param isText isText
     * @param availablePlaylist play list if is available
     * @param snackbarShower SnackbarShower
     */
    fun getIntentForOpenFile(
        context: Context,
        node: MegaNode,
        isText: Boolean,
        availablePlaylist: Boolean,
        snackbarShower: SnackbarShower
    ): Intent?
}