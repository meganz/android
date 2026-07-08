package mega.privacy.android.app.nav

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
import mega.privacy.android.app.presentation.contact.AddContactsComposeActivity
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MegaNavigatorImplAddContactsForResultTest {

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
        getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        navigationQueue = mock<NavigationEventQueue>(),
        activityLifecycleHandler = mock<ActivityLifecycleHandler>(),
        snackbarEventQueue = mock<SnackbarEventQueue>(),
        mainDispatcher = dispatcher,
    )

    @Test
    fun `test that openAddContactsForResult launches the Compose host when the ContactsComposeUI flag is enabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.ContactsComposeUI)).thenReturn(true)
            val launcher = mock<ActivityResultLauncher<Intent>>()
            val navigator = createNavigator(UnconfinedTestDispatcher(testScheduler))

            navigator.openAddContactsForResult(
                context = ApplicationProvider.getApplicationContext(),
                launcher = launcher,
                preselectedHandles = listOf(1L, 2L),
                preselectedEmails = listOf("a@mega.co.nz", "b@mega.co.nz"),
            )
            advanceUntilIdle()

            val captor = argumentCaptor<Intent>()
            verify(launcher).launch(captor.capture())
            val intent = captor.firstValue
            assertThat(intent.component)
                .isEqualTo(
                    ComponentName(
                        ApplicationProvider.getApplicationContext(),
                        AddContactsComposeActivity::class.java,
                    )
                )
        }

    @Test
    fun `test that openAddContactsForResult launches the legacy AddContactActivity with the preselected emails when the flag is disabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.ContactsComposeUI)).thenReturn(false)
            val launcher = mock<ActivityResultLauncher<Intent>>()
            val navigator = createNavigator(UnconfinedTestDispatcher(testScheduler))

            navigator.openAddContactsForResult(
                context = ApplicationProvider.getApplicationContext(),
                launcher = launcher,
                preselectedHandles = listOf(1L, 2L),
                preselectedEmails = listOf("a@mega.co.nz", "b@mega.co.nz"),
            )
            advanceUntilIdle()

            val captor = argumentCaptor<Intent>()
            verify(launcher).launch(captor.capture())
            val intent = captor.firstValue
            assertThat(intent.component)
                .isEqualTo(
                    ComponentName(
                        ApplicationProvider.getApplicationContext(),
                        AddContactActivity::class.java,
                    )
                )
            assertThat(intent.getIntExtra(Constants.INTENT_EXTRA_KEY_CONTACT_TYPE, -1))
                .isEqualTo(Constants.CONTACT_TYPE_MEGA)
            assertThat(intent.getBooleanExtra(Constants.INTENT_EXTRA_KEY_CHAT, false)).isTrue()
            assertThat(
                intent.getStringArrayListExtra(Constants.INTENT_EXTRA_KEY_CONTACTS_SELECTED)
            ).containsExactly("a@mega.co.nz", "b@mega.co.nz").inOrder()
        }
}
