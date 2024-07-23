package mega.privacy.android.app.upgradeAccount

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R

/**
 * Activity to upgrade an account.
 */
@AndroidEntryPoint
class UpgradeAccountActivity : AppCompatActivity() {

    /**
     * Initializes the activity.
     *
     * @param savedInstanceState Bundle containing the activity's previously saved state.
     */
    @SuppressLint("CommitTransaction")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_upgrade_account)

        if (savedInstanceState == null) {
            val fragment = UpgradeAccountFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.upgrade_account_container, fragment)
                .commit()
        }
    }


    /**
     * Handles the back button press.
     *
     * @param item The menu item that was selected.
     * @return True if the event was handled, false otherwise.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {

        /**
         * Key to save the flag indicating if the match is cross account.
         */
        const val IS_CROSS_ACCOUNT_MATCH = "is_cross_account_match"
    }
}