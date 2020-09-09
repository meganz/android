package mega.privacy.android.app.components.saver

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ActivityContext
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.FileUtils.copyFile
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.OfflineUtils.getOfflineFile
import mega.privacy.android.app.utils.OfflineUtils.getTotalSize
import mega.privacy.android.app.utils.SDCardOperator
import mega.privacy.android.app.utils.Util.showSnackbar
import java.io.File
import javax.inject.Inject

class OfflineNodeSaver @Inject constructor(
    @ActivityContext context: Context,
    dbHandler: DatabaseHandler
) : NodeSaver(context, dbHandler) {

    fun save(node: MegaOffline, highPriority: Boolean, activityStarter: (Intent, Int) -> Unit) {
        save(activityStarter) {
            OfflineSaving(getTotalSize(getOfflineFile(context, node)), highPriority, node)
        }
    }

    override fun doDownload(
        parentPath: String,
        externalSDCard: Boolean,
        sdCardOperator: SDCardOperator?
    ) {
        doDownload(
            getOfflineFile(context, (saving as OfflineSaving).node),
            parentPath, externalSDCard, sdCardOperator
        )
        runOnUiThread {
            showSnackbar(
                context, context.resources.getQuantityString(R.plurals.download_finish, 1, 1)
            )
        }
    }

    private fun doDownload(
        file: File,
        parentPath: String,
        externalSDCard: Boolean,
        sdCardOperator: SDCardOperator?
    ) {
        if (file.isDirectory) {
            val dstDir = File(parentPath, file.name)
            if (!externalSDCard) {
                dstDir.mkdirs()
            }
            val children = file.listFiles() ?: return
            children.forEach {
                doDownload(it, dstDir.absolutePath, externalSDCard, sdCardOperator)
            }
        } else {
            if (externalSDCard && sdCardOperator != null) {
                try {
                    sdCardOperator.move(parentPath, file)
                } catch (e: Exception) {
                    logError("Error moving file to the sd card path with exception", e);
                }
            } else {
                copyFile(file, File(parentPath, file.name))
            }
        }
    }

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
}
