package mega.privacy.android.domain.exception.mapper

/**
 * Unknown mapper parameter exception
 *
 * @param mapper the mapper that threw the exception
 * @param input the incorrect/unknown input provided
 */
class UnknownMapperParameterException(mapper: String?, input: String?): Throwable("Mapper [$mapper] called with unknown input: [$input]")