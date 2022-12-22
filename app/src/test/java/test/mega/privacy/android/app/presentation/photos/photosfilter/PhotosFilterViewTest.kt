package test.mega.privacy.android.app.presentation.photos.photosfilter

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource
import mega.privacy.android.app.presentation.photos.timeline.photosfilter.view.PhotosFilterView
import mega.privacy.android.app.presentation.photos.model.FilterMediaType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import test.mega.privacy.android.app.fromId
import test.mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class PhotosFilterViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that choose type prompt is shown`() {
        composeRule.setContent {
            PhotosFilterView()
        }

        composeRule.onNodeWithText(fromId(R.string.filter_prompt_media_type)).assertExists()
    }

    @Test
    fun `test that choose source prompt is shown`() {
        composeRule.setContent { PhotosFilterView() }

        composeRule.onNodeWithText(fromId(R.string.filter_prompt_media_source)).assertExists()
    }

    @Test
    fun `test that changing the media type calls the type on click function`() {
        val typeOnClick = mock<(FilterMediaType) -> Unit>()
        composeRule.setContent { PhotosFilterView(onMediaTypeSelected = typeOnClick) }

        composeRule.onNodeWithText(R.string.section_images).performClick()

        verify(typeOnClick).invoke(FilterMediaType.IMAGES)
    }

    @Test
    fun `test that changing the media source calls the source on click function`() {
        val sourceOnClick = mock<(TimelinePhotosSource) -> Unit>()
        composeRule.setContent { PhotosFilterView(onSourceSelected = sourceOnClick) }

        composeRule.onNodeWithText(R.string.filter_button_cd_only).performClick()

        verify(sourceOnClick).invoke(TimelinePhotosSource.CLOUD_DRIVE)
    }
}
