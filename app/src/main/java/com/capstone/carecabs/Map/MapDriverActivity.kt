package com.capstone.carecabs.Map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.capstone.carecabs.BottomSheetModal.PassengerBookingsBottomSheet
import com.capstone.carecabs.BottomSheetModal.PickupPassengerBottomSheet
import com.capstone.carecabs.Chat.ChatOverviewActivity
import com.capstone.carecabs.Firebase.FirebaseMain
import com.capstone.carecabs.HelpActivity
import com.capstone.carecabs.LoginActivity
import com.capstone.carecabs.MainActivity
import com.capstone.carecabs.Model.PassengerBookingModel
import com.capstone.carecabs.Model.PickupPassengerBottomSheetData
import com.capstone.carecabs.Model.TripHistoryModel
import com.capstone.carecabs.R
import com.capstone.carecabs.TripHistoryActivity
import com.capstone.carecabs.Utility.StaticDataPasser
import com.capstone.carecabs.Utility.VoiceAssistant
import com.capstone.carecabs.databinding.ActivityMapDriverBinding
import com.capstone.carecabs.databinding.DialogCancelNavigationToPassengerBinding
import com.capstone.carecabs.databinding.DialogConfirmDropoffBinding
import com.capstone.carecabs.databinding.DialogConfirmPickupBinding
import com.capstone.carecabs.databinding.DialogEnableLocationServiceBinding
import com.capstone.carecabs.databinding.DialogExitMapBinding
import com.capstone.carecabs.databinding.DialogUserNotVerifiedBinding
import com.capstone.carecabs.databinding.MapboxItemViewAnnotationBinding
import com.capstone.carecabs.databinding.MapboxPassengerWaitingAnnotationBinding
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentReference
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.CameraAnimatorOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.lifecycle.requireMapboxNavigation
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.DistanceRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.EstimatedTimeToArrivalFormatter
import com.mapbox.navigation.ui.tripprogress.model.PercentDistanceTraveledFormatter
import com.mapbox.navigation.ui.tripprogress.model.TimeRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import com.mapbox.navigation.ui.voice.model.SpeechVolume
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.DecimalFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class MapDriverActivity : AppCompatActivity(),
    PickupPassengerBottomSheet.PickupPassengerBottomSheetListener,
    PassengerBookingsBottomSheet.PassengerBookingsBottomSheetListener,
    PickupPassengerBottomSheet.RenavigateListener {

    private val TAG = "MapDriverActivity"
    private lateinit var binding: ActivityMapDriverBinding
    private val LOCATION_PERMISSION_REQUEST_CODE = 123
    private val REQUEST_ENABLE_LOCATION = 1
    private var isNavigatingToDestination = false
    private var isRenavigating = false
    private var isAcceptedABooking = false
    private lateinit var documentReference: DocumentReference
    private lateinit var builder: AlertDialog.Builder
    private lateinit var userNotVerifiedDialog: AlertDialog
    private lateinit var enableLocationServiceDialog: AlertDialog
    private lateinit var exitMapDialog: AlertDialog
    private lateinit var cancelNavigationDialog: AlertDialog
    private lateinit var confirmPickupDialog: AlertDialog
    private lateinit var confirmDropOffDialog: AlertDialog
    private lateinit var passengerTransportedSuccessDialog: AlertDialog
    private lateinit var pleaseWaitDialog: AlertDialog
    private lateinit var voiceAssistant: VoiceAssistant
    private var voiceAssistantState = StaticDataPasser.storeVoiceAssistantState
    private var fontSize = StaticDataPasser.storeFontSize
    private var pointAnnotation: PointAnnotation? = null
    private var lastInteractedAnnotationOptions: PointAnnotationOptions? = null
    private lateinit var pointAnnotationManager: PointAnnotationManager

    //navigation
    private val mapboxReplayer = MapboxReplayer()
    private val replayLocationEngine = ReplayLocationEngine(mapboxReplayer)
    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)
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
    private lateinit var maneuverApi: MapboxManeuverApi
    private lateinit var tripProgressApi: MapboxTripProgressApi
    private lateinit var routeLineApi: MapboxRouteLineApi
    private lateinit var routeLineView: MapboxRouteLineView
    private val routeArrowApi: MapboxRouteArrowApi = MapboxRouteArrowApi()
    private lateinit var routeArrowView: MapboxRouteArrowView
    private var isVoiceInstructionsMuted = false
        set(value) {
            field = value
            if (value) {
                binding.soundBtn.muteAndExtend(BUTTON_ANIMATION_DURATION)
                voiceInstructionsPlayer.volume(SpeechVolume(0f))
            } else {
                binding.soundBtn.unmuteAndExtend(BUTTON_ANIMATION_DURATION)
                voiceInstructionsPlayer.volume(SpeechVolume(1f))
            }
        }

    private lateinit var speechApi: MapboxSpeechApi

    private lateinit var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer

    private val voiceInstructionsObserver = VoiceInstructionsObserver { voiceInstructions ->
        speechApi.generate(voiceInstructions, speechCallback)
    }

    private val speechCallback =
        MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> { expected ->
            expected.fold(
                { error ->
                    // play the instruction via fallback text-to-speech engine
                    voiceInstructionsPlayer.play(
                        error.fallback,
                        voiceInstructionsPlayerCallback
                    )
                },
                { value ->
                    // play the sound file from the external generator
                    voiceInstructionsPlayer.play(
                        value.announcement,
                        voiceInstructionsPlayerCallback
                    )
                }
            )
        }

    private val voiceInstructionsPlayerCallback =
        MapboxNavigationConsumer<SpeechAnnouncement> { value ->
            // remove already consumed file to free-up space
            speechApi.clean(value)
        }

    private val navigationLocationProvider = NavigationLocationProvider()
    private val locationObserver = object : LocationObserver {
        var firstLocationUpdateReceived = false

        override fun onNewRawLocation(rawLocation: Location) {
            // not handled
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val enhancedLocation = locationMatcherResult.enhancedLocation

            createViewAnnotation(
                binding.mapView,
                Point.fromLngLat(enhancedLocation.longitude, enhancedLocation.latitude)
            )

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

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->

        val DISTANCE_THRESHOLD = 50.0

        val remainingDistance = routeProgress.distanceRemaining

        if (remainingDistance <= DISTANCE_THRESHOLD) {
            // Driver has arrived at the destination
            handleDriverArrival()
        }

        // update the camera position to account for the progressed fragment of the route
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()

        // draw the upcoming maneuver arrow on the map
        val style = binding.mapView.getMapboxMap().getStyle()
        if (style != null) {
            val maneuverArrowResult = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
            routeArrowView.renderManeuverUpdate(style, maneuverArrowResult)
        }

        // update top banner with maneuver instructions
        val maneuvers = maneuverApi.getManeuvers(routeProgress)
        maneuvers.fold(
            { error ->
                Toast.makeText(
                    this@MapDriverActivity,
                    error.errorMessage,
                    Toast.LENGTH_SHORT
                ).show()
            },
            {
                binding.maneuverView.visibility = View.VISIBLE
                binding.maneuverView.renderManeuvers(maneuvers)
            }
        )

        // update bottom trip progress summary
        binding.tripProgressView.render(
            tripProgressApi.getTripProgress(routeProgress)
        )
    }

    private val routesObserver = RoutesObserver { routeUpdateResult ->
        if (routeUpdateResult.navigationRoutes.isNotEmpty()) {
            // generate route geometries asynchronously and render them
            routeLineApi.setNavigationRoutes(
                routeUpdateResult.navigationRoutes
            ) { value ->
                binding.mapView.getMapboxMap().getStyle()?.apply {
                    routeLineView.renderRouteDrawData(this, value)
                }
            }

            // update the camera position to account for the new route
            viewportDataSource.onRouteChanged(routeUpdateResult.navigationRoutes.first())
            viewportDataSource.evaluate()
        } else {
            // remove the route line and route arrow from the map
            val style = binding.mapView.getMapboxMap().getStyle()
            if (style != null) {
                routeLineApi.clearRouteLine { value ->
                    routeLineView.renderClearRouteLineValue(
                        style,
                        value
                    )
                }
                routeArrowView.render(style, routeArrowApi.clearArrows())
            }

            // remove the route reference from camera position evaluations
            viewportDataSource.clearRouteData()
            viewportDataSource.evaluate()
        }
    }

    private val mapboxNavigation: MapboxNavigation by requireMapboxNavigation(
        onResumedObserver = object : MapboxNavigationObserver {
            @SuppressLint("MissingPermission")
            override fun onAttached(mapboxNavigation: MapboxNavigation) {
                mapboxNavigation.registerRoutesObserver(routesObserver)
                mapboxNavigation.registerLocationObserver(locationObserver)
                mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
                mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
                mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver)
                // start the trip session to being receiving location updates in free drive
                // and later when a route is set also receiving route progress updates
                mapboxNavigation.startTripSession()
            }

            override fun onDetached(mapboxNavigation: MapboxNavigation) {
                mapboxNavigation.unregisterRoutesObserver(routesObserver)
                mapboxNavigation.unregisterLocationObserver(locationObserver)
                mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
                mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
                mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
            }
        },
        onInitialize = this::initializeNavigationComponents
    )

    private val viewAnnotationMap = mutableMapOf<Point, View>()

    private companion object {
        private const val BUTTON_ANIMATION_DURATION = 1500L
    }

    override fun onDestroy() {
        super.onDestroy()

        mapboxReplayer.finish()
        maneuverApi.cancel()
        routeLineApi.cancel()
        routeLineView.cancel()
        speechApi.cancel()
        voiceInstructionsPlayer.shutdown()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {

        val shouldExit = false
        if (shouldExit) {
            super.onBackPressed()

        } else {
            showExitMapDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tripProgressLayout.visibility = View.INVISIBLE
        binding.soundBtn.visibility = View.INVISIBLE
        binding.maneuverView.visibility = View.INVISIBLE
        binding.confirmDropOffBtn.visibility = View.GONE
        binding.confirmPickupBtn.visibility = View.GONE
        binding.bookingsImgBtn.visibility = View.GONE
        binding.navigationStatusTextView.visibility = View.GONE
        binding.pingLocationImgBtn.visibility = View.GONE
        binding.pingLocationImgBtn2.visibility = View.GONE
        binding.showTripProgressImgBtn.visibility = View.GONE

        checkIfUserIsVerified()

        binding.chatImgBtn.setOnClickListener {
            intent = Intent(this@MapDriverActivity, ChatOverviewActivity::class.java)
            startActivity(intent)
        }

        binding.mapStyleSwitch.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {
                binding.mapView.getMapboxMap().apply {
                    loadStyleUri(Style.SATELLITE_STREETS) {

                        showToast("Changed Map style to Streets")

                    }
                }
            } else {
                binding.mapView.getMapboxMap().apply {
                    loadStyleUri(Style.TRAFFIC_DAY) {

                        showToast("Changed Map style to Satellite")

                    }
                }
            }
        }

        binding.fullscreenImgBtn.setOnClickListener {
            Toast.makeText(
                this@MapDriverActivity,
                "Entered Fullscreen", Toast.LENGTH_SHORT
            ).show()

            binding.fullscreenImgBtn.visibility = View.GONE
            binding.minimizeScreenImgBtn.visibility = View.VISIBLE
            binding.bookingsImgBtn.visibility = View.VISIBLE

            binding.bottomNavigationView.visibility = View.GONE
        }

        binding.minimizeScreenImgBtn.setOnClickListener {
            Toast.makeText(
                this@MapDriverActivity,
                "Exited Fullscreen", Toast.LENGTH_SHORT
            ).show()

            binding.minimizeScreenImgBtn.visibility = View.GONE
            binding.fullscreenImgBtn.visibility = View.VISIBLE
            binding.bookingsImgBtn.visibility = View.GONE

            binding.bottomNavigationView.visibility = View.VISIBLE
        }

        binding.stopNavigationImgBtn.setOnClickListener {
            showCancelNavigationDialog()
        }
        binding.recenterBtn.setOnClickListener {
            navigationCamera.requestNavigationCameraToFollowing()
            binding.routeOverviewBtn.showTextAndExtend(BUTTON_ANIMATION_DURATION)
        }
        binding.routeOverviewBtn.setOnClickListener {
            navigationCamera.requestNavigationCameraToOverview()
            binding.recenterBtn.showTextAndExtend(BUTTON_ANIMATION_DURATION)
        }
        binding.soundBtn.setOnClickListener {
            // mute/unmute voice instructions
            isVoiceInstructionsMuted = !isVoiceInstructionsMuted
        }

        // set initial sounds button state
        binding.soundBtn.unmute()
    }

    private fun checkLocationService() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!isGpsEnabled && !isNetworkEnabled) {
            showEnableLocationServiceDialog()
        } else {
            onMapReady()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun onMapReady() {
        binding.mapView.getMapboxMap().apply {
            loadStyleUri(Style.TRAFFIC_DAY) {

                setupGesturesListener()
                initializeLocationComponent()
                initializeNavigationComponents()
                checkNavigationStatus()
                initializeBottomNavButtons("")

                binding.mapView.camera.apply {
                    val bearing = createBearingAnimator(
                        CameraAnimatorOptions.cameraAnimatorOptions(
                            -45.0
                        )
                    ) {
                        duration = 4000
                        interpolator = AccelerateDecelerateInterpolator()
                    }
                    val zoom = createZoomAnimator(
                        CameraAnimatorOptions.cameraAnimatorOptions(14.0) {
                            startValue(3.0)
                        }
                    ) {
                        duration = 4000
                        interpolator = AccelerateDecelerateInterpolator()
                    }
                    val pitch = createPitchAnimator(
                        CameraAnimatorOptions.cameraAnimatorOptions(55.0) {
                            startValue(0.0)
                        }
                    ) {
                        duration = 4000
                        interpolator = AccelerateDecelerateInterpolator()
                    }
                    playAnimatorsSequentially(zoom)
                }
            }
        }
//        binding.mapView.getMapboxMap().setCamera(
//            CameraOptions.Builder()
//                .zoom(14.0)
//                .build()
//        )
    }

    private fun zoomInCamera(coordinate: Point) {
        binding.zoomInImgBtn.visibility = View.GONE
        binding.zoomOutImgBtn.visibility = View.VISIBLE
        binding.mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .zoom(13.0)
                .center(coordinate)
                .build()
        )
    }

    private fun zoomOutCamera(coordinate: Point) {
        binding.zoomInImgBtn.visibility = View.VISIBLE
        binding.zoomOutImgBtn.visibility = View.GONE
        binding.mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .zoom(9.0)
                .center(coordinate)
                .build()
        )
    }

    private fun initializeNavigationComponents() {
        // initialize Navigation Camera
        viewportDataSource = MapboxNavigationViewportDataSource(binding.mapView.getMapboxMap())
        navigationCamera = NavigationCamera(
            binding.mapView.getMapboxMap(),
            binding.mapView.camera,
            viewportDataSource
        )
        // set the animations lifecycle listener to ensure the NavigationCamera stops
        // automatically following the user location when the map is interacted with
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

        // make sure to use the same DistanceFormatterOptions across different features
        val distanceFormatterOptions = DistanceFormatterOptions.Builder(this).build()

        // initialize maneuver api that feeds the data to the top banner maneuver view
        maneuverApi = MapboxManeuverApi(
            MapboxDistanceFormatter(distanceFormatterOptions)
        )

        // initialize bottom progress view
        tripProgressApi = MapboxTripProgressApi(
            TripProgressUpdateFormatter.Builder(this)
                .distanceRemainingFormatter(
                    DistanceRemainingFormatter(distanceFormatterOptions)
                )
                .timeRemainingFormatter(
                    TimeRemainingFormatter(this)
                )
                .percentRouteTraveledFormatter(
                    PercentDistanceTraveledFormatter()
                )
                .estimatedTimeToArrivalFormatter(
                    EstimatedTimeToArrivalFormatter(this, TimeFormat.NONE_SPECIFIED)
                )
                .build()
        )

        // initialize voice instructions api and the voice instruction player
        speechApi = MapboxSpeechApi(
            this,
            getString(R.string.mapbox_access_token),
            Locale.US.language
        )
        voiceInstructionsPlayer = MapboxVoiceInstructionsPlayer(
            this,
            getString(R.string.mapbox_access_token),
            Locale.US.language
        )

        // initialize route line, the withRouteLineBelowLayerId is specified to place
        // the route line below road labels layer on the map
        // the value of this option will depend on the style that you are using
        // and under which layer the route line should be placed on the map layers stack
        val mapboxRouteLineOptions = MapboxRouteLineOptions.Builder(this)
            .withRouteLineBelowLayerId("road-label-navigation")
            .build()
        routeLineApi = MapboxRouteLineApi(mapboxRouteLineOptions)
        routeLineView = MapboxRouteLineView(mapboxRouteLineOptions)

        // initialize maneuver arrow view to draw arrows on the map
        val routeArrowOptions = RouteArrowOptions.Builder(this).build()
        routeArrowView = MapboxRouteArrowView(routeArrowOptions)

    }

    private fun initializeLocationComponent() {

        MapboxNavigationApp.setup(
            NavigationOptions.Builder(this)
                .accessToken(getString(R.string.mapbox_access_token))
                // comment out the location engine setting block to disable simulation
//                .locationEngine(replayLocationEngine)
                .build()
        )

        // initialize location puck
        binding.mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            this.locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    this@MapDriverActivity,
                    R.drawable.mapbox_navigation_puck_icon
                )
            )
            enabled = true
        }
    }

    private fun initializeBottomNavButtons(bookingID: String) {
        binding.bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.trips -> {
                    intent = Intent(this, TripHistoryActivity::class.java)
                    startActivity(intent)
                }

                R.id.bookings -> {
                    showPassengerBookingsBottomSheet(bookingID)
                }

                R.id.help -> {
                    intent = Intent(this, HelpActivity::class.java)
                    startActivity(intent)
                }
            }

            true
        }
    }

    private fun setupGesturesListener() {
//        binding.mapView.gestures.addOnMoveListener(onMoveListener)

        binding.mapView.gestures.addOnMapLongClickListener {
            findRoute(it)
            true
        }

//        binding.mapView.gestures.addOnMapClickListener {
//            findRoute(it)
//            true
//        }
//        pointAnnotationManager?.apply {
//            addClickListener(
//                OnPointAnnotationClickListener {
//                    Toast.makeText(this@MapPassengerActivity, "id: ${it.id}", Toast.LENGTH_LONG).show()
//                    false
//                }
//            )
//        }
    }

    @SuppressLint("SetTextI18n")
    private fun checkNavigationStatus() {
        val userID = FirebaseMain.getUser().uid

        val bookingReference = FirebaseDatabase.getInstance()
            .getReference(FirebaseMain.bookingCollection)

        bookingReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {

                    for (bookingSnapshot in snapshot.children) {
                        val bookingData =
                            bookingSnapshot.getValue(PassengerBookingModel::class.java)

                        if (bookingData != null) {

                            val getBookingID = bookingData.bookingID

                            if (bookingData.bookingStatus == "Waiting") {
                                loadPassengerLocationToMapFromDatabase()

                            } else if (bookingData.bookingStatus == "Driver on the way"
                                && bookingData.driverUserID == FirebaseMain.getUser().uid
                            ) {
                                val getPassengerType = bookingData.passengerType
                                val getPickupLongitude = bookingData.pickupLongitude
                                val getPickupLatitude = bookingData.pickupLatitude

                                binding.navigationStatusTextView.visibility = View.VISIBLE
                                binding.confirmPickupBtn.visibility = View.VISIBLE
                                binding.confirmDropOffBtn.visibility = View.GONE
                                binding.pingLocationImgBtn.visibility = View.VISIBLE
                                binding.pingLocationImgBtn2.visibility = View.VISIBLE

                                binding.pingLocationImgBtn2.setOnClickListener {
                                    pingCurrentLocation(getBookingID)
                                }

                                binding.pingLocationImgBtn.setOnClickListener {
                                    pingCurrentLocation(getBookingID)
                                }

                                binding.passengersInMapTextView.text =
                                    "You are currently navigating to Passenger's pickup location"
                                binding.navigationStatusTextView.text =
                                    "Navigating to\nPassenger's pickup location"

                                binding.bookingsImgBtn.setOnClickListener {
                                    showPassengerBookingsBottomSheet(getBookingID)
                                }

                                initializeBottomNavButtons(getBookingID)

                                when (getPassengerType) {
                                    "Senior Citizen" -> {
                                        addSeniorAnnotationToMap(
                                            getPickupLongitude,
                                            getPickupLatitude,
                                            getBookingID
                                        )
                                    }

                                    "Person with Disabilities (PWD)" -> {
                                        addPWDAnnotationToMap(
                                            getPickupLongitude,
                                            getPickupLatitude,
                                            getBookingID
                                        )
                                    }
                                }


                            } else if (bookingData.bookingStatus == "Passenger Onboard" &&
                                bookingData.driverUserID == userID
                            ) {
                                val getPassengerID = bookingData.passengerUserID
                                val getTripID = bookingData.tripID
                                val getDestinationLatitude = bookingData.destinationLatitude
                                val getDestinationLongitude = bookingData.destinationLongitude

                                binding.pingLocationImgBtn.visibility = View.GONE
                                binding.pingLocationImgBtn2.visibility = View.GONE
                                binding.navigationStatusTextView.visibility = View.VISIBLE
                                binding.navigationStatusTextView.text =
                                    "Navigating to\nPassenger's destination"
                                binding.passengersInMapTextView.text =
                                    "You are currently navigating to Passenger's destination"

                                //convert to point
                                val destinationLatLng =
                                    LatLng(getDestinationLatitude, getDestinationLongitude)

                                val destinationCoordinates =
                                    Point.fromLngLat(
                                        destinationLatLng.longitude,
                                        destinationLatLng.latitude
                                    )

                                createDestinationAnnotationToMap(destinationCoordinates)

                                binding.confirmPickupBtn.visibility = View.GONE
                                binding.confirmDropOffBtn.visibility = View.VISIBLE
                                binding.confirmDropOffBtn.setOnClickListener {
                                    showConfirmDropOffDialog(
                                        tripID = getTripID,
                                        bookingID = getBookingID,
                                        passengerID = getPassengerID
                                    )
                                }
                            }
                        } else {
                            loadPassengerLocationToMapFromDatabase()
                        }
                    }
                } else {
                    binding.passengersInMapTextView.text = "There are no Passengers right now"

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "checkNavigationStatus - onCancelled: $error")
            }

        })

        val driverReference = FirebaseMain.getFireStoreInstance()
            .collection(FirebaseMain.userCollection)
            .document(userID)

//        driverReference.get()
//            .addOnSuccessListener {
//                if (it.exists()) {
//                    val getNavigationStatus: String = it.getString("navigationStatus") ?: "idle"
//                    val getBookingID: String = it.getString("bookingID") ?: "none"
//
//                    when (getNavigationStatus) {
//                        "Navigating to destination" -> {
//                            isNavigatingToDestination = true
//
//                            val getTripID: String = it.getString("tripID") ?: "none"
//                            val getPassengerID: String = it.getString("passengerID") ?: "none"
//                            val getDestinationLatitude: Double =
//                                it.getDouble("destinationLatitude") ?: 0.0
//                            val getDestinationLongitude: Double =
//                                it.getDouble("destinationLongitude") ?: 0.0
//
//                            binding.pingLocationImgBtn.visibility = View.GONE
//                            binding.pingLocationImgBtn2.visibility = View.GONE
//                            binding.navigationStatusTextView.visibility = View.VISIBLE
//                            binding.navigationStatusTextView.text =
//                                "Navigating to\nPassenger's destination"
//                            binding.passengersInMapTextView.text =
//                                "You are currently navigating to Passenger's destination"
//
//                            //convert to point
//                            val destinationLatLng =
//                                LatLng(getDestinationLatitude, getDestinationLongitude)
//
//                            val destinationCoordinates =
//                                Point.fromLngLat(
//                                    destinationLatLng.longitude,
//                                    destinationLatLng.latitude
//                                )
//
//                            createDestinationAnnotationToMap(destinationCoordinates)
//
//                            binding.confirmPickupBtn.visibility = View.GONE
//                            binding.confirmDropOffBtn.visibility = View.VISIBLE
//                            binding.confirmDropOffBtn.setOnClickListener {
//                                showConfirmDropOffDialog(
//                                    tripID = getTripID,
//                                    bookingID = getBookingID,
//                                    passengerID = getPassengerID
//                                )
//                            }
//                        }
//
//                        "Navigating to pickup location" -> {
//                            val getPickupLatitude: Double = it.getDouble("pickupLatitude") ?: 0.0
//                            val getPickupLongitude: Double = it.getDouble("pickupLongitude") ?: 0.0
//                            val getPassengerType: String = it.getString("passengerType") ?: "none"
//
//                            binding.navigationStatusTextView.visibility = View.VISIBLE
//                            binding.confirmPickupBtn.visibility = View.VISIBLE
//                            binding.confirmDropOffBtn.visibility = View.GONE
//                            binding.pingLocationImgBtn.visibility = View.VISIBLE
//                            binding.pingLocationImgBtn2.visibility = View.VISIBLE
//
//                            binding.pingLocationImgBtn2.setOnClickListener {
//                                pingCurrentLocation(getBookingID)
//                            }
//
//                            binding.pingLocationImgBtn.setOnClickListener {
//                                pingCurrentLocation(getBookingID)
//                            }
//
//                            binding.passengersInMapTextView.text =
//                                "You are currently navigating to Passenger's pickup location"
//                            binding.navigationStatusTextView.text =
//                                "Navigating to\nPassenger's pickup location"
//
//                            binding.bookingsImgBtn.setOnClickListener {
//                                showPassengerBookingsBottomSheet(getBookingID)
//                            }
//
//                            initializeBottomNavButtons(getBookingID)
//
//                            when (getPassengerType) {
//                                "Senior Citizen" -> {
//                                    addSeniorAnnotationToMap(
//                                        getPickupLongitude,
//                                        getPickupLatitude,
//                                        getBookingID
//                                    )
//                                }
//
//                                "Person with Disabilities (PWD)" -> {
//                                    addPWDAnnotationToMap(
//                                        getPickupLongitude,
//                                        getPickupLatitude,
//                                        getBookingID
//                                    )
//                                }
//                            }
//                        }
//
//                        else -> {
//                            loadPassengerLocationToMapFromDatabase()
//                        }
//                    }
//                } else {
//                    loadPassengerLocationToMapFromDatabase()
//
//                    Log.e(TAG, "checkIfCurrentlyNavigatingToDestination: driverReference is null")
//                }
//            }
//            .addOnFailureListener {
//                Log.e(TAG, "checkIfCurrentlyNavigatingToDestination: " + it.message)
//            }
    }

    private fun pingCurrentLocation(bookingID: String) {
        if (isRenavigating) {
            showToast("Can't ping current location because you are not picking up a Passenger")

        } else {
            val userID = FirebaseMain.getUser().uid
            var locationName: String

            val driverReference = FirebaseMain.getFireStoreInstance()
                .collection(FirebaseMain.userCollection)
                .document(userID)

            //ping location geocode
            val pickupLocationGeocode = MapboxGeocoding.builder()
                .accessToken(getString(R.string.mapbox_access_token))
                .query(
                    Point.fromLngLat(
                        StaticDataPasser.storePingedLongitude,
                        StaticDataPasser.storePingedLatitude
                    )
                )
                .geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
                .build()

            pickupLocationGeocode.enqueueCall(object :
                Callback<GeocodingResponse?> {
                @SuppressLint("SetTextI18n", "LongLogTag")
                override fun onResponse(
                    call: Call<GeocodingResponse?>,
                    response: Response<GeocodingResponse?>
                ) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null && body.features().isNotEmpty()
                        ) {
                            val feature = body.features()[0]
                            locationName = feature.placeName().toString()

                            val pingDriverLocation = HashMap<String, Any>()
                            pingDriverLocation["driverPingedLocation"] = locationName
                            pingDriverLocation["driverPingedLongitude"] =
                                StaticDataPasser.storePingedLongitude
                            pingDriverLocation["driverPingedLatitude"] =
                                StaticDataPasser.storePingedLatitude

                            driverReference.update(pingDriverLocation)
                                .addOnSuccessListener {
                                    showToast("Current location pinged")
                                }
                                .addOnFailureListener { exception ->
                                    Log.e(
                                        TAG,
                                        "checkIfCurrentlyNavigatingToDestination: " + exception.message
                                    )
                                }

                            val bookingReference = FirebaseDatabase.getInstance()
                                .getReference(FirebaseMain.bookingCollection)

                            val updateCurrentBooking = HashMap<String, Any>()
                            updateCurrentBooking["driverPingedLocation"] = locationName
                            updateCurrentBooking["driverPingedLongitude"] =
                                StaticDataPasser.storePingedLongitude
                            updateCurrentBooking["driverPingedLatitude"] =
                                StaticDataPasser.storePingedLatitude

                            bookingReference.child(bookingID)
                                .updateChildren(updateCurrentBooking)
                                .addOnSuccessListener {
                                    Log.i(
                                        TAG,
                                        "checkIfCurrentlyNavigatingToDestination onResponse: pinged location success"
                                    )
                                }
                                .addOnFailureListener {
                                    Log.e(
                                        TAG,
                                        "checkIfCurrentlyNavigatingToDestination onResponse: " + it.message
                                    )
                                }

                        } else {
                            Log.e(TAG, "location not found")
                        }
                    } else {
                        Log.e(TAG, "Geocode error " + response.message())
                    }
                }

                @SuppressLint("SetTextI18n", "LongLogTag")
                override fun onFailure(
                    call: Call<GeocodingResponse?>,
                    t: Throwable
                ) {
                    Log.e(TAG, "onFailure: " + t.message)
                }
            })
        }
    }

    private fun loadPassengerLocationToMapFromDatabase() {

        var waitingPassengerCount = 0
        val locationReference = FirebaseDatabase.getInstance()
            .getReference(FirebaseMain.bookingCollection)

        locationReference.addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    var hasPassengersWaiting = false

                    for (locationSnapshot in snapshot.children) {
                        val bookingData =
                            locationSnapshot.getValue(PassengerBookingModel::class.java)

                        if (bookingData != null) {
                            if (bookingData.bookingStatus == "Waiting") {

                                binding.bookingsImgBtn.setOnClickListener {
                                    showPassengerBookingsBottomSheet(bookingData.bookingID)
                                }

                                initializeBottomNavButtons(bookingData.bookingID)

                                waitingPassengerCount++
                                hasPassengersWaiting = true

                                when (bookingData.passengerType) {
                                    "Senior Citizen" -> {
                                        addSeniorAnnotationToMap(
                                            bookingData.pickupLongitude,
                                            bookingData.pickupLatitude,
                                            bookingData.bookingID
                                        )
                                    }

                                    "Person with Disabilities (PWD)" -> {
                                        addPWDAnnotationToMap(
                                            bookingData.pickupLongitude,
                                            bookingData.pickupLatitude,
                                            bookingData.bookingID
                                        )
                                    }
                                }

                            } else if (bookingData.bookingStatus == "Driver on the way"
                                && bookingData.driverUserID == FirebaseMain.getUser().uid
                            ) {
                                binding.bookingsImgBtn.setOnClickListener {
                                    showPassengerBookingsBottomSheet(bookingData.bookingID)
                                }

                                initializeBottomNavButtons(bookingData.bookingID)

                                when (bookingData.passengerType) {
                                    "Senior Citizen" -> {
                                        addSeniorAnnotationToMap(
                                            bookingData.pickupLongitude,
                                            bookingData.pickupLatitude,
                                            bookingData.bookingID
                                        )
                                    }

                                    "Person with Disabilities (PWD)" -> {
                                        addPWDAnnotationToMap(
                                            bookingData.pickupLongitude,
                                            bookingData.pickupLatitude,
                                            bookingData.bookingID
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (isNavigatingToDestination) {
                        binding.passengersInMapTextView.text =
                            "You are currently navigating to Passenger's destination"

                        binding.pingLocationImgBtn.visibility = View.VISIBLE
                        binding.pingLocationImgBtn2.visibility = View.VISIBLE

                    } else {
                        if (hasPassengersWaiting) {
                            binding.passengersInMapTextView.text =
                                "There are $waitingPassengerCount Passenger(s) waiting right now"
                        } else {
                            binding.passengersInMapTextView.text =
                                "There are no Passengers right now"
                        }
                    }
                } else {
                    binding.passengersInMapTextView.text = "There are no Passengers right now"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "loadPassengerLocationToMapFromDatabase: " + error.message)
            }
        })
    }

    private fun addSeniorAnnotationToMap(
        longitude: Double,
        latitude: Double,
        bookingID: String
    ) {

        val latLng = LatLng(
            latitude, longitude
        )
        val point = Point.fromLngLat(latLng.longitude, latLng.latitude)

//        createPassengersWaitingViewAnnotation(binding.mapView, point)

        bitmapFromDrawableRes(
            this@MapDriverActivity,
            R.drawable.senior_location_pin_128
        ).let {
            val annotationApi = binding.mapView.annotations
            pointAnnotationManager = annotationApi.createPointAnnotationManager()
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(Point.fromLngLat(longitude, latitude))
                .withIconImage(it)

            pointAnnotation = pointAnnotationManager.create(pointAnnotationOptions)

            pointAnnotationManager.apply {
                addClickListener(
                    OnPointAnnotationClickListener {
                        lastInteractedAnnotationOptions = pointAnnotationOptions
                        showPickupPassengerBottomSheet(bookingID)
                        true
                    }
                )
            }
        }
    }

    private fun addPWDAnnotationToMap(
        longitude: Double,
        latitude: Double,
        bookingID: String
    ) {
        val latLng = LatLng(
            latitude, longitude
        )
        val point = Point.fromLngLat(latLng.longitude, latLng.latitude)

//        createPassengersWaitingViewAnnotation(binding.mapView, point)

        bitmapFromDrawableRes(
            this@MapDriverActivity,
            R.drawable.pwd_location_pin_128
        ).let {
            val annotationApi = binding.mapView.annotations
            pointAnnotationManager = annotationApi.createPointAnnotationManager()
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(Point.fromLngLat(longitude, latitude))
                .withIconImage(it)

            pointAnnotation = pointAnnotationManager.create(pointAnnotationOptions)

            pointAnnotationManager.apply {
                addClickListener(
                    OnPointAnnotationClickListener {
                        lastInteractedAnnotationOptions = pointAnnotationOptions
                        showPickupPassengerBottomSheet(bookingID)
                        true
                    }
                )
            }
        }
    }

    private fun createDestinationAnnotationToMap(destination: Point) {
        bitmapFromDrawableRes(
            this@MapDriverActivity,
            R.drawable.location_pin_128
        ).let {
            val annotationApi = binding.mapView.annotations
            pointAnnotationManager = annotationApi.createPointAnnotationManager()
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(
                    Point.fromLngLat(
                        destination.longitude(),
                        destination.latitude()
                    )
                )
                .withIconImage(it)

            pointAnnotation = pointAnnotationManager.create(pointAnnotationOptions)

            if (isNavigatingToDestination) {
                pointAnnotationManager.apply {
                    addClickListener(
                        OnPointAnnotationClickListener {
                            findRoute(destination)
                            true
                        }
                    )
                }
            }
        }
    }

    private fun removeDestinationAnnotationFromMap() {
        pointAnnotation?.let { pointAnnotationManager.delete(it) }
    }

    private fun removeAllAnnotationsExceptLastInteracted() {
        val lastInteracted = lastInteractedAnnotationOptions
        pointAnnotationManager.deleteAll()
        lastInteracted?.let { pointAnnotationManager.create(it) }
    }

    private fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int) =
        convertDrawableToBitmap(AppCompatResources.getDrawable(context, resourceId))

    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap {

        return if (sourceDrawable is BitmapDrawable) {
            sourceDrawable.bitmap
        } else {
            val constantState = sourceDrawable?.constantState
            val drawable = constantState?.newDrawable()?.mutate()
            val bitmap: Bitmap = Bitmap.createBitmap(
                drawable!!.intrinsicWidth, drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    private fun onCameraTrackingDismissed() {
//        Toast.makeText(this, "onCameraTrackingDismissed", Toast.LENGTH_SHORT).show()

//        binding.mapView.location
//            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
//        binding.mapView.location
//            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
//        binding.mapView.gestures.removeOnMoveListener(onMoveListener)
    }

    private fun createPassengersWaitingViewAnnotation(mapView: MapView, coordinate: Point) {
        if (viewAnnotationMap[coordinate] == null) {
            mapView.viewAnnotationManager.removeAllViewAnnotations()
            val viewAnnotation = mapView.viewAnnotationManager.addViewAnnotation(
                resId = R.layout.mapbox_passenger_waiting_annotation,
                options = viewAnnotationOptions {
                    geometry(coordinate)
                    offsetY(170)
                },
            ).also { view ->
                viewAnnotationMap[coordinate] = view
            }
//            val locationText = """
//                My Location:
//                Longitude = ${coordinate.longitude()}
//                Latitude = ${coordinate.latitude()}
//            """.trimIndent()
//
            MapboxPassengerWaitingAnnotationBinding.bind(viewAnnotation).apply {
                annotationBackground.clipToOutline = true
            }
        }
    }

    //displays the the "you"
    private fun createViewAnnotation(mapView: MapView, coordinate: Point) {

        //store the current location
        StaticDataPasser.storePingedLatitude = coordinate.latitude()
        StaticDataPasser.storePingedLongitude = coordinate.longitude()

        binding.zoomInImgBtn.setOnClickListener {
            zoomInCamera(coordinate)
        }

        binding.zoomOutImgBtn.setOnClickListener {
            zoomOutCamera(coordinate)
        }

        if (viewAnnotationMap[coordinate] == null) {
            mapView.viewAnnotationManager.removeAllViewAnnotations()
            val viewAnnotation = mapView.viewAnnotationManager.addViewAnnotation(
                resId = R.layout.mapbox_item_view_annotation,
                options = viewAnnotationOptions {
                    geometry(coordinate)
                    offsetY(170)
                },
            ).also { view ->
                viewAnnotationMap[coordinate] = view
            }
//            val locationText = """
//                My Location:
//                Longitude = ${coordinate.longitude()}
//                Latitude = ${coordinate.latitude()}
//            """.trimIndent()
//
            MapboxItemViewAnnotationBinding.bind(viewAnnotation).apply {
                annotationBackground.clipToOutline = true
            }
        }
    }

    //triggers renavigate
    override fun onRenavigateClick(isClicked: Boolean) {
        if (isClicked) {
            isRenavigating = true
        }
    }

    private fun findRoute(destination: Point) {

        binding.hideTripProgressImgBtn.setOnClickListener {
            binding.tripProgressLayout.visibility = View.GONE
            binding.showTripProgressImgBtn.visibility = View.VISIBLE
            binding.pingLocationImgBtn2.visibility = View.VISIBLE
        }

        binding.showTripProgressImgBtn.setOnClickListener {
            binding.tripProgressLayout.visibility = View.VISIBLE
            binding.showTripProgressImgBtn.visibility = View.GONE
            binding.pingLocationImgBtn2.visibility = View.GONE
        }

        binding.mapView.getMapboxMap().apply {
            loadStyleUri(Style.TRAFFIC_DAY) {
            }
        }

        if (!isRenavigating) {
            binding.confirmPickupBtn.visibility = View.GONE
        }

        if (isNavigatingToDestination) {
            showToast("Navigating to Passenger's destination")
        } else {
            showToast("Navigating to Passenger's pickup location")
        }

        val originLocation = navigationLocationProvider.lastLocation
        val originPoint = originLocation?.let {
            Point.fromLngLat(it.longitude, it.latitude)
        } ?: return

        // execute a route request
        // it's recommended to use the
        // applyDefaultNavigationOptions and applyLanguageAndVoiceUnitOptions
        // that make sure the route request is optimized
        // to allow for support of all of the Navigation SDK features
        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(this)
                .coordinatesList(listOf(originPoint, destination))
                // provide the bearing for the origin of the request to ensure
                // that the returned route faces in the direction of the current user movement
                .bearingsList(
                    listOf(
                        Bearing.builder()
                            .angle(originLocation.bearing.toDouble())
                            .degrees(45.0)
                            .build(),
                        null
                    )
                )
                .layersList(listOf(mapboxNavigation.getZLevel(), null))
                .build(),
            object : NavigationRouterCallback {
                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    Log.i(TAG, routeOptions.toString())
                }

                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    Log.e(TAG, reasons.toString())
                }

                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    setRouteAndStartNavigation(routes)
                }
            }
        )
    }

    private fun setRouteAndStartNavigation(routes: List<NavigationRoute>) {
        // set routes, where the first route in the list is the primary route that
        // will be used for active guidance
        mapboxNavigation.setNavigationRoutes(routes)

        // show UI elements
        binding.soundBtn.visibility = View.VISIBLE
        binding.routeOverviewBtn.visibility = View.VISIBLE
        binding.tripProgressLayout.visibility = View.VISIBLE

        binding.bottomNavigationView.visibility = View.GONE
        binding.fullscreenImgBtn.visibility = View.GONE
        binding.minimizeScreenImgBtn.visibility = View.GONE

        // move the camera to overview when new route is available
        navigationCamera.requestNavigationCameraToOverview()
    }

    private fun clearRouteAndStopNavigation() {

        showToast("Cancelled Navigation")

        // clear
        mapboxNavigation.setNavigationRoutes(listOf())

        binding.mapView.getMapboxMap().apply {
            loadStyleUri(Style.TRAFFIC_DAY) {
            }
        }

        // stop simulation
        mapboxReplayer.stop()

        // hide UI elements
        binding.soundBtn.visibility = View.INVISIBLE
        binding.maneuverView.visibility = View.INVISIBLE
        binding.routeOverviewBtn.visibility = View.INVISIBLE
        binding.tripProgressLayout.visibility = View.INVISIBLE

        binding.bottomNavigationView.visibility = View.VISIBLE
        binding.fullscreenImgBtn.visibility = View.VISIBLE
    }

    fun calculateTravelTime(distance: Double, averageSpeed: Double): Double {
        // Distance is in kilometers, averageSpeed is in kilometers per hour
        return distance / averageSpeed // Travel time in hours
    }

    fun calculateArrivalTime(travelTime: Double): Long {
        val currentTimeMillis = System.currentTimeMillis()
        val travelTimeMillis =
            (travelTime * 60 * 60 * 1000).toLong() // Convert travel time to milliseconds
        return currentTimeMillis + travelTimeMillis
    }

    private fun handleDriverArrival() {

        val driverReference = FirebaseMain.getFireStoreInstance()
            .collection(FirebaseMain.userCollection)
            .document(FirebaseMain.getUser().uid)

        driverReference.get()
            .addOnSuccessListener {

                if (it.exists()) {
                    val currentPassengersTransported =
                        it.getLong("passengersTransported") ?: 0
                    val newPassengersTransported = currentPassengersTransported + 1
                    val currentDriverRating = it.getDouble("driverRatings") ?: 0.0
                    val newDriverRating = currentDriverRating + 3.0

                    val updateDriverStatus = HashMap<String, Any>()
                    updateDriverStatus["isAvailable"] = true
                    updateDriverStatus["navigationStatus"] = "idle"
                    updateDriverStatus["driverPingedLocation"] = "none"
                    updateDriverStatus["driverPingedLatitude"] = 0.0
                    updateDriverStatus["driverPingedLongitude"] = 0.0
                    updateDriverStatus["passengersTransported"] = newPassengersTransported
                    updateDriverStatus["driverRatings"] = newDriverRating
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "handleDriverArrival: " + it.message)
            }

        val tripReference = FirebaseMain.getFireStoreInstance()
            .collection(FirebaseMain.tripCollection)
            .document()

        if (isNavigatingToDestination) {

            //update driver status
            val updateDriverStatus = HashMap<String, Any>()
            updateDriverStatus["tripID"] = "none"

            driverReference.get()
                .addOnSuccessListener {

                }
                .addOnFailureListener {
                    Log.e(TAG, "handleDriverArrival: " + it.message)
                }

            //update trip
            val updateTrip = HashMap<String, Any>()
            updateTrip["tripStatus"] = "Passenger has transported to destination"

            tripReference.update(updateTrip)
                .addOnSuccessListener {

                }
                .addOnFailureListener {
                    Log.e(TAG, "handleDriverArrival: " + it.message)
                }

        } else {
            //update driver status
            val updateDriverStatus = HashMap<String, Any>()
            updateDriverStatus["tripID"] = ""

            driverReference.get()
                .addOnSuccessListener { }
                .addOnFailureListener {
                    Log.e(TAG, "handleDriverArrival: " + it.message)
                }

            //update trip
            val updateTrip = HashMap<String, Any>()
            updateTrip["tripStatus"] = "Passenger has transported to destination"

            tripReference.update(updateTrip)
                .addOnSuccessListener {
                    isNavigatingToDestination = true
                }
                .addOnFailureListener {
                    Log.e(TAG, "handleDriverArrival: " + it.message)
                }
        }
    }

    private fun setTripAsComplete(
        tripID: String,
        bookingID: String,
        passengerID: String
    ) {

        //update current booking
        val bookingReference = FirebaseDatabase.getInstance()
            .getReference(FirebaseMain.bookingCollection)

        val updateBooking = HashMap<String, Any>()
        updateBooking["bookingStatus"] = "Transported to destination"
        updateBooking["ratingStatus"] = "Driver not rated"

        bookingReference.child(bookingID).updateChildren(updateBooking)
            .addOnSuccessListener {
                Log.i(TAG, "setTripAsComplete: bookingReference updated successfully ")
            }
            .addOnFailureListener {
                Log.e(TAG, "setTripAsComplete - bookingReference: " + it.message)
            }

        //update current trip
        val tripReference = FirebaseMain.getFireStoreInstance()
            .collection(FirebaseMain.tripCollection)
            .document(tripID)

        val updateTrip = HashMap<String, Any>()
        updateTrip["tripStatus"] = "Passenger has transported to destination"

        tripReference.update(updateTrip)
            .addOnSuccessListener {
                clearRouteAndStopNavigation()
            }
            .addOnFailureListener {
                Log.e(TAG, "setTripAsComplete - tripReference: " + it.message)
            }

        //update driver status
        val driverReference = FirebaseMain.getFireStoreInstance()
            .collection(FirebaseMain.userCollection)
            .document(FirebaseMain.getUser().uid)

        driverReference.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {

                    val getCurrentPassengersTransported: Long =
                        documentSnapshot.getLong("passengersTransported") ?: 0
                    val addPassengersTransported = getCurrentPassengersTransported + 1

                    val currentDriverRating: Double =
                        documentSnapshot.getDouble("driverRatings") ?: 0.0
                    val newDriverRating = currentDriverRating + 3.0
                    val getUsersRated = documentSnapshot.getLong("usersRated") ?: 0

                    val updateDriverInfo = HashMap<String, Any>()

//                    if (getUsersRated <= 0) {
//                        updateDriverInfo["driverRatings"] = newDriverRating
//
//                    } else {
//                        val calculatedRatings = currentDriverRating / getUsersRated
//                        val decimalFormat = DecimalFormat("#.##")
//                        val formattedRatings: Double =
//                            decimalFormat.format(calculatedRatings).toDouble()
//                        updateDriverInfo["driverRatings"] = formattedRatings
//                    }

                    updateDriverInfo["isAvailable"] = true
                    updateDriverInfo["navigationStatus"] = "idle"
                    updateDriverInfo["driverPingedLocation"] = "none"
                    updateDriverInfo["driverPingedLatitude"] = 0.0
                    updateDriverInfo["driverPingedLongitude"] = 0.0
                    updateDriverInfo["driverRatings"] = newDriverRating
                    updateDriverInfo["passengersTransported"] = addPassengersTransported

                    driverReference.update(updateDriverInfo)
                        .addOnSuccessListener {

                            closeConfirmDropOffDialog()
                            showPassengerTransportedSuccessDialog()
                            removeDestinationAnnotationFromMap()
                        }
                        .addOnFailureListener {
                            Log.e(TAG, "setTripAsComplete - driverReference: " + it.message)
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(
                    TAG,
                    "Error getting document in setTripAsComplete - driverReference: $exception"
                )
            }

        val passengerReference = FirebaseMain.getFireStoreInstance()
            .collection(FirebaseMain.userCollection)
            .document(passengerID)

        passengerReference.get()
            .addOnSuccessListener {
                if (it.exists()) {

                    val currentTotalTrips: Long = it.getLong("totalTrips") ?: 0
                    val addTotalTrips = currentTotalTrips + 1

                    val updateTotalTrips = HashMap<String, Any>()
                    updateTotalTrips["totalTrips"] = addTotalTrips

                    passengerReference.update(updateTotalTrips)
                        .addOnSuccessListener {
                            Log.i(TAG, "setTripAsComplete: passengerReference updated successfully")
                        }
                        .addOnFailureListener {
                            Log.e(
                                TAG,
                                "setTripAsComplete - passengerReference" + it.message
                            )
                        }
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "setTripAsComplete - passengerReference: " + it.message)
            }
    }

    private fun getCurrentTimeAndDate(): String {
        val calendar = Calendar.getInstance() // Get a Calendar instance
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Months are 0-based, so add 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        return "$month-$day-$year $hour:$minute:$second"
    }

    private fun updateDriverAvailabilityStatus() {
        if (FirebaseMain.getUser() != null) {
            FirebaseMain.getFireStoreInstance()
                .collection(FirebaseMain.userCollection)
                .document(FirebaseMain.getUser().uid)
                .update("isAvailable", false)
        }
    }

    private fun updatePassengerBooking(
        bookingID: String
    ) {
        //convert to point
        val latLng = LatLng(
            StaticDataPasser.storeDestinationLatitude,
            StaticDataPasser.storeDestinationLongitude
        )
        val point = Point.fromLngLat(latLng.latitude, latLng.longitude)


        val bookingReference = FirebaseDatabase.getInstance()
            .getReference(FirebaseMain.bookingCollection)

        val updateBooking = HashMap<String, Any>()
        updateBooking["bookingStatus"] = "Driver on the way"
        updateBooking["driverUserID"] = FirebaseMain.getUser().uid

        bookingReference.child(bookingID)
            .updateChildren(updateBooking)
            .addOnSuccessListener {

                showToast("Booking Accepted")

                findRoute(point)
            }
            .addOnFailureListener {
                Log.e(TAG, it.message.toString())

                showToast("Booking failed to Accept")
            }
    }

    private fun generateRandomTripID(): String {
        val uuid = UUID.randomUUID()
        return uuid.toString()
    }

    //start the trip when the passenger is on board
    private fun storeTripToDatabase(
        tripID: String,
        bookingID: String,
        passengerID: String,
        passengerName: String,
        driverName: String,
        passengerType: String,
        pickupLocation: String,
        pickupCoordinate: Point,
        destination: String,
        destinationCoordinate: Point
    ) {

        //update current booking
        val bookingReference = FirebaseDatabase.getInstance()
            .getReference(FirebaseMain.bookingCollection)

        val updateBooking = HashMap<String, Any>()
        updateBooking["bookingStatus"] = "Passenger Onboard"
        updateBooking["tripID"] = tripID
        updateBooking["pickupTime"] = getCurrentTimeAndDate()

        bookingReference.child(bookingID).updateChildren(updateBooking)
            .addOnSuccessListener {
                Log.i(TAG, "storeTripToDatabase: bookingReference updated successfully")
            }
            .addOnFailureListener {
                Log.e(TAG, "storeTripToDatabase: bookingReference " + it.message)
            }

        //update current trip
        val tripReference = FirebaseMain.getFireStoreInstance()
            .collection(FirebaseMain.tripCollection)
            .document(tripID)

        val tripHistoryModel = TripHistoryModel(
            tripID = tripID,
            bookingID = bookingID,
            tripStatus = "Passenger Onboard",
            driverUserID = FirebaseMain.getUser().uid,
            passengerName = passengerName,
            passengerType = passengerType,
            passengerUserID = passengerID,
            driverName = driverName,
            tripDate = getCurrentTimeAndDate(),
            pickupLocation = pickupLocation,
            pickupLongitude = pickupCoordinate.longitude(),
            pickupLatitude = pickupCoordinate.latitude(),
            destination = destination,
            destinationLongitude = destinationCoordinate.longitude(),
            destinationLatitude = destinationCoordinate.latitude()
        )

        tripReference.set(tripHistoryModel)
            .addOnSuccessListener {
                showToast("Navigating to destination")
            }
            .addOnFailureListener { e ->
                showToast("Failed to navigate to destination")

                Log.e(TAG, "storeTripToDatabase: " + e.message)
            }

        //update driver status
        val driverReference = FirebaseMain.getFireStoreInstance()
            .collection(FirebaseMain.userCollection)
            .document(FirebaseMain.getUser().uid)

        val updateDriverStatus = HashMap<String, Any>()
        updateDriverStatus["tripID"] = tripID
        updateDriverStatus["passengerID"] = passengerID
        updateDriverStatus["navigationStatus"] = "Navigating to destination"
        updateDriverStatus["destination"] = destination
        updateDriverStatus["destinationLatitude"] = destinationCoordinate.latitude()
        updateDriverStatus["destinationLongitude"] = destinationCoordinate.longitude()

        driverReference.update(updateDriverStatus)
            .addOnSuccessListener {

                isNavigatingToDestination = true

                closeConfirmPickupDialog()
                createDestinationAnnotationToMap(destinationCoordinate)
                findRoute(destinationCoordinate)
                checkNavigationStatus()

                binding.confirmPickupBtn.visibility = View.GONE
                binding.confirmDropOffBtn.visibility = View.VISIBLE
                binding.pingLocationImgBtn.visibility = View.GONE
                binding.pingLocationImgBtn2.visibility = View.GONE

                showToast("Navigating to destination")

            }
            .addOnFailureListener {
                Log.e(TAG, "storeTripToDatabase: " + it.message)
            }
    }

    private fun checkIfUserIsVerified() {
        if (FirebaseMain.getUser() != null) {
            documentReference = FirebaseMain.getFireStoreInstance()
                .collection(FirebaseMain.userCollection)
                .document(FirebaseMain.getUser().uid)

            documentReference.get()
                .addOnSuccessListener {
                    if (it != null && it.exists()) {
                        val isVerified = it.getBoolean("isVerified")

                        if (isVerified != null) {
                            if (isVerified) {
                                checkLocationPermission()

                            } else {
                                showUserNotVerifiedDialog()

                            }
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, it.message.toString())
                }

        } else {
            intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun showPleaseWaitDialog() {
        builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        val dialogView = layoutInflater.inflate(R.layout.dialog_please_wait, null)
        builder.setView(dialogView)
        pleaseWaitDialog = builder.create()
        pleaseWaitDialog.show()
    }

    private fun closePleaseWaitDialog() {
        if (pleaseWaitDialog.isShowing) {
            pleaseWaitDialog.dismiss()
        }
    }

    private fun showUserNotVerifiedDialog() {
        builder = AlertDialog.Builder(this)
        builder.setCancelable(false)

        val binding: DialogUserNotVerifiedBinding =
            DialogUserNotVerifiedBinding.inflate(layoutInflater)
        val dialogView = binding.root

        binding.okayBtn.setOnClickListener {
            intent = Intent(this@MapDriverActivity, MainActivity::class.java)
            startActivity(intent)
            finish()

            closeUserNotVerifiedDialog()
        }

        builder.setView(dialogView)
        userNotVerifiedDialog = builder.create()
        userNotVerifiedDialog.show()
    }

    private fun closeUserNotVerifiedDialog() {
        if (userNotVerifiedDialog.isShowing) {
            userNotVerifiedDialog.dismiss()
        }
    }

    private fun showConfirmPickupDialog(
        generateTripID: String,
        bookingID: String,
        passengerID: String,
        passengerName: String,
        passengerType: String,
        driverName: String,
        pickupLocation: String,
        pickupCoordinates: Point,
        destination: String,
        destinationCoordinates: Point,
    ) {
        builder = AlertDialog.Builder(this)
        val binding: DialogConfirmPickupBinding =
            DialogConfirmPickupBinding.inflate(layoutInflater)
        val dialogView = binding.root

        binding.loadingGif.visibility = View.GONE

        binding.confirmBtn.setOnClickListener {
            binding.loadingGif.visibility = View.VISIBLE
            binding.confirmBtn.visibility = View.GONE
            binding.closeBtn.visibility = View.GONE

            storeTripToDatabase(
                tripID = generateTripID,
                bookingID = bookingID,
                passengerID = passengerID,
                passengerName = passengerName,
                driverName = driverName,
                passengerType = passengerType,
                pickupLocation = pickupLocation,
                pickupCoordinate = pickupCoordinates,
                destination = destination,
                destinationCoordinate = destinationCoordinates
            )
        }

        binding.closeBtn.setOnClickListener {
            closeConfirmPickupDialog()
        }

        builder.setView(dialogView)
        confirmPickupDialog = builder.create()
        confirmPickupDialog.show()
    }

    private fun closeConfirmPickupDialog() {
        if (confirmPickupDialog.isShowing) {
            confirmPickupDialog.dismiss()
        }
    }

    private fun showConfirmDropOffDialog(
        tripID: String,
        bookingID: String,
        passengerID: String
    ) {
        builder = AlertDialog.Builder(this)

        val binding: DialogConfirmDropoffBinding =
            DialogConfirmDropoffBinding.inflate(layoutInflater)
        val dialogView = binding.root

        binding.loadingGif.visibility = View.GONE

        Log.i(TAG, "showConfirmDropOffDialog: $passengerID")

        binding.confirmBtn.setOnClickListener {
            binding.loadingGif.visibility = View.VISIBLE
            binding.closeBtn.visibility = View.GONE
            binding.confirmBtn.visibility = View.GONE

            setTripAsComplete(
                tripID = tripID,
                bookingID = bookingID,
                passengerID = passengerID
            )
        }

        binding.closeBtn.setOnClickListener {
            closeConfirmDropOffDialog()
        }

        builder.setView(dialogView)
        confirmDropOffDialog = builder.create()
        confirmDropOffDialog.show()
    }

    private fun closeConfirmDropOffDialog() {
        if (confirmDropOffDialog.isShowing) {
            confirmDropOffDialog.dismiss()
        }
    }

    private fun showPassengerTransportedSuccessDialog() {
        builder = AlertDialog.Builder(this)
        builder.setCancelable(false)

        val dialogView = layoutInflater.inflate(R.layout.dialog_passenger_transported_success, null)
        val okayBtn = dialogView.findViewById<Button>(R.id.okayBtn)

        okayBtn.setOnClickListener {
            closePassengerTransportedSuccessDialog()
        }

        builder.setView(dialogView)
        passengerTransportedSuccessDialog = builder.create()
        passengerTransportedSuccessDialog.show()
    }

    private fun closePassengerTransportedSuccessDialog() {
        if (passengerTransportedSuccessDialog.isShowing) {
            passengerTransportedSuccessDialog.dismiss()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showCancelNavigationDialog() {
        builder = AlertDialog.Builder(this)

        val binding: DialogCancelNavigationToPassengerBinding =
            DialogCancelNavigationToPassengerBinding.inflate(layoutInflater)
        val dialogView = binding.root

        if (isNavigatingToDestination) {
            binding.titleTextView.text = "NAVIGATING TO DESTINATION"
            binding.bodyTextView.text = "You are currently Navigating to Passenger's Destination"
        }

        binding.cancelBtn.setOnClickListener {
            clearRouteAndStopNavigation()
            checkNavigationStatus()

            closeCancelNavigationDialog()
        }
        binding.goBackBtn.setOnClickListener {
            closeCancelNavigationDialog()
        }

        builder.setView(dialogView)
        cancelNavigationDialog = builder.create()
        cancelNavigationDialog.show()
    }

    private fun closeCancelNavigationDialog() {
        if (cancelNavigationDialog.isShowing) {
            cancelNavigationDialog.dismiss()
        }
    }

    private fun showEnableLocationServiceDialog() {
        builder = AlertDialog.Builder(this)
        val binding: DialogEnableLocationServiceBinding =
            DialogEnableLocationServiceBinding.inflate(layoutInflater)
        val dialogView: View = binding.root

        binding.enableLocationServiceBtn.setOnClickListener { v ->
            intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivityForResult(intent, REQUEST_ENABLE_LOCATION)
            closeEnableLocationServiceDialog()
        }

        builder.setView(dialogView)
        enableLocationServiceDialog = builder.create()
        enableLocationServiceDialog.show()
    }

    private fun closeEnableLocationServiceDialog() {
        if (enableLocationServiceDialog.isShowing) {
            enableLocationServiceDialog.dismiss()
        }
    }

    private fun showExitMapDialog() {
        builder = AlertDialog.Builder(this)
        val binding: DialogExitMapBinding = DialogExitMapBinding.inflate(layoutInflater)
        val dialogView = binding.root

        if (fontSize.equals("large")) {
            val TEXT_SIZE = 22F

            binding.titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25F)
            binding.bodyTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE)
            binding.cancelBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE)
            binding.exitBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE)
        }

        if (voiceAssistantState == "enabled") {
            voiceAssistant =
                VoiceAssistant.getInstance(this@MapDriverActivity)
            voiceAssistant.speak("Are you sure you want to exit Map?")
        }

        binding.exitBtn.setOnClickListener {
            intent = Intent(this@MapDriverActivity, MainActivity::class.java)
            startActivity(intent)
            finish()

            closeExitMapDialog()
        }
        binding.cancelBtn.setOnClickListener {
            closeExitMapDialog()
        }

        builder.setView(dialogView)
        exitMapDialog = builder.create()
        exitMapDialog.show()
    }

    private fun closeExitMapDialog() {
        if (exitMapDialog.isShowing) {
            exitMapDialog.dismiss()
        }
    }

    //once the pickup button is clicked
    @SuppressLint("SetTextI18n")
    override fun onDataReceivedFromPickupPassengerBottomSheet(
        pickupPassengerBottomSheetData: PickupPassengerBottomSheetData
    ) {
        isAcceptedABooking = true
        removeAllAnnotationsExceptLastInteracted()
        if (isNavigatingToDestination) {
            findRoute(pickupPassengerBottomSheetData.destinationCoordinates)

            binding.confirmDropOffBtn.visibility = View.VISIBLE
            binding.navigationStatusTextView.visibility = View.VISIBLE
            binding.pingLocationImgBtn.visibility = View.VISIBLE
            binding.navigationStatusTextView.text = "Navigating to\nPassenger's destination"

        } else {
            findRoute(pickupPassengerBottomSheetData.pickupCoordinates)

            binding.confirmPickupBtn.visibility = View.VISIBLE
            binding.navigationStatusTextView.visibility = View.VISIBLE
            binding.pingLocationImgBtn.visibility = View.VISIBLE
            binding.navigationStatusTextView.text = "Navigating to\nPassenger's pickup location"

            binding.pingLocationImgBtn.setOnClickListener {
                pingCurrentLocation(pickupPassengerBottomSheetData.bookingID)
            }
            binding.pingLocationImgBtn2.setOnClickListener {
                pingCurrentLocation(pickupPassengerBottomSheetData.bookingID)
            }

            binding.confirmPickupBtn.setOnClickListener {

                showConfirmPickupDialog(
                    generateTripID = generateRandomTripID(),
                    bookingID = pickupPassengerBottomSheetData.bookingID,
                    passengerID = pickupPassengerBottomSheetData.passengerID,
                    passengerName = pickupPassengerBottomSheetData.passengerName,
                    passengerType = pickupPassengerBottomSheetData.passengerType,
                    driverName = pickupPassengerBottomSheetData.driverName,
                    pickupLocation = pickupPassengerBottomSheetData.pickupLocation,
                    pickupCoordinates = pickupPassengerBottomSheetData.pickupCoordinates,
                    destination = pickupPassengerBottomSheetData.destination,
                    destinationCoordinates = pickupPassengerBottomSheetData.destinationCoordinates
                )
            }
        }
    }

    //from bookings overview modal sheet
    @SuppressLint("SetTextI18n")
    override fun onDataReceivedFromPassengerBookingsBottomSheet(
        pickupPassengerBottomSheetData: PickupPassengerBottomSheetData
    ) {
        isAcceptedABooking = true
        removeAllAnnotationsExceptLastInteracted()

        if (isNavigatingToDestination) {
            findRoute(pickupPassengerBottomSheetData.destinationCoordinates)

            binding.confirmDropOffBtn.visibility = View.VISIBLE
            binding.navigationStatusTextView.visibility = View.VISIBLE
            binding.pingLocationImgBtn.visibility = View.VISIBLE
            binding.pingLocationImgBtn2.visibility = View.VISIBLE
            binding.navigationStatusTextView.text = "Navigating to\nPassenger's destination"

        } else {
            findRoute(pickupPassengerBottomSheetData.pickupCoordinates)

            binding.confirmPickupBtn.visibility = View.VISIBLE
            binding.navigationStatusTextView.visibility = View.VISIBLE
            binding.pingLocationImgBtn.visibility = View.VISIBLE
            binding.pingLocationImgBtn2.visibility = View.VISIBLE
            binding.navigationStatusTextView.text = "Navigating to\nPassenger's pickup location"

            binding.pingLocationImgBtn.setOnClickListener {
                pingCurrentLocation(pickupPassengerBottomSheetData.bookingID)
            }
            binding.pingLocationImgBtn2.setOnClickListener {
                pingCurrentLocation(pickupPassengerBottomSheetData.bookingID)
            }

            binding.confirmPickupBtn.setOnClickListener {

                showConfirmPickupDialog(
                    generateRandomTripID(),
                    pickupPassengerBottomSheetData.bookingID,
                    pickupPassengerBottomSheetData.passengerID,
                    pickupPassengerBottomSheetData.passengerName,
                    pickupPassengerBottomSheetData.passengerType,
                    pickupPassengerBottomSheetData.driverName,
                    pickupPassengerBottomSheetData.pickupLocation,
                    pickupPassengerBottomSheetData.pickupCoordinates,
                    pickupPassengerBottomSheetData.destination,
                    pickupPassengerBottomSheetData.destinationCoordinates
                )
            }
        }
    }

    //passenger location pin onclick
    private fun showPickupPassengerBottomSheet(bookingID: String) {
        val pickupPassengerBottomSheet = PickupPassengerBottomSheet.newInstance(bookingID)
        pickupPassengerBottomSheet.setPickupPassengerBottomSheetListener(this)
        pickupPassengerBottomSheet.setRenavigateListener(this)
        pickupPassengerBottomSheet.show(supportFragmentManager, PickupPassengerBottomSheet.TAG)
    }


    private fun showPassengerBookingsBottomSheet(bookingID: String) {
        val passengerBookingsBottomSheet = PassengerBookingsBottomSheet.newInstance(bookingID)
        passengerBookingsBottomSheet.setPassengerBookingsBottomSheetListener(this)
        passengerBookingsBottomSheet.show(supportFragmentManager, PassengerBookingsBottomSheet.TAG)
    }

    private fun showToast(message: String) {
        Toast.makeText(this@MapDriverActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            checkLocationService()
        } else {
            // Request permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_ENABLE_LOCATION) {
            // Check if the user enabled location services after going to settings.
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled =
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if (isGpsEnabled || isNetworkEnabled) {
                // Location services are now enabled, open your desired activity.
                onMapReady()
            } else {
                // Location services are still not enabled, you can show a message to the user.
                Toast.makeText(this, "Location services are still disabled.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "onRequestPermissionsResult: location permission granted")
            } else {
                Log.e(TAG, "onRequestPermissionsResult: location permission denied")
            }
        }
    }
}