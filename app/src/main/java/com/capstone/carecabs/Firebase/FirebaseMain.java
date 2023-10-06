package com.capstone.carecabs.Firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.util.HashMap;

import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class FirebaseMain {
	public static String userCollection = "users";
	public static String bookingCollection = "bookings";
	public static String tripCollection = "trips";
	public static String chatCollection = "chats";
	public static String feedbackCollection = "feedbacks";
	public static String tripFeebackCollection = "tripFeedbacks";
	private static FirebaseFirestore firebaseFirestore;
	public static DatabaseReference databaseReference;
	private static FirebaseAuth auth;
	private static FirebaseStorage firebaseStorage;

	public static HashMap<String,String> remoteMsgHeaders = null;

	private static Retrofit retrofit = null;

	public static Retrofit getClient(){
		if(retrofit == null){
			retrofit = new Retrofit.Builder()
					.baseUrl("https://fcm.googleapis.com/fcm/")
					.addConverterFactory(ScalarsConverterFactory.create())
					.build();
		}
		return retrofit;
	}
	public static HashMap<String,String> getRemoteMsgHeaders(){
		if(remoteMsgHeaders == null){
			remoteMsgHeaders = new HashMap<>();
			remoteMsgHeaders.put(
					"Authorization",
					"key=AAAAL79ShUw:APA91bH0mZmKzad678UAk3bRtnlDlvWOiLKDM0rmpFbQK7K4nY87hl58NseGCjgP3Cht5Y7ZPoJE82zzdIU3Motx3K5P8BMXO43auFVAw-R6XVmdki0ugDX-ouOo2oiNKmnBwGkiyiaq");
			remoteMsgHeaders.put("Content-Type","application/json");
		}

		return remoteMsgHeaders;
	}

	public static FirebaseStorage getFirebaseStorageInstance() {
		if (firebaseStorage == null) {
			firebaseStorage = FirebaseStorage.getInstance();
		}
		return firebaseStorage;
	}

	public static DatabaseReference getDatabaseReferenceInstance() {
		if (databaseReference == null) {
			databaseReference = FirebaseDatabase.getInstance().getReference();
		}
		return databaseReference;
	}

	public static FirebaseFirestore getFireStoreInstance() {
		if (firebaseFirestore == null) {
			firebaseFirestore = FirebaseFirestore.getInstance();
		}

		return firebaseFirestore;
	}

	public static FirebaseAuth getAuth() {
		if (auth == null) {
			auth = FirebaseAuth.getInstance();
		}

		return auth;
	}

	public static FirebaseUser getUser() {
		return getAuth().getCurrentUser();
	}

	public static void signOutUser() {
		auth.signOut();
	}
}
