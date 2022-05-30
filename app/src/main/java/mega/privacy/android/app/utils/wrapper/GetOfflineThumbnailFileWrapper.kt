package mega.privacy.android.app.utils.wrapper

import android.content.Context
import mega.privacy.android.app.MegaOffline
import java.io.File

/**
 * Get offline thumbnail wrapper
 *
 * Temporary wrapper interface
 */
interface GetOfflineThumbnailFileWrapper {

    fun getThumbnailFile(context: Context, node: MegaOffline): File
    fun getThumbnailFile(context: Context, handle: String): File

}