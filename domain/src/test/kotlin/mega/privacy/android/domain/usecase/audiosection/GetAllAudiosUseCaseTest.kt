package mega.privacy.android.domain.usecase.audiosection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedAudioNode
import mega.privacy.android.domain.repository.AudioSectionRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetAllAudiosUseCaseTest {
    private lateinit var underTest: GetAllAudiosUseCase
    private val audioSectionRepository = mock<AudioSectionRepository>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()

    val order = SortOrder.ORDER_MODIFICATION_DESC

    @BeforeAll
    fun setUp() {
        underTest = GetAllAudiosUseCase(
            audioSectionRepository = audioSectionRepository,
            getCloudSortOrder = getCloudSortOrder
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(
            audioSectionRepository,
            getCloudSortOrder
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that the list of audios is not empty`() = runTest {
        val list = listOf(mock<TypedAudioNode>())
        whenever(audioSectionRepository.getAllAudios(order)).thenReturn(list)
        whenever(getCloudSortOrder()).thenReturn(order)
        assertThat(underTest()).isNotEmpty()
    }

    @Test
    fun `test that the list of audios is empty`() = runTest {
        whenever(audioSectionRepository.getAllAudios(order)).thenReturn(emptyList())
        whenever(getCloudSortOrder()).thenReturn(order)
        assertThat(underTest()).isEmpty()
    }
}