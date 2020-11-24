package mega.privacy.android.app.components.saver

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ActivityContext
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.FileUtil.copyFile
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.OfflineUtils.getOfflineFile
import mega.privacy.android.app.utils.OfflineUtils.getTotalSize
import mega.privacy.android.app.utils.SDCardOperator
import mega.privacy.android.app.utils.Util.showSnackbar
import java.io.File
import javax.inject.Inject

/**
 * NodeSaver implementation for offline node.
 */
class OfflineNodeSaver @Inject constructor(
    @ActivityContext context: Context,
    dbHandler: DatabaseHandler
) : NodeSaver(context, dbHandler) {

    /**
     * Save an offline node into device.
     *
     * @param handle the handle of the offline node to save
     * @param highPriority whether this download is high priority or not
     * @param activityStarter a high-order function to launch activity when needed
     */
    fun save(handle: Long, highPriority: Boolean, activityStarter: (Intent, Int) -> Unit) {
        save(activityStarter, savingProducer = {
            val node = dbHandler.findByHandle(handle)

            if (node == null) {
                null
            } else {
                OfflineSaving(getTotalSize(getOfflineFile(context, node)), highPriority, node)
            }
        })
    }

    /**
     * Save an offline node into device.
     *
     * @param node the offline node to save
     * @param highPriority whether this download is high priority or not
     * @param activityStarter a high-order function to launch activity when needed
     */
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
}
