package test.mega.privacy.android.app.presentation.search

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.search.model.TypeFilterOptionEntity
import mega.privacy.android.app.presentation.search.view.TypeFilterBottomSheet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalMaterialApi::class)
@RunWith(AndroidJUnit4::class)
class TypeFilterBottomSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        modifier: Modifier = Modifier,
        title: String = "",
        options: List<TypeFilterOptionEntity> = emptyList(),
        onItemSelected: (TypeFilterOptionEntity) -> Unit = {},
    ) {
        composeTestRule.setContent {
            val sheetState = ModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Expanded,
                isSkipHalfExpanded = false,
                density = LocalDensity.current,
            )
            TypeFilterBottomSheet(
                modifier = modifier,
                modalSheetState = sheetState,
                title = title,
                options = options,
                onItemSelected = onItemSelected
            )
        }
    }

    @Test
    fun `test that title is displayed correctly`() {
        val title = "Types"
        setComposeContent(title = title)
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun `test that types are displayed correctly`() {
        val docs = TypeFilterOptionEntity(0, "Documents", false)
        val videos = TypeFilterOptionEntity(1, "Videos", true)
        val photos = TypeFilterOptionEntity(2, "Photos", false)
        val options = listOf(docs, videos, photos)
        setComposeContent(options = options)
        composeTestRule.onNodeWithText(docs.title).assertIsDisplayed()
        composeTestRule.onNodeWithText(videos.title).assertIsDisplayed()
    }

    @Test
    fun `test that onItemSelected is invoked when option is clicked`() {
        val onItemSelected: (TypeFilterOptionEntity) -> Unit = mock()
        val docs = TypeFilterOptionEntity(0, "Documents", false)
        val videos = TypeFilterOptionEntity(1, "Videos", true)
        val photos = TypeFilterOptionEntity(2, "Photos", false)
        val options = listOf(docs, videos, photos)
        setComposeContent(options = options, onItemSelected = onItemSelected)

        composeTestRule.onNodeWithText(docs.title).performClick()
        verify(onItemSelected).invoke(docs)
    }
}
