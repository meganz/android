package mega.privacy.android.feature.devicecenter.ui.model

import androidx.annotation.DrawableRes
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus

/**
 * A UI interface serving as the Base UI Node which all other types of UI Nodes in Device Center are
 * derived from
 *
 * @property id The UI Node ID
 * @property name The UI Node Name
 * @property icon The UI Node Icon as a [DrawableRes]
 * @property status The UI Node Status from [DeviceCenterUINodeStatus]
 */
interface DeviceCenterUINode {
    val id: String

    val name: String

    @get:DrawableRes
    val icon: Int

    val status: DeviceCenterUINodeStatus
}