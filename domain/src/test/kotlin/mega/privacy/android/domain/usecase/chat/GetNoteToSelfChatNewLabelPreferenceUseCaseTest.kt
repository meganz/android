package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.RemotePreferencesRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever


@OptIn(ExperimentalCoroutinesApi::class)
class GetNoteToSelfChatNewLabelPreferenceUseCaseTest {
    private lateinit var underTest: GetNoteToSelfChatNewLabelPreferenceUseCase

    private val remotePreferencesRepository = mock<RemotePreferencesRepository>()

    @Before
    fun setUp() {
        underTest = GetNoteToSelfChatNewLabelPreferenceUseCase(remotePreferencesRepository)
    }

    @Test
    fun `invoke should return note to self preference`() = runTest {
        val item = 3
        whenever(remotePreferencesRepository.getNoteToSelfChatNewLabelPreference()).thenReturn(item.toString())

        val result = underTest.invoke()

        assertThat(result).isEqualTo(item)
    }
}
