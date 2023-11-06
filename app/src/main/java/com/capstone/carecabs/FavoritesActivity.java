package com.capstone.carecabs;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.capstone.carecabs.Adapters.FavoritesAdapter;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Model.FavoritesModel;
import com.capstone.carecabs.databinding.ActivityFavoritesBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FavoritesActivity extends AppCompatActivity {
	private final String TAG = "FavoritesActivity";
	private ActivityFavoritesBinding binding;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityFavoritesBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.noFavoritesTextView.setVisibility(View.GONE);

		FirebaseApp.initializeApp(this);

		loadFavoritesFromFireStore();
	}

	@Override
	public void onBackPressed() {
		finish();
		super.onBackPressed();
	}

	@SuppressLint("NotifyDataSetChanged")
	private void loadFavoritesFromFireStore() {
		if (FirebaseMain.getUser() != null) {
			CollectionReference favoriteReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.favoriteCollection);

			List<FavoritesModel> favoritesModelList = new ArrayList<>();
			FavoritesAdapter favoritesAdapter =
					new FavoritesAdapter(this,
							favoritesModelList,
							favoritesModel -> removeFavoriteFromFireStore(favoritesModel.getFavoriteID()));

			binding.favoritesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
			binding.favoritesRecyclerView.setAdapter(favoritesAdapter);

			favoriteReference.get()
					.addOnSuccessListener(queryDocumentSnapshots -> {
						if (queryDocumentSnapshots != null) {
							favoritesModelList.clear();
							boolean hasFavorites = false;
							for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
								FavoritesModel favoritesModel = documentSnapshot.toObject(FavoritesModel.class);

								if (favoritesModel.getUserID().equals(FirebaseMain.getUser().getUid())) {
									favoritesModelList.add(favoritesModel);

									hasFavorites = true;

								}
							}

							favoritesAdapter.notifyDataSetChanged();

							if (hasFavorites){
								binding.noFavoritesTextView.setVisibility(View.GONE);

							}else {
								binding.noFavoritesTextView.setVisibility(View.VISIBLE);

							}
						}
					})
					.addOnFailureListener(e -> Log.e(TAG, "onFailure: " + e.getMessage()));
		}
	}

	private void removeFavoriteFromFireStore(String favoriteID) {
		if (FirebaseMain.getUser() != null) {
			DocumentReference documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.favoriteCollection)
					.document(favoriteID);

			documentReference.delete()
					.addOnSuccessListener(unused -> {

						Log.i(TAG, "onSuccess: favorite deleted");
					})
					.addOnFailureListener(e -> Log.e(TAG, "onFailure: " + e.getMessage()));
		}
	}

	private String generateRandomFavoritesID() {
		String uuid = String.valueOf(UUID.randomUUID());
		return uuid.toString();
	}

}