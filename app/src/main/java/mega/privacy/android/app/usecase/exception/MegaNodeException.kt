package mega.privacy.android.app.usecase.exception

import java.lang.IllegalArgumentException

/**
 * Class to manage exceptions related to MegaNodes.
 *
 * @property message    Error string.
 */
sealed class MegaNodeException(message: String) : IllegalArgumentException(message) {

    class ChildAlreadyExistsException : MegaNodeException("Child already exists")
    class ParentDoesNotExistException : MegaNodeException("Parent does not exists")
}