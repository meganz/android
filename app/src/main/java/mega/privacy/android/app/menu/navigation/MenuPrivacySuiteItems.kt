package mega.privacy.android.app.menu.navigation

import mega.privacy.android.icon.pack.R
import mega.privacy.android.navigation.contract.NavDrawerItem
import mega.privacy.android.shared.resources.R as sharedR

object MegaVpnItem : NavDrawerItem.PrivacySuite(
    destination = Unit,
    iconRes = R.drawable.ic_vpn,
    title = sharedR.string.pro_plan_feature_vpn_title,
    subTitle = sharedR.string.menu_item_vpn_subtitle,
    link = ""
)

object MegaPassItem : NavDrawerItem.PrivacySuite(
    destination = Unit,
    iconRes = R.drawable.ic_password_manager_medium_thin_outline,
    title = sharedR.string.pro_plan_feature_pass_title,
    subTitle = sharedR.string.menu_item_mega_pass_subtitle,
    link = ""
)

object TransferItItem : NavDrawerItem.PrivacySuite(
    destination = Unit,
    iconRes = R.drawable.ic_transfers_upload,
    title = sharedR.string.general_section_transfer_it,
    subTitle = sharedR.string.menu_item_transfer_it_subtitle,
    link = ""
)

