package com.capstone.carecabs.Utility;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.capstone.carecabs.BookingsActivity;
import com.capstone.carecabs.Map.MapDriverActivity;
import com.capstone.carecabs.R;
import com.capstone.carecabs.ScanIDActivity;

public class NotificationHelper {
	private Context context;
	private final String CHANNEL_ID = "important_channel_id";
	private final int NOTIFICATION_ID = 1;

	public NotificationHelper(Context context) {
		this.context = context;
		createNotificationChannel();
	}

	private void createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			String channelName = "CareCabs Channel";
			String channelDescription = "This is a very important notification channel";
			int importance = NotificationManager.IMPORTANCE_HIGH;

			NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channelName, importance);
			channel.setDescription(channelDescription);
			channel.enableLights(true);
			channel.setLightColor(Color.RED);
			channel.enableVibration(true);

			NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
			notificationManager.createNotificationChannel(channel);
		}
	}

	public void showBookingIsAcceptedNotificationNotification(String title, String content) {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
				.setSmallIcon(R.drawable.logo_2_v2)
				.setContentTitle(title)
				.setContentText(content)
				.setPriority(NotificationCompat.PRIORITY_HIGH) // Set high priority to make it important
				.setCategory(NotificationCompat.CATEGORY_ALARM); // Use an alarm category for urgency

		Notification notification = builder.build();

		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
		if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
			// TODO: Consider calling
			//    ActivityCompat#requestPermissions
			// here to request the missing permissions, and then overriding
			//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
			//                                          int[] grantResults)
			// to handle the case where the user grants the permission. See the documentation
			// for ActivityCompat#requestPermissions for more details.
			return;
		}
		notificationManager.notify(NOTIFICATION_ID, notification);
	}

	public void showChatNotification(String title, String content) {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
				.setSmallIcon(R.drawable.logo_2_v2)
				.setContentTitle(title)
				.setContentText(content)
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setCategory(NotificationCompat.CATEGORY_ALARM)
				.setAutoCancel(true); // Dismiss the notification when clicked

		Notification notification = builder.build();

		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
		if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
			// TODO: Consider calling
			//    ActivityCompat#requestPermissions
			// here to request the missing permissions, and then overriding
			//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
			//                                          int[] grantResults)
			// to handle the case where the user grants the permission. See the documentation
			// for ActivityCompat#requestPermissions for more details.
			return;
		}
		notificationManager.notify(NOTIFICATION_ID, notification);
	}

	public void showDriverOTWNotification(String title, String content) {
		Intent intent = new Intent(context, BookingsActivity.class); // Replace YourActivity with the target activity
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Optional flags

		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
				PendingIntent.FLAG_IMMUTABLE);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
				.setSmallIcon(R.drawable.logo_2_v2)
				.setContentTitle(title)
				.setContentText(content)
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setCategory(NotificationCompat.CATEGORY_ALARM)
				.setContentIntent(pendingIntent)
				.setAutoCancel(true); // Dismiss the notification when clicked

		Notification notification = builder.build();

		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
		if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
			// TODO: Consider calling
			//    ActivityCompat#requestPermissions
			// here to request the missing permissions, and then overriding
			//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
			//                                          int[] grantResults)
			// to handle the case where the user grants the permission. See the documentation
			// for ActivityCompat#requestPermissions for more details.
			return;
		}
		notificationManager.notify(NOTIFICATION_ID, notification);
	}

	public void showProfileNotVerifiedNotification(String title, String content) {
		Intent intent = new Intent(context, ScanIDActivity.class); // Replace YourActivity with the target activity
		intent.putExtra("userType", "From Main");
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Optional flags

		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
				PendingIntent.FLAG_IMMUTABLE);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
				.setSmallIcon(R.drawable.logo_2_v2)
				.setContentTitle(title)
				.setContentText(content)
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setCategory(NotificationCompat.CATEGORY_ALARM)
				.setContentIntent(pendingIntent) // Attach the PendingIntent to the notification
				.setAutoCancel(true); // Dismiss the notification when clicked

		Notification notification = builder.build();

		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
		if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
			// TODO: Consider calling
			//    ActivityCompat#requestPermissions
			// here to request the missing permissions, and then overriding
			//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
			//                                          int[] grantResults)
			// to handle the case where the user grants the permission. See the documentation
			// for ActivityCompat#requestPermissions for more details.
			return;
		}
		notificationManager.notify(NOTIFICATION_ID, notification);
	}

	public void showPassengersWaitingNotification(String title, String content) {
		Intent intent = new Intent(context, MapDriverActivity.class); // Replace YourActivity with the target activity
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Optional flags

		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
				PendingIntent.FLAG_IMMUTABLE);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
				.setSmallIcon(R.drawable.logo_2_v2)
				.setContentTitle(title)
				.setContentText(content)
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setCategory(NotificationCompat.CATEGORY_ALARM)
				.setContentIntent(pendingIntent) // Attach the PendingIntent to the notification
				.setAutoCancel(true); // Dismiss the notification when clicked

		Notification notification = builder.build();

		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
		if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
			// TODO: Consider calling
			//    ActivityCompat#requestPermissions
			// here to request the missing permissions, and then overriding
			//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
			//                                          int[] grantResults)
			// to handle the case where the user grants the permission. See the documentation
			// for ActivityCompat#requestPermissions for more details.
			return;
		}
		notificationManager.notify(NOTIFICATION_ID, notification);
	}

}
