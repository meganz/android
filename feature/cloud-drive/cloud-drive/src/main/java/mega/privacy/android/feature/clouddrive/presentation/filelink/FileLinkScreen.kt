package mega.privacy.android.feature.clouddrive.presentation.filelink

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
import mega.privacy.android.core.transfers.widget.TransfersToolbarWidget
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.navigation.destination.TransfersNavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FileLinkScreen(
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
) {
    MegaScaffoldWithTopAppBarScrollBehavior(
        topBar = {
            MegaTopAppBar(
                modifier = Modifier.testTag(FILE_LINK_APP_BAR_TAG),
                title = "File Link",
                navigationType = AppBarNavigationType.Back(onBack),
                trailingIcons = {
                    TransfersToolbarWidget {
                        onNavigate(TransfersNavKey())
                    }
                },
                actions = buildList {
                    add(MenuActionWithClick(CommonMenuAction.More) {
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
            text = "File Link Screen"
        )
    }
}

internal const val FILE_LINK_APP_BAR_TAG = "file_link_screen:main_app_bar"
