package com.capstone.carecabs.Firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.capstone.carecabs.Utility.NotificationHelper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FirebaseCloudMessaging extends FirebaseMessagingService {
	@Override
	public void onMessageReceived(@NonNull RemoteMessage message) {
		super.onMessageReceived(message);

		Log.i("asdrradawdawawwgerawda", String.valueOf(message.getData()));

		if (message.getData().containsKey("chat")) {
			sendChatNotification(message);
		} else if (message.getData().containsKey("passenger")) {
			sendPassengerWaitingNotification();
		}else if (message.getData().containsKey("driverOTW")) {
			sendDriverOTWNotification(message);
		}
	}

	private void sendChatNotification(RemoteMessage message) {
		NotificationHelper notificationHelper = new NotificationHelper(this);
		notificationHelper.showChatNotification("CareCabs", message.getData().get("chat"));
	}

	private void sendPassengerWaitingNotification() {
		NotificationHelper notificationHelper = new NotificationHelper(this);
		notificationHelper.showPassengersWaitingNotification("CareCabs",
				"Passenger Waiting");
	}
	private void sendDriverOTWNotification(RemoteMessage message) {
		NotificationHelper notificationHelper = new NotificationHelper(this);
		notificationHelper.showDriverOTWNotification("CareCabs",
				"Your Driver is on the way\n" + message.getData().get("driverOTW"));
	}
}
