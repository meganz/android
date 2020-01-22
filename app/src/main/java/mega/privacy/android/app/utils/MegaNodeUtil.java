package mega.privacy.android.app.utils;

import android.content.Context;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import mega.privacy.android.app.R;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.Util.*;

public class MegaNodeUtil {

    /**
     * The method to calculate how many nodes are folders in array list
     * @param nodes the nodes to be calculated
     * @return how many nodes are folders in array list
     */
    public static int getNumberOfFolders(ArrayList<MegaNode> nodes) {

        int folderCount = 0;

        if (nodes == null) return folderCount;

        CopyOnWriteArrayList<MegaNode> safeList = new CopyOnWriteArrayList(nodes);

        for (MegaNode node : safeList) {
            if (node == null) {
                safeList.remove(node);
            } else if (node.isFolder()) {
                folderCount++;
            }
        }

        nodes = new ArrayList<>(safeList);
        return folderCount;
    }

    /**
     * @param node the detected node
     * @return whether the node is taken down
     */
    public static boolean isNodeTakenDown(MegaNode node) {
        return node != null && node.isTakenDown();
    }


    /**
     * @param node if the node is taken down, show the alert dialog
     * @return whether show the Taken down dialog for the mega node or not
     */
    public static boolean showTakenDownDialog(MegaNode node, Context context) {
        if (isNodeTakenDown(node)) {
            showSnackBar(context, SNACKBAR_TYPE, context.getString(R.string.error_download_takendown_node), -1);
            return true;
        } else {
            return false;
        }
    }
}
