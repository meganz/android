package mega.privacy.android.app.components.saver

import android.content.Context
import android.content.Intent
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import mega.privacy.android.app.*
import mega.privacy.android.app.DownloadService.*
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.app.utils.FileUtil.isFileDownloadedLatest
import mega.privacy.android.app.utils.MegaNodeUtil.getDlList
import mega.privacy.android.app.utils.StringResourcesUtils.*
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import java.io.File
import java.util.*

@Parcelize
@TypeParceler<MegaNode, MegaNodeParceler>()
class MegaNodeSaving(
    private val totalSize: Long,
    private val highPriority: Boolean,
    private val isFolderLink: Boolean,
    private val nodes: List<MegaNode>,
    private val fromMediaViewer: Boolean,
    private val needSerialize: Boolean,
    private val isVoiceClip: Boolean = false,
    private val downloadByTap: Boolean = false
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

    override fun fromMediaViewer() = fromMediaViewer

    override fun doDownload(
        megaApi: MegaApiAndroid,
        megaApiFolder: MegaApiAndroid,
        parentPath: String,
        externalSDCard: Boolean,
        sdCardOperator: SDCardOperator?,
        snackbarShower: SnackbarShower?,
    ): AutoPlayInfo {
        val app = MegaApplication.getInstance()
        val api = if (isFolderLink) megaApiFolder else megaApi
        val dbHandler = DatabaseHandler.getDbHandler(app)

        var numberOfNodesAlreadyDownloaded = 0
        var numberOfNodesPending = 0
        var emptyFolders = 0

        var theOnlyLocalFilePath = ""

        app.transfersManagement.isProcessingTransfers = true

        for (node in nodes) {
            if (app.transfersManagement.shouldBreakTransfersProcessing()) {
                return AutoPlayInfo.NO_AUTO_PLAY
            }

            val dlFiles = HashMap<MegaNode, String>()
            val targets = HashMap<Long, String>()

            if (node.type == MegaNode.TYPE_FOLDER && sdCardOperator != null && sdCardOperator.isSDCardDownload) {
                app.transfersManagement.setIsProcessingFolders(true)
                sdCardOperator.buildFileStructure(targets, parentPath, api, node)
                getDlList(api, dlFiles, node, File(sdCardOperator.downloadRoot, node.name))
            } else if (sdCardOperator != null && sdCardOperator.isSDCardDownload) {
                targets[node.handle] = parentPath
                dlFiles[node] = sdCardOperator.downloadRoot
            } else {
                dlFiles[node] = parentPath
            }

            if (dlFiles.isEmpty()) {
                emptyFolders++
            }

            for (document in dlFiles.keys) {
                if (app.transfersManagement.shouldBreakTransfersProcessing()) {
                    return AutoPlayInfo.NO_AUTO_PLAY
                }

                val path = dlFiles[document]
                val targetPath = targets[document.handle]
                if (TextUtil.isTextEmpty(path)) {
                    continue
                }

                val destDir = File(path!!)
                val destFile = if (destDir.isDirectory) {
                    File(
                        destDir,
                        megaApi.escapeFsIncompatible(
                            document.name, destDir.absolutePath + SEPARATOR
                        )
                    )
                } else {
                    destDir
                }

                if (isFileAvailable(destFile)
                    && document.size == destFile.length()
                    && isFileDownloadedLatest(destFile, document)
                ) {
                    numberOfNodesAlreadyDownloaded++

                    theOnlyLocalFilePath = destFile.absolutePath
                } else {
                    numberOfNodesPending++

                    val intent = Intent(app, DownloadService::class.java)

                    if (needSerialize) {
                        intent.putExtra(EXTRA_SERIALIZE_STRING, document.serialize())
                    } else {
                        intent.putExtra(EXTRA_HASH, document.handle)
                    }

                    if (isVoiceClip) {
                        intent.putExtra(EXTRA_OPEN_FILE, false)
                        intent.putExtra(EXTRA_TRANSFER_TYPE, APP_DATA_VOICE_CLIP)
                    }

                    if (sdCardOperator?.isSDCardDownload == true) {
                        intent.putExtra(EXTRA_PATH, path)
                        intent.putExtra(EXTRA_DOWNLOAD_TO_SDCARD, true)
                        intent.putExtra(EXTRA_TARGET_PATH, targetPath)
                        intent.putExtra(EXTRA_TARGET_URI, dbHandler.sdCardUri)
                    } else {
                        intent.putExtra(EXTRA_PATH, path)
                    }

                    intent.putExtra(EXTRA_SIZE, document.size)

                    if (highPriority) {
                        intent.putExtra(HIGH_PRIORITY_TRANSFER, true)
                    }

                    intent.putExtra(EXTRA_DOWNLOAD_BY_TAP, downloadByTap)

                    if (fromMediaViewer) {
                        intent.putExtra(EXTRA_FROM_MV, true)
                    }

                    intent.putExtra(EXTRA_FOLDER_LINK, isFolderLink)

                    app.startService(intent)
                }
            }
        }

        app.transfersManagement.isProcessingTransfers = false
        app.transfersManagement.setIsProcessingFolders(false)

        val message = when {
            numberOfNodesPending == 0 && numberOfNodesAlreadyDownloaded == 0 -> {
                getQuantityString(R.plurals.empty_folders, emptyFolders)
            }
            numberOfNodesAlreadyDownloaded > 0 && numberOfNodesPending == 0 -> {
                getQuantityString(
                    R.plurals.file_already_downloaded,
                    numberOfNodesAlreadyDownloaded,
                    numberOfNodesAlreadyDownloaded
                )
            }
            numberOfNodesAlreadyDownloaded == 0 && numberOfNodesPending > 0 -> {
                getQuantityString(
                    R.plurals.download_began,
                    numberOfNodesPending,
                    numberOfNodesPending
                )
            }
            numberOfNodesAlreadyDownloaded == 1 && numberOfNodesPending == 1 -> {
                getString(R.string.file_already_downloaded_and_file_pending_download)
            }
            numberOfNodesAlreadyDownloaded == 1 -> {
                getString(
                    R.string.file_already_downloaded_and_files_pending_download,
                    numberOfNodesPending
                )
            }
            numberOfNodesPending == 1 -> {
                getString(
                    R.string.files_already_downloaded_and_file_pending_download,
                    numberOfNodesAlreadyDownloaded
                )
            }
            else -> {
                getString(
                    R.string.files_already_downloaded_and_files_pending_download,
                    numberOfNodesAlreadyDownloaded,
                    numberOfNodesPending
                )
            }
        }

        snackbarShower?.showSnackbar(message)

        if (nodes.size != 1 || nodes[0].isFolder || numberOfNodesAlreadyDownloaded != 1) {
            return AutoPlayInfo.NO_AUTO_PLAY
        }

        return AutoPlayInfo(nodes[0].name, nodes[0].handle, theOnlyLocalFilePath)
    }
}
