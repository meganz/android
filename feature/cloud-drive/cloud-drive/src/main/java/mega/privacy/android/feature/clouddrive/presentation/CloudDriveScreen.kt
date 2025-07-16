package mega.privacy.android.feature.clouddrive.presentation

import TestView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.shared.resources.R as sharedR


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudDriveScreen(
    onBack: () -> Unit,
) {
    MegaScaffold(
        topBar = {
            MegaTopAppBar(
                title = stringResource(sharedR.string.general_section_cloud_drive),
                navigationType = AppBarNavigationType.Back(onBack),
            )
        },
        content = { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    TestView()
                }
                val list = (0..75).map { it.toString() }
                items(count = list.size) {
                    MegaText(
                        text = list[it],
                        textColor = TextColor.Primary,
                    )
                }
            }
        }
    )
}