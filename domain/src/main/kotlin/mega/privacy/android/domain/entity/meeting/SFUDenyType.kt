package mega.privacy.android.domain.entity.meeting

/**
 *  SFU Deny type
 */
enum class SFUDenyType {
    /**
     *  Invalid command
     */
    Invalid,

    /**
     *  Av command denied by SFU (enable/disable audio video)
     */
    Audio,

    /**
     *  JOIN command denied by SFU
     */
    Join,

    /**
     *  Unknown
     */
    Unknown,

}