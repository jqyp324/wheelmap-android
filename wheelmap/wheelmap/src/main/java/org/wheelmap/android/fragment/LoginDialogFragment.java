package org.wheelmap.android.fragment;

import org.wheelmap.android.model.Extra;
import org.wheelmap.android.model.UserCredentials;
import org.wheelmap.android.online.R;
import org.wheelmap.android.service.SyncService;
import org.wheelmap.android.service.SyncServiceException;
import org.wheelmap.android.service.SyncServiceHelper;
import org.wheelmap.android.utils.DetachableResultReceiver;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.WazaBe.HoloEverywhere.HoloAlertDialogBuilder;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import de.akquinet.android.androlog.Log;

public class LoginDialogFragment extends SherlockDialogFragment implements
		OnClickListener, DetachableResultReceiver.Receiver,
		OnEditorActionListener {
	public final static String TAG = LoginDialogFragment.class.getSimpleName();

	private EditText mEmailText;
	private EditText mPasswordText;
	private ProgressBar mProgressBar;

	private boolean mSyncing;
	private DetachableResultReceiver mReceiver;
	private OnLoginDialogListener mListener;

	public interface OnLoginDialogListener {
		public void onLoginSuccessful();

		public void onLoginCancelled();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		mReceiver = new DetachableResultReceiver(new Handler());
		mReceiver.setReceiver(this);

	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof OnLoginDialogListener) {
			mListener = (OnLoginDialogListener) activity;
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		HoloAlertDialogBuilder builder = new HoloAlertDialogBuilder(
				getActivity());
		builder.setTitle(R.string.title_login);
		builder.setIcon(R.drawable.ic_menu_search_wm_holo_light);
		builder.setNeutralButton(R.string.login_submit, null);
		builder.setOnCancelListener(this);

		View view = getActivity().getLayoutInflater().inflate(
				R.layout.fragment_dialog_login, null);
		builder.setView(view);

		Dialog d = builder.create();
		return d;
	}

	@Override
	public void onResume() {
		super.onResume();

		AlertDialog dialog = (AlertDialog) getDialog();
		Button button = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
		button.setOnClickListener(this);
		mEmailText = (EditText) dialog.findViewById(R.id.login_email);
		mEmailText.setOnEditorActionListener(this);
		mPasswordText = (EditText) dialog.findViewById(R.id.login_password);
		mPasswordText.setOnEditorActionListener(this);
		load();
		mProgressBar = (ProgressBar) dialog.findViewById(R.id.progressbar);

	}

	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);

		if (mListener != null)
			mListener.onLoginCancelled();
	}

	private void load() {
		UserCredentials userCredentials = new UserCredentials(getActivity());
		String login = userCredentials.getLogin();
		String password = userCredentials.getPassword();

		if (userCredentials.isLoggedIn()) {
			mEmailText.setText(login);
			mPasswordText.setText(password);
		}
	}

	@Override
	public void onReceiveResult(int resultCode, Bundle resultData) {
		Log.d(TAG, "onReceiveResult in list resultCode = " + resultCode);
		switch (resultCode) {
		case SyncService.STATUS_RUNNING:
			mSyncing = true;
			updateRefreshStatus();
			break;
		case SyncService.STATUS_FINISHED:
			mSyncing = false;
			updateRefreshStatus();
			loginSuccessful();
			break;
		case SyncService.STATUS_ERROR:
			// Error happened down in SyncService, show as toast.
			mSyncing = false;
			updateRefreshStatus();
			final SyncServiceException e = resultData
					.getParcelable(Extra.EXCEPTION);
			showErrorDialog(e);
			break;
		default: // noop
		}

	}

	private void updateRefreshStatus() {
		if (mSyncing)
			mProgressBar.setVisibility(View.VISIBLE);
		else
			mProgressBar.setVisibility(View.INVISIBLE);
	}

	private void showErrorDialog(SyncServiceException e) {
		SherlockFragmentActivity activity = getSherlockActivity();
		FragmentManager fm = activity.getSupportFragmentManager();
		ErrorDialogFragment errorDialog = ErrorDialogFragment.newInstance(e);
		if (errorDialog == null)
			return;

		errorDialog.show(fm, ErrorDialogFragment.TAG);
	}

	private void loginSuccessful() {
		dismiss();
		if (mListener != null)
			mListener.onLoginSuccessful();
	}

	@Override
	public void onClick(View v) {
		login();
	}

	private void login() {
		String email = mEmailText.getText().toString();
		String password = mPasswordText.getText().toString();

		if (email.length() == 0 || password.length() == 0)
			return; // TODO: display a dialog requesting for a password

		SyncServiceHelper.executeRetrieveApiKey(getActivity(), email, password,
				mReceiver);
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (EditorInfo.IME_ACTION_DONE == actionId) {
			login();
			return true;
		}

		return false;
	}

}