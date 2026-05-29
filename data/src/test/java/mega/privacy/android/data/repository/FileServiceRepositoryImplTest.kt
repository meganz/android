package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.fileservice.FileServiceReclaimOptionsMapper
import mega.privacy.android.data.mapper.fileservice.MegaFileServiceReclaimOptionsMapper
import mega.privacy.android.domain.entity.fileservice.FileServiceReclaimOptions
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaFileServiceReclaimOptions
import nz.mega.sdk.MegaRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
class FileServiceRepositoryImplTest {

    private lateinit var underTest: FileServiceRepositoryImpl

    private val megaApiGateway = mock<MegaApiGateway>()
    private val megaApiFolderGateway = mock<MegaApiFolderGateway>()
    private val fileServiceReclaimOptionsMapper = mock<FileServiceReclaimOptionsMapper>()
    private val megaFileServiceReclaimOptionsMapper = mock<MegaFileServiceReclaimOptionsMapper>()
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        reset(
            megaApiGateway,
            megaApiFolderGateway,
            fileServiceReclaimOptionsMapper,
            megaFileServiceReclaimOptionsMapper,
        )
        underTest = FileServiceRepositoryImpl(
            megaApiGateway = megaApiGateway,
            megaApiFolderGateway = megaApiFolderGateway,
            fileServiceReclaimOptionsMapper = fileServiceReclaimOptionsMapper,
            megaFileServiceReclaimOptionsMapper = megaFileServiceReclaimOptionsMapper,
            ioDispatcher = testDispatcher,
        )
    }

    @Test
    fun `test that getReclaimOptions returns mapped options when gateway returns non-null`() =
        runTest(testDispatcher) {
            val megaOptions = mock<MegaFileServiceReclaimOptions>()
            val expected = mock<FileServiceReclaimOptions>()
            whenever(megaApiGateway.fileServiceGetReclaimOptions()).thenReturn(megaOptions)
            whenever(fileServiceReclaimOptionsMapper(megaOptions)).thenReturn(expected)

            val result = underTest.getReclaimOptions()

            assertThat(result).isEqualTo(expected)
            verify(megaApiGateway).fileServiceGetReclaimOptions()
            verify(fileServiceReclaimOptionsMapper).invoke(megaOptions)
        }

    @Test
    fun `test that getReclaimOptions returns null when gateway returns null`() =
        runTest(testDispatcher) {
            whenever(megaApiGateway.fileServiceGetReclaimOptions()).thenReturn(null)

            val result = underTest.getReclaimOptions()

            assertThat(result).isNull()
            verify(megaApiGateway).fileServiceGetReclaimOptions()
        }

    @Test
    fun `test that setReclaimOptions calls main gateway with mapped options`() =
        runTest(testDispatcher) {
            val domainOptions = mock<FileServiceReclaimOptions>()
            val megaOptions = mock<MegaFileServiceReclaimOptions>()
            whenever(megaFileServiceReclaimOptionsMapper(domainOptions)).thenReturn(megaOptions)

            underTest.setReclaimOptions(domainOptions)

            verify(megaFileServiceReclaimOptionsMapper).invoke(domainOptions)
            verify(megaApiGateway).fileServiceSetReclaimOptions(megaOptions)
        }

    @Test
    fun `test that setPublicLinkReclaimOptions calls folder gateway with mapped options`() =
        runTest(testDispatcher) {
            val domainOptions = mock<FileServiceReclaimOptions>()
            val megaOptions = mock<MegaFileServiceReclaimOptions>()
            whenever(megaFileServiceReclaimOptionsMapper(domainOptions)).thenReturn(megaOptions)

            underTest.setPublicLinkReclaimOptions(domainOptions)

            verify(megaFileServiceReclaimOptionsMapper).invoke(domainOptions)
            verify(megaApiFolderGateway).fileServiceSetReclaimOptions(megaOptions)
        }

    @Test
    fun `test that reclaim returns total bytes on success`() =
        runTest(testDispatcher) {
            val expectedBytes = 1024L
            val request = mock<MegaRequest> {
                on { totalBytes }.thenReturn(expectedBytes)
            }
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }
            whenever(megaApiGateway.fileServiceReclaim(anyOrNull(), any())).thenAnswer {
                (it.arguments[1] as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    request,
                    error,
                )
            }

            val result = underTest.reclaim()

            assertThat(result).isEqualTo(expectedBytes)
            verify(megaApiGateway).fileServiceReclaim(anyOrNull(), any())
        }

    @Test
    fun `test that reclaim passes mapped options to gateway when options provided`() =
        runTest(testDispatcher) {
            val domainOptions = mock<FileServiceReclaimOptions>()
            val megaOptions = mock<MegaFileServiceReclaimOptions>()
            val request = mock<MegaRequest> {
                on { totalBytes }.thenReturn(0L)
            }
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }
            whenever(megaFileServiceReclaimOptionsMapper(domainOptions)).thenReturn(megaOptions)
            whenever(megaApiGateway.fileServiceReclaim(eq(megaOptions), any())).thenAnswer {
                (it.arguments[1] as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    request,
                    error,
                )
            }

            underTest.reclaim(domainOptions)

            verify(megaFileServiceReclaimOptionsMapper).invoke(domainOptions)
            verify(megaApiGateway).fileServiceReclaim(eq(megaOptions), any())
        }

    @Test
    fun `test that reclaim passes null options to gateway when options not provided`() =
        runTest(testDispatcher) {
            val request = mock<MegaRequest> {
                on { totalBytes }.thenReturn(0L)
            }
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }
            whenever(megaApiGateway.fileServiceReclaim(eq(null), any())).thenAnswer {
                (it.arguments[1] as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    request,
                    error,
                )
            }

            underTest.reclaim()

            verify(megaApiGateway).fileServiceReclaim(eq(null), any())
        }

}
