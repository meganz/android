package mega.privacy.android.app.components.saver

import android.content.Context
import android.content.Intent
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.MegaApiUtils

class OfflineSaving(
    totalSize: Long,
    highPriority: Boolean,
    val node: MegaOffline
) : Saving(totalSize, highPriority) {

    override fun hasUnsupportedFile(context: Context): Boolean {
        return if (node.isFolder) {
            false
        } else {
            unsupportedFileName = node.name
            val checkIntent = Intent(Intent.ACTION_GET_CONTENT)
            checkIntent.type = MimeTypeList.typeForName(node.name).type
            try {
                !MegaApiUtils.isIntentAvailable(context, checkIntent)
            } catch (e: Exception) {
                LogUtil.logWarning("isIntentAvailable error", e)
                true
            }
        }
    }
}
