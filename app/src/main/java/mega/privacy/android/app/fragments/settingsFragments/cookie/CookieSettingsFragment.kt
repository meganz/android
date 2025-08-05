package mega.privacy.android.app.fragments.settingsFragments.cookie

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.TwoButtonsPreference
import mega.privacy.android.app.constants.SettingsConstants.KEY_ADS_PERSONALIZATION
import mega.privacy.android.app.constants.SettingsConstants.KEY_ADS_SETTING
import mega.privacy.android.app.constants.SettingsConstants.KEY_COOKIE_ACCEPT
import mega.privacy.android.app.constants.SettingsConstants.KEY_COOKIE_ANALYTICS
import mega.privacy.android.app.constants.SettingsConstants.KEY_COOKIE_POLICIES
import mega.privacy.android.app.constants.SettingsConstants.KEY_COOKIE_SETTINGS
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.fragments.settingsFragments.SettingsBaseFragment
import mega.privacy.android.app.presentation.advertisements.GoogleAdsManager
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.entity.settings.cookie.CookieType.ANALYTICS
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase
import javax.inject.Inject

@AndroidEntryPoint
class CookieSettingsFragment : SettingsBaseFragment(),
    Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    @Inject
    lateinit var googleAdsManager: GoogleAdsManager

    @Inject
    lateinit var getDomainNameUseCase: GetDomainNameUseCase

    private val viewModel by viewModels<CookieSettingsViewModel>()

    private var acceptCookiesPreference: SwitchPreferenceCompat? = null
    private var analyticsCookiesPreference: SwitchPreferenceCompat? = null
    private var policiesPreference: TwoButtonsPreference? = null
    private var adsSettings: PreferenceCategory? = null
    private var adsPersonalization: Preference? = null
    private var settingsCookie: PreferenceCategory? = null

    override fun onCreatePreferences(bundle: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_cookie)

        acceptCookiesPreference = findPreference(KEY_COOKIE_ACCEPT)
        analyticsCookiesPreference = findPreference(KEY_COOKIE_ANALYTICS)
        policiesPreference = findPreference(KEY_COOKIE_POLICIES)
        adsSettings = findPreference(KEY_ADS_SETTING) as? PreferenceCategory
        adsPersonalization = findPreference(KEY_ADS_PERSONALIZATION)
        settingsCookie = findPreference(KEY_COOKIE_SETTINGS) as? PreferenceCategory
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupView()
    }

    private fun setupObservers() {
        collectFlow(viewModel.uiState) { _ ->
            updateAcceptCookiesPreference()
        }
        viewModel.onEnabledCookies().observe(viewLifecycleOwner, ::showCookies)
        viewLifecycleOwner.collectFlow(viewModel.onUpdateResult()) { value ->
            if (value == true) {
                (context?.applicationContext as MegaApplication?)?.checkEnabledCookies()
            } else if (value == false) {
                Toast.makeText(requireContext(), R.string.error_unknown, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupView() {
        viewLifecycleOwner.lifecycleScope.launch {
            googleAdsManager.checkLatestConsentInformation(requireActivity()) {
                with(googleAdsManager.isPrivacyOptionsRequired) {
                    adsSettings?.isVisible = this
                    settingsCookie?.isVisible = this
                }
            }
        }
        acceptCookiesPreference?.onPreferenceChangeListener = this
        analyticsCookiesPreference?.onPreferenceChangeListener = this
        policiesPreference?.apply {
            setButton1(getString(R.string.settings_about_cookie_policy)) {
                context.launchUrl(cookieUrl())
            }
            setButton2(getString(R.string.settings_about_privacy_policy)) {
                context.launchUrl(privacyUrl())
            }
        }
        adsPersonalization?.onPreferenceClickListener = this
    }

    /**
     * Update the accept cookies preference based on the current state of the other cookies
     * preferences.
     *
     */
    private fun updateAcceptCookiesPreference() {
        acceptCookiesPreference?.isChecked = analyticsCookiesPreference?.isChecked == true
    }

    /**
     * Show current cookies configuration by toggling each cookie switch.
     *
     * @param cookies   Set of enabled cookies
     */
    private fun showCookies(cookies: Set<CookieType>) {
        analyticsCookiesPreference?.isChecked = cookies.contains(ANALYTICS) == true
        updateAcceptCookiesPreference()
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
            acceptCookiesPreference?.key -> viewModel.toggleCookies(enable)
            analyticsCookiesPreference?.key -> viewModel.changeCookie(ANALYTICS, enable)
        }

        return false
    }

    /**
     * Called when a preference has been clicked.
     */
    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            adsPersonalization?.key -> googleAdsManager.showPrivacyOptionsForm(requireActivity())
        }

        return true
    }

    private fun cookieUrl() = "https://${getDomainNameUseCase()}/cookie"
    private fun privacyUrl() = "https://${getDomainNameUseCase()}/privacy"
}
