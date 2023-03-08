package test.mega.privacy.android.app.presentation.view

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.view.ListView
import mega.privacy.android.domain.entity.node.FolderNode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ListViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val stringUtilWrapper: StringUtilWrapper = mock()

    @Test
    fun `test when list item is selected then is shows selected image`() = runTest {
        val node: FolderNode = mock()
        whenever(node.name).thenReturn("Some name")
        whenever(node.childFileCount).thenReturn(1)
        whenever(node.childFolderCount).thenReturn(2)
        whenever(
            stringUtilWrapper.getFolderInfo(
                node.childFolderCount,
                node.childFileCount
            )
        ).thenReturn(" 2 Folder 1 file")
        composeTestRule.setContent {
            ListView(
                modifier = Modifier,
                nodeUIItem = listOf(NodeUIItem(node = node, isSelected = true)),
                stringUtilWrapper = stringUtilWrapper,
                onItemClicked = {},
                onMenuClick = {},
                onLongClick = {},
                onSortOrderClick = {},
                onChangeViewTypeClick = {},
                isListView = false,
                sortOrder = "Any Name"
            )
        }
        composeTestRule.run {
            onAllNodes(hasTestTag("Selected Tag"))
        }
    }

    @Test
    fun `test when list item is favorite and exported then it shows favorite icon`() = runTest {
        val node: FolderNode = mock()
        whenever(node.name).thenReturn("Some name")
        whenever(node.childFileCount).thenReturn(1)
        whenever(node.childFolderCount).thenReturn(2)
        whenever(node.isFavourite).thenReturn(true)
        whenever(node.isExported).thenReturn(true)
        whenever(
            stringUtilWrapper.getFolderInfo(
                node.childFolderCount,
                node.childFileCount
            )
        ).thenReturn(" 2 Folder 1 file")
        composeTestRule.setContent {
            ListView(
                modifier = Modifier,
                nodeUIItem = listOf(NodeUIItem(node = node, isSelected = true)),
                stringUtilWrapper = stringUtilWrapper,
                onItemClicked = {},
                onMenuClick = {},
                onLongClick = {},
                onSortOrderClick = {},
                onChangeViewTypeClick = {},
                isListView = false,
                sortOrder = "Any Name"
            )
        }
        composeTestRule.run {
            onAllNodes(hasTestTag("favorite Tag"))
            onAllNodes(hasTestTag("exported Tag").not())
            onAllNodes(hasTestTag("taken Tag").not())
        }
    }

    @Test
    fun `test when list item is exported then it shows folder exported icon`() = runTest {
        val node: FolderNode = mock()
        whenever(node.name).thenReturn("Some name")
        whenever(node.childFileCount).thenReturn(1)
        whenever(node.childFolderCount).thenReturn(2)
        whenever(node.isExported).thenReturn(true)
        whenever(
            stringUtilWrapper.getFolderInfo(
                node.childFolderCount,
                node.childFileCount
            )
        ).thenReturn(" 2 Folder 1 file")
        composeTestRule.setContent {
            ListView(
                modifier = Modifier,
                nodeUIItem = listOf(NodeUIItem(node = node, isSelected = true)),
                stringUtilWrapper = stringUtilWrapper,
                onItemClicked = {},
                onMenuClick = {},
                onLongClick = {},
                onSortOrderClick = {},
                onChangeViewTypeClick = {},
                isListView = false,
                sortOrder = "Any Name"
            )
        }
        composeTestRule.run {
            onAllNodes(hasTestTag("exported Tag"))
            onAllNodes(hasTestTag("favorite Tag").not())
            onAllNodes(hasTestTag("taken Tag").not())
        }
    }

    @Test
    fun `test when list item is taken down favorite and exported then it shows favorite, exported and taken down icon`() =
        runTest {
            val node: FolderNode = mock()
            whenever(node.name).thenReturn("Some name")
            whenever(node.childFileCount).thenReturn(1)
            whenever(node.childFolderCount).thenReturn(2)
            whenever(node.isTakenDown).thenReturn(true)
            whenever(node.isExported).thenReturn(true)
            whenever(node.isFavourite).thenReturn(true)
            whenever(
                stringUtilWrapper.getFolderInfo(
                    node.childFolderCount,
                    node.childFileCount
                )
            ).thenReturn(" 2 Folder 1 file")
            composeTestRule.setContent {
                ListView(
                    modifier = Modifier,
                    nodeUIItem = listOf(NodeUIItem(node = node, isSelected = false)),
                    stringUtilWrapper = stringUtilWrapper,
                    onItemClicked = {},
                    onMenuClick = {},
                    onLongClick = {},
                    onSortOrderClick = {},
                    onChangeViewTypeClick = {},
                    isListView = false,
                    sortOrder = "Any Name"
                )
            }
            composeTestRule.run {
                onAllNodes(hasTestTag("exported Tag"))
                onAllNodes(hasTestTag("favorite Tag"))
                onAllNodes(hasTestTag("taken Tag"))
            }
        }

    @Test
    fun `test when list item is folder info then it shows folder and file count`() = runTest {
        val node: FolderNode = mock()
        whenever(node.name).thenReturn("Some name")
        whenever(node.childFileCount).thenReturn(1)
        whenever(node.childFolderCount).thenReturn(2)
        whenever(
            stringUtilWrapper.getFolderInfo(
                node.childFolderCount,
                node.childFileCount
            )
        ).thenReturn("2 Folder 1 file")
        composeTestRule.setContent {
            ListView(
                modifier = Modifier,
                nodeUIItem = listOf(NodeUIItem(node = node, isSelected = true)),
                stringUtilWrapper = stringUtilWrapper,
                onItemClicked = {},
                onMenuClick = {},
                onLongClick = {},
                onSortOrderClick = {},
                onChangeViewTypeClick = {},
                isListView = false,
                sortOrder = "Any Name"
            )
        }
        composeTestRule.run {
            onAllNodes(hasTestTag("Info Text"))
        }
    }
}