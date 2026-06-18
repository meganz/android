package mega.privacy.android.feature.cloudexplorer.navigation

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.navigation.destination.NewTextFileDialogNavKey
import mega.privacy.android.navigation.destination.NewURLFileDialogNavKey
import mega.privacy.android.navigation.destination.ShareTextToMegaNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class RememberNewFileNameResultTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that a text file result creates a text file with the shared text and clears the result`() {
        var createdName: String? = null
        var createdContent: String? = null
        var clearedKey: String? = null
        setContent(
            startNavKey = ShareTextToMegaNavKey(text = SHARED_TEXT, subject = null, email = null),
            resultKey = NewTextFileDialogNavKey.FILE_NAME_RESULT,
            resultFileName = FILE_NAME,
            onClearResult = { clearedKey = it },
            onCreateTextFile = { name, content -> createdName = name; createdContent = content },
        )

        assertThat(createdName).isEqualTo(FILE_NAME)
        assertThat(createdContent).isEqualTo(SHARED_TEXT)
        assertThat(clearedKey).isEqualTo(NewTextFileDialogNavKey.FILE_NAME_RESULT)
    }

    @Test
    fun `test that a url file result creates an internet shortcut file and clears the result`() {
        var createdName: String? = null
        var createdContent: String? = null
        var clearedKey: String? = null
        setContent(
            startNavKey = ShareTextToMegaNavKey(text = SHARED_URL, subject = null, email = null),
            resultKey = NewURLFileDialogNavKey.FILE_NAME_RESULT,
            resultFileName = FILE_NAME,
            onClearResult = { clearedKey = it },
            onCreateTextFile = { name, content -> createdName = name; createdContent = content },
        )

        assertThat(createdName).isEqualTo(FILE_NAME)
        assertThat(createdContent).contains("[InternetShortcut]")
        assertThat(createdContent).contains("URL=$SHARED_URL")
        assertThat(clearedKey).isEqualTo(NewURLFileDialogNavKey.FILE_NAME_RESULT)
    }

    private fun setContent(
        startNavKey: ShareTextToMegaNavKey,
        resultKey: String,
        resultFileName: String,
        onClearResult: (String) -> Unit,
        onCreateTextFile: (String, String) -> Unit,
    ) {
        val monitorResult: (String) -> Flow<Any?> = { key ->
            if (key == resultKey) flowOf(resultFileName) else emptyFlow()
        }
        composeTestRule.setContent {
            rememberNewFileNameResult(
                monitorResult = monitorResult,
                clearResult = onClearResult,
                startNavKey = startNavKey,
                createTextFile = onCreateTextFile,
            )
        }
    }

    private companion object {
        const val FILE_NAME = "shared.txt"
        const val SHARED_TEXT = "hello world"
        const val SHARED_URL = "https://mega.io"
    }
}
