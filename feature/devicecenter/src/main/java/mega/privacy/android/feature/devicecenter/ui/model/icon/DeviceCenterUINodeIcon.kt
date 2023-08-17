package mega.privacy.android.feature.devicecenter.ui.model.icon

import androidx.annotation.DrawableRes

/**
 * A UI interface that serves as the base UI Node Icon which all other types of UI Node Icons in
 * Device Center are derived from
 *
 * @property iconRes The UI Node icon as a [DrawableRes]
 * @property applySecondaryColorTint if true, applies the textColorSecondary color from
 * MaterialTheme.colors. No tint is applied if false
 */
interface DeviceCenterUINodeIcon {
    @get:DrawableRes
    val iconRes: Int

    val applySecondaryColorTint: Boolean
}