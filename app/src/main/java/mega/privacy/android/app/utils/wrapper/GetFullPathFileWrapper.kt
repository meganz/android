package mega.privacy.android.app.utils.wrapper

import android.content.Context
import android.net.Uri

/**
 * Get full path of file wrapper
 */
interface GetFullPathFileWrapper {
    /**
     * Get full path from uri and context
     */
    fun getFullPathFromTreeUri(uri: Uri, context: Context): String?
}
