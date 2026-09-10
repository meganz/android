package mega.privacy.android.feature.sharelink.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.a11ytest.ThemeReporters
import mega.privacy.android.a11ytest.screen.setScreenContent
import mega.privacy.android.icon.pack.R as iconPackR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility sweep of the Share link result screen's loaded content.
 *
 * The swept composables are `internal` to `:feature:share-link`, which is why this sweep lives
 * in the module that owns them rather than in a central instrumented module: reaching them from
 * elsewhere would mean widening their visibility for no production reason.
 *
 * The content is mounted stateless with hand-built UI states and no-op callbacks, so no
 * ViewModel, Hilt graph or SDK gateway takes part in the render.
 */
@RunWith(AndroidJUnit4::class)
class ShareLinkContentAccessibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val reporters = ThemeReporters("ShareLinkContentAccessibilityTest")

    @Test
    fun test_that_the_single_node_share_link_content_passes_accessibility_checks() {
        val sweep = composeTestRule.setScreenContent(reporters) {
            ShareLinkContent(
                uiState = singleNodeState,
                onCopyLink = {},
                onCopyKey = {},
            )
        }

        sweep.checkInEachTheme()
    }

    @Test
    fun test_that_the_password_protected_share_link_content_passes_accessibility_checks() {
        val sweep = composeTestRule.setScreenContent(reporters) {
            ShareLinkContent(
                uiState = singleNodeState.copy(
                    isKeySeparate = true,
                    isPasswordSet = true,
                    password = "s3cretPass",
                    linkWithPassword = "https://mega.nz/#P!encryptedLink",
                ),
                onCopyLink = {},
                onCopyKey = {},
                onCopyPassword = {},
            )
        }

        sweep.checkInEachTheme()
    }

    @Test
    fun test_that_the_multi_node_share_link_content_passes_accessibility_checks() {
        val sweep = composeTestRule.setScreenContent(reporters) {
            MultiNodeContent(
                uiState = multiNodeState,
                onCopyLink = {},
            )
        }

        sweep.checkInEachTheme()
    }
}

private val fileNode = ShareLinkNodeItem(
    handle = 1L,
    name = "Presentation.pdf",
    isFolder = false,
    iconRes = iconPackR.drawable.ic_pdf_medium_solid,
    sizeInBytes = 10L * 1024 * 1024,
    modificationTime = 1_749_000_000L,
    childFolderCount = null,
    childFileCount = null,
    link = "https://mega.nz/file/abc123#decryptionKey",
    linkWithoutKey = "https://mega.nz/file/abc123",
    key = "decryptionKey",
)

private val folderNode = ShareLinkNodeItem(
    handle = 2L,
    name = "Documents",
    isFolder = true,
    iconRes = iconPackR.drawable.ic_folder_medium_solid,
    sizeInBytes = null,
    modificationTime = null,
    childFolderCount = 6,
    childFileCount = 12,
    link = "https://mega.nz/folder/def456#folderKey",
    linkWithoutKey = "https://mega.nz/folder/def456",
    key = "folderKey",
)

private val singleNodeState = ShareLinkUiState.Data(
    nodeLinks = listOf(fileNode),
    accountType = null,
)

private val multiNodeState = ShareLinkUiState.Data(
    nodeLinks = listOf(folderNode, fileNode),
    accountType = null,
)
