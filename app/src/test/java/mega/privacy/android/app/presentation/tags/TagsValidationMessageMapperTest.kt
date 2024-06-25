package mega.privacy.android.app.presentation.tags

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.core.test.AnalyticsTestRule
import mega.privacy.android.shared.resources.R
import mega.privacy.mobile.analytics.event.NodeInfoTagsLengthErrorDisplayedEvent
import mega.privacy.mobile.analytics.event.NodeInfoTagsLimitErrorDisplayedEvent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class TagsValidationMessageMapperTest {
    private lateinit var underTest: TagsValidationMessageMapper
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val nodeTags = listOf("tag1", "tag2", "tag3")
    private val nodeTagsFilled =
        listOf("tag1", "tag2", "tag3", "tag4", "tag5", "tag6", "tag7", "tag8", "tag9", "tag10")
    private val userTags = listOf("tag14", "tag25", "tag36")

    @get:Rule
    val analyticsRule = AnalyticsTestRule()

    @Before
    fun setUp() {
        underTest = TagsValidationMessageMapper(context)
    }

    @Test
    fun `test that the message is empty when the tag is empty`() {
        val (message, error) = underTest("", nodeTags, userTags)
        assertThat(message).isEqualTo(null)
        assertThat(error).isEqualTo(false)
    }

    @Test
    fun `test that the message is add_tags_label_tag_description when the tag is blank and userTags is empty`() {
        val (message, error) = underTest("", emptyList(), emptyList())
        assertThat(message)
            .isEqualTo(context.getString(R.string.add_tags_label_tag_description))
        assertThat(error).isEqualTo(false)
    }

    @Test
    fun `test that the message is add_tags_error_special_characters when the tag contains special characters`() {
        val (message, error) = underTest("tag!", nodeTags, userTags)
        assertThat(message)
            .isEqualTo(context.getString(R.string.add_tags_error_special_characters))
        assertThat(error).isEqualTo(true)
    }

    @Test
    fun `test that the message is add_tags_error_max_characters when the tag length is greater than MAX_CHARS_PER_TAG`() {
        val (message, error) = underTest(
            "abcdefghijklmnopqrstuvwxyzabcdefghjishk",
            nodeTags,
            userTags
        )
        assertThat(message).isEqualTo(context.getString(R.string.add_tags_error_maximum_characters))
        assertThat(error).isEqualTo(true)
        assertThat(analyticsRule.events).contains(NodeInfoTagsLengthErrorDisplayedEvent)
    }

    @Test
    fun `test that the message is add_tags_error_max_tags when nodeTags size is greater than MAX_TAGS_PER_NODE and userTags does not contain tag`() {
        val (message, error) = underTest("tag7", nodeTagsFilled, userTags)
        assertThat(message)
            .isEqualTo(
                context.getString(
                    R.string.add_tags_error_max_tags,
                    TagsActivity.MAX_TAGS_PER_NODE
                )
            )
        assertThat(error).isEqualTo(true)
        assertThat(analyticsRule.events).contains(NodeInfoTagsLimitErrorDisplayedEvent)
    }

    @Test
    fun `test that the message is empty when tag is valid`() {
        val (message, error) = underTest("tag", nodeTags, userTags)
        assertThat(message).isEqualTo(null)
        assertThat(error).isEqualTo(false)
    }
}