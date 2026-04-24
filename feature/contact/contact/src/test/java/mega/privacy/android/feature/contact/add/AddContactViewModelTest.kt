package mega.privacy.android.feature.contact.add

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.feature.contact.add.model.AddContactUiState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineMainDispatcherExtension::class)
class AddContactViewModelTest {
    private lateinit var underTest: AddContactViewModel

    @BeforeEach
    fun setup() {
        underTest = AddContactViewModel()
    }

    @Test
    fun `test that initial state is loading`() = runTest {
        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(AddContactUiState.Loading)
            cancelAndIgnoreRemainingEvents()
        }
    }

}