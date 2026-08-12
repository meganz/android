package mega.privacy.android.feature.sync.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.icon.pack.R as IconPackR

class IssueCardsScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SolvedIssueCardStates() {
        AndroidThemeForPreviews {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SolvedIssueCard(
                    title = "Competitors documentation",
                    body = "Issue solved",
                    nodeIcon = IconPackR.drawable.ic_folder_medium_solid,
                )
                SolvedIssueCard(
                    title = "Product roadmap",
                    subTitle = "Documents/Product",
                    body = "Duplicate file removed",
                    nodeIcon = IconPackR.drawable.ic_folder_medium_solid,
                )
                SolvedIssueCard(
                    title = "A folder with a name long enough that the title has to be truncated in the middle",
                    subTitle = "A path long enough that it also has to be truncated in the middle",
                    body = "Issue solved",
                    nodeIcon = IconPackR.drawable.ic_folder_medium_solid,
                )
            }
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun StalledIssueCardStates() {
        AndroidThemeForPreviews {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StalledIssueCard(
                    nodeName = "Competitors documentation",
                    nodePath = "Documents/Competitors",
                    conflictName = "Names would clash in the local folder",
                    icon = IconPackR.drawable.ic_folder_medium_solid,
                    shouldShowMoreIcon = true,
                    moreClicked = {},
                )
                StalledIssueCard(
                    nodeName = "Product roadmap",
                    nodePath = "",
                    conflictName = "Local and remote changed since last sync",
                    icon = IconPackR.drawable.ic_folder_medium_solid,
                    shouldShowMoreIcon = false,
                    moreClicked = {},
                )
            }
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SyncCardLoadingSkeletonDefault() {
        AndroidThemeForPreviews {
            SyncCardLoadingSkeleton(modifier = Modifier.padding(16.dp))
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SyncListNoItemsPlaceHolderDefault() {
        AndroidThemeForPreviews {
            SyncListNoItemsPlaceHolder(
                placeholderText = "No issues",
                placeholderIcon = IconPackR.drawable.ic_check_circle_color,
            )
        }
    }
}
