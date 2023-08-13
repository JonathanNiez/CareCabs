package com.capstone.carecabs.Utility;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

public class NetworkConnectivityChecker {
    private Context context;
    private ConnectivityManager connectivityManager;
    private boolean isConnected;
    private ConnectivityManager.NetworkCallback networkCallback;

    public NetworkConnectivityChecker(Context context) {
        this.context = context;
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        startNetworkCallback();
    }

    private void startNetworkCallback() {
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                isConnected = true;
            }

            @Override
            public void onLost(Network network) {
                isConnected = false;
            }
        };

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void unregisterNetworkCallback() {
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }}
