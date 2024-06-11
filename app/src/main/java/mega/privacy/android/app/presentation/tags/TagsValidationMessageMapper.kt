package mega.privacy.android.app.presentation.tags

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.shared.resources.R
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

            isBlank -> {
                message = null
                isError = false
            }

            tag.all { it.isLetterOrDigit() }.not() -> {
                message = context.getString(R.string.add_tags_error_special_characters)
                isError = true
            }

            tag.length > TagsActivity.MAX_CHARS_PER_TAG -> {
                message = context.getString(
                    R.string.add_tags_error_max_characters,
                    TagsActivity.MAX_CHARS_PER_TAG
                )
                isError = true
            }

            nodeTags.size >= TagsActivity.MAX_TAGS_PER_NODE
                    && userTags.contains(tag).not() -> {
                message = context.getString(
                    R.string.add_tags_error_max_tags,
                    TagsActivity.MAX_TAGS_PER_NODE
                )
                isError = true
            }

            else -> {
                message = null
                isError = false
            }
        }
        return Pair(message, isError)
    }
}