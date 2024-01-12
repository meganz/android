package mega.privacy.android.app.upgradeAccount

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleEventObserver
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.security.PasscodeCheck
import javax.inject.Inject

@AndroidEntryPoint
class UpgradeAccountActivity : AppCompatActivity() {

    /**
     * A [LifecycleEventObserver] to display a passcode screen when the app is resumed
     */
    @Inject
    lateinit var passcodeFacade: PasscodeCheck

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_upgrade_account)

        if (savedInstanceState == null) {
            val fragment = UpgradeAccountFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.upgrade_account_container, fragment)
                .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val IS_CROSS_ACCOUNT_MATCH = "is_cross_account_match"
    }
}