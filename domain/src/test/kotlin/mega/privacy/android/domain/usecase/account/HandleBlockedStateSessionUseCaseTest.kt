package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountBlockedEvent
import mega.privacy.android.domain.entity.account.AccountBlockedType
import mega.privacy.android.domain.usecase.login.DisableChatApiUseCase
import mega.privacy.android.domain.usecase.login.LocalLogoutUseCase
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.kotlin.verify

class HandleBlockedStateSessionUseCaseTest {
    private val localLogoutUseCase = mock<LocalLogoutUseCase>()

    private val disableChatApiUseCase = mock<DisableChatApiUseCase>()

    private val underTest = HandleBlockedStateSessionUseCase(
        localLogoutUseCase = localLogoutUseCase,
        disableChatApiUseCase = disableChatApiUseCase,
    )

    @ParameterizedTest(name = "invoke with event type {0} should not call localLogoutUseCase")
    @EnumSource(
        AccountBlockedType::class,
        names = ["TOS_COPYRIGHT", "TOS_NON_COPYRIGHT", "SUBUSER_DISABLED", "SUBUSER_REMOVED"],
        mode = EnumSource.Mode.EXCLUDE
    )
    fun `test invoke with event type should not call localLogoutUseCase`(eventType: AccountBlockedType) =
        runTest {
            val event = AccountBlockedEvent(
                handle = -1L,
                type = eventType,
                text = ""
            )

            underTest(event)

            // Verify that localLogoutUseCase is not called
            verify(localLogoutUseCase, never()).invoke(disableChatApiUseCase)
        }

    @ParameterizedTest(name = "invoke with event type {0} should call localLogoutUseCase")
    @EnumSource(
        AccountBlockedType::class,
        names = ["TOS_COPYRIGHT", "TOS_NON_COPYRIGHT", "SUBUSER_DISABLED", "SUBUSER_REMOVED"],
        mode = EnumSource.Mode.INCLUDE
    )
    fun `test invoke with event type should call localLogoutUseCase`(eventType: AccountBlockedType) =
        runTest {
            val event = AccountBlockedEvent(
                handle = -1L,
                type = eventType,
                text = ""
            )

            underTest(event)

            // Verify that localLogoutUseCase is called
            verify(localLogoutUseCase).invoke(disableChatApiUseCase)
        }

}


