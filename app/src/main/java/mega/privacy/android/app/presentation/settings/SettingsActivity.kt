package mega.privacy.android.app.presentation.settings

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentResultListener
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.presentation.settings.SettingsFragment.Companion.INITIAL_PREFERENCE
import mega.privacy.android.app.presentation.settings.SettingsFragment.Companion.NAVIGATE_TO_INITIAL_PREFERENCE
import mega.privacy.android.app.presentation.settings.model.TargetPreference
import org.jetbrains.anko.configuration
import javax.inject.Inject

private const val TITLE_TAG = "settingsActivityTitle"

@AndroidEntryPoint
class SettingsActivity : BaseActivity(),
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    @Inject
    lateinit var passCodeFacade: PasscodeCheck

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (shouldRefreshSessionDueToSDK(true)) return
        setContentView(R.layout.settings_activity)
        setSupportActionBar(findViewById(R.id.settings_toolbar))
        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.settings, SettingsFragment().apply {
                        arguments = intent.extras
                    })
                    .commit()
        } else {
            title = savedInstanceState.getCharSequence(TITLE_TAG)
        }
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                setTitle(R.string.action_settings)
            }
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = when (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> false
            else -> true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, title)
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.popBackStackImmediate()) {
            return true
        }
        return super.onSupportNavigateUp()
    }

    override fun onPreferenceStartFragment(
            caller: PreferenceFragmentCompat,
            pref: Preference
    ): Boolean {
        // Instantiate the new Fragment
        return pref.fragment?.let {
            val args = pref.extras
            val fragment = supportFragmentManager.fragmentFactory.instantiate(
                    classLoader,
                    it
            ).apply {
                arguments = args
            }
            // Replace the existing Fragment with the new Fragment
            supportFragmentManager.beginTransaction()
                    .replace(R.id.settings, fragment, pref.key)
                    .addToBackStack(null)
                    .commit()

            if (caller is FragmentResultListener) {
                supportFragmentManager.setFragmentResultListener(pref.key, this, caller)
//            In the calling fragment, implement FragmentResultListener to handle results for a specific preference key
//            In the Called Fragment, setFragmentResult using the tag to pass back any results, use the fragment name for the bundle key
        }

            title = pref.title
            true
        } ?: false
    }

    companion object {
        fun getIntent(context: Context, targetPreference: TargetPreference? = null): Intent {
            return Intent(context, SettingsActivity::class.java).apply {
                putExtra(INITIAL_PREFERENCE, targetPreference?.preferenceId)
                putExtra(NAVIGATE_TO_INITIAL_PREFERENCE, targetPreference?.requiresNavigation)
            }
        }

    }

}