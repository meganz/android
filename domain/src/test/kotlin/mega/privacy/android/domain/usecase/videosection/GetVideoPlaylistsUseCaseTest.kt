package mega.privacy.android.domain.usecase.videosection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetVideoPlaylistsUseCaseTest {
    private lateinit var underTest: GetVideoPlaylistsUseCase

    @BeforeAll
    fun setUp() {
        underTest = GetVideoPlaylistsUseCase()
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }
}