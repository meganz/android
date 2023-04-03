package mega.privacy.android.app.components.saver

import android.content.Context
import android.content.Intent
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import mega.privacy.android.app.DownloadService
import mega.privacy.android.app.DownloadService.Companion.EXTRA_DOWNLOAD_BY_OPEN_WITH
import mega.privacy.android.app.DownloadService.Companion.EXTRA_DOWNLOAD_FOR_PREVIEW
import mega.privacy.android.app.DownloadService.Companion.EXTRA_DOWNLOAD_TO_SDCARD
import mega.privacy.android.app.DownloadService.Companion.EXTRA_FOLDER_LINK
import mega.privacy.android.app.DownloadService.Companion.EXTRA_FROM_MV
import mega.privacy.android.app.DownloadService.Companion.EXTRA_HASH
import mega.privacy.android.app.DownloadService.Companion.EXTRA_OPEN_FILE
import mega.privacy.android.app.DownloadService.Companion.EXTRA_PATH
import mega.privacy.android.app.DownloadService.Companion.EXTRA_SIZE
import mega.privacy.android.app.DownloadService.Companion.EXTRA_TARGET_PATH
import mega.privacy.android.app.DownloadService.Companion.EXTRA_TARGET_URI
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.di.getDbHandler
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.utils.Constants.APP_DATA_VOICE_CLIP
import mega.privacy.android.app.utils.Constants.EXTRA_SERIALIZE_STRING
import mega.privacy.android.app.utils.Constants.EXTRA_TRANSFER_TYPE
import mega.privacy.android.app.utils.Constants.HIGH_PRIORITY_TRANSFER
import mega.privacy.android.app.utils.Constants.SEPARATOR
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.app.utils.FileUtil.isFileDownloadedLatest
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaNodeParceler
import mega.privacy.android.app.utils.MegaNodeUtil.getDlList
import mega.privacy.android.app.utils.SDCardOperator
import mega.privacy.android.app.utils.TextUtil
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File

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
    private val downloadForPreview: Boolean = false,
    private val downloadByOpenWith: Boolean = false,
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
        val app = MegaApplication.getInstance()
        val api = if (isFolderLink) megaApiFolder else megaApi
        val dbHandler = getDbHandler()

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

                    intent.putExtra(EXTRA_DOWNLOAD_FOR_PREVIEW, downloadForPreview)
                    intent.putExtra(EXTRA_DOWNLOAD_BY_OPEN_WITH, downloadByOpenWith)

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
                app.resources.getQuantityString(R.plurals.empty_folders, emptyFolders)
            }
            numberOfNodesAlreadyDownloaded > 0 && numberOfNodesPending == 0 -> {
                app.resources.getQuantityString(
                    R.plurals.file_already_downloaded,
                    numberOfNodesAlreadyDownloaded,
                    numberOfNodesAlreadyDownloaded
                )
            }
            numberOfNodesAlreadyDownloaded == 0 && numberOfNodesPending > 0 -> {
                if (downloadForPreview || downloadByOpenWith) {
                    app.getString(R.string.cloud_drive_snackbar_preparing_file_for_preview_context)
                } else {
                    app.resources.getQuantityString(
                        R.plurals.download_began,
                        numberOfNodesPending,
                        numberOfNodesPending
                    )
                }
            }
            numberOfNodesAlreadyDownloaded == 1 -> {
                app.resources.getQuantityString(
                    R.plurals.file_already_downloaded_and_files_pending_download,
                    numberOfNodesPending,
                    numberOfNodesPending
                )
            }
            numberOfNodesPending == 1 -> {
                app.resources.getQuantityString(
                    R.plurals.files_already_downloaded_and_file_pending_download,
                    numberOfNodesAlreadyDownloaded,
                    numberOfNodesAlreadyDownloaded
                )
            }
            else -> {
                StringBuilder().append(
                    app.resources.getQuantityString(
                        R.plurals.file_already_downloaded,
                        numberOfNodesAlreadyDownloaded,
                        numberOfNodesAlreadyDownloaded
                    )
                ).append(" ").append(
                    app.resources.getQuantityString(
                        R.plurals.file_pending_download,
                        numberOfNodesPending,
                        numberOfNodesPending
                    )
                ).toString()
            }
        }

        snackbarShower?.showSnackbar(message)

        if (nodes.size != 1 || nodes[0].isFolder || numberOfNodesAlreadyDownloaded != 1) {
            return AutoPlayInfo.NO_AUTO_PLAY
        }

        return AutoPlayInfo(nodes[0].name, nodes[0].handle, theOnlyLocalFilePath)
    }

    override fun isDownloadForPreview(): Boolean = downloadForPreview

}
