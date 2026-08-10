package mega.privacy.mobile.home.presentation.whatsnew.detail

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.android.core.ui.components.dialogs.PromotionalIllustrationDialog
import mega.android.core.ui.components.sheets.PromotionalIllustrationSheet
import mega.android.core.ui.components.sheets.SheetButtonAttribute
import mega.android.core.ui.components.text.ContentTextDefaults
import mega.android.core.ui.theme.devicetype.DeviceType
import mega.android.core.ui.theme.devicetype.LocalDeviceType
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.OfflineNavKey
import mega.privacy.android.navigation.destination.WhatsNewNavKey
import mega.privacy.mobile.home.presentation.whatsnew.WhatsNewDetail

object V16_1_WhatsNewDetail : WhatsNewDetail {
    override val screen: @Composable ((NavigationHandler, () -> Unit) -> Unit)
        get() = @Composable { navigationHandler, onHandled ->
            val deviceType = LocalDeviceType.current

            if (deviceType == DeviceType.Tablet) {
                // TODO replace with actual dialog design
                PromotionalIllustrationDialog(
                    modifier = Modifier.statusBarsPadding(),
                    title = "Whats New",
                    headline = "Customise your Home screen",
                    description = ContentTextDefaults.description("Choose what you see and organise it to fit how you use MEGA"),
                    illustration = IconPackR.drawable.illustration_notification_permission,
                    primaryButton = SheetButtonAttribute("Explore") {
                        navigationHandler.navigate(OfflineNavKey())
                        navigationHandler.remove(WhatsNewNavKey)
                        onHandled()
                    },
                    secondaryButton = SheetButtonAttribute("Dismiss") {
                        navigationHandler.remove(WhatsNewNavKey)
                        onHandled()
                    },
                    onDismissRequest = {
                        navigationHandler.remove(WhatsNewNavKey)
                        onHandled()
                    }
                )
            } else {
                // TODO replace with actual dialog design
                PromotionalIllustrationSheet(
                    modifier = Modifier.statusBarsPadding(),
                    title = "Whats New",
                    headline = "Customise your Home screen",
                    description = ContentTextDefaults.description("Choose what you see and organise it to fit how you use MEGA"),
                    illustration = IconPackR.drawable.illustration_notification_permission,
                    primaryButton = SheetButtonAttribute("Explore") {
                        navigationHandler.navigate(OfflineNavKey())
                        navigationHandler.remove(WhatsNewNavKey)
                        onHandled()
                    },
                    secondaryButton = SheetButtonAttribute("Dismiss") {
                        navigationHandler.remove(WhatsNewNavKey)
                        onHandled()
                    },
                    onDismissRequest = {
                        navigationHandler.remove(WhatsNewNavKey)
                        onHandled()
                    }
                )
            }
        }
}