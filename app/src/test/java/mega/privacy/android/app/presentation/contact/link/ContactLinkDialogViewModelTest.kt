package mega.privacy.android.app.presentation.contact.link

import androidx.annotation.Nullable
import androidx.compose.ui.graphics.Color
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.contact.link.dialog.ContactLinkDialogNavKey
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.contacts.ContactLinkQueryResult
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.usecase.avatar.GetUserAvatarColorUseCase
import mega.privacy.android.domain.usecase.contact.GetAvatarFromBase64StringUseCase
import mega.privacy.android.domain.usecase.contact.InviteContactWithHandleUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File

@ExtendWith(CoroutineMainDispatcherExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContactLinkDialogViewModelTest {

    private lateinit var underTest: ContactLinkDialogViewModel

    private val inviteContactWithHandleUseCase = mock<InviteContactWithHandleUseCase>()
    private val getAvatarFromBase64StringUseCase = mock<GetAvatarFromBase64StringUseCase>()
    private val getUserAvatarColorUseCase = mock<GetUserAvatarColorUseCase>()

    private val email = "email@mega.io"
    private val contactHandle = 12345L
    private val contactLinkQueryResult = mock<ContactLinkQueryResult> {
        on { this.contactHandle } doReturn contactHandle
        on { this.email } doReturn email
    }

    @BeforeEach
    fun setup() {
        initViewModel()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            inviteContactWithHandleUseCase,
            getAvatarFromBase64StringUseCase,
            getUserAvatarColorUseCase,
        )
    }

    private fun initViewModel(
        result: ContactLinkQueryResult = contactLinkQueryResult,
    ) {
        underTest = ContactLinkDialogViewModel(
            inviteContactWithHandleUseCase = inviteContactWithHandleUseCase,
            getAvatarFromBase64StringUseCase = getAvatarFromBase64StringUseCase,
            getUserAvatarColorUseCase = getUserAvatarColorUseCase,
            navKey = ContactLinkDialogNavKey(result),
        )
    }

    @Test
    fun `test that contactLinkQueryResult in state is updated with navKey`() = runTest {
        initViewModel()

        underTest.uiState.test {
            assertThat(awaitItem().contactLinkQueryResult).isEqualTo(contactLinkQueryResult)
        }
    }

    @ParameterizedTest
    @Nullable
    @ValueSource(strings = ["asdfiohwighvqiwiqoi3hgoi3wghfw", "none"])
    fun `test that avatarFile in state is updated with file if get`(
        avatarFileInBase64: String?,
    ) = runTest {
        val contactLinkQueryResult = mock<ContactLinkQueryResult> {
            on { this.contactHandle } doReturn contactHandle
            on { this.email } doReturn email
            on { this.avatarFileInBase64 } doReturn avatarFileInBase64
        }
        val requestFile = avatarFileInBase64 != null && avatarFileInBase64 != "none"
        val avatarFile = if (requestFile) mock<File>() else null

        whenever(
            getAvatarFromBase64StringUseCase(
                userHandle = contactHandle,
                base64String = avatarFileInBase64.orEmpty(),
            )
        ) doReturn avatarFile

        initViewModel(contactLinkQueryResult)

        advanceUntilIdle()

        underTest.uiState.map { it.avatarFile }.test {
            assertThat(awaitItem()).isEqualTo(avatarFile)
        }

        if (requestFile.not()) {
            verifyNoInteractions(getAvatarFromBase64StringUseCase)
        }
    }

    @ParameterizedTest
    @Nullable
    @ValueSource(ints = [123])
    fun `test that avatarColor in state is updated with color`(
        colorInt: Int?,
    ) = runTest {
        whenever(
            getUserAvatarColorUseCase(contactLinkQueryResult.contactHandle)
        ).also {
            if (colorInt == null) {
                it doThrow RuntimeException()
            } else {
                it doReturn colorInt
            }
        }

        initViewModel()

        advanceUntilIdle()

        underTest.uiState.map { it.avatarColor }.test {
            assertThat(awaitItem()).isInstanceOf(Color::class.java)
        }
    }

    @ParameterizedTest
    @Nullable
    @EnumSource(InviteContactRequest::class)
    fun `test that inviteContact updates state with inviteContactResult`(
        inviteContactRequest: InviteContactRequest?,
    ) = runTest {
        initViewModel()

        whenever(
            inviteContactWithHandleUseCase(
                email = email,
                handle = contactHandle,
                message = null,
            )
        ).also {
            if (inviteContactRequest == null) {
                it doThrow RuntimeException()
            } else {
                it doReturn inviteContactRequest
            }
        }

        underTest.inviteContact()

        advanceUntilIdle()

        underTest.uiState.map { it.inviteContactResult }.test {
            assertThat(awaitItem()?.getOrNull()).isEqualTo(inviteContactRequest)
        }
    }
}