package mega.privacy.android.app.presentation.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Mapper to get String from StringRes
 */
class GetPluralStringFromStringResMapper @Inject constructor(@ApplicationContext private val context: Context) {
    /**
     * Invoke
     *
     * @param stringId StringRes to convert ot string
     */
    operator fun invoke(stringId: Int, quantity: Int, vararg args: Any) =
        context.resources.getQuantityString(stringId, quantity, *args)
}