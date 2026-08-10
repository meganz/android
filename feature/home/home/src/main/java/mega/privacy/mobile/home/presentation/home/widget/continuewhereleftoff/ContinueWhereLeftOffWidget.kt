package mega.privacy.mobile.home.presentation.home.widget.continuewhereleftoff

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.action.HandleNodeAction3
import mega.privacy.android.core.nodecomponents.action.NodeSourceData
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.navigation.Flagged
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.contract.home.HomeWidget
import mega.privacy.android.navigation.contract.home.HomeWidgetOrder
import mega.privacy.android.navigation.destination.ContinueWhereLeftOffScreenNavKey
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

class ContinueWhereLeftOffWidget @Inject constructor() : HomeWidget, Flagged {
    override val identifier: String = "ContinueWhereLeftOffWidget"
    override val defaultOrder: HomeWidgetOrder = HomeWidgetOrder.ContinueWhereLeftOff
    override val canDelete: Boolean = true
    override val isConfigurable: Boolean = true
    override val isDraggable: Boolean = true
    override val feature: Feature = ApiFeatures.ContinueWhereLeftOff

    override suspend fun getWidgetName() =
        LocalizedText.StringRes(sharedR.string.home_widget_continue_where_left_off)

    @Composable
    override fun DisplayWidget(
        modifier: Modifier,
        navigationHandler: NavigationHandler,
        transferHandler: TransferHandler,
    ) {
        FeatureFlagGate(feature = feature) {
            val viewModel: ContinueWhereLeftOffViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            var openedFileNode by remember { mutableStateOf<TypedFileNode?>(null) }
            val coroutineScope = rememberCoroutineScope()

            EventEffect(
                event = uiState.openNodeEvent,
                onConsumed = viewModel::onOpenNodeEventConsumed,
            ) { node ->
                openedFileNode = node
            }

            ContinueWhereLeftOffCarousel(
                items = uiState.items,
                isLoading = uiState.isLoading,
                onItemClick = { item -> viewModel.onItemClicked(item.nodeHandle, item.type) },
                onViewAllClick = {
                    navigationHandler.navigate(ContinueWhereLeftOffScreenNavKey)
                },
                modifier = modifier,
            )

            openedFileNode?.let { node ->
                HandleNodeAction3(
                    typedFileNode = node,
                    onActionHandled = { openedFileNode = null },
                    nodeSourceData = NodeSourceData.Default(NodeSourceType.CLOUD_DRIVE),
                    onDownloadEvent = transferHandler::setTransferEvent,
                    onNavigate = navigationHandler::navigate,
                    coroutineScope = coroutineScope,
                )
            }
        }
    }
}
