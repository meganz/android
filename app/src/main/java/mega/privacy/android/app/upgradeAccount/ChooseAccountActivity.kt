package mega.privacy.android.app.upgradeAccount

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.AccountType

open class ChooseAccountActivity : PasscodeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(R.layout.activity_choose_account)

        if (savedInstanceState == null) {
            val fragment = ChooseAccountFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.choose_account_container, fragment)
                .commit()
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onFreeClick()
        }

        return super.onOptionsItemSelected(item)
    }

    internal fun onFreeClick() {
        onPlanClicked(AccountType.FREE)
    }

    /**
     * Select a payment for the new account
     *
     * @param accountType Selected payment plan.
     */
    internal fun onPlanClicked(accountType: AccountType) {
        val accountTypeInt = convertAccountTypeToInt(accountType)
        val intent = Intent(this, ManagerActivity::class.java)
            .putExtra(IntentConstants.EXTRA_FIRST_LOGIN, true)
            .putExtra(IntentConstants.EXTRA_NEW_ACCOUNT, true)
            .putExtra(ManagerActivity.NEW_CREATION_ACCOUNT, true)
            .putExtra(IntentConstants.EXTRA_UPGRADE_ACCOUNT, accountTypeInt != Constants.FREE)
            .putExtra(IntentConstants.EXTRA_ACCOUNT_TYPE, accountTypeInt)

        startActivity(intent)
        finish()
    }

    private fun convertAccountTypeToInt(accountType: AccountType): Int {
        return when (accountType) {
            AccountType.PRO_LITE -> Constants.PRO_LITE
            AccountType.PRO_I -> Constants.PRO_I
            AccountType.PRO_II -> Constants.PRO_II
            AccountType.PRO_III -> Constants.PRO_III
            else -> Constants.FREE
        }
    }
}