package mega.privacy.android.app.presentation.tags

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.shared.resources.R
import mega.privacy.mobile.analytics.event.NodeInfoTagsLengthErrorDisplayedEvent
import mega.privacy.mobile.analytics.event.NodeInfoTagsLimitErrorDisplayedEvent
import javax.inject.Inject

/**
 * Mapper to get the message to show when adding a tag.
 *
 * @property context   Context to get the resources
 */
class TagsValidationMessageMapper @Inject constructor(@ApplicationContext private val context: Context) {

    /**
     * Validate the tag and return a message and if it is an error.
     *
     * @param tag       Tag to validate
     * @param nodeTags  List of tags of the node
     * @param userTags  List of tags of the user
     */
    operator fun invoke(
        tag: String,
        nodeTags: List<String> = emptyList(),
        userTags: List<String> = emptyList(),
    ): Pair<String?, Boolean> {
        val message: String?
        val isError: Boolean
        val isBlank = tag.isBlank()
        when {
            isBlank && userTags.isEmpty() -> {
                message = context.getString(R.string.add_tags_label_tag_description)
                isError = false
            }

            !tag.matches(Regex(TAGS_REGEX)) -> {
                message = context.getString(R.string.add_tags_error_special_characters)
                isError = true
            }

            tag.length > TagsActivity.MAX_CHARS_PER_TAG -> {
                message = context.getString(R.string.add_tags_error_maximum_characters)
                isError = true
                Analytics.tracker.trackEvent(NodeInfoTagsLengthErrorDisplayedEvent)
            }

            nodeTags.size >= TagsActivity.MAX_TAGS_PER_NODE -> {
                message = context.getString(
                    R.string.add_tags_error_max_tags,
                    TagsActivity.MAX_TAGS_PER_NODE
                )
                isError = true
                Analytics.tracker.trackEvent(NodeInfoTagsLimitErrorDisplayedEvent)
            }

            else -> {
                message = null
                isError = false
            }
        }
        return Pair(message, isError)
    }

    companion object {

        /**
         * Regular expression to validate tags.
         * This regex ensures that the tag contains only letters, numbers, and diacritical marks, and nothing else.
         *
         *      ^: Asserts the position at the start of the string.
         *      [\\p{L}0-9\\p{Mn}\\p{Me}]*: A character class that matches any combination of:
         *          \\p{L}: Any kind of letter from any language.
         *          0-9: Any digit from 0 to 9.
         *          \\p{Mn}: A non-spacing mark (a character intended to be combined with another character without taking up extra space).
         *          \\p{Me}: An enclosing mark (a character that encloses the character it is combined with).
         *          *: Matches zero or more of the preceding element (the character class).
         *      $: Asserts the position at the end of the string.
         */
        private const val TAGS_REGEX =
            "^[\\p{L}0-9\\p{Mn}\\p{Me}]*$"
    }
}