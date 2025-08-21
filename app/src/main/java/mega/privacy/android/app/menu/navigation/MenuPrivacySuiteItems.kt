package mega.privacy.android.app.menu.navigation

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.utils.Constants.MEGA_PASS_PACKAGE_NAME
import mega.privacy.android.app.utils.Constants.MEGA_TRANSFER_IT_URL
import mega.privacy.android.app.utils.Constants.MEGA_VPN_PACKAGE_NAME
import mega.privacy.android.app.utils.ConstantsUrl.megaPwmUrl
import mega.privacy.android.app.utils.ConstantsUrl.megaVpnUrl
import mega.privacy.android.app.utils.DomainNameFacade
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.NavDrawerItem
import mega.privacy.android.shared.resources.R as sharedR

object MegaVpnItem : NavDrawerItem.PrivacySuite(
    destination = object : NavKey {},
    icon = IconPack.Medium.Thin.Outline.VPN,
    title = sharedR.string.pro_plan_feature_vpn_title,
    subTitle = sharedR.string.menu_item_vpn_subtitle,
    link = megaVpnUrl(DomainNameFacade.getDomainName()),
    appPackage = MEGA_VPN_PACKAGE_NAME
)

object MegaPassItem : NavDrawerItem.PrivacySuite(
    destination = object : NavKey {},
    icon = IconPack.Medium.Thin.Outline.LockKeyholeCircle,
    title = sharedR.string.pro_plan_feature_pass_title,
    subTitle = sharedR.string.menu_item_mega_pass_subtitle,
    link = megaPwmUrl(DomainNameFacade.getDomainName()),
    appPackage = MEGA_PASS_PACKAGE_NAME
)

object TransferItItem : NavDrawerItem.PrivacySuite(
    destination = object : NavKey {},
    icon = IconPack.Medium.Thin.Outline.ArrowUpCircle,
    title = sharedR.string.general_section_transfer_it,
    subTitle = sharedR.string.menu_item_transfer_it_subtitle,
    link = MEGA_TRANSFER_IT_URL,
)
