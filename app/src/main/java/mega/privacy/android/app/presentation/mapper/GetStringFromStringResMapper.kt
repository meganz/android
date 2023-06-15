package mega.privacy.android.app.presentation.mapper

import android.content.Context
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Mapper to get String from StringRes
 */
class GetStringFromStringResMapper @Inject constructor(@ApplicationContext private val context: Context) {
    /**
     * Invoke
     *
     * @param stringId StringRes to convert ot string
     */
    operator fun invoke(@StringRes stringId: Int, vararg args: Any) =
        context.getString(stringId, *args)
}