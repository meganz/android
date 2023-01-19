package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.qrcode.QRCodeQueryResults
import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult
import mega.privacy.android.domain.repository.QRCodeRepository
import mega.privacy.android.domain.usecase.qrcode.DefaultQueryScannedContactLink
import mega.privacy.android.domain.usecase.qrcode.QueryScannedContactLink
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultQueryScannedContactLinkTest {
    private lateinit var underTest: QueryScannedContactLink
    private val qrCodeRepository = mock<QRCodeRepository>()

    @Before
    fun setUp() {
        underTest = DefaultQueryScannedContactLink(qrCodeRepository)
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
    fun `test that database fields are not updated when qrCodeQueryResult is EExist`() = runTest {
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

        verify(qrCodeRepository, never()).updateDatabaseOnQueryScannedContactSuccess(result.handle)
    }

    @Test
    fun `test that database fields are not updated when qrCodeQueryResult is Default`() = runTest {
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

        verify(qrCodeRepository, never()).updateDatabaseOnQueryScannedContactSuccess(result.handle)
    }
}