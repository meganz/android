package mega.privacy.android.data.repository

import android.util.Patterns
import mega.privacy.android.domain.repository.RegexRepository
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Regex repository
 *
 */
class RegexRepositoryImpl @Inject constructor() : RegexRepository {
    companion object {
        /**
         * Regex for invalid name pattern
         */
        private const val INVALID_NAME_REGEX = "[*|\\?:\"<>\\\\\\\\/]"
    }

    override val webUrlPattern: Pattern = Patterns.WEB_URL

    override val invalidNamePattern: Pattern = Pattern.compile(INVALID_NAME_REGEX)
}