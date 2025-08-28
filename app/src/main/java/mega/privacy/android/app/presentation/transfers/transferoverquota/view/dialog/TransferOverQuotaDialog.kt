package mega.privacy.android.app.presentation.transfers.transferoverquota.view.dialog

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.transfers.transferoverquota.TransferOverQuotaViewModel
import mega.privacy.android.app.utils.Constants.LOGIN_FRAGMENT
import mega.privacy.android.app.utils.Constants.VISIBLE_FRAGMENT
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.navigation.megaNavigator
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemeRtlPreviews
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.TransferOverQuotaDialogEvent
import mega.privacy.mobile.analytics.event.TransferOverQuotaUpgradeAccountButtonEvent

@Composable
internal fun TransferOverQuotaDialog(
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<TransferOverQuotaViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    uiState.bandwidthOverQuotaDelay?.let { bandwidthOverQuotaDelay ->
        // Countdown timer state
        var overQuotaDelay: Long by rememberSaveable {
            mutableLongStateOf(bandwidthOverQuotaDelay.inWholeSeconds)
        }

        // Countdown timer effect
        LaunchedEffect(bandwidthOverQuotaDelay) {
            while (overQuotaDelay > 0) {
                delay(1000) // Wait for 1 second
                overQuotaDelay--

                if (overQuotaDelay == 0L) {
                    viewModel.bandwidthOverQuotaDelayConsumed()
                }
            }
        }

        TransferOverQuotaDialogContent(
            isLoggedIn = uiState.isLoggedIn,
            isFreeAccount = uiState.isFreeAccount,
            overQuotaDelay = TimeUtils.getHumanizedTime(overQuotaDelay),
            onNavigateToUpgradeAccount = {
                context.megaNavigator.openUpgradeAccount(context)
            },
            onNavigateToLogin = {
                Intent(context, LoginActivity::class.java).apply {
                    putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }.also { intent -> context.startActivity(intent) }
            },
            onDismiss = viewModel::bandwidthOverQuotaDelayConsumed,
            modifier = modifier
        )
    }
}

@Composable
internal fun TransferOverQuotaDialogContent(
    isLoggedIn: Boolean,
    isFreeAccount: Boolean,
    overQuotaDelay: String,
    onNavigateToUpgradeAccount: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        Analytics.tracker.trackEvent(TransferOverQuotaDialogEvent)
    }

    MegaAlertDialog(
        title = stringResource(R.string.title_depleted_transfer_overquota),
        body = stringResource(
            R.string.text_depleted_transfer_overquota,
            overQuotaDelay
        ),
        icon = R.drawable.transfer_quota_empty,
        confirmButtonText = stringResource(
            when {
                isLoggedIn && isFreeAccount -> sharedR.string.general_upgrade_button
                isLoggedIn -> R.string.plans_depleted_transfer_overquota
                else -> sharedR.string.login_text
            }
        ),
        cancelButtonText = stringResource(R.string.general_dismiss),
        onConfirm = {
            if (isLoggedIn) {
                Analytics.tracker.trackEvent(TransferOverQuotaUpgradeAccountButtonEvent)
                onNavigateToUpgradeAccount()
            } else {
                onNavigateToLogin()
            }
            onDismiss()
        },
        onDismiss = onDismiss,
        modifier = modifier,
        dismissOnClickOutside = false,
        dismissOnBackPress = false,
    )
}

@CombinedThemeRtlPreviews
@Composable
private fun TransferOverQuotaDialogPreview(
    @PreviewParameter(TransferOverQuotaDialogPreviewProvider::class) previewState: TransferOverQuotaDialogPreviewState,
) {
    AndroidThemeForPreviews {
        Box(modifier = Modifier.fillMaxSize()) {
            TransferOverQuotaDialogContent(
                isLoggedIn = previewState.isLoggedIn,
                isFreeAccount = previewState.isFreeAccount,
                overQuotaDelay = "1m 12s",
                onNavigateToUpgradeAccount = {},
                onNavigateToLogin = {},
                onDismiss = {},
            )
        }
    }
}

private class TransferOverQuotaDialogPreviewProvider :
    PreviewParameterProvider<TransferOverQuotaDialogPreviewState?> {
    override val values = sequenceOf(
        TransferOverQuotaDialogPreviewState(
            isLoggedIn = false,
            isFreeAccount = false,
        ),
        TransferOverQuotaDialogPreviewState(
            isLoggedIn = true,
            isFreeAccount = true,
        ),
        TransferOverQuotaDialogPreviewState(
            isLoggedIn = true,
            isFreeAccount = false,
        ),
        TransferOverQuotaDialogPreviewState(
            isLoggedIn = false,
            isFreeAccount = true,
        )
    )
}

private data class TransferOverQuotaDialogPreviewState(
    val isLoggedIn: Boolean,
    val isFreeAccount: Boolean,
)