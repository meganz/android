package mega.privacy.android.app.presentation.zipbrowser.model

/**
 * The type of zip item clicked
 */
enum class ZipItemClickedEventType {

    /**
     * Event: Open folder
     */
    OpenFolder,

    /**
     * Event: Zip file is not unpacked
     */
    ZipFileNotUnpacked,

    /**
     * Event: Zip item doesn't exist
     */
    ZipItemNonExistent,

    /**
     * Event: Open file
     */
    OpenFile
}