package mega.privacy.android.app.presentation.meeting.navigation

import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.UpgradeProPlanBottomSheet
import mega.privacy.android.app.presentation.meeting.view.dialog.FreePlanLimitParticipantsDialog
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.bottomsheet.bottomSheetMetadata
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey

/**
 * Feature destination for meeting-related dialogs and bottom sheets
 */
data object MeetingFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, _ ->

            entry<UpgradeProPlanBottomSheetNavKey>(
                metadata = bottomSheetMetadata()
            ) {
                UpgradeProPlanBottomSheet(
                    onUpgradeToProPlan = {
                        navigationHandler.navigate(destination = UpgradeAccountNavKey())
                    },
                    hideSheet = { navigationHandler.back() }
                )
            }
        }
}

data object FreePlanParticipantsLimitDialogDestination : AppDialogDestinations {
    override val navigationGraph: EntryProviderScope<DialogNavKey>.(NavigationHandler, () -> Unit) -> Unit =
        { navigationHandler, onHandled ->

            entry<FreePlanParticipantsLimitNavKey>(
                metadata = DialogSceneStrategy.dialog(
                    DialogProperties(
                        windowTitle = "Free Plan Participants Limit Dialog"
                    )
                )
            ) {
                FreePlanLimitParticipantsDialog(
                    onConfirm = {
                        onHandled()
                        navigationHandler.back()
                    }
                )
            }
        }
}
