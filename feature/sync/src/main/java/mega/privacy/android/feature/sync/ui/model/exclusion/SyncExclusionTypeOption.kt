package mega.privacy.android.feature.sync.ui.model.exclusion

/**
 * The four entries of the Add exclusion "Type" sheet.
 *
 * [Extensions] is a mode of the form rather than a node type: the SDK grammar has no extension
 * target, so picking it swaps the by-name body for extension entry.
 */
internal enum class SyncExclusionTypeOption {
    Files,
    Folders,
    FilesAndFolders,
    Extensions,
}
