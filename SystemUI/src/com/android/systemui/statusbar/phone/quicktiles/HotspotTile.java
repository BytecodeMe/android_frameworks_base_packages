package com.android.systemui.statusbar.phone.quicktiles;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsTileContent;

public class HotspotTile extends QuickSettingsTileContent implements
		View.OnClickListener, View.OnLongClickListener {

	private static final String TAG = HotspotTile.class.getSimpleName();
	private static final String EMPTY = "";

	public static final int OPEN_INDEX = 0;
	public static final int WPA_INDEX = 1;
	public static final int WPA2_INDEX = 2;

	private static final int CONFIG_SUBTEXT = R.string.wifi_tether_configure_subtext;

	private CharSequence mOriginalSummary;

	private WifiManager mWifiManager;
	private final IntentFilter mIntentFilter;
	private WifiConfiguration mWifiConfig = null;

	private String[] mSecurityType;
	private WifiApDialog mDialog;

	ConnectivityManager mCm;
	private String[] mWifiRegexs;
	private int mSecurityTypeIndex;

	/**
	 * These values are matched in string arrays -- changes must be kept in sync
	 */
	static final int SECURITY_NONE = 0;
	static final int SECURITY_WEP = 1;
	static final int SECURITY_PSK = 2;
	static final int SECURITY_EAP = 3;

	enum PskType {
		UNKNOWN, WPA, WPA2, WPA_WPA2
	}

	int security;
	PskType pskType = PskType.UNKNOWN;
	private WifiConfiguration mConfig;

	private boolean mEnabled = false;
	private boolean mStoppedBySettings = false;

	public HotspotTile(Context context, View view) {
		super(context, view);

		mWifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		mCm = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		mWifiRegexs = mCm.getTetherableWifiRegexs();

		mIntentFilter = new IntentFilter(
				WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
		mIntentFilter
				.addAction(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
		mIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);

		init();
	}

	@Override
	public void init() {
		mContentView.setOnClickListener(this);
		mContentView.setOnLongClickListener(this);

		mTag = TAG;

		mSecurityType = mContext.getResources().getStringArray(
				R.array.wifi_ap_security);

		mWifiConfig = mWifiManager.getWifiApConfiguration();

		mContext.registerReceiver(mReceiver, mIntentFilter);
		mDialog = setupDialog();
		updateGUI();
	}

	@Override
	public void release() {
		mContext.unregisterReceiver(mReceiver);
	}

	@Override
	public void onClick(View v) {
		if (!mStoppedBySettings) {
			if (mEnabled) {
				setSoftapEnabled(false);
			} else {
				setSoftapEnabled(true);
			}
			mStoppedBySettings = false;
		}
	}

	@Override
	public boolean onLongClick(View v) {		
		mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
		mDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		getStatusBarManager().collapsePanels();
		mDialog.show();
		return true;
	}

	@Override
	public void refreshResources() {
		// TODO Auto-generated method stub

	}

	public void setSoftapEnabled(boolean enable) {
		final ContentResolver cr = mContext.getContentResolver();
		/**
		 * Disable Wifi if enabling tethering
		 */
		int wifiState = mWifiManager.getWifiState();
		if (enable
				&& ((wifiState == WifiManager.WIFI_STATE_ENABLING) || (wifiState == WifiManager.WIFI_STATE_ENABLED))) {
			mWifiManager.setWifiEnabled(false);
			Settings.Global.putInt(cr, Settings.Global.WIFI_SAVED_STATE, 1);
		}

		if (mWifiManager.setWifiApEnabled(null, enable)) {
			/* Disable here, enabled on receiving success broadcast */
			mEnabled = false;
		} else {
			mTextView.setText(R.string.wifi_error);
		}

		/**
		 * If needed, restore Wifi on tether disable
		 */
		if (!enable) {
			boolean wifiSavedState = Settings.Global.getInt(cr,
						Settings.Global.WIFI_SAVED_STATE, 0) == 1;
			mWifiManager.setWifiEnabled(wifiSavedState);
			Settings.Global.putInt(cr, Settings.Global.WIFI_SAVED_STATE, 0);
		}
	}

	private void updateTetherState(Object[] available, Object[] tethered,
			Object[] errored) {
		boolean wifiTethered = false;
		boolean wifiErrored = false;

		for (Object o : tethered) {
			String s = (String) o;
			for (String regex : mWifiRegexs) {
				if (s.matches(regex))
					wifiTethered = true;
			}
		}
		for (Object o : errored) {
			String s = (String) o;
			for (String regex : mWifiRegexs) {
				if (s.matches(regex))
					wifiErrored = true;
			}
		}

		if (wifiTethered) {
			WifiConfiguration wifiConfig = mWifiManager
					.getWifiApConfiguration();
			mTextView.setText(wifiConfig.SSID);
		} else if (wifiErrored) {
			mTextView.setText(R.string.wifi_error);
		}
	}

	private void handleWifiApStateChanged(int state) {
		switch (state) {
		case WifiManager.WIFI_AP_STATE_ENABLING:
			mTextView.setText(R.string.wifi_starting);
			mEnabled = false;
			break;
		case WifiManager.WIFI_AP_STATE_ENABLED:
			/**
			 * Summary on enable is handled by tether broadcast notice
			 */
			mEnabled = true;
			/* Doesnt need the airplane check */
			mStoppedBySettings = false;
			break;
		case WifiManager.WIFI_AP_STATE_DISABLING:
			mTextView.setText(R.string.wifi_stopping);
			mEnabled = false;
			break;
		case WifiManager.WIFI_AP_STATE_DISABLED:
			mTextView.setText("Disabled");
			break;
		default:
			mEnabled = false;
			mTextView.setText(R.string.wifi_error);
			break;
		}
	}

	private WifiApDialog setupDialog() {
		WifiApDialog dialog = new WifiApDialog(mContext, mWifiConfig,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == DialogInterface.BUTTON_POSITIVE) {
							mWifiConfig = mDialog.getConfig();
							if (mWifiConfig != null) {
								/**
								 * if soft AP is stopped, bring up else restart
								 * with new config TODO: update config on a
								 * running access point when framework support
								 * is added
								 */
								if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED) {
									mWifiManager.setWifiApEnabled(null, false);
									mWifiManager.setWifiApEnabled(mWifiConfig,
											true);
									mStoppedBySettings = true;
								} else {
									mWifiManager
											.setWifiApConfiguration(mWifiConfig);
								}
							}
						}
						dialog.cancel();

					}
				});
		return dialog;
	}

	protected void resetDefaults(final int temp) {

		final Handler handler = new Handler();
		final Runnable finished = new Runnable() {
			public void run() {
				Settings.System.putInt(mContext.getContentResolver(),
						Settings.System.SHOW_NOTIFICATIONS_DEFAULT, temp);
			}
		};

		new Thread() {
			@Override
			public void run() {
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				handler.post(finished);
			}
		}.start();
	}

	private void updateGUI() {
		mTextView.setText(mEnabled ? R.string.quick_settings_hotspot_on
				: R.string.quick_settings_hotspot_off);
		mTextView.setCompoundDrawablesWithIntrinsicBounds(0,
				mEnabled ? R.drawable.ic_qs_hotspot_on
						: R.drawable.ic_qs_hotspot_off, 0, 0);
	}

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (WifiManager.WIFI_AP_STATE_CHANGED_ACTION.equals(action)) {
				handleWifiApStateChanged(intent.getIntExtra(
						WifiManager.EXTRA_WIFI_AP_STATE,
						WifiManager.WIFI_AP_STATE_FAILED));
			} else if (ConnectivityManager.ACTION_TETHER_STATE_CHANGED
					.equals(action)) {
				ArrayList<String> available = intent
						.getStringArrayListExtra(ConnectivityManager.EXTRA_AVAILABLE_TETHER);
				ArrayList<String> active = intent
						.getStringArrayListExtra(ConnectivityManager.EXTRA_ACTIVE_TETHER);
				ArrayList<String> errored = intent
						.getStringArrayListExtra(ConnectivityManager.EXTRA_ERRORED_TETHER);
				updateTetherState(available.toArray(), active.toArray(),
						errored.toArray());
			} else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
			}
			updateGUI();
		}
	};

	public int getSecurityTypeIndex(WifiConfiguration wifiConfig) {
		if (wifiConfig.allowedKeyManagement.get(KeyMgmt.WPA_PSK)) {
			return WPA_INDEX;
		} else if (wifiConfig.allowedKeyManagement.get(KeyMgmt.WPA2_PSK)) {
			return WPA2_INDEX;
		}
		return OPEN_INDEX;
	}

	/**
	 * Dialog to configure the SSID and security settings for Access Point
	 * operation
	 */
	public class WifiApDialog extends AlertDialog implements
			View.OnClickListener, TextWatcher,
			AdapterView.OnItemSelectedListener {

		static final int BUTTON_SUBMIT = DialogInterface.BUTTON_POSITIVE;

		private final DialogInterface.OnClickListener mListener;

		public static final int OPEN_INDEX = 0;
		public static final int WPA_INDEX = 1;
		public static final int WPA2_INDEX = 2;

		private View mView;
		private TextView mSsid;
		private int mSecurityTypeIndex = OPEN_INDEX;
		private EditText mPassword;

		WifiConfiguration mWifiConfig;

		public WifiApDialog(Context context, WifiConfiguration wifiConfig,
				DialogInterface.OnClickListener listener) {
			super(context);
			mListener = listener;
			mWifiConfig = wifiConfig;
			if (wifiConfig != null) {
				mSecurityTypeIndex = getSecurityTypeIndex(wifiConfig);
			}
		}

		public int getSecurityTypeIndex(WifiConfiguration wifiConfig) {
			if (wifiConfig.allowedKeyManagement.get(KeyMgmt.WPA_PSK)) {
				return WPA_INDEX;
			} else if (wifiConfig.allowedKeyManagement.get(KeyMgmt.WPA2_PSK)) {
				return WPA2_INDEX;
			}
			return OPEN_INDEX;
		}

		public WifiConfiguration getConfig() {

			WifiConfiguration config = new WifiConfiguration();

			/**
			 * TODO: SSID in WifiConfiguration for soft ap is being stored as a
			 * raw string without quotes. This is not the case on the client
			 * side. We need to make things consistent and clean it up
			 */
			config.SSID = mSsid.getText().toString();

			switch (mSecurityTypeIndex) {
			case OPEN_INDEX:
				config.allowedKeyManagement.set(KeyMgmt.NONE);
				return config;

			case WPA_INDEX:
				config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
				config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
				if (mPassword.length() != 0) {
					String password = mPassword.getText().toString();
					config.preSharedKey = password;
				}
				return config;

			case WPA2_INDEX:
				config.allowedKeyManagement.set(KeyMgmt.WPA2_PSK);
				config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
				if (mPassword.length() != 0) {
					String password = mPassword.getText().toString();
					config.preSharedKey = password;
				}
				return config;
			}
			return null;
		}

		@Override
		protected void onCreate(Bundle savedInstanceState) {

			mView = getLayoutInflater().inflate(R.layout.wifi_ap_dialog, null);
			Spinner mSecurity = ((Spinner) mView.findViewById(R.id.security));
			
			setView(mView);
			setInverseBackgroundForced(true);

			Context context = getContext();

			setTitle(R.string.wifi_tether_configure_ap_text);
			mView.findViewById(R.id.type).setVisibility(View.VISIBLE);
			mSsid = (TextView) mView.findViewById(R.id.ssid);
			mPassword = (EditText) mView.findViewById(R.id.password);

			setButton(BUTTON_SUBMIT, context.getString(R.string.wifi_save),
					mListener);
			setButton(DialogInterface.BUTTON_NEGATIVE,
					context.getString(R.string.wifi_cancel), mListener);

			if (mWifiConfig != null) {
				mSsid.setText(mWifiConfig.SSID);
				mSecurity.setSelection(mSecurityTypeIndex);
				if (mSecurityTypeIndex == WPA_INDEX
						|| mSecurityTypeIndex == WPA2_INDEX) {
					mPassword.setText(mWifiConfig.preSharedKey);
				}
			}

			mSsid.addTextChangedListener(this);
			mPassword.addTextChangedListener(this);
			((CheckBox) mView.findViewById(R.id.show_password))
					.setOnClickListener(this);
			mSecurity.setOnItemSelectedListener(this);

			super.onCreate(savedInstanceState);

			showSecurityFields();
			validate();
		}

		private void validate() {
			if ((mSsid != null && mSsid.length() == 0)
					|| (((mSecurityTypeIndex == WPA_INDEX) || (mSecurityTypeIndex == WPA2_INDEX)) && mPassword
							.length() < 8)) {
				getButton(BUTTON_SUBMIT).setEnabled(false);
			} else {
				getButton(BUTTON_SUBMIT).setEnabled(true);
			}
		}

		public void onClick(View view) {
			mPassword
					.setInputType(InputType.TYPE_CLASS_TEXT
							| (((CheckBox) view).isChecked() ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
									: InputType.TYPE_TEXT_VARIATION_PASSWORD));
		}

		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
		}

		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		public void afterTextChanged(Editable editable) {
			validate();
		}

		@Override
		public void onItemSelected(AdapterView<?> parent, View view,
				int position, long id) {
			mSecurityTypeIndex = position;
			showSecurityFields();
			validate();
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
		}

		private void showSecurityFields() {
			if (mSecurityTypeIndex == OPEN_INDEX) {
				mView.findViewById(R.id.fields).setVisibility(View.GONE);
				return;
			}
			mView.findViewById(R.id.fields).setVisibility(View.VISIBLE);
		}
	}

}
