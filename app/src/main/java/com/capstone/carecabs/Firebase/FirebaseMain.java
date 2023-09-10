package com.capstone.carecabs.Firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

public class FirebaseMain {

    private static FirebaseFirestore firebaseFirestore;
    private static FirebaseAuth auth;
    private static FirebaseStorage firebaseStorage;

    public static FirebaseFirestore getFireStoreInstance() {
        if (firebaseFirestore == null) {
            firebaseFirestore = FirebaseFirestore.getInstance();
        }

        return firebaseFirestore;
    }

    public static FirebaseAuth getAuth(){
        if(auth == null){
            auth = FirebaseAuth.getInstance();
        }

        return auth;
    }

    public static FirebaseUser getUser(){
        return getAuth().getCurrentUser();
    }

    public static void signOutUser(){
        auth.signOut();
    }
}
