package mega.privacy.android.domain.usecase.photos

import javax.inject.Inject

/**
 * Get next default album name use case
 *
 */
class GetNextDefaultAlbumNameUseCase @Inject constructor() {
    /**
     * Invoke
     *
     * @param defaultName
     * @param currentNames
     * @return next default name
     */
    operator fun invoke(defaultName: String, currentNames: List<String>): String {
        val firstMissingIndex = currentNames.mapNotNull {
            getDefaultIndex(it, defaultName)
        }.sorted().distinct()
            .mapIndexed { index, i -> index to i }
            .lastOrNull { it.first == it.second }
            ?.first?.plus(1)

        return firstMissingIndex?.let { "$defaultName ($it)" } ?: defaultName
    }

    private fun getDefaultIndex(
        it: String,
        defaultName: String,
    ): Int? {
        val regex = Regex("$defaultName \\((\\d*)\\)")
        return if (it.trim() == defaultName) 0 else regex.matchEntire(it)?.groupValues?.get(1)
            ?.toIntOrNull()
    }
}