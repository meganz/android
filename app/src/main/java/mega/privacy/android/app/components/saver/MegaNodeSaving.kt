package mega.privacy.android.app.components.saver

import android.content.Context
import android.content.Intent
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.MegaApiUtils
import nz.mega.sdk.MegaNode

class MegaNodeSaving(
    totalSize: Long,
    highPriority: Boolean,
    val isFolderLink: Boolean,
    val nodes: List<MegaNode>,
) : Saving(totalSize, highPriority) {

    override fun hasUnsupportedFile(context: Context): Boolean {
        for (node in nodes) {
            if (node.isFolder) {
                continue
            }

            unsupportedFileName = node.name
            val checkIntent = Intent(Intent.ACTION_GET_CONTENT)
            checkIntent.type = MimeTypeList.typeForName(node.name).type
            try {
                val intentAvailable = MegaApiUtils.isIntentAvailable(context, checkIntent)
                if (!intentAvailable) {
                    return true
                }
            } catch (e: Exception) {
                LogUtil.logWarning("isIntentAvailable error", e)
                return true
            }
        }

        return false
    }
}
