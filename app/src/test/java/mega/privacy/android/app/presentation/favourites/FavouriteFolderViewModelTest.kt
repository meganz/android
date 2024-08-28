package mega.privacy.android.app.presentation.favourites

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.favourites.FavouriteFolderViewModel
import mega.privacy.android.app.presentation.favourites.facade.MegaUtilWrapper
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.ChildrenNodesLoadState
import mega.privacy.android.app.presentation.favourites.model.FavouriteFolder
import mega.privacy.android.app.presentation.favourites.model.mapper.FavouriteMapper
import mega.privacy.android.app.utils.wrapper.FetchNodeWrapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.FavouriteFolderInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.GetFileTypeInfoByNameUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.favourites.GetFavouriteFolderInfoUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FavouriteFolderViewModelTest {
    private lateinit var underTest: FavouriteFolderViewModel

    private val getFavouriteFolderInfoUseCase = mock<GetFavouriteFolderInfoUseCase>()
    private val stringUtilWrapper = mock<StringUtilWrapper>()
    private val favouriteMapper = mock<FavouriteMapper>()

    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase> {
        on {
            invoke()
        }.thenReturn(flowOf(AccountDetail()))
    }
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase> {
        on {
            invoke()
        }.thenReturn(flowOf(false))
    }
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase> {
        onBlocking {
            invoke(any())
        }.thenReturn(false)
    }
    private val getFileTypeInfoByNameUseCase = mock<GetFileTypeInfoByNameUseCase>()
    private val getNodeContentUriUseCase = mock<GetNodeContentUriUseCase>()

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

    @BeforeEach
    fun setUp() {
        reset(
            getFavouriteFolderInfoUseCase,
            megaUtilWrapper,
            fetchNodeWrapper,
            stringUtilWrapper,
            getFileTypeInfoByNameUseCase,
            getNodeContentUriUseCase
        )
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
            fetchNodeWrapper = fetchNodeWrapper,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            defaultDispatcher = UnconfinedTestDispatcher(),
            getNodeContentUriUseCase = getNodeContentUriUseCase,
            getFileTypeInfoByNameUseCase = getFileTypeInfoByNameUseCase,
        )
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

    @Test
    fun `test that getFileTypeInfoByNameUseCase function is invoked and returns as expected`() =
        runTest {
            val mockName = "name"
            val expectedFileTypeInfo = VideoFileTypeInfo("", "", 10.seconds)
            whenever(getFileTypeInfoByNameUseCase(mockName)).thenReturn(expectedFileTypeInfo)
            val actual = underTest.getFileTypeInfo(mockName)
            assertThat(actual is VideoFileTypeInfo).isTrue()
            verify(getFileTypeInfoByNameUseCase).invoke(mockName)
        }

    @Test
    fun `test that getFileTypeInfoByNameUseCase returns null when an exception is thrown`() =
        runTest {
            val mockName = "name"
            whenever(getFileTypeInfoByNameUseCase(mockName)).thenThrow(NullPointerException())
            val actual = underTest.getFileTypeInfo(mockName)
            assertThat(actual).isNull()
        }

    @Test
    fun `test that GetNodeContentUriUseCase function is invoked and returns as expected`() =
        runTest {
            val mockTypedFileNode = mock<TypedFileNode>()
            val expectedContentUri = NodeContentUri.RemoteContentUri("", false)
            whenever(getNodeContentUriUseCase(any())).thenReturn(expectedContentUri)
            val actual = underTest.getNodeContentUri(mockTypedFileNode)
            assertThat(actual).isEqualTo(expectedContentUri)
            verify(getNodeContentUriUseCase).invoke(mockTypedFileNode)
        }
}