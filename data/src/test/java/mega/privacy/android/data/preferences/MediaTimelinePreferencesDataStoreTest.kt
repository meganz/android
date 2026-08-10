package mega.privacy.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.DeviceGateway
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MediaTimelinePreferencesDataStoreTest {

    private lateinit var underTest: MediaTimelinePreferencesDataStore

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val mediaTimelinePreferenceDataStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(UnconfinedTestDispatcher()),
            produceFile = { context.preferencesDataStoreFile(mediaTimelinePreferenceFileName) }
        )
    private val deviceGateway: DeviceGateway = mock()

    @Before
    fun setup() {
        underTest = MediaTimelinePreferencesDataStore(
            mediaTimelinePreferenceDataStore = mediaTimelinePreferenceDataStore,
            deviceGateway = deviceGateway
        )
    }

    @After
    fun tearDown() {
        reset(deviceGateway)
    }

    @Test
    fun `test that the camera upload is not shown by default`() = runTest {
        underTest.cameraUploadShownFlow.test {
            assertThat(expectMostRecentItem()).isFalse()
        }
    }

    @Test
    fun `test that the camera upload is successfully shown`() = runTest {
        underTest.setCameraUploadShown()

        underTest.cameraUploadShownFlow.test {
            assertThat(expectMostRecentItem()).isTrue()
        }
    }

    @Test
    fun `test that the enable CU banner is not dismissed by default`() = runTest {
        underTest.enableCameraUploadBannerDismissedTimestamp.test {
            assertThat(expectMostRecentItem()).isNull()
        }
    }

    @Test
    fun `test that the enable CU banner is successfully dismissed`() = runTest {
        val timeInMillis = 123L
        whenever(deviceGateway.now) doReturn timeInMillis

        underTest.setEnableCameraUploadBannerDismissedTimestamp()

        underTest.enableCameraUploadBannerDismissedTimestamp.test {
            assertThat(expectMostRecentItem()).isEqualTo(timeInMillis)
        }
    }

    @Test
    fun `test that the enable CU banner dismiss timestamp is successfully reset`() = runTest {
        val timeInMillis = 123L
        whenever(deviceGateway.now) doReturn timeInMillis

        underTest.setEnableCameraUploadBannerDismissedTimestamp()
        underTest.resetEnableCameraUploadBannerDismissedTimestamp()

        underTest.enableCameraUploadBannerDismissedTimestamp.test {
            assertThat(expectMostRecentItem()).isNull()
        }
    }
}
