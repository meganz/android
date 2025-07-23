@file:OptIn(ExperimentalCoroutinesApi::class)

package mega.privacy.android.app.presentation.photos.mediadiscovery

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetNodeListByIds
import mega.privacy.android.app.domain.usecase.GetPublicNodeListByIds
import mega.privacy.android.app.extensions.asHotFlow
import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.app.presentation.clouddrive.mapper.StorageCapacityMapper
import mega.privacy.android.app.presentation.clouddrive.model.StorageOverQuotaCapacity
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetCameraSortOrder
import mega.privacy.android.domain.usecase.GetFileUrlByNodeHandleUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.MonitorAlmostFullStorageBannerVisibilityUseCase
import mega.privacy.android.domain.usecase.MonitorMediaDiscoveryView
import mega.privacy.android.domain.usecase.SetAlmostFullStorageBannerClosingTimestampUseCase
import mega.privacy.android.domain.usecase.SetCameraSortOrder
import mega.privacy.android.domain.usecase.SetMediaDiscoveryView
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.GetCurrentStorageStateUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.folderlink.GetPublicChildNodeFromIdUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionWithActionUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriByHandleUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.domain.usecase.photos.GetPhotosByFolderIdUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorSubFolderMediaDiscoverySettingsUseCase
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.stream.Stream

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MediaDiscoveryViewModelTest {
    private lateinit var underTest: MediaDiscoveryViewModel

    private val getNodeListByIds = mock<GetNodeListByIds>()
    private val savedStateHandle = mock<SavedStateHandle>()
    private val getPhotosByFolderIdUseCase = mock<GetPhotosByFolderIdUseCase>()
    private val getCameraSortOrder = mock<GetCameraSortOrder>()
    private val setCameraSortOrder = mock<SetCameraSortOrder>()
    private val monitorMediaDiscoveryView = mock<MonitorMediaDiscoveryView>()
    private val setMediaDiscoveryView = mock<SetMediaDiscoveryView>()
    private val getNodeByHandle = mock<GetNodeByHandle>()
    private val getFingerprintUseCase = mock<GetFingerprintUseCase>()
    private val megaApiHttpServerIsRunningUseCase = mock<MegaApiHttpServerIsRunningUseCase>()
    private val megaApiHttpServerStartUseCase = mock<MegaApiHttpServerStartUseCase>()
    private val getFileUrlByNodeHandleUseCase = mock<GetFileUrlByNodeHandleUseCase>()
    private val getLocalFolderLinkFromMegaApiUseCase = mock<GetLocalFolderLinkFromMegaApiUseCase>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val checkNodesNameCollisionWithActionUseCase =
        mock<CheckNodesNameCollisionWithActionUseCase>()
    private val copyRequestMessageMapper = mock<CopyRequestMessageMapper>()
    private val hasCredentialsUseCase = mock<HasCredentialsUseCase>()
    private val getPublicNodeListByIds = mock<GetPublicNodeListByIds>()
    private val setViewType = mock<SetViewType>()
    private val monitorSubFolderMediaDiscoverySettingsUseCase =
        mock<MonitorSubFolderMediaDiscoverySettingsUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val isNodeInRubbishBinUseCase = mock<IsNodeInRubbishBinUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getPublicChildNodeFromIdUseCase = mock<GetPublicChildNodeFromIdUseCase>()
    private val updateNodeSensitiveUseCase = mock<UpdateNodeSensitiveUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val monitorStorageStateUseCase = mock<MonitorStorageStateUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase> {
        on {
            invoke()
        }.thenReturn(flowOf(AccountDetail()))
    }
    private val isHiddenNodesOnboardedUseCase = mock<IsHiddenNodesOnboardedUseCase> {
        onBlocking {
            invoke()
        }.thenReturn(false)
    }
    private val getNodeContentUriByHandleUseCase = mock<GetNodeContentUriByHandleUseCase>()
    private val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase>()
    private val getCurrentStorageStateUseCase = mock<GetCurrentStorageStateUseCase>()
    private val setAlmostFullStorageBannerClosingTimestampUseCase =
        mock<SetAlmostFullStorageBannerClosingTimestampUseCase>()
    private val monitorAlmostFullStorageBannerClosingTimestampUseCase =
        mock<MonitorAlmostFullStorageBannerVisibilityUseCase>()
    private val storageCapacityMapper = mock<StorageCapacityMapper>()

    @BeforeAll
    fun setup() {
        commonStub()
        initViewModel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun initViewModel() {
        underTest = MediaDiscoveryViewModel(
            getNodeListByIds = getNodeListByIds,
            savedStateHandle = savedStateHandle,
            getPhotosByFolderIdUseCase = getPhotosByFolderIdUseCase,
            getCameraSortOrder = getCameraSortOrder,
            setCameraSortOrder = setCameraSortOrder,
            monitorMediaDiscoveryView = monitorMediaDiscoveryView,
            setMediaDiscoveryView = setMediaDiscoveryView,
            getNodeByHandle = getNodeByHandle,
            getFingerprintUseCase = getFingerprintUseCase,
            megaApiHttpServerIsRunningUseCase = megaApiHttpServerIsRunningUseCase,
            megaApiHttpServerStartUseCase = megaApiHttpServerStartUseCase,
            getFileUrlByNodeHandleUseCase = getFileUrlByNodeHandleUseCase,
            getLocalFolderLinkFromMegaApiUseCase = getLocalFolderLinkFromMegaApiUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            checkNodesNameCollisionWithActionUseCase = checkNodesNameCollisionWithActionUseCase,
            copyRequestMessageMapper = copyRequestMessageMapper,
            hasCredentialsUseCase = hasCredentialsUseCase,
            getPublicNodeListByIds = getPublicNodeListByIds,
            setViewType = setViewType,
            monitorSubFolderMediaDiscoverySettingsUseCase = monitorSubFolderMediaDiscoverySettingsUseCase,
            isNodeInRubbishBinUseCase = isNodeInRubbishBinUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            getPublicChildNodeFromIdUseCase = getPublicChildNodeFromIdUseCase,
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            defaultDispatcher = UnconfinedTestDispatcher(),
            isHiddenNodesOnboardedUseCase = isHiddenNodesOnboardedUseCase,
            getNodeContentUriByHandleUseCase = getNodeContentUriByHandleUseCase,
            monitorStorageStateUseCase = monitorStorageStateUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            setAlmostFullStorageBannerClosingTimestampUseCase = setAlmostFullStorageBannerClosingTimestampUseCase,
            monitorAlmostFullStorageBannerClosingTimestampUseCase = monitorAlmostFullStorageBannerClosingTimestampUseCase,
            storageCapacityMapper = storageCapacityMapper,
            durationInSecondsTextMapper = DurationInSecondsTextMapper(),
        )
    }

    private fun commonStub() {
        getCameraSortOrder.stub { onBlocking { invoke() }.thenReturn(SortOrder.ORDER_DEFAULT_ASC) }
        whenever(monitorConnectivityUseCase()).thenReturn(flow { awaitCancellation() })
        whenever(monitorMediaDiscoveryView()).thenReturn(flow { awaitCancellation() })
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flow { awaitCancellation() })
        whenever(monitorAccountDetailUseCase()).thenReturn(flow { awaitCancellation() })
        whenever(savedStateHandle.get<Long>(any())) doReturn (null)
        whenever(savedStateHandle.get<Boolean>(any())) doReturn (null)
        getFeatureFlagValueUseCase.stub { onBlocking { invoke(any()) }.thenReturn(false) }
        whenever(monitorStorageStateUseCase()).thenReturn(
            StorageState.Green.asHotFlow()
        )
        getCurrentStorageStateUseCase.stub { onBlocking { invoke() }.thenReturn(StorageState.Green) }

        setAlmostFullStorageBannerClosingTimestampUseCase.stub {
            onBlocking { invoke() }.thenReturn(Unit)
        }
        whenever(monitorAlmostFullStorageBannerClosingTimestampUseCase()).thenReturn(flowOf(true))
        whenever(
            storageCapacityMapper(
                storageState = any(),
                shouldShow = any()
            )
        ).thenReturn(
            StorageOverQuotaCapacity.DEFAULT
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getNodeListByIds,
            savedStateHandle,
            getPhotosByFolderIdUseCase,
            getCameraSortOrder,
            setCameraSortOrder,
            monitorMediaDiscoveryView,
            setMediaDiscoveryView,
            getNodeByHandle,
            getFingerprintUseCase,
            megaApiHttpServerIsRunningUseCase,
            megaApiHttpServerStartUseCase,
            getFileUrlByNodeHandleUseCase,
            getLocalFolderLinkFromMegaApiUseCase,
            monitorConnectivityUseCase,
            checkNodesNameCollisionWithActionUseCase,
            copyRequestMessageMapper,
            hasCredentialsUseCase,
            getPublicNodeListByIds,
            setViewType,
            monitorSubFolderMediaDiscoverySettingsUseCase,
            getFeatureFlagValueUseCase,
            isNodeInRubbishBinUseCase,
            getNodeByIdUseCase,
            getPublicChildNodeFromIdUseCase,
            monitorAccountDetailUseCase,
            monitorShowHiddenItemsUseCase,
            getNodeContentUriByHandleUseCase,
            monitorStorageStateUseCase,
            getCurrentStorageStateUseCase,
            setAlmostFullStorageBannerClosingTimestampUseCase,
            monitorAlmostFullStorageBannerClosingTimestampUseCase,
            storageCapacityMapper,
        )
        commonStub()
    }

    @Test
    fun `test that start download node event is launched with correct value when on save to device is invoked with download worker feature flag is enabled`() =
        runTest {
            val node = stubSelectedNode()

            underTest.onSaveToDeviceClicked()
            assertThat(underTest.state.value.downloadEvent)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
            val downloadEvent =
                underTest.state.value.downloadEvent as StateEventWithContentTriggered
            assertThat(downloadEvent.content)
                .isInstanceOf(TransferTriggerEvent.StartDownloadNode::class.java)
            val startEvent = downloadEvent.content as TransferTriggerEvent.StartDownloadNode
            assertThat((startEvent.nodes)).containsExactly(node)
        }

    private suspend fun stubSelectedNode(): TypedFileNode {
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(1L)
        }
        val megaNode = mock<MegaNode> {
            on { handle } doReturn 1L
        }
        whenever(getNodeListByIds(any())).thenReturn(listOf(megaNode))
        whenever(getNodeByIdUseCase(NodeId(any()))).thenReturn(node)
        underTest.togglePhotoSelection(1L)
        return node
    }

    @Test
    fun `test that download event initial value is consumed `() =
        runTest {
            assertThat(underTest.state.value.downloadEvent)
                .isInstanceOf(StateEventWithContentConsumed::class.java)
        }

    @Test
    fun `test that download event is consumed when on consume download event is invoked`() =
        runTest {
            stubSelectedNode()

            underTest.onSaveToDeviceClicked()

            assertThat(underTest.state.value.downloadEvent)
                .isInstanceOf(StateEventWithContentTriggered::class.java)

            underTest.consumeDownloadEvent()

            assertThat(underTest.state.value.downloadEvent)
                .isInstanceOf(StateEventWithContentConsumed::class.java)
        }

    @Test
    fun `test that turn on flag HiddenNodes and it is not from folder link then the flow is correct`() =
        runTest {
            commonStub()
            whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)) doReturn true
            whenever(savedStateHandle.get<Boolean>(MediaDiscoveryActivity.INTENT_KEY_FROM_FOLDER_LINK)) doReturn false
            initViewModel()
            verify(monitorShowHiddenItemsUseCase, times(1)).invoke()
            verify(monitorAccountDetailUseCase, times(1)).invoke()
        }

    @Test
    fun `test that turn off flag HiddenNodes and it is not from folder link then the flow is correct`() =
        runTest {
            commonStub()
            whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)) doReturn false
            whenever(savedStateHandle.get<Boolean>(MediaDiscoveryActivity.INTENT_KEY_FROM_FOLDER_LINK)) doReturn false
            initViewModel()
            verifyNoInteractions(monitorShowHiddenItemsUseCase)
            verifyNoInteractions(monitorAccountDetailUseCase)
        }

    @Test
    fun `test that turn on flag HiddenNodes and it is from folder link then the flow is correct`() =
        runTest {
            commonStub()
            whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)) doReturn true
            whenever(savedStateHandle.get<Boolean>(MediaDiscoveryActivity.INTENT_KEY_FROM_FOLDER_LINK)) doReturn true
            initViewModel()
            verifyNoInteractions(monitorShowHiddenItemsUseCase)
            verifyNoInteractions(monitorAccountDetailUseCase)
        }

    @Test
    fun `test that turn off flag HiddenNodes and it is from folder link then the flow is correct`() =
        runTest {
            commonStub()
            whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)) doReturn false
            whenever(savedStateHandle.get<Boolean>(MediaDiscoveryActivity.INTENT_KEY_FROM_FOLDER_LINK)) doReturn true
            initViewModel()
            verifyNoInteractions(monitorShowHiddenItemsUseCase)
            verifyNoInteractions(monitorAccountDetailUseCase)
        }

    @Test
    fun `test that filterNonSensitivePhotos return photos when showHiddenItems is null and isPaid`() =
        runTest {
            commonStub()
            initViewModel()
            val photos = listOf(
                createNonSensitivePhoto(),
                createSensitivePhoto(),
            )
            val expectedPhotos = underTest.filterNonSensitivePhotos(photos, true)
            assertThat(expectedPhotos).isEqualTo(photos)
        }

    @Test
    fun `test that filterNonSensitivePhotos return photos when showHiddenItems is true and isPaid`() =
        runTest {
            commonStub()
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
            initViewModel()
            val photos = listOf(
                createNonSensitivePhoto(),
                createSensitivePhoto(),
            )
            val expectedPhotos = underTest.filterNonSensitivePhotos(photos, true)
            assertThat(expectedPhotos).isEqualTo(photos)
        }


    @Test
    fun `test that filterNonSensitivePhotos return photos when showHiddenItems is true and isNotPaid`() =
        runTest {
            commonStub()
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
            initViewModel()
            val photos = listOf(
                createNonSensitivePhoto(),
                createSensitivePhoto(),
            )
            val expectedPhotos = underTest.filterNonSensitivePhotos(photos, false)
            assertThat(expectedPhotos).isEqualTo(photos)
        }

    @Test
    fun `test that filterNonSensitivePhotos return photos when showHiddenItems is true and isPaid is null`() =
        runTest {
            commonStub()
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
            initViewModel()
            val photos = listOf(
                createNonSensitivePhoto(),
                createSensitivePhoto(),
            )
            val expectedPhotos = underTest.filterNonSensitivePhotos(photos, null)
            assertThat(expectedPhotos).isEqualTo(photos)
        }

    @Test
    fun `test that filterNonSensitivePhotos return non-sensitive photos when showHiddenItems is false and isPaid`() =
        runTest {
            commonStub()
            getFeatureFlagValueUseCase.stub {
                onBlocking { invoke(ApiFeatures.HiddenNodesInternalRelease) }.thenReturn(
                    true
                )
            }
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            initViewModel()
            val nonSensitivePhoto = createNonSensitivePhoto()
            val photos = listOf(
                nonSensitivePhoto,
                createSensitivePhoto(),
            )
            val expectedPhotos = underTest.filterNonSensitivePhotos(photos, true)
            assertThat(expectedPhotos.size).isEqualTo(1)
            assertThat(expectedPhotos).isEqualTo(listOf(nonSensitivePhoto))
        }

    @Test
    fun `test that monitorShowHiddenItems works correctly`() =
        runTest {
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
            underTest.monitorShowHiddenItems(
                loadPhotosDone = true,
                listOf(
                    createNonSensitivePhoto(),
                )
            )
            verify(monitorShowHiddenItemsUseCase, times(1)).invoke()
            assertThat(underTest.showHiddenItems).isTrue()
        }

    @Test
    fun `test that monitorAccountDetail works correctly`() =
        runTest {
            val accountLevelDetail = mock<AccountLevelDetail> {
                on { accountType } doReturn AccountType.FREE
            }
            val accountDetail = mock<AccountDetail> {
                on { levelDetail } doReturn accountLevelDetail
            }
            whenever(monitorAccountDetailUseCase()).thenReturn(
                flowOf(accountDetail)
            )
            underTest.monitorAccountDetail(
                loadPhotosDone = true,
                listOf(
                    createNonSensitivePhoto(),
                )
            )
            verify(monitorAccountDetailUseCase, times(1)).invoke()
            underTest.state.test {
                assertThat(awaitItem().accountType).isEqualTo(AccountType.FREE)
            }
        }

    @Test
    fun `test that handleFolderPhotosAndLogic should update state to go back when sourcePhotos is empty and MD folder is in rubbish`() =
        runTest {
            val photos = listOf<Photo.Image>()
            commonStub()
            whenever(savedStateHandle.get<Long>(MediaDiscoveryFragment.INTENT_KEY_CURRENT_FOLDER_ID)) doReturn 1L
            whenever(isNodeInRubbishBinUseCase(NodeId(1L))) doReturn true
            initViewModel()
            underTest.handleFolderPhotosAndLogic(photos)

            underTest.state.test {
                assertThat(awaitItem().shouldGoBack).isTrue()
            }
        }

    @Test
    fun `test that handleFolderPhotosAndLogic should not update state to go back when sourcePhotos is empty and MD folder is not in rubbish`() =
        runTest {
            val photos = listOf<Photo.Image>()
            commonStub()
            whenever(savedStateHandle.get<Long>(MediaDiscoveryFragment.INTENT_KEY_CURRENT_FOLDER_ID)) doReturn 1L
            whenever(isNodeInRubbishBinUseCase(NodeId(1L))) doReturn false
            initViewModel()
            underTest.handleFolderPhotosAndLogic(photos)

            underTest.state.test {
                assertThat(awaitItem().shouldGoBack).isFalse()
            }
        }

    @Test
    fun `test that handleFolderPhotosAndLogic should not update state to go back when sourcePhotos is not empty and MD folder is in rubbish`() =
        runTest {
            val photos = listOf(createNonSensitivePhoto())
            commonStub()
            whenever(savedStateHandle.get<Long>(MediaDiscoveryFragment.INTENT_KEY_CURRENT_FOLDER_ID)) doReturn 1L
            whenever(isNodeInRubbishBinUseCase(NodeId(1L))) doReturn true
            initViewModel()
            underTest.handleFolderPhotosAndLogic(photos)

            underTest.state.test {
                assertThat(awaitItem().shouldGoBack).isFalse()
            }
        }

    @Test
    fun `test that handleFolderPhotosAndLogic should not update state to go back when sourcePhotos is not empty and MD folder is not in rubbish`() =
        runTest {
            val photos = listOf(createNonSensitivePhoto())
            commonStub()
            whenever(savedStateHandle.get<Long>(MediaDiscoveryFragment.INTENT_KEY_CURRENT_FOLDER_ID)) doReturn 1L
            whenever(isNodeInRubbishBinUseCase(NodeId(1L))) doReturn false
            initViewModel()
            underTest.handleFolderPhotosAndLogic(photos)

            underTest.state.test {
                assertThat(awaitItem().shouldGoBack).isFalse()
            }
        }

    private fun createNonSensitivePhoto(): Photo.Image {
        return mock<Photo.Image> {
            on { isSensitive } doReturn false
            on { isSensitiveInherited } doReturn false
            on { modificationTime } doReturn LocalDateTime.now()
        }
    }

    private fun createSensitivePhoto(
        isSensitive: Boolean = true,
        isSensitiveInherited: Boolean = true,
    ): Photo.Image {
        return mock<Photo.Image> {
            on { this.isSensitive } doReturn isSensitive
            on { this.isSensitiveInherited } doReturn isSensitiveInherited
        }
    }

    @Test
    fun `test that getNodeContentUriByHandleUseCase is invoked and returns as expected`() =
        runTest {
            val paramHandle = 1L
            val expectedContentUri = NodeContentUri.RemoteContentUri("", false)
            whenever(getNodeContentUriByHandleUseCase(paramHandle)).thenReturn(
                expectedContentUri
            )
            val actual = underTest.getNodeContentUri(paramHandle)
            assertThat(actual).isEqualTo(expectedContentUri)
            verify(getNodeContentUriByHandleUseCase).invoke(paramHandle)
        }

    private fun provideStorageStateParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(
            StorageState.Red,
            true,
            StorageOverQuotaCapacity.FULL
        ),
        Arguments.of(
            StorageState.Green,
            true,
            StorageOverQuotaCapacity.DEFAULT
        ), Arguments.of(
            StorageState.Orange,
            true,
            StorageOverQuotaCapacity.ALMOST_FULL
        ), Arguments.of(
            StorageState.Orange,
            false,
            StorageOverQuotaCapacity.DEFAULT
        ),
        Arguments.of(
            StorageState.Change,
            true,
            StorageOverQuotaCapacity.DEFAULT
        ), Arguments.of(
            StorageState.Unknown,
            true,
            StorageOverQuotaCapacity.DEFAULT
        ), Arguments.of(
            StorageState.PayWall,
            true,
            StorageOverQuotaCapacity.DEFAULT
        )
    )

    @ParameterizedTest(name = "when storage state is: {0} and isDismissiblePeriodOver is: {1} then storageCapacity is: {2}")
    @MethodSource("provideStorageStateParameters")
    fun `test that storageCapacity is updated correctly when getCurrentStorageStateUseCase is invoked`(
        storageState: StorageState,
        isDismissiblePeriodOver: Boolean,
        storageOverQuotaCapacity: StorageOverQuotaCapacity,
    ) = runTest {
        commonStub()
        whenever((monitorStorageStateUseCase())).thenReturn(
            storageState.asHotFlow()
        )
        whenever(getCurrentStorageStateUseCase()).thenReturn(
            storageState
        )
        whenever(monitorAlmostFullStorageBannerClosingTimestampUseCase()).thenReturn(
            flowOf(
                isDismissiblePeriodOver
            )
        )
        whenever(
            storageCapacityMapper(
                storageState = storageState,
                shouldShow = isDismissiblePeriodOver
            )
        ).thenReturn(
            storageOverQuotaCapacity
        )
        initViewModel()
        underTest.state.map { it.storageCapacity }.distinctUntilChanged().test {
            assertThat(awaitItem()).isEqualTo(storageOverQuotaCapacity)
        }
    }
}