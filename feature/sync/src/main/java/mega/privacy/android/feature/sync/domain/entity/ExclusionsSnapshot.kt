package mega.privacy.android.feature.sync.domain.entity

/**
 * A sync's exclusions as they were read, together with whether they can be written back.
 *
 * Writability is a property of the location rather than of the file's content, which is why it is
 * carried here instead of on [ParsedMegaIgnore]. It is also read in the same call as the rules
 * rather than offered as a second one, because a caller that forgets to ask is exactly how a
 * read-only root comes to be presented as editable and only fails once the user hits save.
 *
 * @property exclusions the rules as they stand in the file
 * @property isWritable whether the sync root would accept a write, so that a root granted read
 * only access can be shown as such before the user starts editing rather than after
 */
data class ExclusionsSnapshot(
    val exclusions: ParsedMegaIgnore,
    val isWritable: Boolean,
)
