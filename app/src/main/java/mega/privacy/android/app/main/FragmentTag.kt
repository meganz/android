package mega.privacy.android.app.main

internal enum class FragmentTag {
    CLOUD_DRIVE,
    HOMEPAGE,
    PHOTOS,
    BACKUPS,
    INCOMING_SHARES,
    OUTGOING_SHARES,
    SEARCH,
    TRANSFERS_PAGE,
    RECENT_CHAT,
    SYNC,
    RUBBISH_BIN,
    NOTIFICATIONS,
    TURN_ON_NOTIFICATIONS,
    PERMISSIONS,
    LINKS,
    MEDIA_DISCOVERY,
    ALBUM_CONTENT,
    PHOTOS_FILTER,
    RUBBISH_BIN_COMPOSE,
    CLOUD_DRIVE_COMPOSE,
    DEVICE_CENTER;

    val tag: String
        get() = when (this) {
            CLOUD_DRIVE -> "fileBrowserFragment"
            HOMEPAGE -> "homepageFragment"
            SYNC -> "syncFragment"
            RUBBISH_BIN -> "rubbishBinFragment"
            PHOTOS -> "photosFragment"
            BACKUPS -> "backupsFragment"
            INCOMING_SHARES -> "incomingSharesFragment"
            OUTGOING_SHARES -> "outgoingSharesFragment"
            SEARCH -> "searchFragment"
            TRANSFERS_PAGE -> "transferPageFragment"
            RECENT_CHAT -> "chatTabsFragment"
            NOTIFICATIONS -> "notificationsFragment"
            TURN_ON_NOTIFICATIONS -> "turnOnNotificationsFragment"
            PERMISSIONS -> "permissionsFragment"
            LINKS -> "linksFragment"
            MEDIA_DISCOVERY -> "mediaDiscoveryFragment"
            ALBUM_CONTENT -> "fragmentAlbumContent"
            PHOTOS_FILTER -> "fragmentPhotosFilter"
            RUBBISH_BIN_COMPOSE -> "rubbishBinComposeFragment"
            CLOUD_DRIVE_COMPOSE -> "cloudDriveComposeFragment"
            DEVICE_CENTER -> "deviceCenterFragment"
        }
}