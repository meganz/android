package mega.privacy.android.app.components.attacher

import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.listeners.SetAttrUserListener
import mega.privacy.android.app.utils.Constants.CHAT_FOLDER
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.MegaNodeUtil
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaApiJava.USER_ATTR_MY_CHAT_FILES_FOLDER
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaError.API_OK
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequest.*

class AttachmentsCopier(
    private val nodes: List<MegaNode>,
    private val callback: (ArrayList<MegaNode>, Int) -> Unit
) : BaseListener(MegaApplication.getInstance()) {
    private val megaApi = MegaApplication.getInstance().megaApi

    private var successNodes = ArrayList<MegaNode>()
    private var failureCount = 0

    init {
        if (MegaNodeUtil.existsMyChatFilesFolder()) {
            copy(MegaNodeUtil.getMyChatFilesFolder())
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
                        megaApi.renameNode(
                            legacyChatFolderNode,
                            context.getString(R.string.my_chat_files_folder),
                            this
                        )

                        megaApi.setMyChatFilesFolder(
                            legacyChatFolderNode.handle, SetAttrUserListener(context)
                        )

                        chatFilesFolderHandle = legacyChatFolderNode.handle
                    } else {
                        megaApi.createFolder(
                            context.getString(R.string.my_chat_files_folder), megaApi.rootNode, this
                        )
                    }
                }

                if (chatFilesFolderHandle != INVALID_HANDLE) {
                    MegaApplication.getInstance().dbH.myChatFilesFolderHandle =
                        chatFilesFolderHandle

                    copy(megaApi.getNodeByHandle(chatFilesFolderHandle))
                }
            }
            request.type == TYPE_CREATE_FOLDER
                    && request.name == context.getString(R.string.my_chat_files_folder) -> {

                if (e.errorCode == API_OK) {
                    megaApi.setMyChatFilesFolder(request.nodeHandle, SetAttrUserListener(context))

                    copy(megaApi.getNodeByHandle(request.nodeHandle))
                } else {
                    logError("Error creating ${context.getString(R.string.my_chat_files_folder)} folder")
                }
            }
            request.type == TYPE_COPY -> {
                if (e.errorCode == API_OK) {
                    successNodes.add(megaApi.getNodeByHandle(request.nodeHandle))
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
