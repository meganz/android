package mega.privacy.android.app.activities.settingsActivities

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.presentation.settings.chat.SettingsChatFragment
import mega.privacy.android.app.utils.Constants
import timber.log.Timber

/**
 * ChatPreferencesActivity
 */
@AndroidEntryPoint
class ChatPreferencesActivity : PreferencesBaseActivity() {
    private var sttChat: SettingsChatFragment? = null
    private var newFolderDialog: AlertDialog? = null
    private val viewModel by viewModels<ChatPreferencesViewModel>()

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.toolbarSettings.title = getString(R.string.section_chat)
        sttChat = SettingsChatFragment()
        sttChat?.let { replaceFragment(it) }
        collectFlows()
    }

    private fun collectFlows() {
        collectFlow(viewModel.state) { chatPreferencesState ->
            with(chatPreferencesState) {
                if (isPushNotificationSettingsUpdatedEvent) {
                    sttChat?.updateNotifChat()
                    viewModel.onConsumePushNotificationSettingsUpdateEvent()
                }
                if (signalPresenceUpdate) {
                    if (sttChat != null && megaChatApi.presenceConfig != null && !megaChatApi.presenceConfig.isPending) {
                        sttChat?.updatePresenceConfigChat(false)
                    }
                    viewModel.onSignalPresenceUpdateConsumed()
                }
            }
        }
    }

    /**
     * Method for displaying the AutoAwayValue dialogue.
     */
    fun showAutoAwayValueDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        val inflater = layoutInflater
        val v: View = inflater.inflate(R.layout.dialog_autoaway, null)
        builder.setView(v)
        val input = v.findViewById<EditText>(R.id.autoaway_edittext)
        input.setOnEditorActionListener { v1: TextView, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val value = validateAutoAway(v1.text)
                if (value != null) {
                    setAutoAwayValue(value, false)
                }
                newFolderDialog?.dismiss()
                return@setOnEditorActionListener true
            }
            false
        }
        input.setImeActionLabel(getString(R.string.general_create), EditorInfo.IME_ACTION_DONE)
        input.requestFocus()
        builder.setTitle(getString(R.string.title_dialog_set_autoaway_value))
        val set = v.findViewById<Button>(R.id.autoaway_set_button)
        set.setOnClickListener {
            val value = validateAutoAway(input.text)
            if (value != null) {
                setAutoAwayValue(value, false)
            }
            newFolderDialog?.dismiss()
        }
        val cancel = v.findViewById<Button>(R.id.autoaway_cancel_button)
        cancel.setOnClickListener {
            setAutoAwayValue(Constants.INVALID_OPTION, true)
            newFolderDialog?.dismiss()
        }
        newFolderDialog = builder.create()
        newFolderDialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        newFolderDialog?.show()
    }

    /**
     * Method to validate AutoAway.
     *
     * @param value The introduced string
     * @return The result string.
     */
    private fun validateAutoAway(value: CharSequence): String? {
        return try {
            var timeout = value.toString().trim { it <= ' ' }.toInt()
            if (timeout <= 0) {
                timeout = 1
            } else if (timeout > Constants.MAX_AUTOAWAY_TIMEOUT) {
                timeout = Constants.MAX_AUTOAWAY_TIMEOUT
            }
            timeout.toString()
        } catch (e: Exception) {
            Timber.w("Unable to parse user input, user entered: $value")
            null
        }
    }

    /**
     * Establishing the value of Auto Away.
     *
     * @param value     The value.
     * @param cancelled If it is cancelled.
     */
    private fun setAutoAwayValue(value: String, cancelled: Boolean) {
        Timber.d("Value: $value")
        if (cancelled) {
            sttChat?.updatePresenceConfigChat(true)
        } else {
            val timeout = value.toInt()
            megaChatApi.setPresenceAutoaway(true, timeout * 60)
        }
    }

    /**
     * Enable or disable the visibility of last seen.
     *
     * @param enable True to enable. False to disable.
     */
    fun enableLastGreen(enable: Boolean) {
        Timber.d("Enable Last Green: $enable")
        megaChatApi.setLastGreenVisible(enable, null)
    }
}