package mega.privacy.android.app.upgradeAccount

import android.os.Bundle
import android.view.MenuItem
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity

class UpgradeAccountActivity : PasscodeActivity() {

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
}