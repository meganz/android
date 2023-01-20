package test.mega.privacy.android.app.presentation.qrcode

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.qrcode.scan.ScanCodeViewModel
import mega.privacy.android.domain.entity.qrcode.QRCodeQueryResults
import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult
import mega.privacy.android.domain.usecase.qrcode.QueryScannedContactLink
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@ExperimentalCoroutinesApi
class ScanCodeViewModelTest {

    private lateinit var underTest: ScanCodeViewModel
    private val queryScannedContactLink = mock<QueryScannedContactLink>()

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initViewModel()
    }

    private fun initViewModel() {
        underTest = ScanCodeViewModel(queryScannedContactLink)
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.dialogTitleContent).isEqualTo(-1)
            assertThat(initial.dialogTextContent).isEqualTo(-1)
            assertThat(initial.contactNameContent).isNull()
            assertThat(initial.isContact).isFalse()
            assertThat(initial.myEmail).isNull()
            assertThat(initial.handleContactLink).isEqualTo(-1)
            assertThat(initial.success).isTrue()
            assertThat(initial.printEmail).isFalse()
            assertThat(initial.inviteDialogShown).isFalse()
            assertThat(initial.inviteResultDialogShown).isFalse()
            assertThat(initial.showInviteDialog).isFalse()
            assertThat(initial.showInviteResultDialog).isFalse()
            assertThat(initial.finishActivity).isFalse()
            assertThat(initial.finishActivityOnScanComplete).isFalse()
            assertThat(initial.avatarColor).isNull()
            assertThat(initial.avatarFile).isNull()
        }
    }

    @Test
    fun `test that email is updated when new value is provided`() = runTest {
        underTest.state.map { it.myEmail }.distinctUntilChanged().test {
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
            val oldValue = awaitItem()
            val email = "test@gmail.com"
            val contactName = "test"
            val isContact = true
            val handle: Long = 100
            val avatarFile = File("")
            val avatarColor = 4043
            assertThat(oldValue.myEmail).isNull()
            assertThat(oldValue.contactNameContent).isNull()
            assertThat(oldValue.isContact).isFalse()
            assertThat(oldValue.handleContactLink).isEqualTo(-1)
            assertThat(oldValue.showInviteResultDialog).isFalse()
            assertThat(oldValue.showInviteDialog).isFalse()
            assertThat(oldValue.avatarFile).isNull()
            assertThat(oldValue.avatarColor).isNull()
            underTest.showInviteDialog(
                contactName,
                email,
                isContact,
                handle,
                avatarFile,
                avatarColor
            )
            val newValue = awaitItem()
            assertThat(newValue.myEmail).isEqualTo(email)
            assertThat(newValue.contactNameContent).isEqualTo(contactName)
            assertThat(newValue.isContact).isEqualTo(isContact)
            assertThat(newValue.handleContactLink).isEqualTo(handle)
            assertThat(newValue.showInviteDialog).isTrue()
            assertThat(newValue.showInviteResultDialog).isFalse()
            assertThat(newValue.avatarFile).isEqualTo(avatarFile)
            assertThat(newValue.avatarColor).isEqualTo(avatarColor)
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

            whenever(queryScannedContactLink(handle)).thenReturn(result)
            underTest.state.test {
                awaitItem()
                underTest.queryContactLink(handle)
                val newValue = awaitItem()
                assertThat(newValue.myEmail).isEqualTo(expectedEmail)
                assertThat(newValue.contactNameContent).isEqualTo(expectedName)
                assertThat(newValue.handleContactLink).isEqualTo(expectedHandle)
                assertThat(newValue.isContact).isTrue()
                assertThat(newValue.showInviteDialog).isTrue()
                assertThat(newValue.showInviteResultDialog).isFalse()
                assertThat(newValue.avatarFile).isEqualTo(avatarFile)
                assertThat(newValue.avatarColor).isEqualTo(avatarColor)
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

            whenever(queryScannedContactLink(handle)).thenReturn(result)
            underTest.state.test {
                awaitItem()
                underTest.queryContactLink(handle)
                val newValue = awaitItem()
                assertThat(newValue.myEmail).isEqualTo(expectedEmail)
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

            whenever(queryScannedContactLink(handle)).thenReturn(result)
            underTest.state.test {
                awaitItem()
                underTest.queryContactLink(handle)
                val newValue = awaitItem()
                assertThat(newValue.myEmail).isEqualTo(expectedEmail)
                assertThat(newValue.success).isFalse()
                assertThat(newValue.printEmail).isFalse()
                assertThat(newValue.showInviteDialog).isFalse()
                assertThat(newValue.showInviteResultDialog).isTrue()
            }
        }
}