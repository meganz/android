package mega.privacy.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.cryptography.DecryptData2
import mega.privacy.android.data.cryptography.EncryptData2
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyBlocking

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CameraUploadsSettingsPreferenceDataStoreTest {
    private lateinit var underTest: CameraUploadsSettingsPreferenceDataStore

    private val encryptData = mock<EncryptData2>()
    private val decryptData = mock<DecryptData2>()
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

    @Test
    internal fun `test that camera uploads enabled is encrypted when set`() = runTest {
        val input = true

        underTest.setCameraUploadsEnabled(input)

        verifyBlocking(encryptData) { invoke(input.toString()) }
    }

    @Test
    internal fun `test that camera uploads enabled  is decrypted when retrieved`() = runTest {
        val expected = true
        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(true.toString()) }

        Truth.assertThat(underTest.isCameraUploadsEnabled()).isEqualTo(expected)
    }

    @Test
    internal fun `test that media uploads enabled is encrypted when set`() = runTest {
        val input = true

        underTest.setMediaUploadsEnabled(input)

        verifyBlocking(encryptData) { invoke(input.toString()) }
    }

    @Test
    internal fun `test that media uploads enabled  is decrypted when retrieved`() = runTest {
        val expected = true
        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(true.toString()) }

        Truth.assertThat(underTest.isMediaUploadsEnabled()).isEqualTo(expected)
    }

    @Test
    internal fun `test that camera uploads handle is encrypted when set`() = runTest {
        val input = 1L

        underTest.setCameraUploadsHandle(input)

        verifyBlocking(encryptData) { invoke(input.toString()) }
    }

    @Test
    internal fun `test that camera uploads handle is decrypted when retrieved`() = runTest {
        val expected = 1L

        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected.toString()) }

        Truth.assertThat(underTest.getCameraUploadsHandle()).isEqualTo(expected)
    }

    @Test
    internal fun `test that media uploads handle is encrypted when set`() = runTest {
        val input = 2L

        underTest.setMediaUploadsHandle(input)

        verifyBlocking(encryptData) { invoke(input.toString()) }
    }

    @Test
    internal fun `test that media uploads handle is decrypted when retrieved`() = runTest {
        val expected = 2L

        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected.toString()) }

        Truth.assertThat(underTest.getMediaUploadsHandle()).isEqualTo(expected)
    }

    @Test
    internal fun `test that camera uploads local path is encrypted when set`() = runTest {
        val input = "/path/to/CU"

        underTest.setCameraUploadsLocalPath(input)

        verifyBlocking(encryptData) { invoke(input) }
    }

    @Test
    internal fun `test that camera uploads local path is decrypted when retrieved`() = runTest {
        val expected = "/path/to/CU"

        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected) }

        Truth.assertThat(underTest.getCameraUploadsLocalPath()).isEqualTo(expected)
    }

    @Test
    internal fun `test that media uploads local path is encrypted when set`() = runTest {
        val input = "/path/to/MU"

        underTest.setMediaUploadsLocalPath(input)

        verifyBlocking(encryptData) { invoke(input) }
    }

    @Test
    internal fun `test that media uploads local path is decrypted when retrieved`() = runTest {
        val expected = "/path/to/MU"

        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected) }

        Truth.assertThat(underTest.getMediaUploadsLocalPath()).isEqualTo(expected)
    }

    @Test
    internal fun `test that location tags enabled  is encrypted when set`() = runTest {
        val input = true

        underTest.setLocationTagsEnabled(input)

        verifyBlocking(encryptData) { invoke(input.toString()) }
    }

    @Test
    internal fun `test that location tags enabled is decrypted when retrieved`() = runTest {
        val expected = true

        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected.toString()) }

        Truth.assertThat(underTest.areLocationTagsEnabled()).isEqualTo(expected)
    }

    @Test
    internal fun `test that video quality  is encrypted when set`() = runTest {
        val input = 1

        underTest.setUploadVideoQuality(input)

        verifyBlocking(encryptData) { invoke(input.toString()) }
    }

    @Test
    internal fun `test that video quality is decrypted when retrieved`() = runTest {
        val expected = 1

        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected.toString()) }

        Truth.assertThat(underTest.getUploadVideoQuality()).isEqualTo(expected)
    }

    @Test
    internal fun `test that upload file names kept is encrypted when set`() = runTest {
        val input = true

        underTest.setUploadFileNamesKept(input)

        verifyBlocking(encryptData) { invoke(input.toString()) }
    }

    @Test
    internal fun `test that upload file names kept is decrypted when retrieved`() = runTest {
        val expected = true

        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected.toString()) }

        Truth.assertThat(underTest.areUploadFileNamesKept()).isEqualTo(expected)
    }

    @Test
    internal fun `test that charging required for video compression is encrypted when set`() =
        runTest {
            val input = true

            underTest.setChargingRequiredForVideoCompression(input)

            verifyBlocking(encryptData) { invoke(input.toString()) }
        }

    @Test
    internal fun `test that charging required for video compression is decrypted when retrieved`() =
        runTest {
            val expected = true

            decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected.toString()) }

            Truth.assertThat(underTest.isChargingRequiredForVideoCompression()).isEqualTo(expected)
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

            Truth.assertThat(underTest.getVideoCompressionSizeLimit()).isEqualTo(expected)
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

            Truth.assertThat(underTest.getFileUploadOption()).isEqualTo(expected)
        }

    @Test
    internal fun `test that photo time stamp is encrypted when set`() =
        runTest {
            val input = 12L

            underTest.setPhotoTimeStamp(input)

            verifyBlocking(encryptData) { invoke(input.toString()) }
        }

    @Test
    internal fun `test that photo time stamp is decrypted when retrieved`() =
        runTest {
            val expected = 12L

            decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected.toString()) }

            Truth.assertThat(underTest.getPhotoTimeStamp()).isEqualTo(expected)
        }

    @Test
    internal fun `test that video time stamp is encrypted when set`() =
        runTest {
            val input = 12L

            underTest.setVideoTimeStamp(input)

            verifyBlocking(encryptData) { invoke(input.toString()) }
        }

    @Test
    internal fun `test that video time stamp is decrypted when retrieved`() =
        runTest {
            val expected = 12L

            decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected.toString()) }

            Truth.assertThat(underTest.getVideoTimeStamp()).isEqualTo(expected)
        }

    @Test
    internal fun `test that media uploads photo time stamp is encrypted when set`() =
        runTest {
            val input = 12L

            underTest.setMediaUploadsPhotoTimeStamp(input)

            verifyBlocking(encryptData) { invoke(input.toString()) }
        }

    @Test
    internal fun `test that media uploads photo time stamp is decrypted when retrieved`() =
        runTest {
            val expected = 12L

            decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected.toString()) }

            Truth.assertThat(underTest.getMediaUploadsPhotoTimeStamp()).isEqualTo(expected)
        }

    @Test
    internal fun `test that media uploads video time stamp is encrypted when set`() =
        runTest {
            val input = 12L

            underTest.setMediaUploadsVideoTimeStamp(input)

            verifyBlocking(encryptData) { invoke(input.toString()) }
        }

    @Test
    internal fun `test that media uploads video time stamp is decrypted when retrieved`() =
        runTest {
            val expected = 12L

            decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected.toString()) }

            Truth.assertThat(underTest.getMediaUploadsVideoTimeStamp()).isEqualTo(expected)
        }

    @Test
    internal fun `test that upload by wifi is encrypted when set`() =
        runTest {
            val input = true

            underTest.setUploadsByWifi(input)

            verifyBlocking(encryptData) { invoke(input.toString()) }
        }

    @Test
    internal fun `test that upload by wifi is decrypted when retrieved`() =
        runTest {
            val expected = true

            decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected.toString()) }

            Truth.assertThat(underTest.isUploadByWifi()).isEqualTo(expected)
        }
}
