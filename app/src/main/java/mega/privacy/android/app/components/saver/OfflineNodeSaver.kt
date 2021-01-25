package mega.privacy.android.app.components.saver

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ActivityContext
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.FileUtil.copyFile
import mega.privacy.android.app.utils.FileUtil.fileExistsInTargetPath
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.OfflineUtils.getOfflineFile
import mega.privacy.android.app.utils.OfflineUtils.getTotalSize
import mega.privacy.android.app.utils.SDCardOperator
import mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.Util.showSnackbar
import java.io.File
import javax.inject.Inject

class OfflineNodeSaver @Inject constructor(
    @ActivityContext context: Context,
    dbHandler: DatabaseHandler
) : NodeSaver(context, dbHandler) {

    /**
     * Save an offline node into device.
     *
     * @param nodes the offline nodes to save
     * @param highPriority whether this download is high priority or not
     * @param activityStarter a high-order function to launch activity when needed
     */
    fun save(
        nodes: List<MegaOffline>,
        highPriority: Boolean,
        activityStarter: (Intent, Int) -> Unit
    ) {
        save(activityStarter) {
            var totalSize = 0L
            for (node in nodes) {
                totalSize += getTotalSize(getOfflineFile(context, node))
            }
            OfflineSaving(totalSize, highPriority, nodes)
        }
    }

    override fun doDownload(
        parentPath: String,
        externalSDCard: Boolean,
        sdCardOperator: SDCardOperator?
    ) {
        var totalFiles = 0
        var alreadyExists = 0

        val nodes = (saving as OfflineSaving).nodes
        for (node in nodes) {
            val res = doDownload(
                getOfflineFile(context, node),
                parentPath, externalSDCard, sdCardOperator
            )

            totalFiles += res.first
            alreadyExists += res.second
        }

        runOnUiThread {
            val textToShow = if (totalFiles == 0) {
                getQuantityString(R.plurals.empty_folders, nodes.size)
            } else if (totalFiles == alreadyExists && totalFiles == 1) {
                getString(R.string.general_already_downloaded)
            } else if (totalFiles == alreadyExists) {
                getQuantityString(R.plurals.file_already_downloaded, totalFiles, totalFiles)
            } else if (totalFiles == 1) {
                getString(R.string.copy_already_downloaded)
            } else {
                getQuantityString(R.plurals.download_finish, totalFiles, totalFiles)
            }
            showSnackbar(context, textToShow)
        }
    }

    private fun doDownload(
        file: File,
        parentPath: String,
        externalSDCard: Boolean,
        sdCardOperator: SDCardOperator?
    ): Pair<Int, Int> {
        if (file.isDirectory) {
            val dstDir = File(parentPath, file.name)

            if (!externalSDCard) {
                dstDir.mkdirs()
            }

            val children = file.listFiles() ?: return Pair(0, 0)

            var totalFiles = 0
            var alreadyExists = 0

            children.forEach {
                val res = doDownload(it, dstDir.absolutePath, externalSDCard, sdCardOperator)
                totalFiles += res.first
                alreadyExists += res.second
            }

            return Pair(totalFiles, alreadyExists)
        } else {
            if (externalSDCard && sdCardOperator != null) {
                if (sdCardOperator.fileExistsInTargetPath(file, parentPath)) {
                    return Pair(1, 1)
                }

                try {
                    sdCardOperator.moveFile(parentPath, file)
                } catch (e: Exception) {
                    logError("Error moving file to the sd card path with exception", e);
                }

                return Pair(1, 0)
            } else {
                if (fileExistsInTargetPath(file, parentPath)) {
                    return Pair(1, 1)
                }

                copyFile(file, File(parentPath, file.name))

                return Pair(1, 0)
            }
        }
    }
}
