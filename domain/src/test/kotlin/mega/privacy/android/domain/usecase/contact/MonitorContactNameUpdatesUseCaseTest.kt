package mega.privacy.android.domain.usecase.contact

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorContactNameUpdatesUseCaseTest {

    private val contactsRepository = mock<ContactsRepository>()
    private lateinit var underTest: MonitorContactNameUpdatesUseCase

    @BeforeEach
    fun setup() {
        underTest = MonitorContactNameUpdatesUseCase(contactsRepository)
        reset(contactsRepository)
    }

    @ParameterizedTest
    @MethodSource("observedUserChangesProvider")
    fun `test that use case emits updates when observed changes occur`(userChange: UserChanges) = runTest {
        // Given
        val userId = UserId(1L)
        val contactUpdatesFlow = MutableSharedFlow<UserUpdate>()
        val userUpdate = UserUpdate(
            changes = mapOf(userId to listOf(userChange)),
            emailMap = mapOf(userId to "user@example.com")
        )

        whenever(contactsRepository.monitorContactUpdates()).thenReturn(contactUpdatesFlow)

        // When & Then
        underTest().test {
            contactUpdatesFlow.emit(userUpdate)
            val result = awaitItem()
            assertThat(result).isEqualTo(userUpdate)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @ParameterizedTest
    @MethodSource("nonObservedUserChangesProvider")
    fun `test that use case filters out updates with only non-observed changes`(userChange: UserChanges) = runTest {
        // Given
        val userId = UserId(1L)
        val contactUpdatesFlow = MutableSharedFlow<UserUpdate>()
        val userUpdate = UserUpdate(
            changes = mapOf(userId to listOf(userChange)),
            emailMap = mapOf(userId to "user@example.com")
        )

        whenever(contactsRepository.monitorContactUpdates()).thenReturn(contactUpdatesFlow)

        // When & Then
        underTest().test {
            contactUpdatesFlow.emit(userUpdate)
            // Should not emit anything as no observed changes are present
            expectNoEvents()
        }
    }

    companion object {
        @JvmStatic
        fun observedUserChangesProvider() = listOf(
            Arguments.of(UserChanges.AuthenticationInformation),
            Arguments.of(UserChanges.Firstname),
            Arguments.of(UserChanges.Lastname),
            Arguments.of(UserChanges.Alias)
        )

        @JvmStatic
        fun nonObservedUserChangesProvider() = listOf(
            Arguments.of(UserChanges.Email),
            Arguments.of(UserChanges.Avatar),
            Arguments.of(UserChanges.Country),
            Arguments.of(UserChanges.Birthday),
            Arguments.of(UserChanges.Language),
            Arguments.of(UserChanges.StorageState),
            Arguments.of(UserChanges.PushSettings)
        )

        @JvmStatic
        fun mixedChangesProvider() = listOf(
            Arguments.of(UserChanges.Firstname, UserChanges.Email),
            Arguments.of(UserChanges.Lastname, UserChanges.Avatar),
            Arguments.of(UserChanges.Alias, UserChanges.Country),
            Arguments.of(UserChanges.AuthenticationInformation, UserChanges.Language)
        )
    }

    @Test
    fun `test that use case emits updates when multiple observed changes occur`() = runTest {
        // Given
        val userId = UserId(1L)
        val contactUpdatesFlow = MutableSharedFlow<UserUpdate>()
        val userUpdate = UserUpdate(
            changes = mapOf(
                userId to listOf(
                    UserChanges.Firstname,
                    UserChanges.Lastname,
                    UserChanges.Alias
                )
            ),
            emailMap = mapOf(userId to "user@example.com")
        )

        whenever(contactsRepository.monitorContactUpdates()).thenReturn(contactUpdatesFlow)

        // When & Then
        underTest().test {
            contactUpdatesFlow.emit(userUpdate)
            val result = awaitItem()
            assertThat(result).isEqualTo(userUpdate)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that use case emits updates when any user has observed changes`() = runTest {
        // Given
        val userId1 = UserId(1L)
        val userId2 = UserId(2L)
        val contactUpdatesFlow = MutableSharedFlow<UserUpdate>()
        val userUpdate = UserUpdate(
            changes = mapOf(
                userId1 to listOf(UserChanges.Email), // Not observed
                userId2 to listOf(UserChanges.Firstname) // Observed
            ),
            emailMap = mapOf(
                userId1 to "user1@example.com",
                userId2 to "user2@example.com"
            )
        )

        whenever(contactsRepository.monitorContactUpdates()).thenReturn(contactUpdatesFlow)

        // When & Then
        underTest().test {
            contactUpdatesFlow.emit(userUpdate)
            val result = awaitItem()
            assertThat(result).isEqualTo(userUpdate)
            cancelAndIgnoreRemainingEvents()
        }
    }


    @Test
    fun `test that use case filters out updates with empty changes`() = runTest {
        // Given
        val contactUpdatesFlow = MutableSharedFlow<UserUpdate>()
        val userUpdate = UserUpdate(
            changes = emptyMap(),
            emailMap = emptyMap()
        )

        whenever(contactsRepository.monitorContactUpdates()).thenReturn(contactUpdatesFlow)

        // When & Then
        underTest().test {
            contactUpdatesFlow.emit(userUpdate)
            // Should not emit anything as no changes are present
            expectNoEvents()
        }
    }

    @Test
    fun `test that use case handles multiple emissions correctly`() = runTest {
        // Given
        val userId1 = UserId(1L)
        val userId2 = UserId(2L)
        val contactUpdatesFlow = MutableSharedFlow<UserUpdate>()

        val userUpdate1 = UserUpdate(
            changes = mapOf(userId1 to listOf(UserChanges.Firstname)),
            emailMap = mapOf(userId1 to "user1@example.com")
        )

        val userUpdate2 = UserUpdate(
            changes = mapOf(userId2 to listOf(UserChanges.Email)), // Not observed
            emailMap = mapOf(userId2 to "user2@example.com")
        )

        val userUpdate3 = UserUpdate(
            changes = mapOf(userId1 to listOf(UserChanges.Lastname)),
            emailMap = mapOf(userId1 to "user1@example.com")
        )

        whenever(contactsRepository.monitorContactUpdates()).thenReturn(contactUpdatesFlow)

        // When & Then
        underTest().test {
            contactUpdatesFlow.emit(userUpdate1)
            contactUpdatesFlow.emit(userUpdate2)
            contactUpdatesFlow.emit(userUpdate3)

            // Should only emit userUpdate1 and userUpdate3
            assertThat(awaitItem()).isEqualTo(userUpdate1)
            assertThat(awaitItem()).isEqualTo(userUpdate3)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @ParameterizedTest
    @MethodSource("mixedChangesProvider")
    fun `test that use case emits updates when mixed observed and non-observed changes occur`(
        observedChange: UserChanges,
        nonObservedChange: UserChanges
    ) = runTest {
        // Given
        val userId = UserId(1L)
        val contactUpdatesFlow = MutableSharedFlow<UserUpdate>()
        val userUpdate = UserUpdate(
            changes = mapOf(
                userId to listOf(observedChange, nonObservedChange)
            ),
            emailMap = mapOf(userId to "user@example.com")
        )

        whenever(contactsRepository.monitorContactUpdates()).thenReturn(contactUpdatesFlow)

        // When & Then
        underTest().test {
            contactUpdatesFlow.emit(userUpdate)
            val result = awaitItem()
            assertThat(result).isEqualTo(userUpdate)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that use case handles multiple users with different change types`() = runTest {
        // Given
        val userId1 = UserId(1L)
        val userId2 = UserId(2L)
        val userId3 = UserId(3L)
        val contactUpdatesFlow = MutableSharedFlow<UserUpdate>()
        val userUpdate = UserUpdate(
            changes = mapOf(
                userId1 to listOf(UserChanges.Email), // Not observed
                userId2 to listOf(UserChanges.Firstname), // Observed
                userId3 to listOf(UserChanges.Avatar) // Not observed
            ),
            emailMap = mapOf(
                userId1 to "user1@example.com",
                userId2 to "user2@example.com",
                userId3 to "user3@example.com"
            )
        )

        whenever(contactsRepository.monitorContactUpdates()).thenReturn(contactUpdatesFlow)

        // When & Then
        underTest().test {
            contactUpdatesFlow.emit(userUpdate)
            val result = awaitItem()
            assertThat(result).isEqualTo(userUpdate)
            cancelAndIgnoreRemainingEvents()
        }
    }
}