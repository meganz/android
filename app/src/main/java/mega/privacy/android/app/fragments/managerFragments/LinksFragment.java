package mega.privacy.android.app.fragments.managerFragments;

import android.widget.ImageView;

import java.util.ArrayList;

import mega.privacy.android.app.fragments.MegaNodeBaseFragment;
import nz.mega.sdk.MegaNode;

public class LinksFragment extends MegaNodeBaseFragment {

    public static LinksFragment newInstance() {
        return new LinksFragment();
    }

    @Override
    protected void setNodes(ArrayList<MegaNode> nodes) {

    }

    @Override
    protected void setEmptyView() {

    }

    @Override
    protected int onBackPressed() {
        return 0;
    }

    @Override
    protected void itemClick(int position, int[] screenPosition, ImageView imageView) {

    }

    @Override
    protected void refresh() {

    }
}
