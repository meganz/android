package test.mega.privacy.android.app.main.dialog.link

import com.google.common.truth.Truth
import mega.privacy.android.app.R
import mega.privacy.android.app.main.dialog.link.OpenLinkErrorMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenLinkErrorMapperTest {
    private val underTest: OpenLinkErrorMapper = OpenLinkErrorMapper()

    @Test
    fun `test that returns correctly when submittedLink is empty and open link to join meeting`() {
        Truth.assertThat(
            underTest(
                isJoinMeeting = true,
                isChatScreen = false,
                submittedLink = "",
                linkType = null,
                checkLinkResult = null,
            )
        ).isEqualTo(R.string.invalid_meeting_link_empty)
    }

    @Test
    fun `test that returns correctly when submittedLink is empty and open from chat link`() {
        Truth.assertThat(
            underTest(
                isJoinMeeting = false,
                isChatScreen = true,
                submittedLink = "",
                linkType = null,
                checkLinkResult = null,
            )
        ).isEqualTo(R.string.invalid_chat_link_empty)
    }

    @Test
    fun `test that returns correctly when submittedLink is empty and open from cloud drive`() {
        Truth.assertThat(
            underTest(
                isJoinMeeting = false,
                isChatScreen = false,
                submittedLink = "",
                linkType = null,
                checkLinkResult = null,
            )
        ).isEqualTo(R.string.invalid_file_folder_link_empty)
    }
}