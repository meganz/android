package mega.privacy.android.thirdpartylib.twemoji.wrapper

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.thirdpartylib.twemoji.EmojiManager
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import javax.inject.Inject

/**
 * Implementation of [EmojiManagerWrapper]
 */
class EmojiManagerWrapperImpl @Inject constructor(
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) : EmojiManagerWrapper {
    override suspend fun getFirstEmoji(text: String): Int? = withContext(dispatcher) {
        EmojiManager.getInstance().getFirstEmoji(text)?.resource
    }
}