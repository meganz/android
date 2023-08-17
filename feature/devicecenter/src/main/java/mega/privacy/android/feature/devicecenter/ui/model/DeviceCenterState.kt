package mega.privacy.android.feature.devicecenter.ui.model

/**
 * Data class representing the state of the Device Center Screen
 *
 * @property nodes The list of [DeviceCenterUINode] objects
 */
data class DeviceCenterState(
    val nodes: List<DeviceCenterUINode> = emptyList(),
)