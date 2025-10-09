package mega.privacy.android.feature.payment.presentation.upgrade

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.core.sharedcomponents.container.AppContainerProvider
import mega.privacy.android.core.sharedcomponents.serializable
import mega.privacy.android.feature.payment.presentation.upgrade.ChooseAccountViewModel.Companion.EXTRA_IS_UPGRADE_ACCOUNT
import mega.privacy.android.navigation.ExtraConstant
import mega.privacy.android.navigation.payment.UpgradeAccountSource
import javax.inject.Inject

@AndroidEntryPoint
open class ChooseAccountActivity : AppCompatActivity() {

    @Inject
    lateinit var appContainerProvider: AppContainerProvider

    private val openFromSource by lazy {
        intent.serializable(EXTRA_SOURCE) ?: UpgradeAccountSource.UNKNOWN
    }

    private val isUpgradeAccount by lazy {
        intent.getBooleanExtra(EXTRA_IS_UPGRADE_ACCOUNT, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            appContainerProvider.buildSharedAppContainer(
                context = this,
                useLegacyStatusBarColor = false
            ) {
                ChooseAccountRoute(
                    isNewCreationAccount = intent.getBooleanExtra(ExtraConstant.NEW_CREATION_ACCOUNT, false),
                    isUpgradeAccount = isUpgradeAccount,
                    openFromSource = openFromSource,
                    onBack = ::finish
                )
            }
        )
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