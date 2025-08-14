package mega.privacy.android.data.gateway.global

import android.content.Context
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.data.R
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.domain.qualifier.ApplicationScope
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaStringMap
import timber.log.Timber
import javax.inject.Inject

class UserAttributeDatabaseUpdater @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    @ApplicationContext private val context: Context,
    private val localRoomGateway: MegaLocalRoomGateway,
    private val databaseHandler: Lazy<DatabaseHandler>,
) : MegaRequestListenerInterface {

    /**
     * Callback function for onRequestStart
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        // Do nothing
    }

    /**
     * Callback function for onRequestUpdate
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {
        // Do nothing
    }

    /**
     * When calling [MegaApiJava.getUserAttribute], the MegaRequest object won't store
     * node handle, so we can't get user handle from `request.getNodeHandle()`, we should
     * use `api.getContact(request.getEmail())` to get MegaUser, then get user handle from it.
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     * @param e: MegaError
     */
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) =
        with(request) {
            if (type == MegaRequest.TYPE_GET_ATTR_USER) {
                when (paramType) {
                    MegaApiJava.USER_ATTR_MY_CHAT_FILES_FOLDER ->
                        checkMyChatFilesFolderRequest(api, this, e)

                    MegaApiJava.USER_ATTR_FIRSTNAME -> if (e.errorCode == MegaError.API_OK) {
                        applicationScope.launch {
                            if (!email.isNullOrBlank()) {
                                localRoomGateway.updateContactNameByEmail(
                                    firstName = text,
                                    email = email
                                )
                            }
                        }
                    }

                    MegaApiJava.USER_ATTR_LASTNAME -> if (e.errorCode == MegaError.API_OK) {
                        applicationScope.launch {
                            if (!email.isNullOrBlank()) {
                                localRoomGateway.updateContactLastNameByEmail(
                                    lastName = text,
                                    email = email
                                )
                            }
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
    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        // Do nothing
    }

    /**
     * Checks if the USER_ATTR_MY_CHAT_FILES_FOLDER user's attribute exists when the request finished:
     * - If not exists but exists a file with the old "My chat files" name, it renames it with the old one.
     * - If the old one neither exists, it launches a request to create it.
     *
     *
     * Updates the DB with the result and, if necessary updates the data in the current Context.
     *
     * @param api     MegaApiJava object
     * @param request result of the request
     * @param e       MegaError received when the request finished
     */
    private fun checkMyChatFilesFolderRequest(
        api: MegaApiJava,
        request: MegaRequest,
        e: MegaError,
    ) {
        var myChatFolderNode: MegaNode? = null
        when (e.errorCode) {
            MegaError.API_OK -> {
                myChatFolderNode = api.getNodeByHandle(request.nodeHandle)?.apply {
                    api.getNodeByPath(CHAT_FOLDER, api.rootNode)
                }
            }

            MegaError.API_ENOENT -> {
                api.getNodeByPath(CHAT_FOLDER, api.rootNode)?.let {
                    if (!api.isInRubbish(it)) {
                        val name = context.getString(R.string.my_chat_files_folder)
                        if (it.name != name) {
                            api.renameNode(
                                it, name, OptionalMegaRequestListenerInterface(
                                    onRequestFinish = { request, error ->
                                        if (request.type == MegaRequest.TYPE_RENAME) {
                                            if (error.errorCode == MegaError.API_OK) {
                                                Timber.d("Renamed \"My chat files\" folder successfully")
                                            } else {
                                                Timber.w("Error renaming \"My chat files\" folder ${error.errorString}")
                                            }
                                        }
                                    }
                                )
                            )
                        }
                        api.setMyChatFilesFolder(
                            it.handle, OptionalMegaRequestListenerInterface(
                                onRequestFinish = { request, error ->
                                    with(request) {
                                        if (type == MegaRequest.TYPE_SET_ATTR_USER && paramType == MegaApiJava.USER_ATTR_MY_CHAT_FILES_FOLDER){
                                            if (e.errorCode == MegaError.API_OK) {
                                                updateMyChatFilesFolderHandle(megaStringMap)
                                            } else {
                                                Timber.w("Error setting \"My chat files\" folder as user's attribute")
                                            }
                                        }
                                    }
                                }
                            ))
                    }
                }
            }

            else -> {
                Timber.e("Error getting \"My chat files\" folder: %s", e.errorString)
            }
        }
        if (myChatFolderNode != null && !api.isInRubbish(myChatFolderNode)) {
            databaseHandler.get().myChatFilesFolderHandle = myChatFolderNode.handle
        }
    }

    private fun updateMyChatFilesFolderHandle(map: MegaStringMap?) {
        if (map != null && map.size() > 0 && map["h"]?.trim().isNullOrEmpty().not()) {
            val handle = MegaApiJava.base64ToHandle(map["h"])
            if (handle != MegaApiJava.INVALID_HANDLE) {
                databaseHandler.get().myChatFilesFolderHandle = handle
            }
        }
    }

    companion object {
        private const val CHAT_FOLDER = "My chat files"
    }
}