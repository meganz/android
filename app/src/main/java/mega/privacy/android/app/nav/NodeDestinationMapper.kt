package mega.privacy.android.app.nav

import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.domain.entity.node.NodeLocation
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.navigation.destination.DriveSyncNavKey
import mega.privacy.android.navigation.destination.HomeScreensNavKey
import mega.privacy.android.navigation.destination.RubbishBinNavKey
import mega.privacy.android.navigation.destination.SharesNavKey
import javax.inject.Inject

class NodeDestinationMapper @Inject constructor(
    private val deviceGateway: DeviceGateway,
) {

    operator fun invoke(nodeLocation: NodeLocation) = with(nodeLocation) {
        buildList {
            val highlightedInRoot = if (ancestorIds.isEmpty()) node.id else null
            val childDestinations = buildList {
                addAll(
                    ancestorIds
                        .mapIndexed { index, parentId ->
                            CloudDriveNavKey(
                                nodeHandle = parentId.longValue,
                                highlightedNodeHandle = if (index == 0) node.id.longValue else null,
                                nodeSourceType = nodeSourceType,
                            )
                        }
                )
            }.reversed() //reversed as we want the deepest destinations in the back stack last in the list

            if (nodeSourceType == NodeSourceType.CLOUD_DRIVE) {
                //cloud drive under HomeScreensNavKey to show bottom navigation
                add(
                    HomeScreensNavKey(
                        root = DriveSyncNavKey(highlightedNodeHandle = highlightedInRoot?.longValue),
                        destinations = childDestinations.takeIf { it.isNotEmpty() },
                        timestamp = deviceGateway.now
                    )
                )
            } else {
                val rootDestination = when (nodeSourceType) {
                    NodeSourceType.RUBBISH_BIN -> RubbishBinNavKey(highlightedNodeHandle = highlightedInRoot?.longValue)
                    else -> SharesNavKey
                }
                add(rootDestination)
                addAll(childDestinations)
            }
        }
    }
}