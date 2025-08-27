package mega.privacy.android.app.upgradeAccount

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.core.sharedcomponents.serializable
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.feature.payment.presentation.upgrade.ChooseAccountViewModel.Companion.EXTRA_IS_UPGRADE_ACCOUNT

@AndroidEntryPoint
open class ChooseAccountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_choose_account)

        if (savedInstanceState == null) {
            val fragment = ChooseAccountFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(
                        ManagerActivity.NEW_CREATION_ACCOUNT,
                        intent.getBooleanExtra(ManagerActivity.NEW_CREATION_ACCOUNT, true)
                    )
                    putBoolean(
                        EXTRA_IS_UPGRADE_ACCOUNT,
                        intent.getBooleanExtra(EXTRA_IS_UPGRADE_ACCOUNT, false)
                    )
                    putSerializable(
                        EXTRA_SOURCE,
                        intent.serializable(EXTRA_SOURCE)
                    )
                }
            }
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
        val intent = Intent(this, ManagerActivity::class.java).apply {
            putExtras(intent)
            putExtra(IntentConstants.EXTRA_FIRST_LOGIN, true)
            if (extras?.containsKey(IntentConstants.EXTRA_NEW_ACCOUNT) != true) {
                putExtra(IntentConstants.EXTRA_NEW_ACCOUNT, true)
            }
            if (extras?.containsKey(ManagerActivity.NEW_CREATION_ACCOUNT) != true) {
                putExtra(ManagerActivity.NEW_CREATION_ACCOUNT, true)
            }
            putExtra(IntentConstants.EXTRA_UPGRADE_ACCOUNT, accountTypeInt != Constants.FREE)
            putExtra(IntentConstants.EXTRA_ACCOUNT_TYPE, accountTypeInt)
        }

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

    companion object {
        /**
         * Extra key to indicate the source of the upgrade account action.
         */
        const val EXTRA_SOURCE = "EXTRA_SOURCE"

        /**
         * Navigates to the Upgrade Account screen.
         *
         * @param context The context to use for navigation.
         */
        fun navigateToUpgradeAccount(
            context: Context,
            isFromAdsFree: Boolean = false,
        ) {
            val intent = Intent(context, ChooseAccountActivity::class.java).apply {
                putExtra(EXTRA_IS_UPGRADE_ACCOUNT, true)
                putExtra(IntentConstants.EXTRA_NEW_ACCOUNT, false)
                putExtra(ManagerActivity.NEW_CREATION_ACCOUNT, false)
                if (isFromAdsFree) {
                    putExtra(EXTRA_SOURCE, UpgradeAccountSource.ADS_FREE_SCREEN)
                }
            }
            context.startActivity(intent)
        }
    }
}