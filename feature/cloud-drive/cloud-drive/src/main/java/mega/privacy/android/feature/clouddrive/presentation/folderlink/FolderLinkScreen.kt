package mega.privacy.android.feature.clouddrive.presentation.folderlink

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation3.runtime.NavKey
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.privacy.android.core.sharedcomponents.menu.CommonAppBarAction
import mega.privacy.android.core.transfers.widget.TransfersToolbarWidget
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.CLOUD_DRIVE_MAIN_APP_BAR_TAG
import mega.privacy.android.navigation.destination.TransfersNavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FolderLinkScreen(
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
) {
    MegaScaffoldWithTopAppBarScrollBehavior(
        topBar = {
            MegaTopAppBar(
                modifier = Modifier.testTag(CLOUD_DRIVE_MAIN_APP_BAR_TAG),
                title = "Folder Link",
                navigationType = AppBarNavigationType.Back(onBack),
                trailingIcons = {
                    TransfersToolbarWidget {
                        onNavigate(TransfersNavKey())
                    }
                },
                actions = buildList {
                    add(MenuActionWithClick(CommonAppBarAction.Search) {
                        // TODO
                    })
                    add(MenuActionWithClick(CommonAppBarAction.More) {
                        // TODO
                    })
                },
            )
        },
        bottomBar = {
            // TODO
        }
    ) { innerPadding ->
        MegaText(
            modifier = Modifier.padding(innerPadding),
            text = "Folder Link Screen"
        )
    }
}