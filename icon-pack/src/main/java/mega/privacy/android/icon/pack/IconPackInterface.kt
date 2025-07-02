package mega.privacy.android.icon.pack

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Interface defining the structure for the icon pack.
 * This interface should be implemented by the generated IconPack object.
 *
 * Developers should add the icon properties they need to this interface.
 * The generator will read this interface and create the implementation.
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
            }

            interface Solid {
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
                val PasswordManager: ImageVector
            }

            interface Solid {

                val CheckSquare: ImageVector
            }
        }
    }
}
