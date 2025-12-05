package mega.privacy.android.icon.pack

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Interface defining the structure for the icon pack.
 * This interface should be implemented by the generated IconPack object.
 *
 * Developers should add the icon properties they need to this interface.
 * The generator will read this interface and create the implementation.
 * Once the icon is added to this interface, just build the icon-pack module and `IconPack` will be updated.
 *
 * The structure follows the pattern: Size -> Weight -> Style -> IconName
 * Where:
 * - Size: Small, Medium
 * - Weight: Regular, Thin
 * - Style: Outline, Solid
 * - IconName: The actual icon name (e.g., Check, ArrowUp, etc.)
 *
 * You can define multiple combinations for the same icon by creating
 * different object hierarchies in the interface.
 */
interface IconPackInterface {
    /**
     * small
     */
    interface Small {
        /**
         * small_regular
         */
        interface Regular {
            /**
             * small_regular_outline
             */
            interface Outline {}

            /**
             * small_regular_solid
             */
            interface Solid {}
        }

        /**
         * small_thin
         */
        interface Thin {
            /**
             * small_thin_outline
             */
            interface Outline {
                val Grid9: ImageVector
                val Square: ImageVector
                val TransferError: ImageVector
                val TransferWarning: ImageVector
                val TransferPause: ImageVector
                val TransferCheckCircle: ImageVector
                val ArrowUp: ImageVector
                val ArrowDown: ImageVector
                val QueueLine: ImageVector
                val Grid4: ImageVector
                val ListSmall: ImageVector
                val Image01: ImageVector
                val Squares4: ImageVector
                val ChevronDown: ImageVector
                val ChevronUp: ImageVector
                val Image04: ImageVector
                val Check: ImageVector
                val Film: ImageVector
                val Heart: ImageVector
                val ArrowDownCircle: ImageVector
                val MessageChatCircle: ImageVector
                val ClockRotate: ImageVector
            }

            /**
             * small_thin_solid
             */
            interface Solid {
                val Heart: ImageVector
            }
        }
    }

    /**
     * medium
     */
    interface Medium {
        /**
         * medium_regular
         */
        interface Regular {
            /**
             * medium_regular_outline
             */
            interface Outline {}

            /**
             * medium_regular_solid
             */
            interface Solid {}
        }

        /**
         * medium_thin
         */
        interface Thin {
            /**
             * medium_thin_outline
             */
            interface Outline {
                val AlertCircle: ImageVector
                val AlertTriangle: ImageVector
                val Archive: ImageVector
                val ArchiveArrowUp: ImageVector
                val ArrowDownCircle: ImageVector
                val ArrowLeft: ImageVector
                val ArrowsUpDown: ImageVector
                val ArrowsUpDownCircle: ImageVector
                val ArrowUpCircle: ImageVector
                val Bell: ImageVector
                val BellOff: ImageVector
                val CalendarArrowRight: ImageVector
                val Camera: ImageVector
                val Check: ImageVector
                val CheckCircle: ImageVector
                val CheckStack: ImageVector
                val ChevronRight: ImageVector
                val CircleSmall: ImageVector
                val ClockPlay: ImageVector
                val ClockRotate: ImageVector
                val ClockUser: ImageVector
                val Cloud: ImageVector
                val CloudOff: ImageVector
                val CloudUpload: ImageVector
                val CloudDownload: ImageVector
                val Copy01: ImageVector
                val CornerUpRight: ImageVector
                val Database: ImageVector
                val Devices: ImageVector
                val Download: ImageVector
                val Edit: ImageVector
                val EmojiSmile: ImageVector
                val Eraser: ImageVector
                val ExternalLink: ImageVector
                val Eye: ImageVector
                val EyeOff: ImageVector
                val File02: ImageVector
                val FilePlus02: ImageVector
                val fileQuestion01: ImageVector
                val FileScan: ImageVector
                val FileSearch02: ImageVector
                val FileUpload: ImageVector
                val Folder: ImageVector
                val FolderArrow: ImageVector
                val FolderGear01: ImageVector
                val FolderIncoming: ImageVector
                val FolderOpen: ImageVector
                val FolderPlus01: ImageVector
                val FolderUsers: ImageVector
                val Filter: ImageVector
                val GearSix: ImageVector
                val Gif: ImageVector
                val HardDrive: ImageVector
                val Heart: ImageVector
                val HeartBroken: ImageVector
                val HelpCircle: ImageVector
                val Home: ImageVector
                val Image01: ImageVector
                val Images: ImageVector
                val Info: ImageVector
                val Key02: ImageVector
                val Link01: ImageVector
                val Link02: ImageVector
                val LinkOff01: ImageVector
                val LoaderGrad: ImageVector
                val Lock: ImageVector
                val LockKeyholeCircle: ImageVector
                val LogOut02: ImageVector
                val MapPin: ImageVector
                val Maximize02: ImageVector
                val Mega: ImageVector
                val Menu01: ImageVector
                val Menu04: ImageVector
                val MessageArrowUp: ImageVector
                val MessageChatCircle: ImageVector
                val Minimize02: ImageVector
                val MinusCircle: ImageVector
                val MoreVertical: ImageVector
                val Move: ImageVector
                val Pause: ImageVector
                val Pen2: ImageVector
                val Phone01: ImageVector
                val Play: ImageVector
                val PlaySquare: ImageVector
                val Plus: ImageVector
                val PlusCircle: ImageVector
                val RectangleImageStack: ImageVector
                val RectangleStackPlus: ImageVector
                val Rocket: ImageVector
                val RotateCcw: ImageVector
                val RotateCw: ImageVector
                val Screenshot: ImageVector
                val SearchLarge: ImageVector
                val SearchSmall: ImageVector
                val ShareNetwork: ImageVector
                val Shield: ImageVector
                val ShieldInfo: ImageVector
                val SlashCircle: ImageVector
                val SlidersHorizontal01: ImageVector
                val SlidersVertical02: ImageVector
                val Square: ImageVector
                val Star: ImageVector
                val Sync01: ImageVector
                val TagSimple: ImageVector
                val TransferArrowsUpDownAlt: ImageVector
                val TransferArrowsUpDownAltCircleCutout: ImageVector
                val TransferArrowsUpDownAltTriangleCutout: ImageVector
                val Trash: ImageVector
                val User: ImageVector
                val UserPlus: ImageVector
                val UserRight: ImageVector
                val Users: ImageVector
                val UserSquare: ImageVector
                val Video: ImageVector
                val VideoJoin: ImageVector
                val VideoPlus: ImageVector
                val VPN: ImageVector
                val X: ImageVector
                val XCircle: ImageVector
                val Zap: ImageVector
                val ZapAuto: ImageVector
                val ZapOff: ImageVector
                val ZoomIn: ImageVector
            }

            /**
             * medium_thin_solid
             */
            interface Solid {
                val CheckCircle: ImageVector
                val CheckSquare: ImageVector
                val EmojiSmile: ImageVector
                val Eye: ImageVector
                val Folder: ImageVector
                val Heart: ImageVector
                val Home: ImageVector
                val Image01: ImageVector
                val Link01: ImageVector
                val Menu01: ImageVector
                val Phone01: ImageVector
                val PlayCircle: ImageVector
            }
        }
    }
}
