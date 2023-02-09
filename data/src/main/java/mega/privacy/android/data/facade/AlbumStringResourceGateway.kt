package mega.privacy.android.data.facade

/**
 * Album String Resource Gateway
 */
interface AlbumStringResourceGateway {
    /**
     * Get all the system album names
     */
    fun getSystemAlbumNames(): List<String>

    /**
     * Get all the proscribed album names
     */
    fun getProscribedStrings(): List<String>
}