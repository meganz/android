package mega.privacy.android.app.presentation.testpassword

/**
 * Interface for Recovery Key Actions
 */
interface BackupRecoveryKeyAction {
    /**
     * Print Recovery Key
     */
    fun print()

    /**
     * Copy Recovery Key to Clipboard
     */
    fun copyToClipboard()

    /**
     * Save Recovery Key to a file
     */
    fun saveToFile()
}