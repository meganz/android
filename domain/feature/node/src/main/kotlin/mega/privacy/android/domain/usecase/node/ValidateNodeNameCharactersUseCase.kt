package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.exception.DotNameException
import mega.privacy.android.domain.exception.DoubleDotNameException
import mega.privacy.android.domain.exception.EmptyNodeNameException
import mega.privacy.android.domain.exception.InvalidNodeNameException
import mega.privacy.android.domain.repository.RegexRepository
import mega.privacy.android.domain.usecase.node.CheckForValidNameUseCase.Companion.isInvalidDotName
import mega.privacy.android.domain.usecase.node.CheckForValidNameUseCase.Companion.isInvalidDoubleDotName
import javax.inject.Inject

/**
 * Validates the characters of a node name, independently of the parent it will be created under.
 *
 * Names also reach the SDK from surfaces that are not the app's own dialogs - the SAF
 * DocumentsProvider exposes createDocument and renameDocument to any app holding a write URI grant.
 * A name carrying a path separator, "." / ".." or a control character is consumed as a local path
 * segment by this client and by every other client that syncs the account, so it has to be rejected
 * on the way in rather than at each sink.
 */
class ValidateNodeNameCharactersUseCase @Inject constructor(
    private val regexRepository: RegexRepository,
) {

    /**
     * invoke
     *
     * @param name Name of the node to validate
     * @throws EmptyNodeNameException if the name is empty
     * @throws DotNameException if the name is "."
     * @throws DoubleDotNameException if the name is ".."
     * @throws InvalidNodeNameException if the name contains invalid or control characters
     */
    operator fun invoke(name: String) {
        when {
            name.isEmpty() -> throw EmptyNodeNameException()
            name.isInvalidDotName() -> throw DotNameException()
            name.isInvalidDoubleDotName() -> throw DoubleDotNameException()
            regexRepository.invalidNamePattern.matcher(name).find() ->
                throw InvalidNodeNameException()
        }
    }
}
