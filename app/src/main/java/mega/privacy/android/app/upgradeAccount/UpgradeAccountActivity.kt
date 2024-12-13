package mega.privacy.android.app.upgradeAccount

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
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
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_upgrade_account)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                add(R.id.upgrade_account_container, UpgradeAccountFragment().apply {
                    arguments = intent.extras
                })
            }
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
        const val EXTRA_SOURCE = "EXTRA_SOURCE"

        /**
         * Navigates to the Upgrade Account Activity.
         */
        fun navigate(
            context: Context,
            source: UpgradeAccountSource = UpgradeAccountSource.UNKNOWN,
        ) {
            context.startActivity(Intent(context, UpgradeAccountActivity::class.java).apply {
                putExtra(EXTRA_SOURCE, source)
            })
        }
    }
}