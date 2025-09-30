package mega.privacy.android.app.menu.navigation

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.payment.UpgradeAccountNavKey
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.NavDrawerItem
import mega.privacy.android.navigation.destination.AchievementNavKey
import mega.privacy.android.navigation.destination.ContactsNavKey
import mega.privacy.android.navigation.destination.LegacySettingsNavKey
import mega.privacy.android.navigation.destination.MyAccountNavKey
import mega.privacy.android.navigation.destination.RubbishBinNavKey
import mega.privacy.android.navigation.destination.SharesNavKey
import mega.privacy.android.navigation.destination.TransfersNavKey
import mega.privacy.android.shared.resources.R as sharedR

object CurrentPlanItem : NavDrawerItem.Account(
    destination = UpgradeAccountNavKey(),
    icon = IconPack.Medium.Thin.Outline.Shield,
    title = sharedR.string.account_upgrade_account_pro_plan_info_current_plan_label,
    actionLabel = sharedR.string.general_upgrade_button
)

object StorageItem : NavDrawerItem.Account(
    destination = MyAccountNavKey,
    icon = IconPack.Medium.Thin.Outline.HardDrive,
    title = sharedR.string.account_cancel_account_screen_plan_storage
)

object ContactsItem : NavDrawerItem.Account(
    destination = ContactsNavKey(ContactsNavKey.NavType.List),
    icon = IconPack.Medium.Thin.Outline.UserSquare,
    title = sharedR.string.general_section_contacts,
)

object AchievementsItem : NavDrawerItem.Account(
    destination = AchievementNavKey,
    icon = IconPack.Medium.Thin.Outline.Rocket,
    title = sharedR.string.general_section_achievements,
)

object SharedItemsItem : NavDrawerItem.Account(
    destination = SharesNavKey,
    icon = IconPack.Medium.Thin.Outline.FolderUsers,
    title = sharedR.string.video_section_videos_location_option_shared_items
)

object DeviceCentreItem : NavDrawerItem.Account(
    destination = object : NavKey {},
    icon = IconPack.Medium.Thin.Outline.Devices,
    title = sharedR.string.general_section_device_centre
)

object TransfersItem : NavDrawerItem.Account(
    destination = TransfersNavKey(),
    icon = IconPack.Medium.Thin.Outline.ArrowsUpDownCircle,
    title = sharedR.string.general_section_transfers
)

object OfflineFilesItem : NavDrawerItem.Account(
    destination = object : NavKey {},
    icon = IconPack.Medium.Thin.Outline.CloudOff,
    title = sharedR.string.general_section_offline_files
)

object RubbishBinItem : NavDrawerItem.Account(
    destination = RubbishBinNavKey(),
    icon = IconPack.Medium.Thin.Outline.Trash,
    title = sharedR.string.general_section_rubbish_bin
)

object SettingsItem : NavDrawerItem.Account(
    destination = LegacySettingsNavKey(null),
    icon = IconPack.Medium.Thin.Outline.GearSix,
    title = sharedR.string.general_settings
)