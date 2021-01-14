package mega.privacy.android.app.lollipop.managerSections;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.text.HtmlCompat;

import java.util.ArrayList;

import mega.privacy.android.app.AndroidCompletedTransfer;
import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.managerFragments.TransfersBaseFragment;
import mega.privacy.android.app.lollipop.adapters.MegaCompletedTransfersAdapter;

import static mega.privacy.android.app.DatabaseHandler.MAX_TRANSFERS;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;


public class CompletedTransfersFragmentLollipop extends TransfersBaseFragment {
	private MegaCompletedTransfersAdapter adapter;
	public ArrayList<AndroidCompletedTransfer> tL = new ArrayList<>();

	public static CompletedTransfersFragmentLollipop newInstance() {
		return new CompletedTransfersFragmentLollipop();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		super.onCreateView(inflater, container, savedInstanceState);

		View v = initView(inflater, container);

		emptyImage.setImageResource(isScreenInPortrait(context) ? R.drawable.ic_zero_portrait_transfers : R.drawable.ic_zero_landscape_saved_for_offline);

		String textToShow = context.getString(R.string.completed_transfers_empty_new);
		try {
			textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
			textToShow = textToShow.replace("[/A]", "</font>");
			textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
			textToShow = textToShow.replace("[/B]", "</font>");
		} catch (Exception e) {
			logWarning("Exception formatting string", e);
		}
		emptyText.setText(HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY));

		setCompletedTransfers();

		adapter = new MegaCompletedTransfersAdapter(context, tL);
		listView.setAdapter(adapter);

		return v;
	}

	private void setCompletedTransfers() {
		tL.clear();
		tL.addAll(dbH.getCompletedTransfers());
		setEmptyView(tL.size());
	}

	/**
	 * Adds new completed transfer.
	 *
	 * @param transfer	the transfer to add
	 */
	public void transferFinish(AndroidCompletedTransfer transfer) {
		if (tL != null) {
			tL.add(0, transfer);

			if (tL.size() >= MAX_TRANSFERS) {
				tL.remove(tL.size() - 1);
			}
		} else {
			tL = new ArrayList<>();
			tL.add(transfer);
		}

		if (tL.size() == 1) {
			managerActivity.invalidateOptionsMenu();
		}

		setEmptyView(tL.size());
		adapter.notifyDataSetChanged();
		managerActivity.supportInvalidateOptionsMenu();
	}

	/**
	 * Checks if there is any completed transfer.
	 *
	 * @return True if there is any completed transfer, false otherwise.
	 */
	public boolean isAnyTransferCompleted() {
		return !tL.isEmpty();
	}

	/**
	 * Removes a completed transfer.
	 *
	 * @param transfer	transfer to remove
	 */
	public void transferRemoved(AndroidCompletedTransfer transfer) {
		for (int i = 0; i < tL.size(); i++) {
			AndroidCompletedTransfer completedTransfer = tL.get(i);
			if (completedTransfer == null) continue;

			if (areTheSameTransfer(transfer, completedTransfer)) {
				tL.remove(i);
				adapter.removeItemData(i);
				break;
			}
		}

		setEmptyView(tL.size());
        managerActivity.supportInvalidateOptionsMenu();
	}

	/**
	 * Compares both transfers received and checks if are the same.
	 *
	 * @param transfer1	first AndroidCompletedTransfer to compare and check.
	 * @param transfer2	second AndroidCompletedTransfer to compare and check.
	 * @return True if both transfers are the same, false otherwise.
	 */
	private boolean areTheSameTransfer(AndroidCompletedTransfer transfer1, AndroidCompletedTransfer transfer2) {
        return transfer1.getId() == transfer2.getId()
                || (isValidHandle(transfer1) && isValidHandle(transfer2) && transfer1.getNodeHandle().equals(transfer2.getNodeHandle()))
                || (transfer1.getError().equals(transfer2.getError()) && transfer1.getFileName().equals(transfer2.getFileName()) && transfer1.getSize().equals(transfer2.getSize()));
    }

	/**
	 * Checks if a transfer has a valid handle.
	 *
	 * @param transfer	AndroidCompletedTransfer to check.
	 * @return True if the transfer has a valid handle, false otherwise.
	 */
	private boolean isValidHandle(AndroidCompletedTransfer transfer) {
	    return !isTextEmpty(transfer.getNodeHandle()) && !transfer.getNodeHandle().equals(Long.toString(INVALID_HANDLE));
    }

	/**
	 * Removes all completed transfers.
	 */
	public void clearCompletedTransfers() {
		tL.clear();
		adapter.setTransfers(tL);
		setEmptyView(tL.size());
	}
}
