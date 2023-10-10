package com.capstone.carecabs;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Model.FeedbackModel;
import com.capstone.carecabs.databinding.ActivityFeedbackBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;

import java.util.Calendar;
import java.util.UUID;

public class FeedbackActivity extends AppCompatActivity {
	private final String TAG = "FeedbackActivity";
	private Intent intent;
	private ActivityFeedbackBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityFeedbackBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.imgBackBtn.setOnClickListener(v -> {
			finish();
		});

		binding.submitBtn.setOnClickListener(v -> {
			String comment = binding.commentEditText.getText().toString();
			if (comment.isEmpty()) {
				Toast.makeText(this, "Please enter your comment", Toast.LENGTH_LONG).show();
				return;
			} else {
				submitFeedback(generateRandomFeedbackID(),
						comment);
			}
		});

		binding.backBtn.setOnClickListener(v -> {
			finish();
		});

	}

	private String generateRandomFeedbackID() {
		UUID uuid = UUID.randomUUID();
		return uuid.toString();
	}

	private String getCurrentTimeAndDate() {
		Calendar calendar = Calendar.getInstance(); // Get a Calendar instance
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH) + 1; // Months are 0-based, so add 1
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		int second = calendar.get(Calendar.SECOND);

		return month + "-" + day + "-" + year + " " + hour + ":" + minute + ":" + second;
	}

	private void submitFeedback(String feedbackID,
	                            String comment) {
		if (FirebaseMain.getUser() != null) {
			DocumentReference documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.feedbackCollection)
					.document(feedbackID);

			FeedbackModel feedbackModel = new FeedbackModel(
					feedbackID,
					FirebaseMain.getUser().getUid(),
					comment,
					getCurrentTimeAndDate()
			);
			documentReference.set(feedbackModel).addOnSuccessListener(unused -> {

				binding.commentEditText.setText("");
				Toast.makeText(FeedbackActivity.this, "Feedback submitted", Toast.LENGTH_SHORT).show();

			}).addOnFailureListener(e -> {
				Toast.makeText(FeedbackActivity.this, "Feedback failed to submit", Toast.LENGTH_LONG).show();

				Log.e(TAG, e.getMessage());
			});
		}
	}

	@Override
	public void onBackPressed() {
		finish();
	}

}