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
    override val webUrlPattern: Pattern = Patterns.WEB_URL
}