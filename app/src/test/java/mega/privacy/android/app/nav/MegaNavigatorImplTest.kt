package mega.privacy.android.app.nav

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler
import mega.privacy.android.app.presentation.settings.compose.navigation.SettingsNavigatorImpl
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.nodecomponents.mapper.NodeContentUriIntentMapper
import mega.privacy.android.domain.usecase.GetFileTypeInfoByNameUseCase
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetFileTypeInfoUseCase
import mega.privacy.android.navigation.contract.queue.NavPriority
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.OpenTextEditorParams
import mega.privacy.android.navigation.destination.LegacyTextEditorNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MegaNavigatorImplTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val applicationScope = CoroutineScope(UnconfinedTestDispatcher())
    private val nodeContentUriIntentMapper = mock<NodeContentUriIntentMapper>()
    private val getFileTypeInfoUseCase = mock<GetFileTypeInfoUseCase>()
    private val getFileTypeInfoByNameUseCase = mock<GetFileTypeInfoByNameUseCase>()
    private val settingsNavigator =
        mock<SettingsNavigatorImpl>()
    private val getDomainNameUseCase = mock<GetDomainNameUseCase>()
    private val mediaPlayerIntentMapper = mock<MediaPlayerIntentMapper>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val navigationQueue = mock<NavigationEventQueue>()
    private val activityLifecycleHandler = mock<ActivityLifecycleHandler>()
    private val snackbarEventQueue = mock<SnackbarEventQueue>()

    private lateinit var underTest: MegaNavigatorImpl

    private val context = mock<Context>()

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeAll
    fun setup() {
        underTest = MegaNavigatorImpl(
            applicationScope = applicationScope,
            nodeContentUriIntentMapper = nodeContentUriIntentMapper,
            getFileTypeInfoUseCase = getFileTypeInfoUseCase,
            getFileTypeInfoByNameUseCase = getFileTypeInfoByNameUseCase,
            settingsNavigator = settingsNavigator,
            getDomainNameUseCase = getDomainNameUseCase,
            mediaPlayerIntentMapper = mediaPlayerIntentMapper,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            navigationQueue = navigationQueue,
            activityLifecycleHandler = activityLifecycleHandler,
            snackbarEventQueue = snackbarEventQueue,
            mainDispatcher = UnconfinedTestDispatcher()
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(
            context,
            nodeContentUriIntentMapper,
            getFileTypeInfoUseCase,
            getFileTypeInfoByNameUseCase,
            settingsNavigator,
            getDomainNameUseCase,
            mediaPlayerIntentMapper,
            getFeatureFlagValueUseCase,
            navigationQueue,
            activityLifecycleHandler,
            snackbarEventQueue,
        )
    }

    @Test
    fun `test that getPendingIntentConsideringSingleActivity returns singleActivityPendingIntent`() =
        runTest {
            val expectedPendingIntent = mock<PendingIntent>()
            val singleActivityPendingIntent: () -> PendingIntent = { expectedPendingIntent }

            val result = underTest.getPendingIntentConsideringSingleActivity(
                context = context,
                singleActivityPendingIntent = singleActivityPendingIntent,
            )

            assertThat(result).isEqualTo(expectedPendingIntent)
        }

    @Test
    fun `test that sendMessageConsideringSingleActivity queues message to snackbarEventQueue`() =
        runTest {
            whenever(activityLifecycleHandler.getCurrentActivity()).thenReturn(mock<MegaActivity>())
            val message = "Test warning message"

            underTest.sendMessageConsideringSingleActivity(context, message)

            verify(snackbarEventQueue).queueMessage(message)
            verify(context, never()).startActivity(argThat<Intent> {
                component?.className == MegaActivity::class.java.name
            })
        }

    @Test
    fun `test that sendMessageConsideringSingleActivity launches MegaActivity when current activity is not MegaActivity`() =
        runTest {
            whenever(activityLifecycleHandler.getCurrentActivity()).thenReturn(null)
            val message = "Test warning message"

            underTest.sendMessageConsideringSingleActivity(context, message)

            verify(context, atLeastOnce()).startActivity(any())
            verify(snackbarEventQueue).queueMessage(message)
        }

    @Test
    fun `test that openTextEditor LocalFile offline emits nav key`() =
        runTest {
            whenever(activityLifecycleHandler.getCurrentActivity()).thenReturn(mock<MegaActivity>())

            underTest.openTextEditor(
                context = context,
                params = OpenTextEditorParams.LocalFile(
                    localPath = "/data/offline/note.txt",
                    fileName = "note.txt",
                    nodeSourceType = Constants.OFFLINE_ADAPTER,
                ),
            )

            verify(navigationQueue).emit(
                argThat<List<NavKey>> { navKeys ->
                    navKeys.size == 1 &&
                            navKeys[0] is LegacyTextEditorNavKey &&
                            (navKeys[0] as LegacyTextEditorNavKey).let {
                                it.localPath == "/data/offline/note.txt" &&
                                        it.fileName == "note.txt" &&
                                        it.nodeSourceType == Constants.OFFLINE_ADAPTER
                            }
                },
                eq(NavPriority.Default),
                isNull(),
            )
        }

    @Test
    fun `test that openTextEditor LocalFile zip emits nav key`() =
        runTest {
            whenever(activityLifecycleHandler.getCurrentActivity()).thenReturn(mock<MegaActivity>())

            underTest.openTextEditor(
                context = context,
                params = OpenTextEditorParams.LocalFile(
                    localPath = "/data/zip/readme.txt",
                    fileName = "readme.txt",
                    nodeSourceType = Constants.ZIP_ADAPTER,
                ),
            )

            verify(navigationQueue).emit(
                argThat<List<NavKey>> { navKeys ->
                    navKeys.size == 1 &&
                            navKeys[0] is LegacyTextEditorNavKey &&
                            (navKeys[0] as LegacyTextEditorNavKey).let {
                                it.localPath == "/data/zip/readme.txt" &&
                                        it.fileName == "readme.txt" &&
                                        it.nodeSourceType == Constants.ZIP_ADAPTER
                            }
                },
                eq(NavPriority.Default),
                isNull(),
            )
        }

    @Test
    fun `test that openTextEditor Chat emits nav key`() =
        runTest {
            whenever(activityLifecycleHandler.getCurrentActivity()).thenReturn(mock<MegaActivity>())

            underTest.openTextEditor(
                context = context,
                params = OpenTextEditorParams.Chat(chatId = 123L, messageId = 456L),
            )

            verify(navigationQueue).emit(
                argThat<List<NavKey>> { navKeys ->
                    navKeys.size == 1 &&
                            navKeys[0] is LegacyTextEditorNavKey &&
                            (navKeys[0] as LegacyTextEditorNavKey).let {
                                it.chatId == 123L && it.messageId == 456L
                            }
                },
                eq(NavPriority.Default),
                isNull(),
            )
        }

    @Test
    fun `test that openTextEditor FileLink starts activity directly`() = runTest {
        underTest.openTextEditor(
            context = context,
            params = OpenTextEditorParams.FileLink(
                serializedNode = "serialized_node_data",
                urlFileLink = "https://mega.nz/file/abc123",
            ),
        )

        verify(context).startActivity(any())
    }
}
