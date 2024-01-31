package mega.privacy.android.app.presentation.photos.mediadiscovery

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.AuthorizeNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetNodeListByIds
import mega.privacy.android.app.domain.usecase.GetPublicNodeListByIds
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper
import mega.privacy.android.app.presentation.transfers.startdownload.model.TransferTriggerEvent
import mega.privacy.android.app.usecase.CopyNodeListUseCase
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetCameraSortOrder
import mega.privacy.android.domain.usecase.GetFileUrlByNodeHandleUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.MonitorMediaDiscoveryView
import mega.privacy.android.domain.usecase.SetCameraSortOrder
import mega.privacy.android.domain.usecase.SetMediaDiscoveryView
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.folderlink.GetPublicChildNodeFromIdUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.photos.GetPhotosByFolderIdInFolderLinkUseCase
import mega.privacy.android.domain.usecase.photos.GetPhotosByFolderIdUseCase
import mega.privacy.android.domain.usecase.setting.MonitorSubFolderMediaDiscoverySettingsUseCase
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
class MediaDiscoveryViewModelTest {
    private lateinit var underTest: MediaDiscoveryViewModel

    private val getNodeListByIds = mock<GetNodeListByIds>()
    private val savedStateHandle = mock<SavedStateHandle>()
    private val getPhotosByFolderIdUseCase = mock<GetPhotosByFolderIdUseCase>()
    private val getPhotosByFolderIdInFolderLinkUseCase =
        mock<GetPhotosByFolderIdInFolderLinkUseCase>()
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
    private val checkNameCollisionUseCase = mock<CheckNameCollisionUseCase>()
    private val authorizeNode = mock<AuthorizeNode>()
    private val copyNodeListUseCase = mock<CopyNodeListUseCase>()
    private val copyRequestMessageMapper = mock<CopyRequestMessageMapper>()
    private val hasCredentialsUseCase = mock<HasCredentialsUseCase>()
    private val getPublicNodeListByIds = mock<GetPublicNodeListByIds>()
    private val setViewType = mock<SetViewType>()
    private val monitorSubFolderMediaDiscoverySettingsUseCase =
        mock<MonitorSubFolderMediaDiscoverySettingsUseCase>()
    private val getFeatureFlagUseCase = mock<GetFeatureFlagValueUseCase>()
    private val isNodeInRubbish = mock<IsNodeInRubbish>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getPublicChildNodeFromIdUseCase = mock<GetPublicChildNodeFromIdUseCase>()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        commonStub()
        underTest = MediaDiscoveryViewModel(
            getNodeListByIds,
            savedStateHandle,
            getPhotosByFolderIdUseCase,
            getPhotosByFolderIdInFolderLinkUseCase,
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
            checkNameCollisionUseCase,
            authorizeNode,
            copyNodeListUseCase,
            copyRequestMessageMapper,
            hasCredentialsUseCase,
            getPublicNodeListByIds,
            setViewType,
            monitorSubFolderMediaDiscoverySettingsUseCase,
            getFeatureFlagUseCase,
            isNodeInRubbish,
            getNodeByIdUseCase,
            getPublicChildNodeFromIdUseCase,
        )
    }

    private fun commonStub() = runTest {
        whenever(getCameraSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(monitorConnectivityUseCase()).thenReturn(emptyFlow())
        whenever(monitorMediaDiscoveryView()).thenReturn(emptyFlow())
        whenever(savedStateHandle.get<Long>(any())) doReturn (null)
        whenever(savedStateHandle.get<Boolean>(any())) doReturn (null)
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() = reset(
        getNodeListByIds,
        savedStateHandle,
        getPhotosByFolderIdUseCase,
        getPhotosByFolderIdInFolderLinkUseCase,
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
        checkNameCollisionUseCase,
        authorizeNode,
        copyNodeListUseCase,
        copyRequestMessageMapper,
        hasCredentialsUseCase,
        getPublicNodeListByIds,
        setViewType,
        monitorSubFolderMediaDiscoverySettingsUseCase,
        getFeatureFlagUseCase,
        isNodeInRubbish,
        getNodeByIdUseCase,
        getPublicChildNodeFromIdUseCase,
    )

    @Test
    fun `test that start download node event is launched with correct value when on save to device is invoked with download worker feature flag is enabled`() =
        runTest {
            val node = stubSelectedNode()
            whenever(getFeatureFlagUseCase(AppFeatures.DownloadWorker)).thenReturn(true)

            underTest.onSaveToDeviceClicked(mock())
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
    fun `test that legacy lambda is launched when on save to device is invoked with download worker feature flag is disabled`() =
        runTest {
            whenever(getFeatureFlagUseCase(AppFeatures.DownloadWorker)).thenReturn(false)
            val legacySaveToDevice = mock<() -> Unit>()
            underTest.onSaveToDeviceClicked(legacySaveToDevice)
            verify(legacySaveToDevice)()
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
            whenever(getFeatureFlagUseCase(AppFeatures.DownloadWorker)).thenReturn(true)

            underTest.onSaveToDeviceClicked(mock())

            assertThat(underTest.state.value.downloadEvent)
                .isInstanceOf(StateEventWithContentTriggered::class.java)

            underTest.consumeDownloadEvent()

            assertThat(underTest.state.value.downloadEvent)
                .isInstanceOf(StateEventWithContentConsumed::class.java)
        }
}