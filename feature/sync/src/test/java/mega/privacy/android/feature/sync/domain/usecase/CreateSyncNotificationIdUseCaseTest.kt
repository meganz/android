package mega.privacy.android.feature.sync.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.usecase.notifcation.CreateSyncNotificationIdUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateSyncNotificationIdUseCaseTest {
    private lateinit var underTest: CreateSyncNotificationIdUseCase

    @BeforeAll
    fun setup() {
        underTest = CreateSyncNotificationIdUseCase()
    }

    @Test
    fun `test that every time the use case generates a different value`() = runTest {
        val firstInvocation = underTest()
        val secondInvocation = underTest()

        assertThat(firstInvocation).isNotEqualTo(secondInvocation)
    }
}
