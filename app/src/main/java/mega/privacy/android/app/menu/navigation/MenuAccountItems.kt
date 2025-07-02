package mega.privacy.android.app.menu.navigation

import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.navigation.contract.NavDrawerItem
import mega.privacy.android.shared.resources.R as sharedR

object CurrentPlanItem : NavDrawerItem.Account(
    destination = Unit,
    iconRes = IconPackR.drawable.ic_shield_medium_thin_outline,
    title = sharedR.string.account_upgrade_account_pro_plan_info_current_plan_label,
    actionLabel = sharedR.string.general_upgrade_button
)

object StorageItem : NavDrawerItem.Account(
    destination = Unit,
    iconRes = IconPackR.drawable.ic_hard_drive_medium_thin_outline,
    title = sharedR.string.account_cancel_account_screen_plan_storage
)

object ContactsItem : NavDrawerItem.Account(
    destination = Unit,
    iconRes = IconPackR.drawable.ic_user_square_medium_thin_outline,
    title = sharedR.string.general_section_contacts
)

object AchievementsItem : NavDrawerItem.Account(
    destination = Unit,
    iconRes = IconPackR.drawable.ic_menu_rocket_medium_thin_outline,
    title = sharedR.string.general_section_achievements
)

object SharedItemsItem : NavDrawerItem.Account(
    destination = Unit,
    iconRes = IconPackR.drawable.ic_folder_users_medium_thin_outline,
    title = sharedR.string.video_section_videos_location_option_shared_items
)

object DeviceCentreItem : NavDrawerItem.Account(
    destination = Unit,
    iconRes = IconPackR.drawable.ic_devices_medium_thin_outline,
    title = sharedR.string.general_section_device_centre
)

object TransfersItem : NavDrawerItem.Account(
    destination = Unit,
    iconRes = IconPackR.drawable.ic_arrows_up_down_circle_medium_thin_outline,
    title = sharedR.string.general_section_transfers
)

object OfflineFilesItem : NavDrawerItem.Account(
    destination = Unit,
    iconRes = IconPackR.drawable.ic_cloud_off_medium_thin_outline,
    title = sharedR.string.general_section_offline_files
)

object RubbishBinItem : NavDrawerItem.Account(
    destination = Unit,
    iconRes = IconPackR.drawable.ic_menu_trash_medium_thin_outline,
    title = sharedR.string.general_section_rubbish_bin
)

object SettingsItem : NavDrawerItem.Account(
    destination = Unit,
    iconRes = IconPackR.drawable.ic_gear_six_medium_thin_outline,
    title = sharedR.string.general_settings
)