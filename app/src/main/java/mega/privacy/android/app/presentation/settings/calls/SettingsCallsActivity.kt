package mega.privacy.android.app.presentation.settings.calls

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.extensions.enableEdgeToEdgeAndConsumeInsets
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature.chat.settings.calls.CallSettingsFragment
import javax.inject.Inject

/**
 * Activity which allows to change the calls settings.
 */
@AndroidEntryPoint
class SettingsCallsActivity : AppCompatActivity() {

    @Inject
    lateinit var passCodeFacade: PasscodeCheck

    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    @SuppressLint("CommitTransaction")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeAndConsumeInsets()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        setSupportActionBar(findViewById(R.id.settings_toolbar))
        if (savedInstanceState == null) {
            lifecycleScope.launch {
                if (getFeatureFlagValueUseCase(AppFeatures.CallSettingsNewComponents)) {
                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.root, CallSettingsFragment())
                        .commit()
                } else {
                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.settings, SettingsCallsFragment())
                        .commit()
                }
            }
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            else -> {
                super.onOptionsItemSelected(item)
            }
        }
}