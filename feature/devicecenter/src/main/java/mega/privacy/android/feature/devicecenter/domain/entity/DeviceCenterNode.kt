package mega.privacy.android.feature.devicecenter.domain.entity

/**
 * A domain interface serving as the Base Node which all other types of Nodes in Device Center are
 * derived from
 *
 * @property id The Node ID
 * @property name The Node Name
 * @property status The Node Status
 */
interface DeviceCenterNode {
    val id: String

    val name: String

    val status: DeviceCenterNodeStatus
}