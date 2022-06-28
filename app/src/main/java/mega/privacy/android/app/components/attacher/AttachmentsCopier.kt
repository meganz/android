package mega.privacy.android.app.components.attacher

import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.listeners.SetAttrUserListener
import mega.privacy.android.app.utils.Constants.CHAT_FOLDER
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.*
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaApiJava.USER_ATTR_MY_CHAT_FILES_FOLDER
import nz.mega.sdk.MegaError.API_OK
import nz.mega.sdk.MegaRequest.*
import timber.log.Timber

class AttachmentsCopier(
    private val megaApi: MegaApiAndroid,
    private val nodes: List<MegaNode>,
    private val callback: (ArrayList<MegaNode>, Int) -> Unit
) : BaseListener(MegaApplication.getInstance()) {

    private var successNodes = ArrayList<MegaNode>()
    private var failureCount = 0

    init {
        if (MegaNodeUtil.existsMyChatFilesFolder()) {
            copy(MegaNodeUtil.myChatFilesFolder)
        } else {
            megaApi.getMyChatFilesFolder(this)
        }
    }

    private fun copy(parent: MegaNode) {
        for (node in nodes) {
            megaApi.copyNode(node, parent, this)
        }
    }

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        when {
            request.type == TYPE_GET_ATTR_USER
                    && request.paramType == USER_ATTR_MY_CHAT_FILES_FOLDER -> {

                var chatFilesFolderHandle = INVALID_HANDLE

                if (e.errorCode == API_OK) {
                    chatFilesFolderHandle = request.nodeHandle
                } else if (e.errorCode == MegaError.API_ENOENT) {
                    val legacyChatFolderNode = api.getNodeByPath(CHAT_FOLDER, api.rootNode)

                    if (legacyChatFolderNode != null) {
                        api.renameNode(
                            legacyChatFolderNode,
                            getString(R.string.my_chat_files_folder),
                            this
                        )

                        api.setMyChatFilesFolder(
                            legacyChatFolderNode.handle, SetAttrUserListener(context)
                        )

                        chatFilesFolderHandle = legacyChatFolderNode.handle
                    } else {
                        api.createFolder(
                            getString(R.string.my_chat_files_folder), api.rootNode, this
                        )
                    }
                }

                if (chatFilesFolderHandle != INVALID_HANDLE) {
                    MegaApplication.getInstance().dbH.myChatFilesFolderHandle =
                        chatFilesFolderHandle

                    copy(api.getNodeByHandle(chatFilesFolderHandle))
                }
            }
            request.type == TYPE_CREATE_FOLDER
                    && request.name == getString(R.string.my_chat_files_folder) -> {

                if (e.errorCode == API_OK) {
                    api.setMyChatFilesFolder(request.nodeHandle, SetAttrUserListener(context))

                    copy(api.getNodeByHandle(request.nodeHandle))
                } else {
                    Timber.e("Error creating ${getString(R.string.my_chat_files_folder)} folder")
                }
            }
            request.type == TYPE_COPY -> {
                if (e.errorCode == API_OK) {
                    successNodes.add(api.getNodeByHandle(request.nodeHandle))
                } else {
                    failureCount++
                }

                if (successNodes.size + failureCount == nodes.size) {
                    callback(successNodes, failureCount)
                }
            }
        }
    }
}
