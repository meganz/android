package mega.privacy.android.app.menu.navigation

import mega.privacy.android.app.utils.Constants.MEGA_PASS_PACKAGE_NAME
import mega.privacy.android.app.utils.Constants.MEGA_PASS_URL
import mega.privacy.android.app.utils.Constants.MEGA_TRANSFER_IT_URL
import mega.privacy.android.app.utils.Constants.MEGA_VPN_PACKAGE_NAME
import mega.privacy.android.app.utils.Constants.MEGA_VPN_URL
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.navigation.contract.NavDrawerItem
import mega.privacy.android.shared.resources.R as sharedR

object MegaVpnItem : NavDrawerItem.PrivacySuite(
    destination = Unit,
    iconRes = IconPackR.drawable.ic_menu_vpn_medium_thin_outline,
    title = sharedR.string.pro_plan_feature_vpn_title,
    subTitle = sharedR.string.menu_item_vpn_subtitle,
    link = MEGA_VPN_URL,
    appPackage = MEGA_VPN_PACKAGE_NAME
)

object MegaPassItem : NavDrawerItem.PrivacySuite(
    destination = Unit,
    iconRes = IconPackR.drawable.ic_lock_keyhole_circle_medium_thin_outline,
    title = sharedR.string.pro_plan_feature_pass_title,
    subTitle = sharedR.string.menu_item_mega_pass_subtitle,
    link = MEGA_PASS_URL,
    appPackage = MEGA_PASS_PACKAGE_NAME
)

object TransferItItem : NavDrawerItem.PrivacySuite(
    destination = Unit,
    iconRes = IconPackR.drawable.ic_arrow_up_circle_medium_thin_outline,
    title = sharedR.string.general_section_transfer_it,
    subTitle = sharedR.string.menu_item_transfer_it_subtitle,
    link = MEGA_TRANSFER_IT_URL,
)
