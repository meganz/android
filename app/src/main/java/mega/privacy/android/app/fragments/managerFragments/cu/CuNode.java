package mega.privacy.android.app.fragments.managerFragments.cu;

import android.util.Pair;

import java.io.File;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.Constants.INVALID_VALUE;

/**
 * Class used to manage Camera Uploads and Media Uploads content.
 */
public class CuNode {
    /**
     * Three different types of nodes.
     */
    public static final int TYPE_HEADER = 1;
    public static final int TYPE_IMAGE = 2;
    public static final int TYPE_VIDEO = 3;

    private final MegaNode mNode;
    private final int mIndexForViewer;
    private File mThumbnail;
    private final int mType;
    private final String mModifyDate;
    private final Pair<String, String> mHeaderDate;

    private boolean mSelected;

    /**
     * Creates a TYPE_IMAGE or TYPE_VIDEO CuNode.
     *
     * @param node           MegaNode representing the item.
     * @param indexForViewer Index needed on viewers to show the dismiss animation after a drag event.
     * @param thumbnail      Thumbnail or preview of the node if exists, null otherwise.
     * @param type           TYPE_IMAGE if photo, TYPE_VIDEO if video.
     * @param modifyDate     String containing the modified date of the node.
     * @param selected       True if the node is selected on list, false otherwise.
     */
    public CuNode(MegaNode node, int indexForViewer, File thumbnail, int type,
                  String modifyDate, boolean selected) {
        mNode = node;
        mIndexForViewer = indexForViewer;
        mThumbnail = thumbnail;
        mType = type;
        mModifyDate = modifyDate;
        mSelected = selected;
        mHeaderDate = null;
    }

    /**
     * Creates a TYPE_HEADER CuNode.
     *
     * @param modifyDate String containing the complete header date.
     * @param headerDate Pair containing the text to show as header in adapter:
     *                   - First: Month.
     *                   - Second: Year if not current year, empty otherwise.
     */
    public CuNode(String modifyDate, Pair<String, String> headerDate) {
        mNode = null;
        mIndexForViewer = INVALID_VALUE;
        mThumbnail = null;
        mType = TYPE_HEADER;
        mModifyDate = modifyDate;
        mSelected = false;
        mHeaderDate = headerDate;
    }

    public MegaNode getNode() {
        return mNode;
    }

    public int getIndexForViewer() {
        return mIndexForViewer;
    }

    public void setThumbnail(File thumbnail) {
        mThumbnail = thumbnail;
    }

    public File getThumbnail() {
        return mThumbnail;
    }

    public int getType() {
        return mType;
    }

    public String getModifyDate() {
        return mModifyDate;
    }

    public Pair<String, String> getHeaderDate() {
        return mHeaderDate;
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean selected) {
        mSelected = selected;
    }
}
