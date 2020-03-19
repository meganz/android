package mega.privacy.android.app.utils;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static nz.mega.sdk.MegaApiJava.*;
public class SortUtil {

    public static void sortByMailDescending(ArrayList<MegaNode> nodes) {
        ArrayList<MegaNode> folderNodes = new ArrayList<>();
        ArrayList<MegaNode> fileNodes = new ArrayList<>();

        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i) == null) {
                continue;
            }
            if (nodes.get(i).isFolder()) {
                folderNodes.add(nodes.get(i));
            } else {
                fileNodes.add(nodes.get(i));
            }
        }

        Collections.reverse(folderNodes);
        Collections.reverse(fileNodes);

        nodes.clear();
        nodes.addAll(folderNodes);
        nodes.addAll(fileNodes);
    }

    public static void sortByNameDescending(ArrayList<MegaNode> nodes) {
        sort(nodes, true);
    }

    public static void sortByNameAscending(ArrayList<MegaNode> nodes) {
        sort(nodes, false);
    }

    private static void sort(ArrayList<MegaNode> nodes, boolean reverse) {
        ArrayList<String> foldersOrder = new ArrayList<>();
        ArrayList<String> filesOrder = new ArrayList<>();
        ArrayList<MegaNode> tempNodes = new ArrayList<>();


        for (int k = 0; k < nodes.size(); k++) {
            MegaNode node = nodes.get(k);
            if (node == null) {
                continue;
            }
            if (node.isFolder()) {
                foldersOrder.add(node.getName());
            } else {
                filesOrder.add(node.getName());
            }
        }


        Collections.sort(foldersOrder, String.CASE_INSENSITIVE_ORDER);
        Collections.sort(filesOrder, String.CASE_INSENSITIVE_ORDER);
        if (reverse) {
            Collections.reverse(foldersOrder);
            Collections.reverse(filesOrder);
        }

        for (int k = 0; k < foldersOrder.size(); k++) {
            for (int j = 0; j < nodes.size(); j++) {
                String name = foldersOrder.get(k);
                if (nodes.get(j) == null) {
                    continue;
                }
                String nameNode = nodes.get(j).getName();
                if (name.equals(nameNode)) {
                    tempNodes.add(nodes.get(j));
                }
            }

        }

        for (int k = 0; k < filesOrder.size(); k++) {
            for (int j = 0; j < nodes.size(); j++) {
                String name = filesOrder.get(k);
                if (nodes.get(j) == null) {
                    continue;
                }
                String nameNode = nodes.get(j).getName();
                if (name.equals(nameNode)) {
                    tempNodes.add(nodes.get(j));
                }
            }

        }

        nodes.clear();
        nodes.addAll(tempNodes);
    }

    public static void sortOfflineByNameDescending(ArrayList<MegaOffline> mOffList) {
        sort(ORDER_DEFAULT_DESC, mOffList);
    }


    public static void sortOfflineByNameAscending(ArrayList<MegaOffline> mOffList) {
        sort(ORDER_DEFAULT_ASC, mOffList);
    }

    public static void sortOfflineByModificationDateAscending(ArrayList<MegaOffline> mOffList) {
        sort(ORDER_MODIFICATION_ASC, mOffList);
    }

    public static void sortOfflineByModificationDateDescending(ArrayList<MegaOffline> mOffList) {
        sort(ORDER_MODIFICATION_DESC, mOffList);
    }

    public static void sortOfflineBySizeAscending(ArrayList<MegaOffline> mOffList) {
        sort(ORDER_SIZE_ASC, mOffList);
    }

    public static void sortOfflineBySizeDescending(ArrayList<MegaOffline> mOffList) {
        sort(ORDER_SIZE_DESC, mOffList);
    }




    /**
     * sort the list of MegaOffline Node according to different order
     *
     * @param order    the passed order
     * @param mOffList the list required to be sorted
     */
    public static void sort(int order, ArrayList<MegaOffline> mOffList) {
        final Context context = MegaApplication.getInstance();
        ArrayList<MegaOffline> foldersOrder = new ArrayList<>();
        ArrayList<MegaOffline> filesOrder = new ArrayList<>();
        ArrayList<MegaOffline> tempOffline = new ArrayList<>();

        //Remove MK before sorting
        if (mOffList.size() > 0) {
            MegaOffline lastItem = mOffList.get(mOffList.size() - 1);
            if (lastItem.getHandle().equals("0")) {
                mOffList.remove(mOffList.size() - 1);
            }
        } else {
            return;
        }

        for (MegaOffline node : mOffList) {
            if (node.getType().equals(MegaOffline.FOLDER)) {
                foldersOrder.add(node);
            } else {
                filesOrder.add(node);
            }
        }

        Comparator<MegaOffline> modificationDateComparator = new Comparator<MegaOffline>() {
            @Override
            public int compare(MegaOffline o1, MegaOffline o2) {
                return Long.compare(o1.getModificationDate(context), o2.getModificationDate(context));
            }
        };

        Comparator<MegaOffline> sizeComparator = new Comparator<MegaOffline>() {
            @Override
            public int compare(MegaOffline o1, MegaOffline o2) {
                return Long.compare(o1.getSize(context), o2.getSize(context));
            }
        };

        Comparator<MegaOffline> nameComparator = new Comparator<MegaOffline>() {
            @Override
            public int compare(MegaOffline o1, MegaOffline o2) {
                String name1 = o1.getName();
                String name2 = o2.getName();
                if (isFileNameNumeric(name1) && isFileNameNumeric(name2)) {
                    try {
                        int pureInteger1 = Integer.parseInt(getFileNameWithoutExtension(name1));
                        int pureInteger2 = Integer.parseInt(getFileNameWithoutExtension(name2));
                        return pureInteger1 - pureInteger2;
                    } catch (Exception ex) {
                        logError("Exception happens" + ex.toString());
                    }
                }

                int n1 = name1.length(), n2 = name2.length();
                int n = n1 < n2 ? n1 : n2;
                for (int i = 0;
                     i < n;
                     i++) {
                    char c1 = name1.charAt(i);
                    char c2 = name2.charAt(i);
                    if (c1 != c2) {
                        if (c1 >= 'A' && c1 <= 'Z') { //Fast lower case
                            c1 = (char) (c1 | 0x20);
                        }
                        if (c2 >= 'A' && c2 <= 'Z') {
                            c2 = (char) (c2 | 0x20);
                        }
                        if (c1 != c2) {
                            return c1 - c2;
                        }
                    }
                }
                return n1 - n2;
            }
        };

        Comparator<MegaOffline> comparator = nameComparator;
        switch (order) {
            case ORDER_DEFAULT_ASC:
            case ORDER_DEFAULT_DESC: {
                comparator = nameComparator;
                break;
            }
            case ORDER_MODIFICATION_ASC:
            case ORDER_MODIFICATION_DESC: {
                comparator = modificationDateComparator;
                break;
            }
            case ORDER_SIZE_ASC:
            case ORDER_SIZE_DESC: {
                comparator = sizeComparator;
                break;
            }
            default: {
                break;
            }
        }

        Collections.sort(foldersOrder, nameComparator);

        Collections.sort(filesOrder, comparator);

        Boolean isDescending = false;
        switch (order) {
            case ORDER_DEFAULT_DESC:
            case ORDER_MODIFICATION_DESC:
            case ORDER_SIZE_DESC: {
                isDescending = true;
                break;
            }
            default: {
                break;
            }
        }

        if (isDescending) {
            if (order == ORDER_DEFAULT_DESC) {
                Collections.reverse(foldersOrder);
            }
            Collections.reverse(filesOrder);
        }

        tempOffline.addAll(foldersOrder);

        tempOffline.addAll(filesOrder);

        mOffList.clear();
        mOffList.addAll(tempOffline);
    }
}
