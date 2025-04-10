package mega.privacy.android.feature.sync.domain.exception

/**
 * Backup already exists exception when creating a new backup
 */
class BackupAlreadyExistsException : RuntimeException("Backup already exists")