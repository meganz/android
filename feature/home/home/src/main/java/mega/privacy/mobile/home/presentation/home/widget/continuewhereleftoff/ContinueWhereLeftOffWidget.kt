package mega.privacy.mobile.home.presentation.home.widget.continuewhereleftoff

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.action.HandleNodeAction3
import mega.privacy.android.core.nodecomponents.action.NodeSourceData
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.contract.home.HomeWidget
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

class ContinueWhereLeftOffWidget @Inject constructor() : HomeWidget {
    override val identifier: String = "ContinueWhereLeftOffWidget"
    override val defaultOrder: Int = 4
    override val canDelete: Boolean = true

    override suspend fun getWidgetName() =
        LocalizedText.StringRes(sharedR.string.home_widget_continue_where_left_off)

    @Composable
    override fun DisplayWidget(
        modifier: Modifier,
        navigationHandler: NavigationHandler,
        transferHandler: TransferHandler,
    ) {
        FeatureFlagGate(feature = ApiFeatures.ContinueWhereLeftOff) {
            val viewModel: ContinueWhereLeftOffViewModel = hiltViewModel()
            val items by viewModel.items.collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()
            var openedFileNode by remember {
                mutableStateOf<TypedFileNode?>(null)
            }

            ContinueWhereLeftOffCarousel(
                items = items,
                onItemClick = { item ->
                    scope.launch {
                        viewModel.resolveNode(item.nodeHandle)?.let {
                            openedFileNode = it
                        }
                    }
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
                )
            }
        }
    }
}
