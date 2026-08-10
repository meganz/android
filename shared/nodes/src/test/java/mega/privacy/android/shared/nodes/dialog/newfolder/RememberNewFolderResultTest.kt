package mega.privacy.android.shared.nodes.dialog.newfolder

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.navigation.destination.NewFolderDialogNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RememberNewFolderResultTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that a folder result invokes onFolderCreated with the created node and clears the result`() {
        var createdNode: NodeId? = null
        var clearedKey: String? = null
        val monitorResult: (String) -> Flow<Any?> = { key ->
            if (key == NewFolderDialogNavKey.FOLDER_HANDLE_RESULT) flowOf(FOLDER_HANDLE) else emptyFlow()
        }

        composeTestRule.setContent {
            rememberNewFolderResult(
                monitorResult = monitorResult,
                clearResult = { clearedKey = it },
                onFolderCreated = { createdNode = it },
            )
        }

        assertThat(createdNode).isEqualTo(NodeId(FOLDER_HANDLE))
        assertThat(clearedKey).isEqualTo(NewFolderDialogNavKey.FOLDER_HANDLE_RESULT)
    }

    @Test
    fun `test that no result does not invoke onFolderCreated`() {
        var createdNode: NodeId? = null
        var clearedKey: String? = null

        composeTestRule.setContent {
            rememberNewFolderResult(
                monitorResult = { emptyFlow() },
                clearResult = { clearedKey = it },
                onFolderCreated = { createdNode = it },
            )
        }

        assertThat(createdNode).isNull()
        assertThat(clearedKey).isNull()
    }

    private companion object {
        const val FOLDER_HANDLE = 456L
    }
}
