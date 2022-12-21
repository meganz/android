package mega.privacy.android.data.repository

import mega.privacy.android.data.gateway.ClipboardGateway
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class DefaultClipboardRepositoryTest {

    private lateinit var underTest: DefaultClipboardRepository
    private val clipboardGateway: ClipboardGateway = mock()

    @Before
    fun setup() {
        underTest = DefaultClipboardRepository(clipboardGateway)
    }

    @Test
    fun `test that clipboard gateway method is invoked`() {
        val label = "label param"
        val text = "clipboard text param"
        underTest.setClip(label = label, text = text)
        verify(clipboardGateway).setClip(label, text)
    }
}