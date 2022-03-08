package mega.privacy.android.app.usecase.exception

import java.lang.IllegalArgumentException

/**
 * Class to manage exceptions related to MegaNodes.
 *
 * @property message    Error string.
 */
sealed class MegaNodeException(message: String) : IllegalArgumentException(message) {

    class ChildDoesNotExistsException : MegaNodeException("Child does not exist")
    class ParentDoesNotExistException : MegaNodeException("Parent does not exist")
}