package mega.privacy.android.domain.usecase.audiosection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetAllAudioUseCaseTest {
    private lateinit var underTest: GetAllAudioUseCase

    @BeforeAll
    fun setUp() {
        underTest = GetAllAudioUseCase()
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }
}