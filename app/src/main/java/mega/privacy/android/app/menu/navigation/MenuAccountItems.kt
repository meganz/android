package mega.privacy.android.app.menu.navigation

import mega.privacy.android.icon.pack.R
import mega.privacy.android.navigation.contract.NavDrawerItem
import mega.privacy.android.shared.resources.R as sharedR

object CurrentPlanItem : NavDrawerItem.Account(
    destination = Unit,
    iconRes = R.drawable.ic_cloud,
    title = sharedR.string.account_upgrade_account_pro_plan_info_current_plan_label,
    actionLabel = sharedR.string.general_upgrade_button
)

object StorageItem : NavDrawerItem.Account(
    destination = Unit,
    iconRes = R.drawable.ic_vpn,
    title = sharedR.string.account_cancel_account_screen_plan_storage
)

object ContactsItem : NavDrawerItem.Account(
    destination = Unit,
    iconRes = R.drawable.ic_warning_icon,
    title = sharedR.string.general_section_contacts
)

object AchievementsItem : NavDrawerItem.Account(
    destination = Unit,
    iconRes = R.drawable.ic_add_to_album,
    title = sharedR.string.general_section_achievements
)

object SharedItemsItem : NavDrawerItem.Account(
    destination = Unit,
    iconRes = R.drawable.ic_add_to_album,
    title = sharedR.string.video_section_videos_location_option_shared_items
)

object DeviceCentreItem : NavDrawerItem.Account(
    destination = Unit,
    iconRes = R.drawable.ic_achievements_check,
    title = sharedR.string.general_section_device_centre
)

object TransfersItem : NavDrawerItem.Account(
    destination = Unit,
    iconRes = R.drawable.ic_3d_medium_solid,
    title = sharedR.string.general_section_transfers
)

object OfflineFilesItem : NavDrawerItem.Account(
    destination = Unit,
    iconRes = R.drawable.ic_aftereffects_medium_solid,
    title = sharedR.string.general_section_offline_files
)

object RubbishBinItem : NavDrawerItem.Account(
    destination = Unit,
    iconRes = R.drawable.ic_cloud,
    title = sharedR.string.general_section_rubbish_bin
)

object SettingsItem : NavDrawerItem.Account(
    destination = Unit,
    iconRes = R.drawable.ic_vpn,
    title = sharedR.string.general_settings
)