package mega.privacy.android.app.fragments.settingsFragments.cookie

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.components.TwoButtonsPreference
import mega.privacy.android.app.constants.SettingsConstants.KEY_COOKIE_ACCEPT
import mega.privacy.android.app.constants.SettingsConstants.KEY_COOKIE_ANALYTICS
import mega.privacy.android.app.constants.SettingsConstants.KEY_COOKIE_POLICIES
import mega.privacy.android.app.fragments.settingsFragments.SettingsBaseFragment
import mega.privacy.android.app.fragments.settingsFragments.cookie.data.CookieType
import mega.privacy.android.app.fragments.settingsFragments.cookie.data.CookieType.ANALYTICS

@AndroidEntryPoint
class CookieSettingsFragment : SettingsBaseFragment() {

    private val viewModel by activityViewModels<CookieSettingsViewModel>()

    private lateinit var acceptCookiesPreference: SwitchPreferenceCompat
    private lateinit var analyticsCookiesPreference: SwitchPreferenceCompat
    private lateinit var policiesPreference: TwoButtonsPreference

    override fun onCreatePreferences(bundle: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_cookie)

        acceptCookiesPreference = findPreference(KEY_COOKIE_ACCEPT)!!
        analyticsCookiesPreference = findPreference(KEY_COOKIE_ANALYTICS)!!
        policiesPreference = findPreference(KEY_COOKIE_POLICIES)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupView()
    }

    private fun setupObservers() {
        viewModel.onEnabledCookies().observe(viewLifecycleOwner, ::showCookies)
        viewModel.onUpdateResult().observe(viewLifecycleOwner) { success ->
            if (success) {
                (context?.applicationContext as MegaApplication?)?.checkEnabledCookies()
            } else if (isVisible) {
                Toast.makeText(requireContext(), R.string.error_unknown, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupView() {
        acceptCookiesPreference.onPreferenceChangeListener = this
        analyticsCookiesPreference.onPreferenceChangeListener = this

        policiesPreference.apply {
            setButton1(getString(R.string.settings_about_cookie_policy)) {
                openBrowser("https://mega.nz/cookie".toUri())
            }
            setButton2(getString(R.string.settings_about_privacy_policy)) {
                openBrowser("https://mega.nz/privacy".toUri())
            }
        }
    }

    /**
     * Show current cookies configuration by toggling each cookie switch.
     *
     * @param cookies   Set of enabled cookies
     */
    private fun showCookies(cookies: Set<CookieType>) {
        analyticsCookiesPreference.isChecked = cookies.contains(ANALYTICS) == true

        acceptCookiesPreference.isChecked = analyticsCookiesPreference.isChecked
    }

    /**
     * Called when a preference has been changed by the user. This is called before the state
     * of the preference is about to be updated and before the state is persisted.
     *
     * @param preference The changed preference
     * @param newValue   The new value of the preference
     * @return {@code true} to update the state of the preference with the new value
     */
    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val enable = newValue as? Boolean ?: false

        when (preference.key) {
            acceptCookiesPreference.key -> viewModel.toggleCookies(enable)
            analyticsCookiesPreference.key -> viewModel.changeCookie(ANALYTICS, enable)
        }

        return false
    }

    /**
     * Open browser screen to show an Uri
     *
     * @param uri   Uri to be shown on the browser
     */
    private fun openBrowser(uri: Uri) {
        startActivity(Intent(requireContext(), WebViewActivity::class.java).apply {
            data = uri
        })
    }
}
