package mega.privacy.android.feature.devicecenter.ui.model.icon

import mega.privacy.android.feature.devicecenter.R

/**
 * A sealed UI interface that represents different Device Icons
 */
sealed interface DeviceIconType : DeviceCenterUINodeIcon {

    /**
     * Represents an Android Device Icon
     */
    object Android : DeviceIconType {
        override val iconRes = R.drawable.ic_device_android
        override val applySecondaryColorTint = true
    }

    /**
     * Represents an iOS Device Icon
     */
    object IOS : DeviceIconType {
        override val iconRes = R.drawable.ic_device_ios
        override val applySecondaryColorTint = true
    }

    /**
     * Represents a Linux Device Icon
     */
    object Linux : DeviceIconType {
        override val iconRes = R.drawable.ic_device_pc_linux
        override val applySecondaryColorTint = true
    }

    /**
     * Represents a Mac Device Icon
     */
    object Mac : DeviceIconType {
        override val iconRes = R.drawable.ic_device_pc_mac
        override val applySecondaryColorTint = true
    }

    /**
     * Represents a Mobile Device Icon
     */
    object Mobile : DeviceIconType {
        override val iconRes = R.drawable.ic_device_mobile
        override val applySecondaryColorTint = true
    }

    /**
     * Represents a PC Device Icon
     */
    object PC : DeviceIconType {
        override val iconRes = R.drawable.ic_device_pc
        override val applySecondaryColorTint = true
    }

    /**
     * Represents a Windows Device Icon
     */
    object Windows : DeviceIconType {
        override val iconRes = R.drawable.ic_device_pc_windows
        override val applySecondaryColorTint = true
    }
}