package mega.privacy.android.feature.devicecenter.ui.model

import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceCenterUINodeIcon
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus

/**
 * A UI interface serving as the Base UI Node which all other types of UI Nodes in Device Center are
 * derived from
 *
 * @property id The UI Node ID
 * @property name The UI Node Name
 * @property icon The UI Node Icon from [DeviceCenterUINodeIcon]
 * @property status The UI Node Status from [DeviceCenterUINodeStatus]
 */
interface DeviceCenterUINode {
    val id: String
    val name: String
    val icon: DeviceCenterUINodeIcon
    val status: DeviceCenterUINodeStatus
}