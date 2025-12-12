package mega.privacy.android.core.nodecomponents.model

/**
 * Constants for Node Source Types represented as Int
 * This is temporary solution to avoid changing all the code that uses Int to [mega.privacy.android.domain.entity.node.NodeSourceType]
 */
object NodeSourceTypeInt {
    const val FILE_BROWSER_ADAPTER: Int = 2000
    const val RUBBISH_BIN_ADAPTER: Int = 2002
    const val OUTGOING_SHARES_ADAPTER: Int = 2009
    const val INCOMING_SHARES_ADAPTER: Int = 2010
    const val BACKUPS_ADAPTER: Int = 2011
    const val LINKS_ADAPTER: Int = 2025
    const val AUDIO_BROWSE_ADAPTER: Int = 2028
    const val DOCUMENTS_BROWSE_ADAPTER: Int = 2030
    const val FAVOURITES_ADAPTER: Int = 2039
    const val SEARCH_BY_ADAPTER: Int = 2018
    const val VIDEO_BROWSE_ADAPTER: Int = 2032
}
