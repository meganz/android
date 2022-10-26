package test.mega.privacy.android.app.presentation.favourites

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.favourites.FavouriteFolderViewModel
import mega.privacy.android.app.presentation.favourites.facade.MegaUtilWrapper
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.ChildrenNodesLoadState
import mega.privacy.android.app.presentation.favourites.model.mapper.FavouriteMapper
import mega.privacy.android.app.utils.wrapper.FetchNodeWrapper
import mega.privacy.android.domain.entity.NodeFolder
import mega.privacy.android.domain.entity.FavouriteFolderInfo
import mega.privacy.android.domain.usecase.GetFavouriteFolderInfo
import nz.mega.sdk.MegaNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class FavouriteFolderViewModelTest {
    private lateinit var underTest: FavouriteFolderViewModel

    private val getFavouriteFolderInfo = mock<GetFavouriteFolderInfo>()
    private val stringUtilWrapper = mock<StringUtilWrapper>()
    private val favouriteMapper = mock<FavouriteMapper>()

    private val megaNode: MegaNode = mock {
        on { handle }.thenReturn(123)
        on { label }.thenReturn(MegaNode.NODE_LBL_RED)
        on { size }.thenReturn(1000L)
        on { parentHandle }.thenReturn(1234)
        on { base64Handle }.thenReturn("base64Handle")
        on { modificationTime }.thenReturn(1234567890)
        on { isFolder }.thenReturn(true)
        on { isInShare }.thenReturn(true)
        on { name }.thenReturn("testName.txt")
    }

    private val fetchNodeWrapper = mock<FetchNodeWrapper> {
        onBlocking { invoke(any()) }.thenReturn(megaNode)
    }

    private val megaUtilWrapper = mock<MegaUtilWrapper>()

    private val favourite = NodeFolder(
        id = megaNode.handle,
        name = megaNode.name,
        label = megaNode.label,
        parentId = megaNode.parentHandle,
        base64Id = megaNode.base64Handle,
        hasVersion = false,
        numChildFolders = 0,
        numChildFiles = 0,
        isFavourite = true,
        isExported = false,
        isTakenDown = false,
        isInRubbishBin = false,
        isIncomingShare = false,
        isShared = false,
        isPendingShare = false,
        device = ""
    )

    private val list = listOf(favourite)
    private val rootHandle: Long = -1
    private val currentHandle: Long = 1

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = FavouriteFolderViewModel(
            context = mock(),
            ioDispatcher = UnconfinedTestDispatcher(),
            getFavouriteFolderInfo = getFavouriteFolderInfo,
            stringUtilWrapper = stringUtilWrapper,
            megaUtilWrapper = megaUtilWrapper,
            savedStateHandle = SavedStateHandle(),
            favouriteMapper = favouriteMapper,
            fetchNode = fetchNodeWrapper,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that start with loading state and children nodes is empty`() = runTest {
        whenever(getFavouriteFolderInfo(-1)).thenReturn(
            flowOf(
                FavouriteFolderInfo(
                    emptyList(),
                    "test name",
                    1,
                    1
                )
            )
        )
        underTest.childrenNodesState.test {
            assertTrue(awaitItem() is ChildrenNodesLoadState.Loading)
            assertTrue(awaitItem() is ChildrenNodesLoadState.Empty)
        }
    }

    @Test
    fun `test that start with loading state, children nodes is not empty and back pressed callback is not enable`() =
        runTest {
            whenever(stringUtilWrapper.getFolderInfo(0, 0)).thenReturn("info")
            whenever(getFavouriteFolderInfo(rootHandle)).thenReturn(
                flowOf(
                    FavouriteFolderInfo(
                        list,
                        "testName",
                        rootHandle,
                        1
                    )
                )
            )
            whenever(favouriteMapper(any(), any(), any(), any(), any())).thenReturn(mock())
            whenever(fetchNodeWrapper(anyOrNull())).thenReturn(megaNode)
            whenever(megaUtilWrapper.availableOffline(anyOrNull(),
                anyOrNull())).thenReturn(true)
            underTest.childrenNodesState.test {
                assertTrue(awaitItem() is ChildrenNodesLoadState.Loading)
                val actual = awaitItem()
                assertTrue(actual is ChildrenNodesLoadState.Success)
                assertThat(actual.isBackPressedEnable).isFalse()
            }
        }

    @Test
    fun `test that start with loading state, children nodes is not empty and back pressed callback is enable`() =
        runTest {
            whenever(stringUtilWrapper.getFolderInfo(0, 0)).thenReturn("info")
            whenever(getFavouriteFolderInfo(rootHandle)).thenReturn(
                flowOf(
                    FavouriteFolderInfo(
                        list,
                        "testName",
                        currentHandle,
                        1
                    )
                )
            )
            whenever(favouriteMapper(any(), any(), any(), any(), any())).thenReturn(mock())
            whenever(fetchNodeWrapper(anyOrNull())).thenReturn(megaNode)
            whenever(megaUtilWrapper.availableOffline(anyOrNull(),
                anyOrNull())).thenReturn(true)
            underTest.childrenNodesState.test {
                assertTrue(awaitItem() is ChildrenNodesLoadState.Loading)
                val actual = awaitItem()
                assertTrue(actual is ChildrenNodesLoadState.Success)
                assertThat(actual.isBackPressedEnable).isTrue()
            }
        }
}