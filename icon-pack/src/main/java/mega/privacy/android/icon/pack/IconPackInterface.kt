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
    interface Small {
        interface Regular {
            interface Outline {
                val Grid4: ImageVector
                val ListSmall: ImageVector
                val Image01: ImageVector
            }

            interface Solid {
            }
        }

        interface Thin {
            interface Outline {
                val ArrowUp: ImageVector
                val ArrowDown: ImageVector
                val QueueLine: ImageVector
            }

            interface Solid {

            }
        }
    }

    interface Medium {
        interface Regular {
            interface Outline {
                val EmojiSmile: ImageVector
                val Play: ImageVector
                val Pause: ImageVector
                val FolderPlus01: ImageVector
                val FolderGear01: ImageVector
                val FolderOpen: ImageVector
                val ZoomIn: ImageVector
                val ExternalLink: ImageVector
                val FolderIncoming: ImageVector
                val UserRight: ImageVector
                val RotateCcw: ImageVector
                val RectangleStackPlus: ImageVector
                val Eye: ImageVector
                val EyeOff: ImageVector
                val CornerUpRight: ImageVector
                val TagSimple: ImageVector
                val Heart: ImageVector
                val HeartBroken: ImageVector
                val AlertCircle: ImageVector
                val File02: ImageVector
                val ClockUser: ImageVector
                val Users: ImageVector
                val CalendarArrowRight: ImageVector
                val UserPlus: ImageVector
                val Menu04: ImageVector
                val Cloud: ImageVector
                val ClockRotate: ImageVector
                val Minimize02: ImageVector
                val Maximize02: ImageVector
                val Lock: ImageVector
                val Screenshot: ImageVector
                val ArrowLeft: ImageVector
                val Menu01: ImageVector
                val SearchLarge: ImageVector
                val Edit: ImageVector
                val Archive: ImageVector
                val BellOff: ImageVector
                val Bell: ImageVector
                val VideoPlus: ImageVector
                val ArchiveArrowUp: ImageVector
                val MessageChatCircle: ImageVector
                val Database: ImageVector
                val Sync01: ImageVector
                val FilePlus02: ImageVector
                val Camera: ImageVector
                val FileScan: ImageVector
                val FolderArrow: ImageVector
                val FileUpload: ImageVector
                val RotateCw: ImageVector
                val XCircle: ImageVector
                val SlashCircle: ImageVector
                val CheckCircle: ImageVector
                val ZapAuto: ImageVector
                val Zap: ImageVector
                val ZapOff: ImageVector
                val SlidersVertical02: ImageVector
                val ClockPlay: ImageVector
                val ArrowDownCircle: ImageVector
                val CloudUpload: ImageVector
                val Link01: ImageVector
                val ShareNetwork: ImageVector
                val Video: ImageVector
                val Phone01: ImageVector
                val ShieldInfo: ImageVector
                val GearSix: ImageVector
                val LogOut02: ImageVector
                val Trash: ImageVector
                val Copy01: ImageVector
                val Move: ImageVector
                val Pen2: ImageVector
                val MessageArrowUp: ImageVector
                val FolderUsers: ImageVector
                val LinkOff01: ImageVector
                val AlertTriangle: ImageVector
                val ChevronRight: ImageVector
                val HelpCircle: ImageVector
                val CircleSmall: ImageVector
                val MoreVertical: ImageVector
                val X: ImageVector
                val Key02: ImageVector
                val Folder: ImageVector
                val Eraser: ImageVector
                val Info: ImageVector
                val Check: ImageVector
                val Download: ImageVector
            }

            interface Solid {
                val EmojiSmile: ImageVector
                val PlayCircle: ImageVector
                val Eye: ImageVector
                val Link01: ImageVector
                val Heart: ImageVector
                val CheckCircle: ImageVector

            }
        }

        interface Thin {
            interface Outline {
                val Trash: ImageVector
                val Square: ImageVector
                val ExternalLink: ImageVector
                val FileSearch02: ImageVector
                val CheckStack: ImageVector
                val MinusCircle: ImageVector
                val Eraser: ImageVector
                val MoreVertical: ImageVector
                val Play: ImageVector
                val Pause: ImageVector
                val RotateCcw: ImageVector
                val Link01: ImageVector
                val fileQuestion01: ImageVector
                val Cloud: ImageVector
                val ArrowsUpDown: ImageVector
                val VPN: ImageVector
                val LockKeyholeCircle: ImageVector
            }

            interface Solid {

                val CheckSquare: ImageVector
            }
        }
    }
}
