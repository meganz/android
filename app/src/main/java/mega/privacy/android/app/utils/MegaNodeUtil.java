package mega.privacy.android.app.utils;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import nz.mega.sdk.MegaNode;

public class MegaNodeUtil {

    /*
     * The method to calculate how many nodes are folders in array list
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

    public static boolean isNodeTakenDown(MegaNode node) {
        return node != null && node.isTakenDown();
    }
}
