package mega.privacy.android.app.fragments.settingsFragments.cookie

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.components.ClickableSummarySwitchPreference
import mega.privacy.android.app.components.StaticSwitchPreference
import mega.privacy.android.app.components.TwoButtonsPreference
import mega.privacy.android.app.constants.SettingsConstants.*
import mega.privacy.android.app.fragments.settingsFragments.SettingsBaseFragment
import mega.privacy.android.app.fragments.settingsFragments.cookie.data.CookieType
import mega.privacy.android.app.fragments.settingsFragments.cookie.data.CookieType.*

@AndroidEntryPoint
class CookieSettingsFragment : SettingsBaseFragment() {

    private val viewModel by viewModels<CookieSettingsViewModel>()

    private lateinit var acceptCookiesPreference: SwitchPreferenceCompat
    private lateinit var essentialCookiesPreference: StaticSwitchPreference
    private lateinit var preferenceCookiesPreference: SwitchPreferenceCompat
    private lateinit var analyticsCookiesPreference: SwitchPreferenceCompat
    private lateinit var advertisingCookiesPreference: SwitchPreferenceCompat
    private lateinit var thirdPartyCookiesPreference: ClickableSummarySwitchPreference
    private lateinit var policiesPreference: TwoButtonsPreference

    override fun onCreatePreferences(bundle: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_cookie)

        acceptCookiesPreference = findPreference(KEY_COOKIE_ACCEPT)!!
        essentialCookiesPreference = findPreference(KEY_COOKIE_ESSENTIAL)!!
        preferenceCookiesPreference = findPreference(KEY_COOKIE_PREFERENCE)!!
        analyticsCookiesPreference = findPreference(KEY_COOKIE_ANALYTICS)!!
        advertisingCookiesPreference = findPreference(KEY_COOKIE_ADVERTISING)!!
        thirdPartyCookiesPreference = findPreference(KEY_COOKIE_THIRD_PARTY)!!
        policiesPreference = findPreference(KEY_COOKIE_POLICIES)!!
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupObservers()
        setupView()
    }

    private fun setupObservers() {
        viewModel.onEnabledCookies().observe(viewLifecycleOwner, ::showCookies)
        viewModel.onUpdateResult().observe(viewLifecycleOwner) { success ->
            if (success) {
                (context?.applicationContext as MegaApplication?)?.checkEnabledCookies()
            }
        }
    }

    private fun setupView() {
        acceptCookiesPreference.onPreferenceChangeListener = this
        preferenceCookiesPreference.onPreferenceChangeListener = this
        analyticsCookiesPreference.onPreferenceChangeListener = this
        advertisingCookiesPreference.onPreferenceChangeListener = this
        thirdPartyCookiesPreference.onPreferenceChangeListener = this
        thirdPartyCookiesPreference.setOnSummaryClickListener {
            showThirdPartyInfoDialog()
        }
        policiesPreference.setButton1(getString(R.string.preference_cookies_policies_cookie)) {
            openBrowser("https://mega.nz/cookie".toUri())
        }
        policiesPreference.setButton2(getString(R.string.preference_cookies_policies_privacy)) {
            openBrowser("https://mega.nz/privacy".toUri())
        }
    }

    /**
     * Show current cookies configuration by toggling each cookie switch.
     *
     * @param cookies   Set of enabled cookies
     */
    private fun showCookies(cookies: Set<CookieType>?) {
        essentialCookiesPreference.isChecked = true
        preferenceCookiesPreference.isChecked = cookies?.contains(PREFERENCE) == true
        analyticsCookiesPreference.isChecked = cookies?.contains(ANALYTICS) == true
        advertisingCookiesPreference.isChecked = cookies?.contains(ADVERTISEMENT) == true
        thirdPartyCookiesPreference.isChecked = cookies?.contains(THIRDPARTY) == true

        acceptCookiesPreference.isChecked = preferenceCookiesPreference.isChecked &&
                analyticsCookiesPreference.isChecked &&
                advertisingCookiesPreference.isChecked &&
                thirdPartyCookiesPreference.isChecked
    }

    /**
     * Called when a preference has been changed by the user. This is called before the state
     * of the preference is about to be updated and before the state is persisted.
     *
     * @param preference The changed preference
     * @param newValue   The new value of the preference
     * @return {@code true} to update the state of the preference with the new value
     */
    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        val enable = newValue as? Boolean ?: false

        when (preference?.key) {
            acceptCookiesPreference.key -> viewModel.toggleCookies(enable)
            preferenceCookiesPreference.key -> viewModel.changeCookie(PREFERENCE, enable)
            analyticsCookiesPreference.key -> viewModel.changeCookie(ANALYTICS, enable)
            advertisingCookiesPreference.key -> viewModel.changeCookie(ADVERTISEMENT, enable)
            thirdPartyCookiesPreference.key -> viewModel.changeCookie(THIRDPARTY, enable)
        }

        return false
    }

    /**
     * Show third party information dialog.
     */
    private fun showThirdPartyInfoDialog() {
        MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialogStyle)
            .setView(R.layout.dialog_cookie_thirdparty)
            .setPositiveButton(android.R.string.yes, null)
            .create()
            .show()
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
