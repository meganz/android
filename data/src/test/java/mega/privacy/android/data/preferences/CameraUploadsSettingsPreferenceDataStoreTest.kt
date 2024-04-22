package mega.privacy.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.cryptography.EncryptData
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyBlocking

/**
 * Test class for [CameraUploadsSettingsPreferenceDataStore]
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CameraUploadsSettingsPreferenceDataStoreTest {
    private lateinit var underTest: CameraUploadsSettingsPreferenceDataStore

    private val encryptData = mock<EncryptData>()
    private val decryptData = mock<DecryptData>()
    private val preferences = mock<Preferences> {
        on { get<String>(any()) }.thenReturn("encrypted data")
    }

    private val dataStore = mock<DataStore<Preferences>> {
        on { data }.thenReturn(flow {
            emit(preferences)
            awaitCancellation()
        })
    }

    @BeforeAll
    internal fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = CameraUploadsSettingsPreferenceDataStore(
            dataStore,
            encryptData = encryptData,
            decryptData = decryptData,
        )
    }

    @BeforeEach
    internal fun resetMocks() {
        reset(encryptData, decryptData)
    }

    @AfterAll
    internal fun tearDown() {
        Dispatchers.resetMain()
    }

    @ParameterizedTest(name = "new camera uploads enabled state: {0}")
    @ValueSource(booleans = [true, false])
    internal fun `test that the new camera uploads enabled state is encrypted when set`(
        isCameraUploadsEnabled: Boolean,
    ) = runTest {
        underTest.setCameraUploadsEnabled(isCameraUploadsEnabled)

        verifyBlocking(encryptData) { invoke(isCameraUploadsEnabled.toString()) }
    }

    @ParameterizedTest(name = "is camera uploads enabled: {0}")
    @ValueSource(booleans = [true, false])
    internal fun `test that the camera uploads enabled state is decrypted when retrieved`(
        isCameraUploadsEnabled: Boolean,
    ) = runTest {
        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(isCameraUploadsEnabled.toString()) }

        assertThat(underTest.isCameraUploadsEnabled()).isEqualTo(isCameraUploadsEnabled)
    }

    @ParameterizedTest(name = "new media uploads enabled state: {0}")
    @ValueSource(booleans = [true, false])
    internal fun `test that the new media uploads enabled state is encrypted when set`(
        isMediaUploadsEnabled: Boolean,
    ) = runTest {
        underTest.setMediaUploadsEnabled(isMediaUploadsEnabled)

        verifyBlocking(encryptData) { invoke(isMediaUploadsEnabled.toString()) }
    }

    @ParameterizedTest(name = "is media uploads enabled: {0}")
    @ValueSource(booleans = [true, false])
    internal fun `test that the media uploads enabled state is decrypted when retrieved`(
        isMediaUploadsEnabled: Boolean,
    ) = runTest {
        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(isMediaUploadsEnabled.toString()) }

        assertThat(underTest.isMediaUploadsEnabled()).isEqualTo(isMediaUploadsEnabled)
    }

    @Test
    internal fun `test that the new camera uploads handle is encrypted when set`() = runTest {
        val input = 1L

        underTest.setCameraUploadsHandle(input)

        verifyBlocking(encryptData) { invoke(input.toString()) }
    }

    @Test
    internal fun `test that the camera uploads handle is decrypted when retrieved`() = runTest {
        val expected = 1L

        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected.toString()) }

        assertThat(underTest.getCameraUploadsHandle()).isEqualTo(expected)
    }

    @Test
    internal fun `test that the new media uploads handle is encrypted when set`() = runTest {
        val input = 2L

        underTest.setMediaUploadsHandle(input)

        verifyBlocking(encryptData) { invoke(input.toString()) }
    }

    @Test
    internal fun `test that the media uploads handle is decrypted when retrieved`() = runTest {
        val expected = 2L

        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected.toString()) }

        assertThat(underTest.getMediaUploadsHandle()).isEqualTo(expected)
    }

    @Test
    internal fun `test that the new camera uploads local path is encrypted when set`() = runTest {
        val input = "/path/to/CU"

        underTest.setCameraUploadsLocalPath(input)

        verifyBlocking(encryptData) { invoke(input) }
    }

    @Test
    internal fun `test that the camera uploads local path is decrypted when retrieved`() = runTest {
        val expected = "/path/to/CU"

        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected) }

        assertThat(underTest.getCameraUploadsLocalPath()).isEqualTo(expected)
    }

    @Test
    internal fun `test that the new media uploads local path is encrypted when set`() = runTest {
        val input = "/path/to/MU"

        underTest.setMediaUploadsLocalPath(input)

        verifyBlocking(encryptData) { invoke(input) }
    }

    @Test
    internal fun `test that the media uploads local path is decrypted when retrieved`() = runTest {
        val expected = "/path/to/MU"

        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected) }

        assertThat(underTest.getMediaUploadsLocalPath()).isEqualTo(expected)
    }

    @ParameterizedTest(name = "new are location tags enabled state: {0}")
    @ValueSource(booleans = [true, false])
    internal fun `test that the new location tags enabled state is encrypted when set`(
        areLocationTagsEnabled: Boolean,
    ) = runTest {
        underTest.setLocationTagsEnabled(areLocationTagsEnabled)

        verifyBlocking(encryptData) { invoke(areLocationTagsEnabled.toString()) }
    }

    @ParameterizedTest(name = "are location tags enabled: {0}")
    @ValueSource(booleans = [true, false])
    internal fun `test that the location tags enabled state is decrypted when retrieved`(
        areLocationTagsEnabled: Boolean,
    ) = runTest {
        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(areLocationTagsEnabled.toString()) }

        assertThat(underTest.areLocationTagsEnabled()).isEqualTo(areLocationTagsEnabled)
    }

    @Test
    internal fun `test that the new video quality is encrypted when set`() = runTest {
        val input = 1

        underTest.setUploadVideoQuality(input)

        verifyBlocking(encryptData) { invoke(input.toString()) }
    }

    @Test
    internal fun `test that the video quality is decrypted when retrieved`() = runTest {
        val expected = 1

        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected.toString()) }

        assertThat(underTest.getUploadVideoQuality()).isEqualTo(expected)
    }

    @ParameterizedTest(name = "new are upload file names kept state: {0}")
    @ValueSource(booleans = [true, false])
    internal fun `test that the new upload file names kept state is encrypted when set`(
        areUploadFileNamesKept: Boolean,
    ) = runTest {
        underTest.setUploadFileNamesKept(areUploadFileNamesKept)

        verifyBlocking(encryptData) { invoke(areUploadFileNamesKept.toString()) }
    }

    @ParameterizedTest(name = "are upload file names kept: {0}")
    @ValueSource(booleans = [true, false])
    internal fun `test that the upload file names kept state is decrypted when retrieved`(
        areUploadFileNamesKept: Boolean,
    ) = runTest {
        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(areUploadFileNamesKept.toString()) }

        assertThat(underTest.areUploadFileNamesKept()).isEqualTo(areUploadFileNamesKept)
    }

    @ParameterizedTest(name = "new is charging required for video compression state: {0}")
    @ValueSource(booleans = [true, false])
    internal fun `test that the new charging required for video compression state is encrypted when set`(
        chargingRequired: Boolean,
    ) = runTest {
        underTest.setChargingRequiredForVideoCompression(chargingRequired)

        verifyBlocking(encryptData) { invoke(chargingRequired.toString()) }
    }

    @ParameterizedTest(name = "is charging required for video compression: {0}")
    @ValueSource(booleans = [true, false])
    internal fun `test that the charging required for video compression state is decrypted when retrieved`(
        chargingRequired: Boolean,
    ) = runTest {
        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(chargingRequired.toString()) }

        assertThat(underTest.isChargingRequiredForVideoCompression()).isEqualTo(chargingRequired)
    }

    @Test
    internal fun `test that video compression size limit is encrypted when set`() =
        runTest {
            val input = 100

            underTest.setVideoCompressionSizeLimit(input)

            verifyBlocking(encryptData) { invoke(input.toString()) }
        }

    @Test
    internal fun `test that video compression size is decrypted when retrieved`() =
        runTest {
            val expected = 100

            decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected.toString()) }

            assertThat(underTest.getVideoCompressionSizeLimit()).isEqualTo(expected)
        }

    @Test
    internal fun `test that file upload option is encrypted when set`() =
        runTest {
            val input = 1

            underTest.setFileUploadOption(input)

            verifyBlocking(encryptData) { invoke(input.toString()) }
        }

    @Test
    internal fun `test that file upload option is decrypted when retrieved`() =
        runTest {
            val expected = 1

            decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected.toString()) }

            assertThat(underTest.getFileUploadOption()).isEqualTo(expected)
        }

    @ParameterizedTest(name = "new should upload by wifi state: {0}")
    @ValueSource(booleans = [true, false])
    internal fun `test that the new upload by wifi state is encrypted when set`(shouldUploadByWifi: Boolean) =
        runTest {
            underTest.setUploadsByWifi(shouldUploadByWifi)

            verifyBlocking(encryptData) { invoke(shouldUploadByWifi.toString()) }
        }

    @ParameterizedTest(name = "should upload by wifi: {0}")
    @ValueSource(booleans = [true, false])
    internal fun `test that the upload by wifi state is decrypted when retrieved`(shouldUploadByWifi: Boolean) =
        runTest {
            decryptData.stub { onBlocking { invoke(any()) }.thenReturn(shouldUploadByWifi.toString()) }

            assertThat(underTest.isUploadByWifi()).isEqualTo(shouldUploadByWifi)
        }

    @Test
    internal fun `test that the charging required to upload content state is monitored and decrypted`() =
        runTest {
            val chargingRequired = true
            decryptData.stub { onBlocking { invoke(any()) }.thenReturn(chargingRequired.toString()) }

            underTest.monitorIsChargingRequiredToUploadContent().test {
                assertThat(awaitItem()).isEqualTo(chargingRequired)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    internal fun `test that the charging required to upload content state is monitored and decrypted as null when it is not a boolean`() =
        runTest {
            decryptData.stub { onBlocking { invoke(any()) }.thenReturn("12345") }

            underTest.monitorIsChargingRequiredToUploadContent().test {
                assertThat(awaitItem()).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @ParameterizedTest(name = "new is charging required to upload content state: {0}")
    @ValueSource(booleans = [true, false])
    internal fun `test that the new charging required to upload content state is encrypted when set`(
        chargingRequired: Boolean,
    ) = runTest {
        underTest.setChargingRequiredToUploadContent(chargingRequired)

        verifyBlocking(encryptData) { invoke(chargingRequired.toString()) }
    }

    @ParameterizedTest(name = "is charging required to upload content: {0}")
    @ValueSource(booleans = [true, false])
    internal fun `test that the charging required to upload content state is decrypted when retrieved`(
        chargingRequired: Boolean,
    ) = runTest {
        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(chargingRequired.toString()) }

        assertThat(underTest.isChargingRequiredToUploadContent()).isEqualTo(chargingRequired)
    }
}
