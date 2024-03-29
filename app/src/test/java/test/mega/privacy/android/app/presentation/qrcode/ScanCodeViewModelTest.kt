package test.mega.privacy.android.app.presentation.qrcode

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.qrcode.scan.ScanCodeViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.qrcode.QRCodeQueryResults
import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult
import mega.privacy.android.domain.usecase.contact.InviteContactUseCase
import mega.privacy.android.domain.usecase.qrcode.QueryScannedContactLinkUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.InstantExecutorExtension
import java.io.File

@ExtendWith(value = [CoroutineMainDispatcherExtension::class, InstantExecutorExtension::class])
@ExperimentalCoroutinesApi
class ScanCodeViewModelTest {

    private lateinit var underTest: ScanCodeViewModel
    private val queryScannedContactLinkUseCase = mock<QueryScannedContactLinkUseCase>()
    private val inviteContactUseCase = mock<InviteContactUseCase>()

    @BeforeEach
    fun setUp() {
        initViewModel()
    }

    private fun initViewModel() {
        underTest = ScanCodeViewModel(queryScannedContactLinkUseCase, inviteContactUseCase)
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.dialogTitleContent).isEqualTo(-1)
            assertThat(initial.dialogTextContent).isEqualTo(-1)
            assertThat(initial.email).isNull()
            assertThat(initial.success).isTrue()
            assertThat(initial.printEmail).isFalse()
            assertThat(initial.inviteDialogShown).isFalse()
            assertThat(initial.inviteResultDialogShown).isFalse()
            assertThat(initial.showInviteDialog).isFalse()
            assertThat(initial.showInviteResultDialog).isFalse()
            assertThat(initial.finishActivity).isFalse()
            assertThat(initial.finishActivityOnScanComplete).isFalse()
            assertThat(initial.scannedContactLinkResult).isNull()
        }
    }

    @Test
    fun `test that email is updated when new value is provided`() = runTest {
        underTest.state.map { it.email }.distinctUntilChanged().test {
            val newValue = "test@gmail.com"
            assertThat(awaitItem()).isNull()
            underTest.updateMyEmail(newValue)
            assertThat(awaitItem()).isEqualTo(newValue)
        }
    }

    @Test
    fun `test that invite dialog shown boolean is updated when new value is provided`() = runTest {
        underTest.state.map { it.inviteDialogShown }.distinctUntilChanged().test {
            assertThat(awaitItem()).isFalse()
            underTest.updateInviteShown(true)
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that invite result dialog shown is updated when new value is provided`() = runTest {
        underTest.state.map { it.inviteResultDialogShown }.distinctUntilChanged().test {
            assertThat(awaitItem()).isFalse()
            underTest.updateInviteResultDialogShown(true)
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that show invite result dialog is updated when new value is provided`() = runTest {
        underTest.state.map { it.showInviteResultDialog }.distinctUntilChanged().test {
            assertThat(awaitItem()).isFalse()
            underTest.updateShowInviteResultDialog(true)
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that show invite dialog is updated when new value is provided`() = runTest {
        underTest.state.map { it.showInviteDialog }.distinctUntilChanged().test {
            assertThat(awaitItem()).isFalse()
            underTest.updateShowInviteDialog(true)
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that finish activity on scan complete is updated when new value is provided`() =
        runTest {
            underTest.state.map { it.finishActivityOnScanComplete }.distinctUntilChanged().test {
                assertThat(awaitItem()).isFalse()
                underTest.updateFinishActivityOnScanComplete(true)
                assertThat(awaitItem()).isTrue()
            }
        }

    @Test
    fun `test that when show invite dialog is called values are updated`() = runTest {
        underTest.state.test {
            val expectedResult = ScannedContactLinkResult(
                "test",
                "test@gmail.com",
                100,
                true,
                QRCodeQueryResults.CONTACT_QUERY_OK,
                File(""),
                4043
            )

            val oldValue = awaitItem()
            assertThat(oldValue.showInviteResultDialog).isFalse()
            assertThat(oldValue.showInviteDialog).isFalse()
            assertThat(oldValue.scannedContactLinkResult).isNull()

            underTest.showInviteDialog(expectedResult)

            val newValue = awaitItem()
            assertThat(newValue.showInviteDialog).isTrue()
            assertThat(newValue.showInviteResultDialog).isFalse()
            assertThat(newValue.scannedContactLinkResult).isEqualTo(expectedResult)
        }
    }

    @Test
    fun `test that when show invite result dialog is called values are updated`() = runTest {
        underTest.state.test {
            val oldValue = awaitItem()
            val title = 1
            val text = 2
            val success = true
            val printEmail = true
            assertThat(oldValue.dialogTitleContent).isEqualTo(-1)
            assertThat(oldValue.dialogTextContent).isEqualTo(-1)
            assertThat(oldValue.success).isTrue()
            assertThat(oldValue.printEmail).isFalse()
            assertThat(oldValue.showInviteResultDialog).isFalse()
            assertThat(oldValue.showInviteDialog).isFalse()
            underTest.showInviteResultDialog(title, text, success, printEmail)
            val newValue = awaitItem()
            assertThat(newValue.dialogTitleContent).isEqualTo(title)
            assertThat(newValue.dialogTextContent).isEqualTo(text)
            assertThat(newValue.success).isEqualTo(success)
            assertThat(newValue.printEmail).isEqualTo(printEmail)
            assertThat(newValue.showInviteDialog).isFalse()
            assertThat(newValue.showInviteResultDialog).isTrue()
        }
    }

    @Test
    fun `test that on querying contact detail and on result OK email is updated and show invite dialog values are updated`() =
        runTest {
            val handle = "1234"
            val expectedEmail = "abc@gmail.com"
            val expectedName = "abc"
            val expectedHandle: Long = 12345
            val avatarFile = File("")
            val avatarColor = 4040
            val result = ScannedContactLinkResult(
                expectedName,
                expectedEmail,
                expectedHandle,
                true,
                QRCodeQueryResults.CONTACT_QUERY_OK,
                avatarFile,
                avatarColor
            )

            whenever(queryScannedContactLinkUseCase(handle)).thenReturn(result)
            underTest.state.test {
                awaitItem()
                underTest.queryContactLink(handle)
                val newValue = awaitItem()
                assertThat(newValue.email).isEqualTo(expectedEmail)
                assertThat(newValue.showInviteDialog).isTrue()
                assertThat(newValue.showInviteResultDialog).isFalse()
                assertThat(newValue.scannedContactLinkResult).isEqualTo(result)
            }
        }

    @Test
    fun `test that on querying contact detail and on result EExist email is updated and show invite result dialog values are updated`() =
        runTest {
            val handle = "1234"
            val expectedEmail = "abc@gmail.com"
            val result = ScannedContactLinkResult(
                "abc",
                expectedEmail,
                12345,
                false,
                QRCodeQueryResults.CONTACT_QUERY_EEXIST
            )

            whenever(queryScannedContactLinkUseCase(handle)).thenReturn(result)
            underTest.state.test {
                awaitItem()
                underTest.queryContactLink(handle)
                val newValue = awaitItem()
                assertThat(newValue.email).isEqualTo(expectedEmail)
                assertThat(newValue.success).isTrue()
                assertThat(newValue.printEmail).isTrue()
                assertThat(newValue.showInviteDialog).isFalse()
                assertThat(newValue.showInviteResultDialog).isTrue()
            }
        }

    @Test
    fun `test that on querying contact detail and on result Default email is updated and show invite result dialog values are updated`() =
        runTest {
            val handle = "1234"
            val expectedEmail = "abc@gmail.com"
            val result = ScannedContactLinkResult(
                "abc",
                expectedEmail,
                12345,
                false,
                QRCodeQueryResults.CONTACT_QUERY_DEFAULT
            )

            whenever(queryScannedContactLinkUseCase(handle)).thenReturn(result)
            underTest.state.test {
                awaitItem()
                underTest.queryContactLink(handle)
                val newValue = awaitItem()
                assertThat(newValue.email).isEqualTo(expectedEmail)
                assertThat(newValue.success).isFalse()
                assertThat(newValue.printEmail).isFalse()
                assertThat(newValue.showInviteDialog).isFalse()
                assertThat(newValue.showInviteResultDialog).isTrue()
            }
        }

    @Test
    fun `test that on sending invite and on result Sent show invite result dialog values are updated`() =
        runTest {
            whenever(inviteContactUseCase(any(), any(), anyOrNull())).thenReturn(
                InviteContactRequest.Sent
            )
            underTest.state.test {
                awaitItem()
                underTest.sendInvite()
                val newValue = awaitItem()
                assertThat(newValue.success).isTrue()
                assertThat(newValue.printEmail).isFalse()
                assertThat(newValue.showInviteDialog).isFalse()
                assertThat(newValue.showInviteResultDialog).isTrue()
            }
        }

    @Test
    fun `test that on sending invite and on result InvalidEmail show invite result dialog values are updated`() =
        runTest {
            whenever(inviteContactUseCase(any(), any(), anyOrNull()))
                .thenReturn(InviteContactRequest.InvalidEmail)

            underTest.state.test {
                awaitItem()
                underTest.sendInvite()
                val newValue = awaitItem()
                assertThat(newValue.success).isTrue()
                assertThat(newValue.printEmail).isFalse()
                assertThat(newValue.showInviteDialog).isFalse()
                assertThat(newValue.showInviteResultDialog).isTrue()
            }
        }

    @Test
    fun `test that on sending invite and on result AlreadyContact show invite result dialog values are updated`() =
        runTest {
            whenever(inviteContactUseCase(any(), any(), anyOrNull()))
                .thenReturn(InviteContactRequest.AlreadyContact)

            underTest.state.test {
                awaitItem()
                underTest.sendInvite()
                val newValue = awaitItem()
                assertThat(newValue.success).isTrue()
                assertThat(newValue.printEmail).isTrue()
                assertThat(newValue.showInviteDialog).isFalse()
                assertThat(newValue.showInviteResultDialog).isTrue()
            }
        }

    @Test
    fun `test that show invite result dialog values are updated when sending invite throw exception`() =
        runTest {
            whenever(inviteContactUseCase(any(), any(), anyOrNull()))
                .thenAnswer { throw RuntimeException() }

            underTest.state.test {
                awaitItem()
                underTest.sendInvite()
                val newValue = awaitItem()
                assertThat(newValue.success).isFalse()
                assertThat(newValue.printEmail).isFalse()
                assertThat(newValue.showInviteDialog).isFalse()
                assertThat(newValue.showInviteResultDialog).isTrue()
            }
        }
}