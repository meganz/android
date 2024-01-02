package mega.privacy.android.app.fragments.settingsFragments.cookie

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.components.TwoButtonsPreference
import mega.privacy.android.app.constants.SettingsConstants.KEY_COOKIE_ACCEPT
import mega.privacy.android.app.constants.SettingsConstants.KEY_COOKIE_ADS
import mega.privacy.android.app.constants.SettingsConstants.KEY_COOKIE_ANALYTICS
import mega.privacy.android.app.constants.SettingsConstants.KEY_COOKIE_POLICIES
import mega.privacy.android.app.featuretoggle.ABTestFeatures
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.fragments.settingsFragments.SettingsBaseFragment
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.entity.settings.cookie.CookieType.ADVERTISEMENT
import mega.privacy.android.domain.entity.settings.cookie.CookieType.ANALYTICS
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class CookieSettingsFragment : SettingsBaseFragment(),
    Preference.OnPreferenceChangeListener {

    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    private val viewModel by activityViewModels<CookieSettingsViewModel>()

    private var acceptCookiesPreference: SwitchPreferenceCompat? = null
    private var analyticsCookiesPreference: SwitchPreferenceCompat? = null
    private var adsCookiesPreference: SwitchPreferenceCompat? = null
    private var policiesPreference: TwoButtonsPreference? = null
    private var showAdsCookiePreference = false

    override fun onCreatePreferences(bundle: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_cookie)

        acceptCookiesPreference = findPreference(KEY_COOKIE_ACCEPT)
        analyticsCookiesPreference = findPreference(KEY_COOKIE_ANALYTICS)
        adsCookiesPreference = findPreference(KEY_COOKIE_ADS)
        policiesPreference = findPreference(KEY_COOKIE_POLICIES)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        checkForInAppAdvertisement()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupView()
    }

    private fun checkForInAppAdvertisement() {
        lifecycleScope.launch {
            runCatching {
                if (getFeatureFlagValueUseCase(AppFeatures.InAppAdvertisement) &&
                    getFeatureFlagValueUseCase(ABTestFeatures.ads) &&
                    getFeatureFlagValueUseCase(ABTestFeatures.adse)
                ) {
                    showAdsCookiePreference = true
                    adsCookiesPreference?.isVisible = true
                }
            }.onFailure {
                Timber.e("Failed to fetch feature flag with error: ${it.message}")
            }
        }
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
        acceptCookiesPreference?.onPreferenceChangeListener = this
        analyticsCookiesPreference?.onPreferenceChangeListener = this
        adsCookiesPreference?.onPreferenceChangeListener = this
        policiesPreference?.apply {
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
        analyticsCookiesPreference?.isChecked = cookies.contains(ANALYTICS) == true
        adsCookiesPreference?.isChecked = cookies.contains(ADVERTISEMENT) == true
        if (showAdsCookiePreference) {
            acceptCookiesPreference?.isChecked = analyticsCookiesPreference?.isChecked ?: false ||
                    adsCookiesPreference?.isChecked ?: false
        } else {
            acceptCookiesPreference?.isChecked = analyticsCookiesPreference?.isChecked ?: false
        }
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
            adsCookiesPreference?.key -> viewModel.changeCookie(ADVERTISEMENT, enable)
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
