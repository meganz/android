package mega.privacy.android.app.appstate

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.analytics.test.AnalyticsTestExtension
import mega.privacy.android.app.R
import mega.privacy.android.app.appstate.content.navigation.NavigationResultManager
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.nodecomponents.sheet.home.HomeFabOption
import mega.privacy.android.core.nodecomponents.sheet.home.HomeFabOptionsBottomSheetNavKey
import mega.privacy.android.domain.entity.node.root.RefreshEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.entity.ConnectivityState
import mega.privacy.android.domain.usecase.network.GetCurrentConnectivityStateUseCase
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.ChatListNavKey
import mega.privacy.android.navigation.destination.DeepLinksDialogNavKey
import mega.privacy.android.navigation.destination.ShareToMegaNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MegaActivityIntentActionHandlerTest {

    private lateinit var underTest: MegaActivityIntentActionHandler

    private val navigationEventQueue = mock<NavigationEventQueue>()
    private val navigationResultManager = mock<NavigationResultManager>()
    private val snackbarEventQueue = mock<SnackbarEventQueue>()
    private val getCurrentConnectivityStateUseCase = mock<GetCurrentConnectivityStateUseCase>()

    companion object {
        @JvmField
        @RegisterExtension
        val analyticsExtension = AnalyticsTestExtension()
    }

    @BeforeAll
    fun setUp() {
        underTest = MegaActivityIntentActionHandler(
            navigationEventQueue = navigationEventQueue,
            navigationResultManager = navigationResultManager,
            snackbarEventQueue = snackbarEventQueue,
            getCurrentConnectivityStateUseCase = getCurrentConnectivityStateUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            navigationEventQueue,
            navigationResultManager,
            snackbarEventQueue,
            getCurrentConnectivityStateUseCase,
        )
    }

    @Test
    fun `test that handleAction calls refreshSession with ManualRefresh when action is ACTION_REFRESH`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Constants.ACTION_REFRESH)
            var refreshedWith: RefreshEvent? = null

            underTest.handleAction(intent, { refreshedWith = it }, { null })

            assertThat(refreshedWith).isEqualTo(RefreshEvent.ManualRefresh)
        }

    @Test
    fun `test that handleAction calls refreshSession with ChangeEnvironment when action is ACTION_REFRESH_API_SERVER`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Constants.ACTION_REFRESH_API_SERVER)
            var refreshedWith: RefreshEvent? = null

            underTest.handleAction(intent, { refreshedWith = it }, { null })

            assertThat(refreshedWith).isEqualTo(RefreshEvent.ChangeEnvironment)
        }

    @Test
    fun `test that handleAction emits DeepLinksDialogNavKey when action is ACTION_DEEP_LINKS`() =
        runTest {
            val intent = mock<Intent>()
            val deepLink = "https://mega.nz/test"
            whenever(intent.action).thenReturn(MegaActivity.ACTION_DEEP_LINKS)
            whenever(intent.dataString).thenReturn(deepLink)

            underTest.handleAction(intent, {}, { null })

            verify(navigationEventQueue).emit(DeepLinksDialogNavKey(deepLink))
        }

    @Test
    fun `test that handleAction calls refreshSession with SdkReload when action is SdkReload`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(RefreshEvent.SdkReload.name)
            var refreshedWith: RefreshEvent? = null

            underTest.handleAction(intent, { refreshedWith = it }, { null })

            assertThat(refreshedWith).isEqualTo(RefreshEvent.SdkReload)
        }

    @Test
    fun `test that handleAction emits ShareToMegaNavKey when action is ACTION_SEND`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Intent.ACTION_SEND)
            val shareUris = listOf(UriPath("content://test"))

            underTest.handleAction(intent, {}, { shareUris })

            verify(navigationEventQueue).emit(ShareToMegaNavKey(shareUris))
        }

    @Test
    fun `test that handleAction navigates to upload when shortcut upload and connected`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Constants.ACTION_SHORTCUT_UPLOAD)
            whenever(getCurrentConnectivityStateUseCase()).thenReturn(ConnectivityState.Connected(isOnWifi = true))

            underTest.handleAction(intent, {}, { null })

            verify(navigationResultManager).returnResult(
                HomeFabOptionsBottomSheetNavKey.KEY,
                HomeFabOption.UploadFiles
            )
        }

    @Test
    fun `test that handleAction shows snackbar when shortcut upload and not connected`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Constants.ACTION_SHORTCUT_UPLOAD)
            whenever(getCurrentConnectivityStateUseCase()).thenReturn(ConnectivityState.Disconnected)

            underTest.handleAction(intent, {}, { null })

            verify(snackbarEventQueue).queueMessage(R.string.error_server_connection_problem)
            verify(navigationResultManager, never()).returnResult(any(), any<HomeFabOption>())
        }

    @Test
    fun `test that handleAction navigates to scan document when shortcut scan document and connected`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Constants.ACTION_SHORTCUT_SCAN_DOCUMENT)
            whenever(getCurrentConnectivityStateUseCase()).thenReturn(ConnectivityState.Connected(isOnWifi = true))

            underTest.handleAction(intent, {}, { null })

            verify(navigationResultManager).returnResult(
                HomeFabOptionsBottomSheetNavKey.KEY,
                HomeFabOption.ScanDocument
            )
        }

    @Test
    fun `test that handleAction shows snackbar when shortcut scan document and not connected`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Constants.ACTION_SHORTCUT_SCAN_DOCUMENT)
            whenever(getCurrentConnectivityStateUseCase()).thenReturn(ConnectivityState.Disconnected)

            underTest.handleAction(intent, {}, { null })

            verify(snackbarEventQueue).queueMessage(R.string.error_server_connection_problem)
            verify(navigationResultManager, never()).returnResult(any(), any<HomeFabOption>())
        }

    @Test
    fun `test that handleAction navigates to chat when shortcut chat and connected`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Constants.ACTION_SHORTCUT_CHAT)
            whenever(getCurrentConnectivityStateUseCase()).thenReturn(ConnectivityState.Connected(isOnWifi = true))

            underTest.handleAction(intent, {}, { null })

            verify(navigationEventQueue).emit(
                navKey = any(),
                priority = any(),
                navOptions = anyOrNull(),
            )
        }

    @Test
    fun `test that handleAction shows snackbar when shortcut chat and not connected`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Constants.ACTION_SHORTCUT_CHAT)
            whenever(getCurrentConnectivityStateUseCase()).thenReturn(ConnectivityState.Disconnected)

            underTest.handleAction(intent, {}, { null })

            verify(snackbarEventQueue).queueMessage(R.string.error_server_connection_problem)
            verify(navigationEventQueue, never()).emit(
                navKey = any(),
                priority = any(),
                navOptions = anyOrNull(),
            )
        }
}
