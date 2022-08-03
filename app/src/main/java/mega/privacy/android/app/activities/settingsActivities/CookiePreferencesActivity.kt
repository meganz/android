package mega.privacy.android.app.activities.settingsActivities

import android.content.DialogInterface
import android.os.Bundle
import androidx.activity.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.settingsFragments.cookie.CookieSettingsFragment
import mega.privacy.android.app.fragments.settingsFragments.cookie.CookieSettingsViewModel
import mega.privacy.android.app.utils.StringResourcesUtils

@AndroidEntryPoint
class CookiePreferencesActivity : PreferencesBaseActivity() {

    private val viewModel by viewModels<CookieSettingsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        unregisterReceiver(cookieSettingsReceiver) // Don't need to show the confirmation snackbar here

        setTitle(R.string.settings_about_cookie_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        replaceFragment(CookieSettingsFragment())

        showSaveButton {
            saveCookieSettings()
        }
    }

    override fun onBackPressed() {
        if (psaWebBrowser != null && psaWebBrowser?.consumeBack() == true) return

        if (!viewModel.areCookiesSaved()) {
            showUnsavedDialog()
        } else {
            super.onBackPressed()
        }
    }

    private fun showUnsavedDialog() {
        MaterialAlertDialogBuilder(this)
            .setMessage(StringResourcesUtils.getString(R.string.dialog_cookie_alert_unsaved))
            .setPositiveButton(StringResourcesUtils.getString(R.string.save_action)) { _: DialogInterface, _: Int ->
                saveCookieSettings()
            }
            .setNegativeButton(StringResourcesUtils.getString(R.string.button_cancel), null)
            .create()
            .show()
    }

    private fun saveCookieSettings() {
        viewModel.saveCookieSettings()
        finish()
    }
}
