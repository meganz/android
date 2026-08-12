package mega.privacy.android.feature.contact.invite.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.privacy.android.feature.contact.invite.InviteContactViewModel
import mega.privacy.android.feature.contact.invite.model.InvitationResult
import mega.privacy.android.feature.contact.invite.model.InviteContactUiState
import mega.privacy.android.feature.contact.invite.model.SmsInvite
import mega.privacy.android.feature.contact.invite.view.InviteContactsScreen
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.InviteContactNavKey

/**
 * Invite contact entry. Renders the redesigned invite flow and threads every screen callback to the
 * [InviteContactViewModel]. The app-only side-effects that require platform intents (sending SMS,
 * sharing the contact link, opening the personal QR code) or app navigation (the invitation-result
 * snackbar) are hoisted out as callbacks supplied by the app-module host.
 *
 * The two events the screen does not consume itself are handled here: the invitation result is either
 * returned to the achievements caller as a sent-count via [NavigationHandler.returnResult] or surfaced
 * as a snackbar through [onShowInviteSnackbar]; the SMS request is delegated to [onSendSms].
 *
 * Hosted by the app module's gated `InviteContactNavKey` destination (behind `ContactComposeFeature`).
 *
 * @param navigationHandler
 * @param isFromAchievement whether the flow was launched from the achievements screen.
 * @param onShareLink invoked with the contact link when the share-link toolbar action is tapped.
 * @param onOpenMyQr invoked when the my-QR-code toolbar action is tapped.
 * @param onSendSms invoked with the SMS invitation request to launch the device SMS composer.
 * @param onShowInviteSnackbar invoked with the invitation result to surface as a snackbar.
 */
@SuppressLint("ComposeViewModelInjection")
@Composable
fun InviteContactEntry(
    navigationHandler: NavigationHandler,
    isFromAchievement: Boolean,
    onShareLink: (String) -> Unit,
    onOpenMyQr: () -> Unit,
    onSendSms: (SmsInvite) -> Unit,
    onShowInviteSnackbar: (InvitationResult.Snackbar) -> Unit,
) {
    val viewModel = hiltViewModel<InviteContactViewModel, InviteContactViewModel.Factory> { factory ->
        factory.create(isFromAchievement = isFromAchievement)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    (state as? InviteContactUiState.Data)?.let { data ->
        EventEffect(
            event = data.invitationResultEvent,
            onConsumed = viewModel::onInvitationResultConsumed,
        ) { result ->
            when (result) {
                is InvitationResult.Achievement ->
                    navigationHandler.returnResult(InviteContactNavKey.KEY, result.sentNumber)

                is InvitationResult.Snackbar -> onShowInviteSnackbar(result)
            }
        }
        EventEffect(
            event = data.sendSmsEvent,
            onConsumed = viewModel::onSendSmsConsumed,
        ) { onSendSms(it) }
    }

    InviteContactsScreen(
        state = state,
        onSearchQueryChange = viewModel::setQuery,
        onInvite = viewModel::inviteContacts,
        onBack = navigationHandler::back,
        onReadContactsPermissionGranted = viewModel::onReadContactsPermissionGranted,
        onContactsPicked = viewModel::onContactsPicked,
        onPhoneContactsPickedConsumed = viewModel::onPhoneContactsPickedConsumed,
        onSubmitManualInput = viewModel::validateManualInput,
        onManualEmailAcceptedConsumed = viewModel::onManualEmailAcceptedConsumed,
        onManualPhoneAcceptedConsumed = viewModel::onManualPhoneAcceptedConsumed,
        onEmailValidationConsumed = viewModel::onEmailValidationConsumed,
        onScanClick = viewModel::onScanClicked,
        onScanConfirmed = viewModel::onScanConfirmed,
        onOngoingCallConfirmConsumed = viewModel::onOngoingCallConfirmConsumed,
        onScannedContactDialogDismissed = viewModel::onScannedContactDialogDismissed,
        onInviteScannedContactConfirmed = viewModel::onInviteScannedContactConfirmed,
        onScannedContactSelectConsumed = viewModel::onScannedContactSelectConsumed,
        onScannedContactInviteConsumed = viewModel::onScannedContactInviteConsumed,
        onShareLink = { (state as? InviteContactUiState.Data)?.contactLink?.let(onShareLink) },
        onOpenMyQr = onOpenMyQr,
    )
}
