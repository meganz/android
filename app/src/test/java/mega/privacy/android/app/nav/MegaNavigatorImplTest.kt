package mega.privacy.android.app.nav

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.core.nodecomponents.mapper.NodeContentUriIntentMapper
import mega.privacy.android.domain.usecase.GetFileTypeInfoByNameUseCase
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetFileTypeInfoUseCase
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MegaNavigatorImplTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val applicationScope = CoroutineScope(UnconfinedTestDispatcher())
    private val nodeContentUriIntentMapper = mock<NodeContentUriIntentMapper>()
    private val getFileTypeInfoUseCase = mock<GetFileTypeInfoUseCase>()
    private val getFileTypeInfoByNameUseCase = mock<GetFileTypeInfoByNameUseCase>()
    private val settingsNavigator =
        mock<mega.privacy.android.app.presentation.settings.compose.navigation.SettingsNavigatorImpl>()
    private val getDomainNameUseCase = mock<GetDomainNameUseCase>()
    private val mediaPlayerIntentMapper = mock<MediaPlayerIntentMapper>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val navigationQueue = mock<NavigationEventQueue>()
    private val activityLifecycleHandler = mock<ActivityLifecycleHandler>()

    private lateinit var underTest: MegaNavigatorImpl

    private val context = mock<Context>()

    @BeforeEach
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
        )
    }

    @BeforeAll
    fun cleanUp() {
        reset(
            nodeContentUriIntentMapper,
            getFileTypeInfoUseCase,
            getFileTypeInfoByNameUseCase,
            settingsNavigator,
            getDomainNameUseCase,
            mediaPlayerIntentMapper,
            getFeatureFlagValueUseCase,
            navigationQueue,
            activityLifecycleHandler
        )
    }

    @Test
    fun `test that getPendingIntentConsideringSingleActivity returns singleActivityPendingIntent when SingleActivity is enabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.SingleActivity)).thenReturn(true)
            val expectedPendingIntent = mock<PendingIntent>()
            val createPendingIntent: (Intent) -> PendingIntent = mock()
            val singleActivityPendingIntent: () -> PendingIntent = { expectedPendingIntent }

            val result = underTest.getPendingIntentConsideringSingleActivity(
                context = context,
                legacyActivityClass = ManagerActivity::class.java,
                createPendingIntent = createPendingIntent,
                singleActivityPendingIntent = singleActivityPendingIntent,
            )

            assertThat(result).isEqualTo(expectedPendingIntent)
        }

    @Test
    fun `test that getPendingIntentConsideringSingleActivity returns legacy pendingIntent when SingleActivity is disabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.SingleActivity)).thenReturn(false)
            val expectedPendingIntent = mock<PendingIntent>()
            val createPendingIntent: (Intent) -> PendingIntent = { intent ->
                expectedPendingIntent
            }
            val singleActivityPendingIntent: () -> PendingIntent = mock()

            val result = underTest.getPendingIntentConsideringSingleActivity(
                context = context,
                legacyActivityClass = ManagerActivity::class.java,
                createPendingIntent = createPendingIntent,
                singleActivityPendingIntent = singleActivityPendingIntent,
            )

            assertThat(result).isEqualTo(expectedPendingIntent)
        }

    @Test
    fun `test that getPendingIntentConsideringSingleActivity creates intent with correct activity class when SingleActivity is disabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.SingleActivity)).thenReturn(false)
            val expected = mock<PendingIntent>()
            val createPendingIntent: (Intent) -> PendingIntent = { intent ->
                expected
            }
            val singleActivityPendingIntent: () -> PendingIntent = mock()

            val result = underTest.getPendingIntentConsideringSingleActivity(
                context = context,
                legacyActivityClass = ManagerActivity::class.java,
                createPendingIntent = createPendingIntent,
                singleActivityPendingIntent = singleActivityPendingIntent,
            )

            assertThat(result).isEqualTo(expected)
        }

    @Test
    fun `test that singleActivityPendingIntent lambda is not called when SingleActivity is disabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.SingleActivity)).thenReturn(false)
            var singleActivityPendingIntentCalled = false
            val createPendingIntent: (Intent) -> PendingIntent = { mock() }
            val singleActivityPendingIntent: () -> PendingIntent = {
                singleActivityPendingIntentCalled = true
                mock()
            }

            underTest.getPendingIntentConsideringSingleActivity(
                context = context,
                legacyActivityClass = ManagerActivity::class.java,
                createPendingIntent = createPendingIntent,
                singleActivityPendingIntent = singleActivityPendingIntent,
            )

            assertThat(singleActivityPendingIntentCalled).isFalse()
        }

    @Test
    fun `test that createPendingIntent lambda is not called when SingleActivity is enabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.SingleActivity)).thenReturn(true)
            var createPendingIntentCalled = false
            val createPendingIntent: (Intent) -> PendingIntent = {
                createPendingIntentCalled = true
                mock()
            }
            val singleActivityPendingIntent: () -> PendingIntent = { mock() }

            underTest.getPendingIntentConsideringSingleActivity(
                context = context,
                legacyActivityClass = ManagerActivity::class.java,
                createPendingIntent = createPendingIntent,
                singleActivityPendingIntent = singleActivityPendingIntent,
            )

            assertThat(createPendingIntentCalled).isFalse()
        }
}

