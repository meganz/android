package mega.privacy.android.core.nodecomponents.list.view

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class NodeThumbnailViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val gridSize = ThumbnailLayoutType.Grid.placeholderSize
    private val listSize = ThumbnailLayoutType.List.placeholderSize

    private fun setContent(
        data: Any? = null,
        defaultImage: Int = R.drawable.illustration_mega_anniversary,
        contentDescription: String? = "Test Thumbnail",
        blurImage: Boolean = false,
        layoutType: ThumbnailLayoutType = ThumbnailLayoutType.Grid,
        modifier: Modifier = Modifier,
        contentScale: ContentScale = ContentScale.Crop,
    ) {
        composeTestRule.setContent {
            NodeThumbnailView(
                data = data,
                defaultImage = defaultImage,
                modifier = modifier,
                contentScale = contentScale,
                contentDescription = contentDescription,
                blurImage = blurImage,
                layoutType = layoutType,
            )
        }
    }

    @Test
    fun `test that null data shows placeholder with grid layout size`() {
        setContent(
            data = null,
            layoutType = ThumbnailLayoutType.Grid
        )

        composeTestRule.onNodeWithTag(NODE_THUMBNAIL_PLACEHOLDER_TAG)
            .assertIsDisplayed()
            .assertWidthIsEqualTo(gridSize)
            .assertHeightIsEqualTo(gridSize)
    }

    @Test
    fun `test that null data shows placeholder with list layout size`() {
        setContent(
            data = null,
            layoutType = ThumbnailLayoutType.List
        )

        composeTestRule.onNodeWithTag(NODE_THUMBNAIL_PLACEHOLDER_TAG)
            .assertIsDisplayed()
            .assertWidthIsEqualTo(listSize)
            .assertHeightIsEqualTo(listSize)
    }

    @Test
    fun `test that error state shows placeholder with correct size`() {
        setContent(
            data = "invalid://url",
            layoutType = ThumbnailLayoutType.Grid
        )

        composeTestRule.onNodeWithTag(NODE_THUMBNAIL_PLACEHOLDER_TAG)
            .assertIsDisplayed()
            .assertWidthIsEqualTo(gridSize)
            .assertHeightIsEqualTo(gridSize)
    }
}