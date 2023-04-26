package mega.privacy.android.app.listeners

import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.di.DatabaseEntryPoint
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaStringMap
import timber.log.Timber

/**
 * SetAttrUserListener
 *
 * @param context: Context
 */
class SetAttrUserListener(private val context: Context) : MegaRequestListenerInterface {

    private val databaseEntryPoint: DatabaseEntryPoint by lazy {
        EntryPointAccessors.fromApplication(context.applicationContext)
    }

    /**
     * Callback function for onRequestStart
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestStart(api: MegaApiJava?, request: MegaRequest?) {
        // DO nothing
    }

    /**
     * Callback function for onRequestUpdate
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestUpdate(api: MegaApiJava?, request: MegaRequest?) {
        // DO nothing
    }

    /**
     * Callback function for onRequestFinish
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     * @param e: MegaError
     */
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) =
        with(request) {
            if (type == MegaRequest.TYPE_SET_ATTR_USER) {
                when (paramType) {
                    MegaApiJava.USER_ATTR_MY_CHAT_FILES_FOLDER -> if (e.errorCode == MegaError.API_OK) {
                        updateMyChatFilesFolderHandle(megaStringMap)
                    } else {
                        Timber.w("Error setting \"My chat files\" folder as user's attribute")
                    }

                    MegaApiJava.USER_ATTR_FIRSTNAME -> if (e.errorCode == MegaError.API_OK) {
                        databaseEntryPoint.applicationScope().launch {
                            databaseEntryPoint.localRoomGateway.setContactName(
                                firstName = text,
                                mail = email
                            )
                        }
                    }

                    MegaApiJava.USER_ATTR_LASTNAME -> if (e.errorCode == MegaError.API_OK) {
                        databaseEntryPoint.applicationScope().launch {
                            databaseEntryPoint.localRoomGateway.setContactLastName(
                                lastName = text,
                                mail = email
                            )
                        }
                    }

                    MegaApiJava.USER_ATTR_ALIAS -> when (e.errorCode) {
                        MegaError.API_OK -> {
                            databaseEntryPoint.applicationScope().launch {
                                databaseEntryPoint.localRoomGateway.setContactNickname(
                                    nodeHandle,
                                    text
                                )
                                ContactUtil.notifyNicknameUpdate(context, nodeHandle)
                            }
                            val message = if (text == null) {
                                context?.getString(R.string.snackbar_nickname_removed)
                            } else {
                                context?.getString(R.string.snackbar_nickname_added)
                            }
                            Util.showSnackbar(context, message)
                        }

                        MegaError.API_ENOENT -> {
                            databaseEntryPoint.applicationScope().launch {
                                databaseEntryPoint.localRoomGateway.setContactNickname(
                                    nodeHandle,
                                    null
                                )
                                ContactUtil.notifyNicknameUpdate(context, nodeHandle)
                            }
                        }

                        else -> {
                            Timber.e("Error adding, updating or removing the alias%s", e.errorCode)
                        }
                    }

                    MegaApiJava.USER_ATTR_RUBBISH_TIME -> if (e.errorCode == MegaError.API_OK) {
                        Intent(BroadcastConstants.ACTION_UPDATE_RB_SCHEDULER).run {
                            putExtra(BroadcastConstants.DAYS_COUNT, number)
                            MegaApplication.getInstance().sendBroadcast(this)
                        }
                    } else {
                        Util.showSnackbar(context, context?.getString(R.string.error_general_nodes))
                    }

                    MegaApiJava.USER_ATTR_RICH_PREVIEWS -> if (e.errorCode != MegaError.API_OK) {
                        MegaApplication.getInstance()
                            .sendBroadcast(Intent(BroadcastConstants.BROADCAST_ACTION_INTENT_RICH_LINK_SETTING_UPDATE))
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
        // DO nothing
    }

    /**
     * Updates in DB the handle of "My chat files" folder node if the request
     * for set a node as USER_ATTR_MY_CHAT_FILES_FOLDER finished without errors.
     *
     *
     * Before update the DB, it has to obtain the handle contained in a MegaStringMap,
     * where one of the entries will contain a key "h" and its value, the handle in base64.
     *
     * @param map MegaStringMap which contains the handle of the node set as USER_ATTR_MY_CHAT_FILES_FOLDER.
     */
    private fun updateMyChatFilesFolderHandle(map: MegaStringMap?) {
        if (map != null && map.size() > 0 && !TextUtil.isTextEmpty(map["h"])) {
            val handle = MegaApiJava.base64ToHandle(map["h"])
            if (handle != MegaApiJava.INVALID_HANDLE) {
                MegaApplication.getInstance().dbH.myChatFilesFolderHandle = handle
            }
        }
    }
}
