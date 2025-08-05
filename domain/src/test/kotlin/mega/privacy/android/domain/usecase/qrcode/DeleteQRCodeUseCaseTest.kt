package mega.privacy.android.domain.usecase.qrcode

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.account.qr.GetQRCodeFileUseCase
import mega.privacy.android.domain.usecase.contact.DeleteContactLinkUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

/**
 * Test class for [DeleteQRCodeUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
internal class DeleteQRCodeUseCaseTest {

    private lateinit var underTest: DeleteQRCodeUseCase

    private val deleteContactLinkUseCase = mock<DeleteContactLinkUseCase>()
    private val nodeRepository = mock<NodeRepository>()
    private val getQRCodeFileUseCase = mock<GetQRCodeFileUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = DeleteQRCodeUseCase(
            deleteContactLinkUseCase = deleteContactLinkUseCase,
            nodeRepository = nodeRepository,
            ioDispatcher = UnconfinedTestDispatcher(),
            getQRCodeFileUseCase = getQRCodeFileUseCase

        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(deleteContactLinkUseCase, nodeRepository, getQRCodeFileUseCase)
    }

    @ParameterizedTest
    @MethodSource("domainProvider")
    fun `test that the QR code is successfully deleted`(domain: String) = runTest {
        val base64handle = "MTAwMDAwMA=="
        val contactLink = "https://$domain/C!$base64handle"
        val handle = 1000000L
        val qrCodeFile: File = mock()

        whenever(nodeRepository.convertBase64ToHandle(base64handle)).thenReturn(handle)
        whenever(getQRCodeFileUseCase.invoke()).thenReturn(qrCodeFile)

        underTest(contactLink)

        verify(deleteContactLinkUseCase).invoke(handle)
    }

    @ParameterizedTest
    @MethodSource("domainProvider")
    fun `test that an exception is thrown if the account link fails to delete`(domain: String) =
        runTest {
            val base64handle = "MTAwMDAwMA=="
            val contactLink = "https://$domain/C!$base64handle"
            val handle = 1000000L

            whenever(nodeRepository.convertBase64ToHandle(base64handle)).thenReturn(handle)
            whenever(deleteContactLinkUseCase(handle)).thenAnswer {
                throw MegaException(
                    errorCode = -1,
                    errorString = "error"
                )
            }

            assertThrows<MegaException> { underTest(contactLink) }
        }

    @Test
    fun `test that an exception is thrown if the account link is invalid`() = runTest {
        val base64handle = "MTAwMDAwMA=="
        val contactLink = "https://mega.nz/C!$base64handle"

        whenever(nodeRepository.convertBase64ToHandle(base64handle)).thenAnswer {
            throw MegaException(
                -1,
                "convert base64 failed"
            )
        }

        assertThrows<MegaException> { underTest(contactLink) }
    }

    private fun domainProvider() = listOf("mega.nz", "mega.app")
}