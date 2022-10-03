package mega.privacy.android.app.components.saver

import android.content.Context
import android.content.Intent
import kotlinx.parcelize.Parcelize
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.utils.FileUtil.copyFile
import mega.privacy.android.app.utils.FileUtil.fileExistsInTargetPath
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.OfflineUtils.getOfflineFile
import mega.privacy.android.app.utils.RunOnUIThreadUtils.post
import mega.privacy.android.app.utils.SDCardOperator
import mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.MegaApiAndroid
import timber.log.Timber
import java.io.File

@Parcelize
class OfflineSaving(
    private val totalSize: Long, private val nodes: List<MegaOffline>,
    private val fromMediaViewer: Boolean,
) : Saving() {

    override fun totalSize() = totalSize

    override fun hasUnsupportedFile(context: Context): Boolean {
        for (node in nodes) {
            if (node.isFolder) {
                continue
            }

            unsupportedFileName = node.name
            val checkIntent = Intent(Intent.ACTION_GET_CONTENT)
            checkIntent.type = MimeTypeList.typeForName(node.name).type
            try {
                !MegaApiUtils.isIntentAvailable(context, checkIntent)
            } catch (e: Exception) {
                Timber.w(e, "isIntentAvailable error")
                return true
            }
        }

        return false
    }

    override fun fromMediaViewer() = fromMediaViewer

    override fun doDownload(
        megaApi: MegaApiAndroid,
        megaApiFolder: MegaApiAndroid,
        parentPath: String,
        externalSDCard: Boolean,
        sdCardOperator: SDCardOperator?,
        snackbarShower: SnackbarShower?,
    ): AutoPlayInfo {
        val context = MegaApplication.getInstance()

        var totalFiles = 0
        var alreadyExists = 0

        for (node in nodes) {
            val res = doDownload(
                getOfflineFile(context, node), parentPath, externalSDCard, sdCardOperator
            )

            totalFiles += res.first
            alreadyExists += res.second
        }

        post {
            val message = if (totalFiles == 0) {
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
            snackbarShower?.showSnackbar(message)
        }

        if (nodes.size != 1 || nodes[0].isFolder) {
            return AutoPlayInfo.NO_AUTO_PLAY
        }

        return AutoPlayInfo(
            nodes[0].name,
            nodes[0].handle.toLong(),
            getOfflineFile(context, nodes[0]).absolutePath
        )
    }

    private fun doDownload(
        file: File,
        parentPath: String,
        externalSDCard: Boolean,
        sdCardOperator: SDCardOperator?,
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
                    Timber.e(e, "Error moving file to the sd card path with exception");
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

    override fun isDownloadByTap(): Boolean = false
}
