package mega.privacy.android.app.main.dialog.storagestatus

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.core.sharedcomponents.serializable
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.core.nodecomponents.dialog.storage.StorageStatusDialogState
import mega.privacy.android.core.nodecomponents.dialog.storage.StorageStatusViewModel
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
internal class StorageStatusDialogFragment : DialogFragment() {
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    @Inject
    lateinit var megaNavigator: MegaNavigator

    private val viewModel by viewModels<StorageStatusViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Timber.d("showStorageStatusDialog")
        val storageState = requireArguments().serializable<StorageState>(EXTRA_STORAGE_STATE)!!
        val overQuotaAlert = requireArguments().getBoolean(EXTRA_OVER_QUOTA_ALERT)
        val preWarning = requireArguments().getBoolean(EXTRA_PRE_WARNING)
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by monitorThemeModeUseCase()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val uiState by viewModel.state.collectAsStateWithLifecycle()
                val coroutineScope = rememberCoroutineScope()
                OriginalTheme(isDark = themeMode.isDarkMode()) {
                    StorageStatusDialogView(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        state = StorageStatusDialogState(
                            storageState = storageState,
                            isAchievementsEnabled = uiState.isAchievementsEnabled,
                            product = uiState.product,
                            accountType = uiState.accountType,
                            preWarning = preWarning,
                            overQuotaAlert = overQuotaAlert
                        ),
                        dismissClickListener = { dismissAllowingStateLoss() },
                        actionButtonClickListener = {
                            coroutineScope.launch {
                                openUpdateAccountScreen(uiState.accountType)
                            }
                        },
                        achievementButtonClickListener = {
                            dismissAllowingStateLoss()
                            navigateToAchievements()
                        },
                        usePlatformDefaultWidth = false
                    )
                }
            }
        }
    }

    private suspend fun openUpdateAccountScreen(accountType: AccountType) {
        val email = viewModel.getUserEmail()
        when (accountType) {
            AccountType.PRO_III -> {
                AlertsAndWarnings.askForCustomizedPlan(requireContext(), email, accountType)
            }

            else -> {
                megaNavigator.openUpgradeAccount(
                    context = requireContext(),
                )
            }
        }
        dismissAllowingStateLoss()
    }

    private fun navigateToAchievements() {
        val accountIntent = Intent(requireContext(), MyAccountActivity::class.java)
            .setAction(IntentConstants.ACTION_OPEN_ACHIEVEMENTS)
        startActivity(accountIntent)
    }

    companion object {
        const val TAG = "StorageStatusDialogFragment"
        private const val EXTRA_STORAGE_STATE = "EXTRA_STORAGE_STATE"
        private const val EXTRA_OVER_QUOTA_ALERT = "EXTRA_OVER_QUOTA_ALERT"
        private const val EXTRA_PRE_WARNING = "EXTRA_PRE_WARNING"
        fun newInstance(
            storageState: StorageState,
            overQuotaAlert: Boolean,
            preWarning: Boolean,
        ) = StorageStatusDialogFragment().apply {
            arguments = bundleOf(
                EXTRA_STORAGE_STATE to storageState,
                EXTRA_OVER_QUOTA_ALERT to overQuotaAlert,
                EXTRA_PRE_WARNING to preWarning
            )
        }
    }
}