package mega.privacy.android.app.lollipop.managerSections;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import mega.privacy.android.app.AndroidCompletedTransfer;
import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.managerFragments.TransfersBaseFragment;
import mega.privacy.android.app.lollipop.adapters.MegaCompletedTransfersAdapter;

import static mega.privacy.android.app.DatabaseHandler.MAX_TRANSFERS;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;


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
		emptyText.setText(getSpannedHtmlText(textToShow));

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
	}

	public boolean isAnyTransferCompleted() {
		return !tL.isEmpty();
	}

	public void transferRemoved(AndroidCompletedTransfer transfer) {
		for (int i = 0; i < tL.size(); i++) {
			AndroidCompletedTransfer completedTransfer = tL.get(i);
			if (completedTransfer != null && completedTransfer.getId() == transfer.getId()) {
				tL.remove(i);
				adapter.removeItemData(i);
				break;
			}
		}

		setEmptyView(tL.size());
	}

	public void clearCompletedTransfers() {
		tL.clear();
		adapter.setTransfers(tL);
		setEmptyView(tL.size());
	}
}
