package mega.privacy.android.feature.payment.presentation.upgrade

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.core.sharedcomponents.serializable
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.feature.payment.model.AccountTypeInt
import mega.privacy.android.feature.payment.presentation.upgrade.ChooseAccountViewModel.Companion.EXTRA_IS_UPGRADE_ACCOUNT
import mega.privacy.android.navigation.ExtraConstant
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.payment.UpgradeAccountSource
import javax.inject.Inject

@AndroidEntryPoint
open class ChooseAccountActivity : AppCompatActivity() {

    @Inject
    lateinit var megaNavigator: MegaNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        if (savedInstanceState == null) {
            val fragment = ChooseAccountFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(
                        ExtraConstant.NEW_CREATION_ACCOUNT,
                        intent.getBooleanExtra(ExtraConstant.NEW_CREATION_ACCOUNT, true)
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
                .add(android.R.id.content, fragment)
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
        val bundle = Bundle().apply {
            intent.extras?.let { putAll(it) }
            putBoolean(ExtraConstant.EXTRA_FIRST_LOGIN, true)
            if (!containsKey(ExtraConstant.EXTRA_NEW_ACCOUNT)) {
                putBoolean(ExtraConstant.EXTRA_NEW_ACCOUNT, true)
            }
            if (!containsKey(ExtraConstant.NEW_CREATION_ACCOUNT)) {
                putBoolean(ExtraConstant.NEW_CREATION_ACCOUNT, true)
            }
            putBoolean(ExtraConstant.EXTRA_UPGRADE_ACCOUNT, accountType != AccountType.FREE)
            putInt(ExtraConstant.EXTRA_ACCOUNT_TYPE, accountTypeInt)
        }

        megaNavigator.openManagerActivity(
            context = this,
            data = intent.data,
            action = intent.action,
            bundle = bundle
        )
        finish()
    }

    private fun convertAccountTypeToInt(accountType: AccountType): Int {
        return when (accountType) {
            AccountType.PRO_LITE -> AccountTypeInt.PRO_LITE
            AccountType.PRO_I -> AccountTypeInt.PRO_I
            AccountType.PRO_II -> AccountTypeInt.PRO_II
            AccountType.PRO_III -> AccountTypeInt.PRO_III
            else -> AccountTypeInt.FREE
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
            source: UpgradeAccountSource = UpgradeAccountSource.UNKNOWN,
        ) {
            val intent = Intent(context, ChooseAccountActivity::class.java).apply {
                putExtra(EXTRA_IS_UPGRADE_ACCOUNT, true)
                putExtra(ExtraConstant.EXTRA_NEW_ACCOUNT, false)
                putExtra(ExtraConstant.NEW_CREATION_ACCOUNT, false)
                putExtra(EXTRA_SOURCE, source)
            }
            context.startActivity(intent)
        }
    }
}