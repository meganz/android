package mega.privacy.android.app.activities

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.utils.Constants.ACTION_CHAT_NOTIFICATION_MESSAGE
import mega.privacy.android.app.utils.Constants.CHAT_ID
import mega.privacy.android.app.utils.JobUtil
import mega.privacy.android.app.utils.PasscodeUtil
import mega.privacy.android.app.utils.Util.setAppFontSize
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import javax.inject.Inject

@AndroidEntryPoint
open class PasscodeActivity : BaseActivity() {

    companion object {
        private const val SCREEN_ORIENTATION = "SCREEN_ORIENTATION"
    }

    @Inject
    lateinit var passcodeUtil: PasscodeUtil
    @Inject
    lateinit var passcodeManagement: PasscodeManagement
    private var lastStart = 0L

    /**
     * Used to control when onResume comes from a screen orientation change.
     * Since onConfigurationChanged is not implemented in all activities,
     * it cannot be used for that purpose.
     *
     * @see android.app.Activity#onConfigurationChanged(Configuration)
     */
    private var isScreenRotation = false

    /**
     * Used to disable passcode.
     * E.g.: PdfViewerActivity when it is opened from outside the app.
     */
    private var isDisabled = false

    /**
     * Used to ignore onPause when it is opening a chat message notification.
     */
    private var ignoringDueToChatNotification = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null &&
            resources.configuration.orientation != savedInstanceState.getInt(SCREEN_ORIENTATION)
        ) {
            isScreenRotation = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(SCREEN_ORIENTATION, resources.configuration.orientation)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()

        if (isDisabled || ignoringDueToChatNotification) {
            ignoringDueToChatNotification = false
            return
        }

        passcodeUtil.pauseUpdate()
        lastStart = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()

        ignoringDueToChatNotification = shouldIgnoreDueToChatNotification()

        if (isDisabled || ignoringDueToChatNotification) {
            return
        }

        setAppFontSize(this)

        if (isScreenRotation && !passcodeManagement.needsOpenAgain) {
            isScreenRotation = false
        } else if (passcodeManagement.showPasscodeScreen) {
            passcodeUtil.resume()
        }
    }

    protected fun disablePasscode() {
        isDisabled = true
    }

    /**
     * Checks if should proceed with passcode check due to a chat notification message.
     *
     * If the app is opening a chat notification message, means first the ManagerActivity
     * will be opened and then ChatActivity. That also means the passcode would be shown
     * first, and then the chat room. This will cause the user could interact with the app,
     * without having to enter the passcode. This will cause too the passcode will be shown after
     * the chat room and the app be closed. Which results in bad a behavior.
     * In summary, if all those requirements are true, the passcode should not be checked. Which
     * results in the right behaviour: If enabled and complies all requirements, the passcode
     * should be asked in the chat room screen.
     *
     * @return True if should proceed with the passcode check, false otherwise.
     */
    private fun shouldIgnoreDueToChatNotification(): Boolean =
        this is ManagerActivity
                && ACTION_CHAT_NOTIFICATION_MESSAGE == intent?.action
                && MEGACHAT_INVALID_HANDLE != intent?.getLongExtra(CHAT_ID, MEGACHAT_INVALID_HANDLE)
}