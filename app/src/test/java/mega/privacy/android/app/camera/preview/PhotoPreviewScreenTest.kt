package mega.privacy.android.app.camera.preview

import android.net.Uri
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.camera.PreviewViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import mega.privacy.android.app.fromId

@RunWith(AndroidJUnit4::class)
class PhotoPreviewScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val previewViewModel = mock<PreviewViewModel>()

    private val viewModelStore = mock<ViewModelStore> {
        on { get(argThat<String> { contains(PreviewViewModel::class.java.canonicalName.orEmpty()) }) } doReturn previewViewModel
    }
    private val viewModelStoreOwner = mock<ViewModelStoreOwner> {
        on { this.viewModelStore } doReturn viewModelStore
    }

    @Test
    fun `test that photo preview image is shown correctly`() {
        val uri = mock<Uri>()
        initComposeRuleContent(uri = uri)
        composeTestRule.onNodeWithTag(TEST_TAG_PHOTO_PREVIEW_IMAGE).assertIsDisplayed()
    }

    @Test
    fun `test that title shows correctly`() {
        val uri = mock<Uri>()
        val title = "Title"
        initComposeRuleContent(uri = uri, title = title)
        composeTestRule.onNodeWithText(
            fromId(
                R.string.camera_send_to,
                title
            )
        ).assertIsDisplayed()
    }

    private fun initComposeRuleContent(
        uri: Uri,
        title: String = "",
        onBackPressed: () -> Unit = {},
        onSendPhoto: (Uri) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                PhotoPreviewScreen(
                    uri = uri,
                    title = title,
                    onBackPressed = onBackPressed,
                    onSendPhoto = onSendPhoto,
                    viewModel = previewViewModel,
                )
            }
        }
    }
}