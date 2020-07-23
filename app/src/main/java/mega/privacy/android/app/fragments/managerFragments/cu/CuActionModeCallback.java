package mega.privacy.android.app.fragments.managerFragments.cu;

import android.content.Context;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.appcompat.view.ActionMode;
import java.util.ArrayList;
import java.util.List;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil;
import mega.privacy.android.app.utils.MegaNodeUtil;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.Util.mutateIconSecondary;

class CuActionModeCallback implements ActionMode.Callback {

  private final Context context;
  private final CameraUploadsFragment fragment;
  private final CuViewModel viewModel;
  private final MegaApiAndroid megaApi;

  CuActionModeCallback(Context context, CameraUploadsFragment fragment,
      CuViewModel viewModel, MegaApiAndroid megaApi) {
    this.context = context;
    this.fragment = fragment;
    this.viewModel = viewModel;
    this.megaApi = megaApi;
  }

  @Override
  public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
    logDebug("onActionItemClicked");
    List<MegaNode> documents = viewModel.getSelectedNodes();
    if (documents.isEmpty()) {
      return false;
    }

    switch (item.getItemId()) {
      case R.id.cab_menu_download:
        viewModel.clearSelection();
        new NodeController(context)
            .prepareForDownload(getDocumentHandles(documents), false);
        break;
      case R.id.cab_menu_copy:
        viewModel.clearSelection();
        new NodeController(context)
            .chooseLocationToCopyNodes(getDocumentHandles(documents));
        break;
      case R.id.cab_menu_move:
        viewModel.clearSelection();
        new NodeController(context)
            .chooseLocationToMoveNodes(getDocumentHandles(documents));
        break;
      case R.id.cab_menu_share_out:
        viewModel.clearSelection();
        MegaNodeUtil.shareNodes(context, documents);
        break;
      case R.id.cab_menu_share_link:
      case R.id.cab_menu_edit_link:
        logDebug("Public link option");
        viewModel.clearSelection();
        if (documents.size() == 1
            && documents.get(0).getHandle() != MegaApiJava.INVALID_HANDLE) {
          ((ManagerActivityLollipop) context)
              .showGetLinkActivity(documents.get(0).getHandle());
        }
        break;
      case R.id.cab_menu_remove_link:
        logDebug("Remove public link option");
        viewModel.clearSelection();
        if (documents.size() == 1) {
          ((ManagerActivityLollipop) context)
              .showConfirmationRemovePublicLink(documents.get(0));
        }
        break;
      case R.id.cab_menu_send_to_chat:
        logDebug("Send files to chat");
        viewModel.clearSelection();
        new NodeController(context).checkIfNodesAreMineAndSelectChatsToSendNodes(
            (ArrayList<MegaNode>) documents);
        break;
      case R.id.cab_menu_trash:
        viewModel.clearSelection();
        ((ManagerActivityLollipop) context).askConfirmationMoveToRubbish(
            getDocumentHandles(documents));
        break;
      case R.id.cab_menu_select_all:
        fragment.selectAll();
        break;
      case R.id.cab_menu_clear_selection:
        viewModel.clearSelection();
        break;
    }
    return true;
  }

  /**
   * Get handles for selected nodes.
   *
   * @return handles for selected nodes.
   */
  private ArrayList<Long> getDocumentHandles(List<MegaNode> documents) {
    ArrayList<Long> handles = new ArrayList<>();

    for (MegaNode node : documents) {
      handles.add(node.getHandle());
    }

    return handles;
  }

  @Override
  public boolean onCreateActionMode(ActionMode mode, Menu menu) {
    logDebug("onCreateActionMode");
    MenuInflater inflater = mode.getMenuInflater();
    inflater.inflate(R.menu.cloud_storage_action, menu);
    ((ManagerActivityLollipop) context).showHideBottomNavigationView(true);
    Util.changeStatusBarColor(context, ((ManagerActivityLollipop) context).getWindow(),
        R.color.accentColorDark);
    ((ManagerActivityLollipop) context).setDrawerLockMode(true);
    fragment.checkScroll();
    return true;
  }

  @Override
  public void onDestroyActionMode(ActionMode mode) {
    logDebug("onDestroyActionMode");
    viewModel.clearSelection();
    ((ManagerActivityLollipop) context).showHideBottomNavigationView(false);
    Util.changeStatusBarColor(context, ((ManagerActivityLollipop) context).getWindow(),
        R.color.black);
    fragment.checkScroll();
    ((ManagerActivityLollipop) context).setDrawerLockMode(false);
  }

  @Override
  public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
    logDebug("onPrepareActionMode");
    List<MegaNode> selected = viewModel.getSelectedNodes();
    if (selected.isEmpty()) {
      return false;
    }

    CloudStorageOptionControlUtil.Control control =
        new CloudStorageOptionControlUtil.Control();

    if (selected.size() == 1
        && megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_OWNER).getErrorCode()
        == MegaError.API_OK) {
      if (selected.get(0).isExported()) {
        control.manageLink().setVisible(true)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        control.removeLink().setVisible(true);
      } else {
        control.getLink().setVisible(true)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
      }
    }

    menu.findItem(R.id.cab_menu_send_to_chat)
        .setIcon(mutateIconSecondary(context, R.drawable.ic_send_to_contact,
            R.color.white));

    control.sendToChat().setVisible(true)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

    control.shareOut().setVisible(true)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

    control.trash().setVisible(MegaNodeUtil.canMoveToRubbish(selected));

    control.move().setVisible(true);
    control.copy().setVisible(true);
    if (selected.size() > 1) {
      control.move().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    control.selectAll().setVisible(selected.size() < viewModel.getRealNodesCount() + 1);

    CloudStorageOptionControlUtil.applyControl(menu, control);

    return true;
  }
}
