package mega.privacy.android.feature.devicecenter.ui.model.icon

import mega.privacy.android.feature.devicecenter.R

/**
 * A sealed UI interface that represents different Device Icons
 */
sealed interface DeviceIconType : DeviceCenterUINodeIcon {

    /**
     * Represents an Android Device Icon
     */
    data object Android : DeviceIconType {
        override val iconRes = R.drawable.ic_device_android

        @Deprecated("Temporary used in order to fix icon color until we change to the new icon set. Will be removed soon.")
        override val applySecondaryColorTint = true
    }

    /**
     * Represents an iOS Device Icon
     */
    data object IOS : DeviceIconType {
        override val iconRes = R.drawable.ic_device_ios

        @Deprecated("Temporary used in order to fix icon color until we change to the new icon set. Will be removed soon.")
        override val applySecondaryColorTint = true
    }

    /**
     * Represents a Linux Device Icon
     */
    data object Linux : DeviceIconType {
        override val iconRes = R.drawable.ic_device_pc_linux

        @Deprecated("Temporary used in order to fix icon color until we change to the new icon set. Will be removed soon.")
        override val applySecondaryColorTint = true
    }

    /**
     * Represents a Mac Device Icon
     */
    data object Mac : DeviceIconType {
        override val iconRes = R.drawable.ic_device_pc_mac

        @Deprecated("Temporary used in order to fix icon color until we change to the new icon set. Will be removed soon.")
        override val applySecondaryColorTint = true
    }

    /**
     * Represents a Mobile Device Icon
     */
    data object Mobile : DeviceIconType {
        override val iconRes = R.drawable.ic_device_mobile

        @Deprecated("Temporary used in order to fix icon color until we change to the new icon set. Will be removed soon.")
        override val applySecondaryColorTint = true
    }

    /**
     * Represents a PC Device Icon
     */
    data object PC : DeviceIconType {
        override val iconRes = R.drawable.ic_device_pc

        @Deprecated("Temporary used in order to fix icon color until we change to the new icon set. Will be removed soon.")
        override val applySecondaryColorTint = true
    }

    /**
     * Represents a Windows Device Icon
     */
    data object Windows : DeviceIconType {
        override val iconRes = R.drawable.ic_device_pc_windows

        @Deprecated("Temporary used in order to fix icon color until we change to the new icon set. Will be removed soon.")
        override val applySecondaryColorTint = true
    }
}