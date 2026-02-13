package mega.privacy.android.app.menu.navigation

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.usecase.call.MonitorActiveCallUseCase
import mega.privacy.android.domain.usecase.chat.GetNumUnreadChatsUseCase
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.DefaultIconBadge
import mega.privacy.android.navigation.contract.DefaultNumberBadge
import mega.privacy.android.navigation.contract.NavDrawerItem
import mega.privacy.android.navigation.destination.AchievementNavKey
import mega.privacy.android.navigation.destination.ChatListNavKey
import mega.privacy.android.navigation.destination.ContactsNavKey
import mega.privacy.android.navigation.destination.DeviceCenterNavKey
import mega.privacy.android.navigation.destination.LegacySettingsNavKey
import mega.privacy.android.navigation.destination.MyAccountNavKey
import mega.privacy.android.navigation.destination.OfflineNavKey
import mega.privacy.android.navigation.destination.RubbishBinNavKey
import mega.privacy.android.navigation.destination.SharesNavKey
import mega.privacy.android.navigation.destination.TransfersNavKey
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.MyMenuAchievementsNavigationItemEvent
import mega.privacy.mobile.analytics.event.MyMenuChatNavigationItemEvent
import mega.privacy.mobile.analytics.event.MyMenuContactsNavigationItemEvent
import mega.privacy.mobile.analytics.event.MyMenuDeviceCentreNavigationItemEvent
import mega.privacy.mobile.analytics.event.MyMenuOfflineFilesNavigationItemEvent
import mega.privacy.mobile.analytics.event.MyMenuRubbishBinNavigationItemEvent
import mega.privacy.mobile.analytics.event.MyMenuSettingsNavigationItemEvent
import mega.privacy.mobile.analytics.event.MyMenuSharedItemsNavigationItemEvent
import mega.privacy.mobile.analytics.event.MyMenuStorageNavigationItemEvent
import mega.privacy.mobile.analytics.event.MyMenuTransfersNavigationItemEvent
import mega.privacy.mobile.analytics.event.MyMenuUpgradeNavigationItemEvent
import timber.log.Timber

object CurrentPlanItem : NavDrawerItem.Account(
    destination = UpgradeAccountNavKey(),
    icon = IconPack.Medium.Thin.Outline.Shield,
    title = sharedR.string.account_upgrade_account_pro_plan_info_current_plan_label,
    actionLabel = sharedR.string.general_upgrade_button,
    analyticsEventIdentifier = MyMenuUpgradeNavigationItemEvent,
)

object StorageItem : NavDrawerItem.Account(
    destination = MyAccountNavKey(action = Constants.ACTION_OPEN_USAGE_METER_FROM_MENU),
    icon = IconPack.Medium.Thin.Outline.HardDrive,
    title = sharedR.string.account_cancel_account_screen_plan_storage,
    analyticsEventIdentifier = MyMenuStorageNavigationItemEvent,
)

object ContactsItem : NavDrawerItem.Account(
    destination = ContactsNavKey(ContactsNavKey.NavType.List),
    icon = IconPack.Medium.Thin.Outline.UserSquare,
    title = sharedR.string.general_section_contacts,
    analyticsEventIdentifier = MyMenuContactsNavigationItemEvent,
)

object AchievementsItem : NavDrawerItem.Account(
    destination = AchievementNavKey,
    icon = IconPack.Medium.Thin.Outline.Rocket,
    title = sharedR.string.general_section_achievements,
    analyticsEventIdentifier = MyMenuAchievementsNavigationItemEvent,
)

object SharedItemsItem : NavDrawerItem.Account(
    destination = SharesNavKey,
    icon = IconPack.Medium.Thin.Outline.FolderUsers,
    title = sharedR.string.video_section_videos_location_option_shared_items,
    analyticsEventIdentifier = MyMenuSharedItemsNavigationItemEvent,
)

class ChatItem(
    getNumUnreadChatsUseCase: GetNumUnreadChatsUseCase,
    monitorActiveCallUseCase: MonitorActiveCallUseCase,
) : NavDrawerItem.Account(
    destination = ChatListNavKey(),
    icon = IconPack.Medium.Thin.Outline.MessageChatCircle,
    title = sharedR.string.general_chat,
    badge = combine(
        monitorActiveCallUseCase()
            .onEach {
                Timber.d("ChatMenuItem Active call event: $it")
            }
            .map {
                it != null
            },
        getNumUnreadChatsUseCase()
    ) { ongoingCall, unreadChats ->
        if (ongoingCall) {
            DefaultIconBadge(IconPack.Medium.Thin.Solid.Phone01)
        } else if (unreadChats > 0) {
            DefaultNumberBadge(unreadChats)
        } else {
            null
        }
    },
    analyticsEventIdentifier = MyMenuChatNavigationItemEvent,
)

object DeviceCentreItem : NavDrawerItem.Account(
    destination = DeviceCenterNavKey,
    icon = IconPack.Medium.Thin.Outline.Devices,
    title = sharedR.string.general_section_device_centre,
    analyticsEventIdentifier = MyMenuDeviceCentreNavigationItemEvent,
)

object TransfersItem : NavDrawerItem.Account(
    destination = TransfersNavKey(),
    icon = IconPack.Medium.Thin.Outline.ArrowsUpDownCircle,
    title = sharedR.string.general_section_transfers,
    availableOffline = true,
    analyticsEventIdentifier = MyMenuTransfersNavigationItemEvent,
)

object OfflineFilesItem : NavDrawerItem.Account(
    destination = OfflineNavKey(),
    icon = IconPack.Medium.Thin.Outline.CloudOff,
    title = sharedR.string.general_section_offline_files,
    availableOffline = true,
    analyticsEventIdentifier = MyMenuOfflineFilesNavigationItemEvent,
)

object RubbishBinItem : NavDrawerItem.Account(
    destination = RubbishBinNavKey(),
    icon = IconPack.Medium.Thin.Outline.Trash,
    title = sharedR.string.general_section_rubbish_bin,
    analyticsEventIdentifier = MyMenuRubbishBinNavigationItemEvent,
)

object SettingsItem : NavDrawerItem.Account(
    destination = LegacySettingsNavKey(null),
    icon = IconPack.Medium.Thin.Outline.GearSix,
    title = sharedR.string.general_settings,
    analyticsEventIdentifier = MyMenuSettingsNavigationItemEvent,
)
