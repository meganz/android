//
// Generated automatically by GenerateIconVectors.
// Do not modify this file manually.
//
// This file contains the main IconPack object that implements IconPackInterface.
// Each icon property references a function in a separate file.
//
package mega.privacy.android.icon.pack

import androidx.compose.ui.graphics.vector.ImageVector
import mega.privacy.android.icon.pack.vectors.createMediumRegularOutlineEraserImageVector
import mega.privacy.android.icon.pack.vectors.createMediumThinOutlineCheckStackImageVector
import mega.privacy.android.icon.pack.vectors.createMediumThinOutlineEraserImageVector
import mega.privacy.android.icon.pack.vectors.createMediumThinOutlineExternalLinkImageVector
import mega.privacy.android.icon.pack.vectors.createMediumThinOutlineFileSearch02ImageVector
import mega.privacy.android.icon.pack.vectors.createMediumThinOutlineLink01ImageVector
import mega.privacy.android.icon.pack.vectors.createMediumThinOutlineMinusCircleImageVector
import mega.privacy.android.icon.pack.vectors.createMediumThinOutlineMoreVerticalImageVector
import mega.privacy.android.icon.pack.vectors.createMediumThinOutlinePauseImageVector
import mega.privacy.android.icon.pack.vectors.createMediumThinOutlinePlayImageVector
import mega.privacy.android.icon.pack.vectors.createMediumThinOutlineRotateCcwImageVector
import mega.privacy.android.icon.pack.vectors.createMediumThinOutlineSquareImageVector
import mega.privacy.android.icon.pack.vectors.createMediumThinOutlinefileQuestion01ImageVector
import mega.privacy.android.icon.pack.vectors.createMediumThinSolidCheckSquareImageVector
import mega.privacy.android.icon.pack.vectors.createSmallRegularOutlineImage01ImageVector
import mega.privacy.android.icon.pack.vectors.createSmallThinOutlineArrowDownImageVector
import mega.privacy.android.icon.pack.vectors.createSmallThinOutlineArrowUpImageVector
import mega.privacy.android.icon.pack.vectors.createSmallThinOutlineQueueLineImageVector

public object IconPack : IconPackInterface {
    public object Medium : IconPackInterface.Medium {
        public object Regular : IconPackInterface.Medium.Regular {
            public object Outline : IconPackInterface.Medium.Regular.Outline {
                override val Eraser: ImageVector by lazy {
                        createMediumRegularOutlineEraserImageVector() }
            }
        }

        public object Thin : IconPackInterface.Medium.Thin {
            public object Outline : IconPackInterface.Medium.Thin.Outline {
                override val CheckStack: ImageVector by lazy {
                        createMediumThinOutlineCheckStackImageVector() }

                override val Eraser: ImageVector by lazy {
                        createMediumThinOutlineEraserImageVector() }

                override val ExternalLink: ImageVector by lazy {
                        createMediumThinOutlineExternalLinkImageVector() }

                override val FileSearch02: ImageVector by lazy {
                        createMediumThinOutlineFileSearch02ImageVector() }

                override val Link01: ImageVector by lazy {
                        createMediumThinOutlineLink01ImageVector() }

                override val MinusCircle: ImageVector by lazy {
                        createMediumThinOutlineMinusCircleImageVector() }

                override val MoreVertical: ImageVector by lazy {
                        createMediumThinOutlineMoreVerticalImageVector() }

                override val Pause: ImageVector by lazy { createMediumThinOutlinePauseImageVector()
                        }

                override val Play: ImageVector by lazy { createMediumThinOutlinePlayImageVector() }

                override val RotateCcw: ImageVector by lazy {
                        createMediumThinOutlineRotateCcwImageVector() }

                override val Square: ImageVector by lazy {
                        createMediumThinOutlineSquareImageVector() }

                override val fileQuestion01: ImageVector by lazy {
                        createMediumThinOutlinefileQuestion01ImageVector() }
            }

            public object Solid : IconPackInterface.Medium.Thin.Solid {
                override val CheckSquare: ImageVector by lazy {
                        createMediumThinSolidCheckSquareImageVector() }
            }
        }
    }

    public object Small : IconPackInterface.Small {
        public object Regular : IconPackInterface.Small.Regular {
            public object Outline : IconPackInterface.Small.Regular.Outline {
                override val Image01: ImageVector by lazy {
                        createSmallRegularOutlineImage01ImageVector() }
            }
        }

        public object Thin : IconPackInterface.Small.Thin {
            public object Outline : IconPackInterface.Small.Thin.Outline {
                override val ArrowDown: ImageVector by lazy {
                        createSmallThinOutlineArrowDownImageVector() }

                override val ArrowUp: ImageVector by lazy {
                        createSmallThinOutlineArrowUpImageVector() }

                override val QueueLine: ImageVector by lazy {
                        createSmallThinOutlineQueueLineImageVector() }
            }
        }
    }
}
