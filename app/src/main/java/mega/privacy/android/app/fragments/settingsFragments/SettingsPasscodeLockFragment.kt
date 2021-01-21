package mega.privacy.android.app.fragments.settingsFragments

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.lollipop.PinLockActivityLollipop
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.StringResourcesUtils

class SettingsPasscodeLockFragment : SettingsBaseFragment() {
    companion object {
        private const val INVALID_OPTION = -1
        private const val PIN_4_OPTION = 0
        private const val PIN_6_OPTION = 1
        private const val PIN_ALPHANUMERIC_OPTION = 2
    }

    private var pinLockEnableSwitch: SwitchPreferenceCompat? = null
    private var pinLockCode: Preference? = null
    private var pinLockCodeTxt: String? = ""
    private var pinLock = false

    private lateinit var setPinDialog: AlertDialog

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_passcode)
        pinLockEnableSwitch = findPreference(SettingsConstants.KEY_PIN_LOCK_ENABLE)
        pinLockEnableSwitch?.onPreferenceClickListener = this

        pinLockCode = findPreference(SettingsConstants.KEY_PIN_LOCK_CODE)
        pinLockCode?.onPreferenceClickListener = this
        pinLockCode?.onPreferenceChangeListener = this

        updatePinLock()

        if (pinLock) {
            dbH.setPinLockCode(pinLockCodeTxt)
            preferenceScreen.addPreference(pinLockCode)
        } else {
            preferenceScreen.removePreference(pinLockCode)
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            SettingsConstants.KEY_PIN_LOCK_ENABLE -> {
                pinLock = !pinLock

                if (pinLock) {
                    showPanelSetPinLock()
                } else {
                    dbH.setPinLockEnabled(false)
                    dbH.setPinLockCode("")
                    preferenceScreen.removePreference(pinLockCode)
                }
            }

            SettingsConstants.KEY_PIN_LOCK_CODE -> resetPinLock()
        }
        return true
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        prefs = dbH.preferences

        if (preference.key == SettingsConstants.KEY_PIN_LOCK_CODE) {
            dbH.setPinLockCode(newValue.toString())
        }

        return true
    }

    /**
     * Method for show the Panel to update the pin.
     */
    private fun showPanelSetPinLock() {
        val items = arrayOf<CharSequence>(
            getString(R.string.four_pin_lock),
            getString(R.string.six_pin_lock),
            getString(R.string.AN_pin_lock)
        )

        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.apply {
            setSingleChoiceItems(
                items,
                INVALID_OPTION
            ) { dialog: DialogInterface?, item: Int ->
                when (item) {
                    PIN_4_OPTION -> {
                        dbH.setPinLockType(Constants.PIN_4)
                        intentToPinLock()
                    }
                    PIN_6_OPTION -> {
                        dbH.setPinLockType(Constants.PIN_6)
                        intentToPinLock()
                    }
                    PIN_ALPHANUMERIC_OPTION -> {
                        dbH.setPinLockType(Constants.PIN_ALPHANUMERIC)
                        intentToPinLock()
                    }
                }

                dialog?.dismiss()
            }

            setTitle(StringResourcesUtils.getString(R.string.pin_lock_type))

            setOnKeyListener { _: DialogInterface?, keyCode: Int, _: KeyEvent? ->
                return@setOnKeyListener if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dismissDialog()
                    true
                } else false
            }

            setOnCancelListener { dismissDialog() }
        }

        setPinDialog = dialogBuilder.create()
        setPinDialog.show()
    }

    private fun dismissDialog() {
        if (!isPinDialogInitialized()) return

        setPinDialog.dismiss()
        cancelSetPinLock()
    }

    private fun isPinDialogInitialized(): Boolean {
        return this::setPinDialog.isInitialized
    }

    override fun onResume() {
        prefs = dbH.preferences
        updatePinLock()
        super.onResume()
    }

    private fun updatePinLock() {
        if (prefs == null || prefs.pinLockEnabled == null) {
            cancelSetPinLock()
        } else {
            pinLock = java.lang.Boolean.parseBoolean(prefs.pinLockEnabled)
            pinLockEnableSwitch!!.isChecked = pinLock
            pinLockCodeTxt = prefs.pinLockCode

            if (pinLockCodeTxt == null) {
                pinLockCodeTxt = ""
                dbH.setPinLockCode(pinLockCodeTxt)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        prefs = dbH.preferences

        if (requestCode != Constants.SET_PIN) return

        if (resultCode == Activity.RESULT_OK) {
            afterSetPinLock()
        } else {
            LogUtil.logWarning("Set PIN ERROR")
        }
    }

    private fun afterSetPinLock() {
        prefs = dbH.preferences

        if (prefs != null) {
            pinLockCodeTxt = prefs.pinLockCode

            if (pinLockCodeTxt == null) {
                pinLockCodeTxt = ""
            }

            dbH.setPinLockCode(pinLockCodeTxt)
            preferenceScreen.addPreference(pinLockCode)
            dbH.setPinLockEnabled(true)
        }
    }

    private fun cancelSetPinLock() {
        pinLock = false
        pinLockEnableSwitch!!.isChecked = pinLock
        dbH.setPinLockEnabled(false)
        dbH.setPinLockCode("")
    }

    private fun resetPinLock() {
        val intent = Intent(context, PinLockActivityLollipop::class.java)
        intent.action = PinLockActivityLollipop.ACTION_RESET_PIN_LOCK
        startActivity(intent)
    }

    private fun intentToPinLock() {
        val intent = Intent(context, PinLockActivityLollipop::class.java)
        intent.action = PinLockActivityLollipop.ACTION_SET_PIN_LOCK
        startActivityForResult(intent, Constants.SET_PIN)
    }
}