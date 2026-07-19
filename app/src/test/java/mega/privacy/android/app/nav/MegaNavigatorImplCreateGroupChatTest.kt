package mega.privacy.android.app.nav

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.app.mediaplayer.Nav3AudioPlayerRouteLauncher
import mega.privacy.android.app.presentation.contact.CreateGroupChatComposeActivity
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
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MegaNavigatorImplCreateGroupChatTest {

    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

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
        navigationQueue = mock<NavigationEventQueue>(),
        activityLifecycleHandler = mock<ActivityLifecycleHandler>(),
        snackbarEventQueue = mock<SnackbarEventQueue>(),
        mainDispatcher = dispatcher,
    )

    private fun buildActivity(): Activity =
        Robolectric.buildActivity(Activity::class.java).setup().get()

    @Test
    fun `test that openCreateGroupChatForResult with requestCode launches the Compose host when the ContactsComposeUI flag is enabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.ContactsComposeUI)).thenReturn(true)
            val activity = buildActivity()
            val navigator = createNavigator(UnconfinedTestDispatcher(testScheduler))

            navigator.openCreateGroupChatForResult(
                activity = activity,
                requestCode = REQUEST_CODE,
                allowEmptyGroup = false,
            )
            advanceUntilIdle()

            val started = shadowOf(activity).nextStartedActivityForResult
            assertThat(started.requestCode).isEqualTo(REQUEST_CODE)
            assertThat(started.intent.component)
                .isEqualTo(
                    ComponentName(
                        ApplicationProvider.getApplicationContext(),
                        CreateGroupChatComposeActivity::class.java,
                    )
                )
        }

    @Test
    fun `test that openCreateGroupChatForResult with requestCode launches the legacy AddContactActivity in only-create-group mode when the ContactsComposeUI flag is disabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.ContactsComposeUI)).thenReturn(false)
            val activity = buildActivity()
            val navigator = createNavigator(UnconfinedTestDispatcher(testScheduler))

            navigator.openCreateGroupChatForResult(
                activity = activity,
                requestCode = REQUEST_CODE,
                allowEmptyGroup = false,
            )
            advanceUntilIdle()

            val started = shadowOf(activity).nextStartedActivityForResult
            assertThat(started.requestCode).isEqualTo(REQUEST_CODE)
            assertThat(started.intent.component)
                .isEqualTo(
                    ComponentName(
                        ApplicationProvider.getApplicationContext(),
                        AddContactActivity::class.java,
                    )
                )
            assertThat(
                started.intent.getIntExtra(AddContactActivity.EXTRA_CONTACT_TYPE, -1)
            ).isEqualTo(Constants.CONTACT_TYPE_MEGA)
            assertThat(
                started.intent.getBooleanExtra(AddContactActivity.EXTRA_ONLY_CREATE_GROUP, false)
            ).isTrue()
            assertThat(
                started.intent.getBooleanExtra(
                    AddContactActivity.EXTRA_IS_START_CONVERSATION,
                    false,
                )
            ).isFalse()
        }

    @Test
    fun `test that openCreateGroupChatForResult with launcher launches the Compose host when the ContactsComposeUI flag is enabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.ContactsComposeUI)).thenReturn(true)
            val launcher = mock<ActivityResultLauncher<Intent>>()
            val navigator = createNavigator(UnconfinedTestDispatcher(testScheduler))

            navigator.openCreateGroupChatForResult(
                context = ApplicationProvider.getApplicationContext(),
                launcher = launcher,
                allowEmptyGroup = false,
            )
            advanceUntilIdle()

            val captor = argumentCaptor<Intent>()
            verify(launcher).launch(captor.capture())
            assertThat(captor.firstValue.component)
                .isEqualTo(
                    ComponentName(
                        ApplicationProvider.getApplicationContext(),
                        CreateGroupChatComposeActivity::class.java,
                    )
                )
        }

    @Test
    fun `test that openCreateGroupChatForResult with launcher launches the legacy AddContactActivity in only-create-group mode when the ContactsComposeUI flag is disabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.ContactsComposeUI)).thenReturn(false)
            val launcher = mock<ActivityResultLauncher<Intent>>()
            val navigator = createNavigator(UnconfinedTestDispatcher(testScheduler))

            navigator.openCreateGroupChatForResult(
                context = ApplicationProvider.getApplicationContext(),
                launcher = launcher,
                allowEmptyGroup = false,
            )
            advanceUntilIdle()

            val captor = argumentCaptor<Intent>()
            verify(launcher).launch(captor.capture())
            assertThat(captor.firstValue.component)
                .isEqualTo(
                    ComponentName(
                        ApplicationProvider.getApplicationContext(),
                        AddContactActivity::class.java,
                    )
                )
            assertThat(
                captor.firstValue.getIntExtra(AddContactActivity.EXTRA_CONTACT_TYPE, -1)
            ).isEqualTo(Constants.CONTACT_TYPE_MEGA)
            assertThat(
                captor.firstValue.getBooleanExtra(AddContactActivity.EXTRA_ONLY_CREATE_GROUP, false)
            ).isTrue()
        }

    @Test
    fun `test that openCreateGroupChatForResult with allowEmptyGroup sets EXTRA_IS_START_CONVERSATION on the legacy intent when the ContactsComposeUI flag is disabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.ContactsComposeUI)).thenReturn(false)
            val launcher = mock<ActivityResultLauncher<Intent>>()
            val navigator = createNavigator(UnconfinedTestDispatcher(testScheduler))

            navigator.openCreateGroupChatForResult(
                context = ApplicationProvider.getApplicationContext(),
                launcher = launcher,
                allowEmptyGroup = true,
            )
            advanceUntilIdle()

            val captor = argumentCaptor<Intent>()
            verify(launcher).launch(captor.capture())
            assertThat(captor.firstValue.component)
                .isEqualTo(
                    ComponentName(
                        ApplicationProvider.getApplicationContext(),
                        AddContactActivity::class.java,
                    )
                )
            assertThat(
                captor.firstValue.getBooleanExtra(AddContactActivity.EXTRA_ONLY_CREATE_GROUP, false)
            ).isTrue()
            assertThat(
                captor.firstValue.getBooleanExtra(
                    AddContactActivity.EXTRA_IS_START_CONVERSATION,
                    false,
                )
            ).isTrue()
        }

    @Test
    fun `test that openCreateGroupChatForResult with allowEmptyGroup launches the Compose host when the ContactsComposeUI flag is enabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.ContactsComposeUI)).thenReturn(true)
            val launcher = mock<ActivityResultLauncher<Intent>>()
            val navigator = createNavigator(UnconfinedTestDispatcher(testScheduler))

            navigator.openCreateGroupChatForResult(
                context = ApplicationProvider.getApplicationContext(),
                launcher = launcher,
                allowEmptyGroup = true,
            )
            advanceUntilIdle()

            val captor = argumentCaptor<Intent>()
            verify(launcher).launch(captor.capture())
            assertThat(captor.firstValue.component)
                .isEqualTo(
                    ComponentName(
                        ApplicationProvider.getApplicationContext(),
                        CreateGroupChatComposeActivity::class.java,
                    )
                )
        }

    private companion object {
        private const val REQUEST_CODE = 1001
    }
}
