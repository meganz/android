package mega.privacy.android.app.utils;

import android.view.Menu;
import android.view.MenuItem;

import java.util.Arrays;
import java.util.List;

import mega.privacy.android.app.R;

public class CloudStorageOptionControlUtil {
  public static final int MAX_ACTION_COUNT = 4;

  public static class Option {
    private boolean visible;
    private int showAsAction;

    public Option(boolean visible, int showAsAction) {
      this.visible = visible;
      this.showAsAction = showAsAction;
    }

    public Option(boolean visible) {
      this(visible, MenuItem.SHOW_AS_ACTION_NEVER);
    }

    public boolean isVisible() {
      return visible;
    }

    public int getShowAsAction() {
      return showAsAction;
    }

    public Option setVisible(boolean visible) {
      this.visible = visible;
      return this;
    }

    public Option setShowAsAction(int showAsAction) {
      this.showAsAction = showAsAction;
      return this;
    }
  }

  public static void applyControl(Menu menu, Control control) {
    menu.findItem(R.id.cab_menu_select_all).setVisible(control.selectAll.visible);
    menu.findItem(R.id.cab_menu_select_all).setShowAsAction(control.selectAll.showAsAction);

    menu.findItem(R.id.cab_menu_clear_selection).setVisible(control.clearSelection.visible);
    menu.findItem(R.id.cab_menu_clear_selection)
        .setShowAsAction(control.clearSelection.showAsAction);

    menu.findItem(R.id.cab_menu_hide).setVisible(control.hide.visible);
    menu.findItem(R.id.cab_menu_hide).setShowAsAction(control.hide.showAsAction);

    menu.findItem(R.id.cab_menu_unhide).setVisible(control.unhide.visible);
    menu.findItem(R.id.cab_menu_unhide).setShowAsAction(control.unhide.showAsAction);

    menu.findItem(R.id.cab_menu_remove_link).setVisible(control.removeLink.visible);
    menu.findItem(R.id.cab_menu_remove_link).setShowAsAction(control.removeLink.showAsAction);

    menu.findItem(R.id.cab_menu_remove_share).setVisible(control.removeShare.visible);
    menu.findItem(R.id.cab_menu_remove_share).setShowAsAction(control.removeShare.showAsAction);

    menu.findItem(R.id.cab_menu_rename).setVisible(control.rename.visible);
    menu.findItem(R.id.cab_menu_rename).setShowAsAction(control.rename.showAsAction);

    menu.findItem(R.id.cab_menu_download).setVisible(control.saveToDevice.visible);
    menu.findItem(R.id.cab_menu_download).setShowAsAction(control.saveToDevice.showAsAction);

    menu.findItem(R.id.cab_menu_share_link).setVisible(control.getLink.visible);
    menu.findItem(R.id.cab_menu_share_link).setShowAsAction(control.getLink.showAsAction);

    menu.findItem(R.id.cab_menu_edit_link).setVisible(control.manageLink.visible);
    menu.findItem(R.id.cab_menu_edit_link).setShowAsAction(control.manageLink.showAsAction);

    menu.findItem(R.id.cab_menu_share_folder).setVisible(control.shareFolder.visible);
    menu.findItem(R.id.cab_menu_share_folder).setShowAsAction(control.shareFolder.showAsAction);

    menu.findItem(R.id.cab_menu_send_to_chat).setVisible(control.sendToChat.visible);
    menu.findItem(R.id.cab_menu_send_to_chat).setShowAsAction(control.sendToChat.showAsAction);

    menu.findItem(R.id.cab_menu_share_out).setVisible(control.shareOut.visible);
    menu.findItem(R.id.cab_menu_share_out).setShowAsAction(control.shareOut.showAsAction);

    menu.findItem(R.id.cab_menu_move).setVisible(control.move.visible);
    menu.findItem(R.id.cab_menu_move).setShowAsAction(control.move.showAsAction);

    menu.findItem(R.id.cab_menu_copy).setVisible(control.copy.visible);
    menu.findItem(R.id.cab_menu_copy).setShowAsAction(control.copy.showAsAction);

    menu.findItem(R.id.cab_menu_leave_share).setVisible(control.leaveShare.visible);
    menu.findItem(R.id.cab_menu_leave_share).setShowAsAction(control.leaveShare.showAsAction);

    menu.findItem(R.id.cab_menu_trash).setVisible(control.trash.visible);
    menu.findItem(R.id.cab_menu_trash).setShowAsAction(control.trash.showAsAction);

    menu.findItem(R.id.cab_menu_add_favourites).setVisible(control.addFavourites.visible);
    menu.findItem(R.id.cab_menu_add_favourites).setShowAsAction(control.addFavourites.showAsAction);

    menu.findItem(R.id.cab_menu_remove_favourites).setVisible(control.removeFavourites.visible);
    menu.findItem(R.id.cab_menu_remove_favourites).setShowAsAction(control.removeFavourites.showAsAction);

    menu.findItem(R.id.cab_menu_dispute).setVisible(control.disputeTakedown.visible);
    menu.findItem(R.id.cab_menu_dispute).setShowAsAction(control.disputeTakedown.showAsAction);

    menu.findItem(R.id.cab_menu_add_to_album).setVisible(control.addToAlbum.visible);
    menu.findItem(R.id.cab_menu_add_to).setVisible(control.addTo.visible);

    menu.findItem(R.id.cab_menu_add_label).setVisible(control.addLabel.visible);
  }

  public static class Control {
    private final Option selectAll;
    private final Option clearSelection;
    private final Option hide;
    private final Option unhide;
    private final Option removeLink;
    private final Option removeShare;
    private final Option rename;
    private final Option saveToDevice;
    private final Option getLink;
    private final Option manageLink;
    private final Option shareFolder;
    private final Option sendToChat;
    private final Option shareOut;
    private final Option move;
    private final Option copy;
    private final Option leaveShare;
    private final Option trash;
    private final Option addFavourites;
    private final Option removeFavourites;

    private final Option disputeTakedown;
    private final Option addToAlbum;
    private final Option addTo;
    private final Option addLabel;

    private final List<Option> options;

    public Control() {
      selectAll = new Option(true);
      clearSelection = new Option(true);
      hide = new Option(false);
      unhide = new Option(false);
      removeLink = new Option(false);
      removeShare = new Option(false);
      rename = new Option(false);
      saveToDevice = new Option(true, MenuItem.SHOW_AS_ACTION_ALWAYS);
      getLink = new Option(false);
      manageLink = new Option(false);
      shareFolder = new Option(false);
      sendToChat = new Option(false);
      shareOut = new Option(false);
      move = new Option(false);
      copy = new Option(false);
      leaveShare = new Option(false);
      trash = new Option(true);
      addFavourites = new Option(false);
      removeFavourites = new Option(false);
      disputeTakedown = new Option(false);
      addToAlbum = new Option(false);
      addTo = new Option(false);
      addLabel = new Option(false);

      options = Arrays.asList(
              selectAll, clearSelection, hide, unhide, removeLink, removeShare, rename, saveToDevice, getLink, disputeTakedown,
              manageLink, shareFolder, sendToChat, shareOut, move, copy, leaveShare, trash, addFavourites, removeFavourites, addToAlbum, addTo, addLabel);
    }

    public Option selectAll() {
      return selectAll;
    }

    public Option clearSelection() {
      return clearSelection;
    }

    public Option hide() {
      return hide;
    }

    public Option unhide() {
      return unhide;
    }

    public Option removeLink() {
      return removeLink;
    }

    public Option removeShare() {
      return removeShare;
    }

    public Option rename() {
      return rename;
    }

    public Option saveToDevice() {
      return saveToDevice;
    }

    public Option getLink() {
      return getLink;
    }

    public Option manageLink() {
      return manageLink;
    }

    public Option shareFolder() {
      return shareFolder;
    }

    public Option sendToChat() {
      return sendToChat;
    }

    public Option shareOut() {
      return shareOut;
    }

    public Option move() {
      return move;
    }

    public Option copy() {
      return copy;
    }

    public Option leaveShare() {
      return leaveShare;
    }

    public Option trash() {
      return trash;
    }

    public Option addFavourites() {
      return addFavourites;
    }

    public Option removeFavourites() {
      return removeFavourites;
    }

    public Option disputeTakedown() {
      return disputeTakedown;
    }

    public Option addToAlbum() {
      return addToAlbum;
    }

    public Option addTo() {
      return addTo;
    }

    public Option addLabel() {
      return addLabel;
    }

    public int alwaysActionCount() {
      int count = 0;
      for (Option option : options) {
        if (option.visible && option.showAsAction == MenuItem.SHOW_AS_ACTION_ALWAYS) {
          count++;
        }
      }
      return count;
    }
  }
}
