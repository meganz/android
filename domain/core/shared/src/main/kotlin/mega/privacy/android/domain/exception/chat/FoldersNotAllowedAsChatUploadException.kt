package mega.privacy.android.domain.exception.chat

/**
 * Folders not allowed as chat upload exception
 */
class FoldersNotAllowedAsChatUploadException :
    IllegalArgumentException("Folders not allowed as chat upload, please add files only")