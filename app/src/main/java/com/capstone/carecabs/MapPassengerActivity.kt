package com.capstone.carecabs

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.capstone.carecabs.Firebase.FirebaseMain
import com.capstone.carecabs.Utility.StaticDataPasser
import com.capstone.carecabs.databinding.ActivityMapPassengerBinding
import com.google.firebase.firestore.DocumentReference
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.lifecycle.requireMapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.dropin.map.MapViewObserver
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import java.util.UUID

class MapPassengerActivity : AppCompatActivity() {
    private val TAG: String = "MapPassengerActivity"
    private lateinit var documentReference: DocumentReference
    private lateinit var binding: ActivityMapPassengerBinding
    private lateinit var userNotVerifiedDialog: AlertDialog
    private lateinit var builder: AlertDialog.Builder
    private lateinit var intent: Intent

    private companion object {
        private const val BUTTON_ANIMATION_DURATION = 1500L
    }

    private val navigationLocationProvider = NavigationLocationProvider()
    private lateinit var navigationCamera: NavigationCamera
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource
    private val pixelDensity = Resources.getSystem().displayMetrics.density
    private val overviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            140.0 * pixelDensity,
            40.0 * pixelDensity,
            120.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }
    private val landscapeOverviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0 * pixelDensity,
            380.0 * pixelDensity,
            110.0 * pixelDensity,
            20.0 * pixelDensity
        )
    }
    private val followingPadding: EdgeInsets by lazy {
        EdgeInsets(
            180.0 * pixelDensity,
            40.0 * pixelDensity,
            150.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }
    private val landscapeFollowingPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0 * pixelDensity,
            380.0 * pixelDensity,
            110.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }


    private var annotationApi: AnnotationPlugin? = null
    private lateinit var annotationConfig: AnnotationConfig
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var layerID = "map_annotation"

    private val onMapLongClick = object : MapViewObserver(), OnMapLongClickListener {

        override fun onAttached(mapView: MapView) {
            mapView.gestures.addOnMapLongClickListener(this)
        }

        override fun onDetached(mapView: MapView) {
            mapView.gestures.removeOnMapLongClickListener(this)
        }

        override fun onMapLongClick(point: Point): Boolean {
            Toast.makeText(
                this@MapPassengerActivity,
                "Map was long clicked",
                Toast.LENGTH_LONG
            ).show()
            return false
        }
    }

    private val locationObserver = object : LocationObserver {
        var firstLocationUpdateReceived = false

        override fun onNewRawLocation(rawLocation: Location) {
            // not handled
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val enhancedLocation = locationMatcherResult.enhancedLocation
            // update location puck's position on the map
            navigationLocationProvider.changePosition(
                location = enhancedLocation,
                keyPoints = locationMatcherResult.keyPoints,
            )

            // update camera position to account for new location
            viewportDataSource.onLocationChanged(enhancedLocation)
            viewportDataSource.evaluate()

            // if this is the first location update the activity has received,
            // it's best to immediately move the camera to the current user location
            if (!firstLocationUpdateReceived) {
                firstLocationUpdateReceived = true
                navigationCamera.requestNavigationCameraToOverview(
                    stateTransitionOptions = NavigationCameraTransitionOptions.Builder()
                        .maxDuration(0) // instant transition
                        .build()
                )
            }
        }
    }

    private val mapboxNavigation: MapboxNavigation by requireMapboxNavigation(
        onResumedObserver = object : MapboxNavigationObserver {
            @SuppressLint("MissingPermission")
            override fun onAttached(mapboxNavigation: MapboxNavigation) {
                mapboxNavigation.registerLocationObserver(locationObserver)
                // start the trip session to being receiving location updates in free drive
                // and later when a route is set also receiving route progress updates
                mapboxNavigation.startTripSession()
            }

            override fun onDetached(mapboxNavigation: MapboxNavigation) {
                mapboxNavigation.unregisterLocationObserver(locationObserver)
            }
        },
        onInitialize = this::initializeNavigation
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapPassengerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkIfUserIsVerified()
        bottomNavButtons()
        loadMapStyle()

        binding.setLocationLayout.visibility = View.GONE

        binding.setLocationBtn.setOnClickListener {
            mapboxNavigation.setNavigationRoutes(listOf())
        }

        binding.recenterBtn.setOnClickListener {
            navigationCamera.requestNavigationCameraToFollowing()
            binding.routeOverviewBtn.showTextAndExtend(BUTTON_ANIMATION_DURATION)
        }
        binding.routeOverviewBtn.setOnClickListener {
            navigationCamera.requestNavigationCameraToOverview()
            binding.recenterBtn.showTextAndExtend(BUTTON_ANIMATION_DURATION)
        }

        viewportDataSource = MapboxNavigationViewportDataSource(binding.mapView.getMapboxMap())
        navigationCamera = NavigationCamera(
            binding.mapView.getMapboxMap(),
            binding.mapView.camera,
            viewportDataSource
        )
        binding.mapView.camera.addCameraAnimationsLifecycleListener(
            NavigationBasicGesturesHandler(navigationCamera)
        )
        navigationCamera.registerNavigationCameraStateChangeObserver { navigationCameraState ->
            // shows/hide the recenter button depending on the camera state
            when (navigationCameraState) {
                NavigationCameraState.TRANSITION_TO_FOLLOWING,
                NavigationCameraState.FOLLOWING -> binding.recenterBtn.visibility = View.INVISIBLE

                NavigationCameraState.TRANSITION_TO_OVERVIEW,
                NavigationCameraState.OVERVIEW,
                NavigationCameraState.IDLE -> binding.recenterBtn.visibility = View.VISIBLE
            }
        }
        // set the padding values depending on screen orientation and visible view layout
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.overviewPadding = landscapeOverviewPadding
        } else {
            viewportDataSource.overviewPadding = overviewPadding
        }
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.followingPadding = landscapeFollowingPadding
        } else {
            viewportDataSource.followingPadding = followingPadding
        }

    }

    private fun loadMapStyle(){
        binding.mapView.getMapboxMap()
            .loadStyleUri(getString(R.string.custom_map_style_url))
        // After the style is loaded, initialize the Location component.
        {
            binding.mapView.gestures.addOnMapClickListener{

                Toast.makeText(
                    this@MapPassengerActivity,
                    "Map was clicked",
                    Toast.LENGTH_LONG
                ).show()

                true
            }


            annotationApi = binding.mapView.annotations
            annotationConfig = AnnotationConfig(
                layerId = layerID
            )

            pointAnnotationManager =
                annotationApi?.createPointAnnotationManager(annotationConfig)
        }
    }
    private fun bottomNavButtons() {
        binding.bottomNavigationView.selectedItemId = R.id.setLocation

        binding.bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.setLocation -> {
                    binding.setLocationLayout.visibility = View.VISIBLE
                }

                R.id.myBookings -> {
                    binding.setLocationLayout.visibility = View.GONE
                    val intent = Intent(this, BookingsActivity::class.java)
                    startActivity(intent)
                }

                R.id.help -> {
                    binding.setLocationLayout.visibility = View.GONE

                }
            }
            true
        }
    }

    private fun generateRandomTripId(): String {
        val uuid = UUID.randomUUID()
        return uuid.toString()
    }

    private fun initializeNavigation() {
        MapboxNavigationApp.setup(
            NavigationOptions.Builder(this)
                .accessToken(getString(R.string.mapbox_access_token))
                // comment out the location engine setting block to disable simulation
//                .locationEngine(replayLocationEngine)
                .build()
        )

        binding.mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            this.locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    this@MapPassengerActivity,
                    R.drawable.mapbox_navigation_puck_icon
                )
            )
            enabled = true
        }
    }

    private fun checkIfUserIsVerified() {
        if (FirebaseMain.getUser() != null) {
            documentReference = FirebaseMain.getFireStoreInstance()
                .collection(StaticDataPasser.userCollection)
                .document(FirebaseMain.getUser().uid)

            documentReference.get().addOnSuccessListener {

                if (it != null && it.exists()) {
                    val getVerificationStatus = it.getString("verificationStatus")

                    if (getVerificationStatus.equals("Not Verified")) {
                        showUserNotVerifiedDialog()

                    }
                }

            }.addOnFailureListener {
                Log.e(TAG, it.message.toString())
            }

        } else {
            FirebaseMain.signOutUser()

            intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun showUserNotVerifiedDialog() {
        builder = AlertDialog.Builder(this)
        builder.setCancelable(false)

        val dialogView = layoutInflater.inflate(R.layout.dialog_user_not_verified, null)
        val okBtn = dialogView.findViewById<Button>(R.id.okBtn)

        okBtn.setOnClickListener { v: View? ->
            intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()

            closeUserNotVerifiedDialog()
        }

        builder.setView(dialogView)
        userNotVerifiedDialog = builder.create()
        userNotVerifiedDialog.show()
    }

    private fun closeUserNotVerifiedDialog() {
        if (userNotVerifiedDialog != null && userNotVerifiedDialog.isShowing) {
            userNotVerifiedDialog.dismiss()
        }
    }

    override fun onBackPressed() {

        intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()

    }
}