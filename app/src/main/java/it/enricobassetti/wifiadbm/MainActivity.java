package it.enricobassetti.wifiadbm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

	private static final int NOTIFICATION_ID = 1444;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		boolean adbenabled;
		try {
			adbenabled = checkADBEnabled();
		} catch(SecurityException e) {
			Toast.makeText(MainActivity.this, "Questa app ha bisogno dei permessi di root", Toast.LENGTH_LONG)
				.show();
			finish();
			return;
		}

		// https://code.google.com/p/android/issues/detail?id=178010
		// https://android.googlesource.com/platform/system/netd/+/8830b94cf4824e5a6c738d39d3015c8eec976352/CommandListener.cpp
		final Switch btEnable = (Switch) findViewById(R.id.switch1);
		btEnable.setChecked(adbenabled);
		btEnable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				btEnable.setChecked(!isChecked);
				if (isChecked) {
					enableADB();
				} else {
					disableADB();
				}
			}
		});

		WifiManager wifiMgr = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
		int ip = wifiInfo.getIpAddress();
		String ipAddress = Formatter.formatIpAddress(ip);
		TextView iptxt = (TextView) findViewById(R.id.textView);
		iptxt.setText(ipAddress);
	}

	private boolean checkADBEnabled() throws SecurityException {
		boolean ret = false;
		try {
			Process fw = Runtime.getRuntime().exec(new String[]{"su", "-c", "getprop service.adb.tcp.port"});
			int pid = fw.waitFor();
			if(pid != 0) {
				throw new SecurityException();
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(fw.getInputStream()));
			String port = reader.readLine();
			if(!"-1".equals(port)) {
				ret = true;
			}
			reader.close();
		} catch(IOException | InterruptedException e) {
			throw new SecurityException();
		}
		return ret;
	}

	private void enableADB() {
		Toast t;
		try {
			Process fw = Runtime.getRuntime().exec(new String[]{"su", "-c",
					"ndc firewall set_uid_rule standby 2000 allow && setprop service.adb.tcp.port 5555 && stop adbd && start adbd"});
			fw.waitFor();

			t = Toast.makeText(MainActivity.this, "Enabled", Toast.LENGTH_SHORT);
			Switch btEnable = (Switch) findViewById(R.id.switch1);
			btEnable.setChecked(true);
			setNotify();
		} catch (IOException | InterruptedException e) {
			t = Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG);
		}
		t.show();
	}

	private void disableADB() {
		Toast t;
		try {
			Process fw = Runtime.getRuntime().exec(new String[]{"su", "-c",
					"ndc firewall set_uid_rule standby 2000 deny && setprop service.adb.tcp.port -1 && stop adbd && start adbd"});
			fw.waitFor();

			t = Toast.makeText(MainActivity.this, "Disabled", Toast.LENGTH_SHORT);
			Switch btEnable = (Switch) findViewById(R.id.switch1);
			btEnable.setChecked(false);

			clearNotify();
		} catch (IOException | InterruptedException e) {
			t = Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG);
		}
		t.show();
	}

	private void setNotify() {
		Intent intent = new Intent(MainActivity.this, MainActivity.class);

		PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this,
				NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		Notification.Builder builder = new Notification.Builder(MainActivity.this)
				.setContentTitle("WIFI ADB M")
				.setContentText("WIFI ADB M is enabled")
				.setContentIntent(pendingIntent)
				.setSmallIcon(R.drawable.bridge_icon)
				.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.bridge_icon))
				;
		Notification n;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			n = builder.build();
		} else {
			n = builder.getNotification();
		}

		n.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(NOTIFICATION_ID, n);
	}

	private void clearNotify() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(NOTIFICATION_ID);
	}
}
