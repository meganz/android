package mega.privacy.android.app.nav

import android.app.Activity
import android.content.ComponentName
import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler
import mega.privacy.android.app.mediaplayer.Nav3AudioPlayerRouteLauncher
import mega.privacy.android.app.presentation.contactinfo.ContactInfoActivity
import mega.privacy.android.app.presentation.contactinfo.ContactInfoComposeActivity
import mega.privacy.android.app.presentation.settings.compose.navigation.SettingsNavigatorImpl
import mega.privacy.android.app.presentation.videoplayer.Nav3VideoPlayerRouteLauncher
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.nodecomponents.mapper.NodeContentUriIntentMapper
import mega.privacy.android.domain.usecase.GetFileTypeInfoByNameUseCase
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetFileTypeInfoUseCase
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import nz.mega.sdk.MegaChatApiJava
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MegaNavigatorImplContactInfoTest {

    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val navigationQueue = mock<NavigationEventQueue>()
    private val activityLifecycleHandler = mock<ActivityLifecycleHandler>()

    private fun createNavigator(dispatcher: CoroutineDispatcher) = MegaNavigatorImpl(
        applicationScope = CoroutineScope(dispatcher),
        nodeContentUriIntentMapper = mock<NodeContentUriIntentMapper>(),
        getFileTypeInfoUseCase = mock<GetFileTypeInfoUseCase>(),
        getFileTypeInfoByNameUseCase = mock<GetFileTypeInfoByNameUseCase>(),
        settingsNavigator = mock<SettingsNavigatorImpl>(),
        getDomainNameUseCase = mock<GetDomainNameUseCase>(),
        mediaPlayerIntentMapper = mock<MediaPlayerIntentMapper>(),
        nav3VideoPlayerRouteLauncher = mock<Nav3VideoPlayerRouteLauncher>(),
        nav3AudioPlayerRouteLauncher = mock<Nav3AudioPlayerRouteLauncher>(),
        getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        navigationQueue = navigationQueue,
        activityLifecycleHandler = activityLifecycleHandler,
        snackbarEventQueue = mock<SnackbarEventQueue>(),
        mainDispatcher = dispatcher,
    )

    private val activity: Activity =
        Robolectric.buildActivity(Activity::class.java).create().get()

    private fun context() = activity

    private fun nextStartedIntent() = shadowOf(activity).nextStartedActivity

    @Test
    fun `test that openContactInfoActivity by email launches the Compose host when ContactInfoComposeUI is enabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.ContactInfoComposeUI)).thenReturn(true)
            val navigator = createNavigator(UnconfinedTestDispatcher(testScheduler))

            navigator.openContactInfoActivity(context(), "contact@mega.co.nz")
            advanceUntilIdle()

            val intent = nextStartedIntent()
            assertThat(intent.component)
                .isEqualTo(ComponentName(context(), ContactInfoComposeActivity::class.java))
            assertThat(intent.getStringExtra(Constants.NAME)).isEqualTo("contact@mega.co.nz")
            verify(navigationQueue, never()).emit(any<NavKey>(), any(), any())
        }

    @Test
    fun `test that openContactInfoActivity by email launches the legacy ContactInfoActivity when ContactInfoComposeUI is disabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.ContactInfoComposeUI)).thenReturn(false)
            val navigator = createNavigator(UnconfinedTestDispatcher(testScheduler))

            navigator.openContactInfoActivity(context(), "contact@mega.co.nz")
            advanceUntilIdle()

            val intent = nextStartedIntent()
            assertThat(intent.component)
                .isEqualTo(ComponentName(context(), ContactInfoActivity::class.java))
            assertThat(intent.getStringExtra(Constants.NAME)).isEqualTo("contact@mega.co.nz")
            verify(navigationQueue, never()).emit(any<NavKey>(), any(), any())
        }

    @Test
    fun `test that openContactInfoActivity by chatId launches the Compose host when ContactInfoComposeUI is enabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.ContactInfoComposeUI)).thenReturn(true)
            val navigator = createNavigator(UnconfinedTestDispatcher(testScheduler))

            navigator.openContactInfoActivity(context(), 123L)
            advanceUntilIdle()

            val intent = nextStartedIntent()
            assertThat(intent.component)
                .isEqualTo(ComponentName(context(), ContactInfoComposeActivity::class.java))
            assertThat(intent.getLongExtra(Constants.HANDLE, MegaChatApiJava.MEGACHAT_INVALID_HANDLE))
                .isEqualTo(123L)
            verify(navigationQueue, never()).emit(any<NavKey>(), any(), any())
        }

    @Test
    fun `test that openContactInfoActivity by chatId launches the legacy ContactInfoActivity when ContactInfoComposeUI is disabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.ContactInfoComposeUI)).thenReturn(false)
            val navigator = createNavigator(UnconfinedTestDispatcher(testScheduler))

            navigator.openContactInfoActivity(context(), 123L)
            advanceUntilIdle()

            val intent = nextStartedIntent()
            assertThat(intent.component)
                .isEqualTo(ComponentName(context(), ContactInfoActivity::class.java))
            assertThat(intent.getLongExtra(Constants.HANDLE, MegaChatApiJava.MEGACHAT_INVALID_HANDLE))
                .isEqualTo(123L)
            verify(navigationQueue, never()).emit(any<NavKey>(), any(), any())
        }
}
