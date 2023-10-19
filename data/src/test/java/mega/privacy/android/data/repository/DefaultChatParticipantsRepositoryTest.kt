package mega.privacy.android.data.repository

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.data.mapper.chat.ChatPermissionsMapper
import mega.privacy.android.data.mapper.chat.UserStatusToIntMapper
import mega.privacy.android.data.mapper.contact.UserChatStatusMapper
import mega.privacy.android.data.mapper.handles.MegaHandleListMapper
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.RequestLastGreen
import mega.privacy.android.domain.usecase.account.GetUserAliasUseCase
import mega.privacy.android.domain.usecase.avatar.GetAvatarFileFromEmailUseCase
import mega.privacy.android.domain.usecase.avatar.GetAvatarFileFromHandleUseCase
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.domain.usecase.avatar.GetUserAvatarColorUseCase
import mega.privacy.android.domain.usecase.chat.GetUserPrivilegeUseCase
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import mega.privacy.android.domain.usecase.contact.GetContactEmail
import mega.privacy.android.domain.usecase.contact.GetContactFullNameUseCase
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultChatParticipantsRepositoryTest {

    private lateinit var underTest: DefaultChatParticipantsRepository
    private val megaChatApiGateway: MegaChatApiGateway = mock()
    private val megaApiGateway: MegaApiGateway = mock()
    private val getMyAvatarColorUseCase: GetMyAvatarColorUseCase = mock()
    private val getContactEmail: GetContactEmail = mock()
    private val getUserAvatarColorUseCase: GetUserAvatarColorUseCase = mock()
    private val getMyAvatarFileUseCase: GetMyAvatarFileUseCase = mock()
    private val getAvatarFileFromEmailUseCase: GetAvatarFileFromEmailUseCase = mock()
    private val getAvatarFileFromHandleUseCase: GetAvatarFileFromHandleUseCase = mock()
    private val getUserAliasUseCase: GetUserAliasUseCase = mock()
    private val getContactFullNameUseCase: GetContactFullNameUseCase = mock()
    private val areCredentialsVerifiedUseCase: AreCredentialsVerifiedUseCase = mock()
    private val getUserPrivilegeUseCase: GetUserPrivilegeUseCase = mock()
    private val requestLastGreen: RequestLastGreen = mock()
    private val chatPermissionsMapper: ChatPermissionsMapper = mock()
    private val megaHandleListMapper: MegaHandleListMapper = mock()
    private val userStatusToIntMapper: UserStatusToIntMapper = mock()
    private val userChatStatusMapper: UserChatStatusMapper = mock()


    @BeforeAll
    fun setup() {
        underTest = DefaultChatParticipantsRepository(
            megaChatApiGateway,
            megaApiGateway,
            getMyAvatarColorUseCase,
            requestLastGreen,
            getContactEmail,
            getUserAvatarColorUseCase,
            chatPermissionsMapper,
            megaHandleListMapper,
            userStatusToIntMapper,
            getMyAvatarFileUseCase,
            getAvatarFileFromEmailUseCase,
            getAvatarFileFromHandleUseCase,
            getUserAliasUseCase,
            areCredentialsVerifiedUseCase,
            getContactFullNameUseCase,
            getUserPrivilegeUseCase,
            UnconfinedTestDispatcher(),
            userChatStatusMapper,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            megaChatApiGateway,
            megaApiGateway,
            getMyAvatarColorUseCase,
            requestLastGreen,
            getContactEmail,
            getUserAvatarColorUseCase,
            chatPermissionsMapper,
            megaHandleListMapper,
            userStatusToIntMapper,
            getMyAvatarFileUseCase,
            getAvatarFileFromEmailUseCase,
            getAvatarFileFromHandleUseCase,
            getUserAliasUseCase,
            areCredentialsVerifiedUseCase,
            getContactFullNameUseCase,
            getUserPrivilegeUseCase,
        )
    }

    @Test
    fun `test that setOnlineStatus returns success when api returns API_OK`() =
        runTest {
            val megaError = mock<MegaChatError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_OK)
            }
            val megaRequest = mock<MegaChatRequest>()

            whenever(
                megaChatApiGateway.setOnlineStatus(
                    status = any(),
                    listener = any(),
                )
            ).thenAnswer {
                (it.arguments[1] as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                    api = mock(),
                    request = megaRequest,
                    error = megaError,
                )
            }
            Truth.assertThat(underTest.setOnlineStatus(any())).isEqualTo(Unit)
        }

    @Test
    fun `test that setOnlineStatus returns success when api returns ERROR_ARGS`() =
        runTest {
            val megaError = mock<MegaChatError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_ARGS)
            }
            val megaRequest = mock<MegaChatRequest>()

            whenever(
                megaChatApiGateway.setOnlineStatus(
                    status = any(),
                    listener = any(),
                )
            ).thenAnswer {
                (it.arguments[1] as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                    api = mock(),
                    request = megaRequest,
                    error = megaError,
                )
            }
            Truth.assertThat(underTest.setOnlineStatus(any())).isEqualTo(Unit)
        }

    @Test
    fun `test that setOnlineStatus returns an exception when api does not return API_OK`() =
        runTest {
            val megaError = mock<MegaChatError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_NOENT)
            }
            val megaRequest = mock<MegaChatRequest> {}
            whenever(
                megaChatApiGateway.setOnlineStatus(
                    status = any(),
                    listener = any(),
                )
            ).thenAnswer {
                (it.arguments[1] as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                    api = mock(),
                    request = megaRequest,
                    error = megaError,
                )
            }

            assertFailsWith(
                exceptionClass = MegaException::class,
                block = { underTest.setOnlineStatus(any()) },
            )
        }
}