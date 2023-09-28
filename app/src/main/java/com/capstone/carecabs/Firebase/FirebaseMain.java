package com.capstone.carecabs.Firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

public class FirebaseMain {
	public static String userCollection = "users";
	public static String bookingCollection = "bookings";
	public static String tripCollection = "trips";
	public static String chatCollection = "chats";

	private static FirebaseFirestore firebaseFirestore;
	public static DatabaseReference databaseReference;
	private static FirebaseAuth auth;
	private static FirebaseStorage firebaseStorage;

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
