package test.mega.privacy.android.app.presentation.favourites

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.favourites.FavouriteFolderViewModel
import mega.privacy.android.app.presentation.favourites.facade.MegaUtilWrapper
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.ChildrenNodesLoadState
import mega.privacy.android.app.presentation.favourites.model.FavouriteFolder
import mega.privacy.android.app.presentation.favourites.model.mapper.FavouriteMapper
import mega.privacy.android.app.utils.wrapper.FetchNodeWrapper
import mega.privacy.android.domain.entity.FavouriteFolderInfo
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.favourites.GetFavouriteFolderInfoUseCase
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FavouriteFolderViewModelTest {
    private lateinit var underTest: FavouriteFolderViewModel

    private val getFavouriteFolderInfoUseCase = mock<GetFavouriteFolderInfoUseCase>()
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

    private val favourite = mock<TypedFolderNode>()

    private val list = listOf(favourite)
    private val rootHandle: Long = -1
    private val currentHandle: Long = 1

    @BeforeAll
    fun initialise() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeEach
    fun setUp() {
        reset(getFavouriteFolderInfoUseCase, megaUtilWrapper, fetchNodeWrapper, stringUtilWrapper)
        wheneverBlocking { getFavouriteFolderInfoUseCase(any()) }.thenReturn(emptyFlow())
        initViewModel()
    }

    private fun initViewModel() {
        underTest = FavouriteFolderViewModel(
            context = mock(),
            ioDispatcher = UnconfinedTestDispatcher(),
            getFavouriteFolderInfoUseCase = getFavouriteFolderInfoUseCase,
            stringUtilWrapper = stringUtilWrapper,
            megaUtilWrapper = megaUtilWrapper,
            savedStateHandle = SavedStateHandle(),
            favouriteMapper = favouriteMapper,
            fetchNodeWrapper = fetchNodeWrapper
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that start with loading state and children nodes is empty`() = runTest {
        whenever(getFavouriteFolderInfoUseCase(-1)).thenReturn(
            flowOf(
                FavouriteFolderInfo(
                    emptyList(),
                    "test name",
                    1,
                    1
                )
            )
        )
        initViewModel()
        underTest.childrenNodesState.test {
            assertThat(awaitItem()).isInstanceOf(ChildrenNodesLoadState.Empty::class.java)
        }
    }

    @Test
    fun `test that start with loading state, children nodes is not empty and back pressed callback is not enable`() =
        runTest {
            whenever(stringUtilWrapper.getFolderInfo(eq(0), eq(0), any())).thenReturn("info")
            whenever(getFavouriteFolderInfoUseCase(rootHandle)).thenReturn(
                flowOf(
                    FavouriteFolderInfo(
                        list,
                        "testName",
                        rootHandle,
                        1
                    )
                )
            )
            whenever(
                favouriteMapper(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(mock<FavouriteFolder>())
            whenever(fetchNodeWrapper(anyOrNull())).thenReturn(megaNode)
            whenever(
                megaUtilWrapper.availableOffline(
                    anyOrNull(),
                    anyOrNull()
                )
            ).thenReturn(true)
            initViewModel()
            underTest.childrenNodesState.test {
                val actual = awaitItem()
                assertTrue(actual is ChildrenNodesLoadState.Success)
                assertThat(actual.isBackPressedEnable).isFalse()
            }
        }

    @Test
    fun `test that start with loading state, children nodes is not empty and back pressed callback is enable`() =
        runTest {
            whenever(stringUtilWrapper.getFolderInfo(eq(0), eq(0), any())).thenReturn("info")
            whenever(getFavouriteFolderInfoUseCase(rootHandle)).thenReturn(
                flowOf(
                    FavouriteFolderInfo(
                        list,
                        "testName",
                        currentHandle,
                        1
                    )
                )
            )
            whenever(
                favouriteMapper(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(mock<FavouriteFolder>())
            whenever(fetchNodeWrapper(anyOrNull())).thenReturn(megaNode)
            whenever(
                megaUtilWrapper.availableOffline(
                    anyOrNull(),
                    anyOrNull()
                )
            ).thenReturn(true)
            initViewModel()
            underTest.childrenNodesState.test {
                val actual = awaitItem()
                assertTrue(actual is ChildrenNodesLoadState.Success)
                assertThat(actual.isBackPressedEnable).isTrue()
            }
        }
}