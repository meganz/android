package mega.privacy.android.app.components.attacher

import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.listeners.SetAttrUserListener
import mega.privacy.android.app.utils.Constants.CHAT_FOLDER
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaApiJava.USER_ATTR_MY_CHAT_FILES_FOLDER
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaError.API_OK
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequest.TYPE_COPY
import nz.mega.sdk.MegaRequest.TYPE_CREATE_FOLDER
import nz.mega.sdk.MegaRequest.TYPE_GET_ATTR_USER
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber

/**
 * AttachmentsCopier
 *
 * @property megaApi : MegaApiAndroid
 * @property nodes: List<MegaNode>
 * @property callback : callback: (ArrayList<MegaNode>, Int)
 */
class AttachmentsCopier(
    private val megaApi: MegaApiAndroid,
    private val nodes: List<MegaNode>,
    private val callback: (ArrayList<MegaNode>, Int) -> Unit,
) : MegaRequestListenerInterface {

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

    /**
     * Callback function for onRequestStart
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestStart(api: MegaApiJava?, request: MegaRequest?) {
        // Do nothing
    }

    /**
     * Callback function for onRequestUpdate
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestUpdate(api: MegaApiJava?, request: MegaRequest?) {
        // Do nothing
    }

    /**
     * Callback function for onRequestFinish
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     * @param e: MegaError
     */
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        with(request) {
            when {
                type == TYPE_GET_ATTR_USER
                        && paramType == USER_ATTR_MY_CHAT_FILES_FOLDER -> {

                    var chatFilesFolderHandle = INVALID_HANDLE

                    if (e.errorCode == API_OK) {
                        chatFilesFolderHandle = nodeHandle
                    } else if (e.errorCode == MegaError.API_ENOENT) {
                        api.getNodeByPath(CHAT_FOLDER, api.rootNode)?.let {
                            api.renameNode(
                                it,
                                getString(R.string.my_chat_files_folder),
                                this@AttachmentsCopier
                            )

                            api.setMyChatFilesFolder(
                                it.handle,
                                SetAttrUserListener(MegaApplication.getInstance())
                            )

                            chatFilesFolderHandle = it.handle
                        } ?: run {
                            api.createFolder(
                                getString(R.string.my_chat_files_folder),
                                api.rootNode,
                                this@AttachmentsCopier
                            )
                        }
                    }

                    if (chatFilesFolderHandle != INVALID_HANDLE) {
                        MegaApplication.getInstance().dbH.myChatFilesFolderHandle =
                            chatFilesFolderHandle

                        copy(api.getNodeByHandle(chatFilesFolderHandle))
                    }
                }
                type == TYPE_CREATE_FOLDER
                        && name == getString(R.string.my_chat_files_folder) -> {

                    if (e.errorCode == API_OK) {
                        api.setMyChatFilesFolder(nodeHandle,
                            SetAttrUserListener(MegaApplication.getInstance()))

                        copy(api.getNodeByHandle(nodeHandle))
                    } else {
                        Timber.e("Error creating ${getString(R.string.my_chat_files_folder)} folder")
                    }
                }
                type == TYPE_COPY -> {
                    if (e.errorCode == API_OK) {
                        successNodes.add(api.getNodeByHandle(nodeHandle))
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

    /**
     * Callback function for onRequestTemporaryError
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     * @param e: MegaError
     */
    override fun onRequestTemporaryError(api: MegaApiJava?, request: MegaRequest?, e: MegaError?) {
        // Do nothing
    }
}
