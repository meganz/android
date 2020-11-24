package mega.privacy.android.app.components.saver

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ActivityContext
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.DownloadService
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.lollipop.controllers.NodeController
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.DownloadUtil.showSnackBarWhenDownloading
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import java.io.File
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

/**
 * NodeSaver implementation for MegaNode.
 */
class MegaNodeSaver @Inject constructor(
    @ActivityContext context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    dbHandler: DatabaseHandler,
) : NodeSaver(context, dbHandler) {

    /**
     * Save a list of MegaNode into device.
     *
     * @param handles the handle list of nodes to save
     * @param highPriority whether this download is high priority or not
     * @param isFolderLink whether this download is a folder link
     * @param activityStarter a high-order function to launch activity when needed
     */
    fun save(
        handles: List<Long>, highPriority: Boolean, isFolderLink: Boolean,
        activityStarter: (Intent, Int) -> Unit
    ) {
        save(activityStarter) {
            val nodes = ArrayList<MegaNode>()
            var totalSize = 0L

            for (handle in handles) {
                val node = megaApi.getNodeByHandle(handle)
                if (node != null) {
                    nodes.add(node)
                    totalSize += node.size
                }
            }

            MegaNodeSaving(totalSize, highPriority, isFolderLink, nodes)
        }
    }

    override fun doDownload(
        parentPath: String,
        externalSDCard: Boolean,
        sdCardOperator: SDCardOperator?
    ) {
        var numberOfNodesAlreadyDownloaded = 0
        var numberOfNodesPending = 0
        var emptyFolders = 0

        val isFolderLink = (saving as MegaNodeSaving).isFolderLink

        for (node in (saving as MegaNodeSaving).nodes) {
            val dlFiles = HashMap<MegaNode, String>()
            val targets = HashMap<Long, String>()

            if (node.type == MegaNode.TYPE_FOLDER) {
                if (sdCardOperator != null && sdCardOperator.isSDCardDownload) {
                    sdCardOperator.buildFileStructure(targets, parentPath, megaApi, node)
                    MegaNodeUtil.getDlList(
                        megaApi, dlFiles, node, File(sdCardOperator.downloadRoot, node.name)
                    )
                } else {
                    MegaNodeUtil.getDlList(
                        megaApi, dlFiles, node, File(parentPath, node.name)
                    )
                }
            } else {
                if (sdCardOperator != null && sdCardOperator.isSDCardDownload) {
                    targets[node.handle] = parentPath
                    dlFiles[node] = sdCardOperator.downloadRoot
                } else {
                    dlFiles[node] = parentPath
                }
            }

            if (dlFiles.isEmpty()) {
                emptyFolders++
            }

            for (document in dlFiles.keys) {
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
                            document.name, destDir.absolutePath + Constants.SEPARATOR
                        )
                    )
                } else {
                    destDir
                }

                if (FileUtil.isFileAvailable(destFile)
                    && document.size == destFile.length()
                    && FileUtil.isFileDownloadedLatest(destFile, document)
                ) {
                    numberOfNodesAlreadyDownloaded++
                } else {
                    numberOfNodesPending++

                    var service = Intent(context, DownloadService::class.java)
                    service.putExtra(DownloadService.EXTRA_HASH, document.handle)
                    if (sdCardOperator!!.isSDCardDownload) {
                        service = NodeController.getDownloadToSDCardIntent(
                            service, path, targetPath, dbHandler.sdCardUri
                        )
                    } else {
                        service.putExtra(DownloadService.EXTRA_PATH, path)
                    }
                    service.putExtra(DownloadService.EXTRA_SIZE, document.size)
                    if (saving.highPriority) {
                        service.putExtra(Constants.HIGH_PRIORITY_TRANSFER, true)
                    }

                    service.putExtra(DownloadService.EXTRA_FOLDER_LINK, isFolderLink)

                    context.startService(service)
                }
            }
        }

        showSnackBarWhenDownloading(
            context, numberOfNodesPending, numberOfNodesAlreadyDownloaded, emptyFolders
        )
    }
}
