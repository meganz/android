package mega.privacy.android.app.presentation.filecontact.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.components.util.shimmerEffect
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.app.presentation.filecontact.model.FileContactListState
import mega.privacy.android.domain.entity.node.NodeId

@Composable
internal fun FileContactLoadingScreen(
    state: FileContactListState.Loading,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaScaffold(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding(),
        topBar = {
            MegaTopAppBar(
                title = state.folderName,
                navigationType = AppBarNavigationType.Back(onBackPressed),
            )
        },
    ) { paddingValues ->
        BoxSurface(
            surfaceColor = SurfaceColor.PageBackground,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column {
                repeat(3) {
                    LoadingItemView()
                }
            }
        }
    }
}

@Composable
fun LoadingItemView() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            modifier = Modifier
                .size(48.dp)
                .shimmerEffect(shape = CircleShape)
        )
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
        ){
            Spacer(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
            Spacer(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth(0.33f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
        }
        Spacer(modifier = Modifier.weight(0.1f))
        Spacer(
            modifier = Modifier
                .width(16.dp)
                .height(32.dp)
                .shimmerEffect(shape = RoundedCornerShape(4.dp))
        )
    }
}

@CombinedThemePreviews
@Composable
private fun FileContactLoadingScreenPreview() {
    AndroidThemeForPreviews {
        FileContactLoadingScreen(
            state = FileContactListState.Loading(
                folderName = "Folder name",
                folderId = NodeId(123456789L),
            ),
            onBackPressed = {},
        )
    }
}