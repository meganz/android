package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.RemotePreferencesRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify


@OptIn(ExperimentalCoroutinesApi::class)
class SetNoteToSelfChatNewLabelPreferenceUseCaseTest {
    private lateinit var underTest: SetNoteToSelfChatNewLabelPreferenceUseCase

    private val remotePreferencesRepository = mock<RemotePreferencesRepository>()

    @Before
    fun setUp() {
        underTest = SetNoteToSelfChatNewLabelPreferenceUseCase(remotePreferencesRepository)
    }

    @Test
    fun `test that invoke sets note to self preference`() =
        runTest {
            val item = 3

            underTest.invoke(item)
            verify(remotePreferencesRepository).setNoteToSelfChatNewLabelPreference(item.toString())
        }
}
