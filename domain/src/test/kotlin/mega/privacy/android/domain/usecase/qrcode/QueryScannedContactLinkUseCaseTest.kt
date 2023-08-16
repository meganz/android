package mega.privacy.android.domain.usecase.qrcode

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.qrcode.QRCodeQueryResults
import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.repository.QRCodeRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class QueryScannedContactLinkUseCaseTest {
    private lateinit var underTest: QueryScannedContactLinkUseCase
    private val qrCodeRepository = mock<QRCodeRepository>()
    private val avatarRepository = mock<AvatarRepository>()

    @Before
    fun setUp() {
        underTest = QueryScannedContactLinkUseCase(qrCodeRepository, avatarRepository)
    }

    @Test
    fun `test that database fields are updated when qrCodeQueryResult is OK`() = runTest {
        val handle = "1245"
        val result = ScannedContactLinkResult(
            contactName = "Abc",
            email = "abc@gmail.com",
            handle = 12345,
            isContact = false,
            qrCodeQueryResult = QRCodeQueryResults.CONTACT_QUERY_OK
        )
        whenever(qrCodeRepository.queryScannedContactLink(handle)).thenReturn(result)
        underTest(handle)

        verify(qrCodeRepository).updateDatabaseOnQueryScannedContactSuccess(result.handle)
    }

    @Test
    fun `test that user avatar file and color is fetched when contact is existing contact and qrCodeQueryResult is OK`() =
        runTest {
            val handle = "1245"
            val nodeHandle: Long = 12345
            val result = ScannedContactLinkResult(
                contactName = "Abc",
                email = "abc@gmail.com",
                handle = nodeHandle,
                isContact = true,
                qrCodeQueryResult = QRCodeQueryResults.CONTACT_QUERY_OK
            )
            whenever(qrCodeRepository.queryScannedContactLink(handle)).thenReturn(result)
            whenever(avatarRepository.getAvatarColor(nodeHandle)).thenReturn(1234)
            underTest(handle)

            verify(avatarRepository).getAvatarFile(result.email)
            verify(avatarRepository).getAvatarColor(result.handle)
        }

    @Test
    fun `test that user avatar file and color is not fetched when contact is not existing contact and qrCodeQueryResult is OK`() =
        runTest {
            val handle = "1245"
            val result = ScannedContactLinkResult(
                contactName = "Abc",
                email = "abc@gmail.com",
                handle = 12345,
                isContact = false,
                qrCodeQueryResult = QRCodeQueryResults.CONTACT_QUERY_OK
            )
            whenever(qrCodeRepository.queryScannedContactLink(handle)).thenReturn(result)
            underTest(handle)

            verify(avatarRepository, never()).getAvatarFile(result.email)
            verify(avatarRepository, never()).getAvatarColor(result.handle)
        }

    @Test
    fun `test that database fields are not updated and contact avatar is not fetched when qrCodeQueryResult is EExist`() =
        runTest {
            val handle = "1245"
            val result = ScannedContactLinkResult(
                contactName = "Abc",
                email = "abc@gmail.com",
                handle = 12345,
                isContact = true,
                qrCodeQueryResult = QRCodeQueryResults.CONTACT_QUERY_EEXIST
            )
            whenever(qrCodeRepository.queryScannedContactLink(handle)).thenReturn(result)
            underTest(handle)

            verify(qrCodeRepository, never())
                .updateDatabaseOnQueryScannedContactSuccess(result.handle)
            verify(avatarRepository, never()).getAvatarFile(result.email)
            verify(avatarRepository, never()).getAvatarColor(result.handle)
        }

    @Test
    fun `test that database fields are not updated and contact avatar file and color is not fetched when qrCodeQueryResult is Default`() =
        runTest {
            val handle = "1245"
            val result = ScannedContactLinkResult(
                contactName = "Abc",
                email = "abc@gmail.com",
                handle = 12345,
                isContact = false,
                qrCodeQueryResult = QRCodeQueryResults.CONTACT_QUERY_EEXIST
            )
            whenever(qrCodeRepository.queryScannedContactLink(handle)).thenReturn(result)
            underTest(handle)

            verify(qrCodeRepository, never())
                .updateDatabaseOnQueryScannedContactSuccess(result.handle)
            verify(avatarRepository, never()).getAvatarFile(result.email)
            verify(avatarRepository, never()).getAvatarColor(result.handle)
        }
}