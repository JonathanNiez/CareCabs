package com.capstone.carecabs

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
import android.os.Bundle
import android.util.Log
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
import com.capstone.carecabs.Firebase.FirebaseMain
import com.capstone.carecabs.Fragments.ModalBottomSheet
import com.capstone.carecabs.Fragments.PassengerBookingsBottomSheet
import com.capstone.carecabs.Model.BottomSheetData
import com.capstone.carecabs.Model.PassengerBookingModel
import com.capstone.carecabs.Model.PickupPassengerBottomSheetData
import com.capstone.carecabs.Model.TripModel
import com.capstone.carecabs.Utility.StaticDataPasser
import com.capstone.carecabs.databinding.ActivityMapDriverBinding
import com.capstone.carecabs.databinding.MapboxItemViewAnnotationBinding
import com.capstone.carecabs.databinding.MapboxPassengerWaitingAnnotationBinding
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.RouteOptions
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
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class MapDriverActivity : AppCompatActivity(), ModalBottomSheet.BottomSheetListener,
    PassengerBookingsBottomSheet.PassengerBookingsBottomSheetListener {

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

    private val TAG: String = "MapActivity"
    private val LOCATION_PERMISSION_REQUEST_CODE = 123

    private companion object {
        private const val BUTTON_ANIMATION_DURATION = 1500L
    }

    private lateinit var documentReference: DocumentReference
    private lateinit var collectionReference: CollectionReference
    private var themeMode: CurrentTheme? = null


    //    private lateinit var intent: Intent
    private lateinit var builder: AlertDialog.Builder
    private lateinit var userNotVerifiedDialog: AlertDialog
    private lateinit var passengerLocationInfoDialog: AlertDialog
    private lateinit var exitMapDialog: AlertDialog
    private lateinit var cancelNavtigationDialog: AlertDialog
    private lateinit var binding: ActivityMapDriverBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tripProgressLayout.visibility = View.INVISIBLE
        binding.soundBtn.visibility = View.INVISIBLE
        binding.maneuverView.visibility = View.INVISIBLE

        checkIfUserIsVerified()
        checkLocationPermission()

        binding.mapStyleSwitch.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {
                binding.mapView.getMapboxMap().apply {
                    loadStyleUri(Style.MAPBOX_STREETS) {
                        Toast.makeText(
                            this@MapDriverActivity,
                            "Changed Map style to Streets",
                            Toast.LENGTH_SHORT
                        ).show()

                    }
                }
            } else {
                binding.mapView.getMapboxMap().apply {
                    loadStyleUri(Style.SATELLITE_STREETS) {
                        Toast.makeText(
                            this@MapDriverActivity,
                            "Changed Map style to Satellite",
                            Toast.LENGTH_SHORT
                        ).show()

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

            binding.bottomNavigationView.visibility = View.GONE
        }

        binding.minimizeScreenImgBtn.setOnClickListener {
            Toast.makeText(
                this@MapDriverActivity,
                "Exited Fullscreen", Toast.LENGTH_SHORT
            ).show()

            binding.minimizeScreenImgBtn.visibility = View.GONE
            binding.fullscreenImgBtn.visibility = View.VISIBLE

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

    data class CurrentTheme(val theme: Int)

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
        showExitMapDialog()
    }

    private fun onMapReady() {
        binding.mapView.getMapboxMap().apply {
            loadStyleUri(Style.SATELLITE_STREETS) {

                setupGesturesListener()
                loadPassengerLocationToMapFromDatabase()
                initializeLocationComponent()
                initializeNavigationComponents()

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
                .zoom(12.0)
                .center(coordinate)
                .build()
        )
    }

    private fun zoomOutCamera(coordinate: Point) {
        binding.zoomInImgBtn.visibility = View.VISIBLE
        binding.zoomOutImgBtn.visibility = View.GONE
        binding.mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .zoom(8.0)
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
                    intent = Intent(this, TripsActivity::class.java)
                    startActivity(intent)
                }

                R.id.bookings -> {
                    showPassengerBookingsBottomSheetDialog(bookingID)
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
            val pointAnnotationManager = annotationApi.createPointAnnotationManager(binding.mapView)
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(Point.fromLngLat(longitude, latitude))
                .withIconImage(it)
            pointAnnotationManager.create(pointAnnotationOptions)

            pointAnnotationManager.apply {
                addClickListener(
                    OnPointAnnotationClickListener {

//                        showPassengerBookingLocationInfoDialog(bookingID)
                        showBottomSheetDialog(bookingID)

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
            val pointAnnotationManager = annotationApi.createPointAnnotationManager(binding.mapView)
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(Point.fromLngLat(longitude, latitude))
                .withIconImage(it)
            pointAnnotationManager.create(pointAnnotationOptions)

            pointAnnotationManager.apply {
                addClickListener(
                    OnPointAnnotationClickListener {

//                        showPassengerBookingLocationInfoDialog(bookingID)

                        showBottomSheetDialog(bookingID)
                        true
                    }
                )
            }

        }
    }

    private fun createDestinationAnnotationToMap(destinationCoordinate: Point) {
        bitmapFromDrawableRes(
            this@MapDriverActivity,
            R.drawable.location_pin_128
        ).let {
            val annotationApi = binding.mapView.annotations
            val pointAnnotationManager = annotationApi.createPointAnnotationManager(binding.mapView)
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(
                    Point.fromLngLat(
                        destinationCoordinate.longitude(),
                        destinationCoordinate.latitude()
                    )
                )
                .withIconImage(it)
            pointAnnotationManager.create(pointAnnotationOptions)

            pointAnnotationManager.apply {
                addClickListener(
                    OnPointAnnotationClickListener {

//                        showPassengerBookingLocationInfoDialog(bookingID)

                        true
                    }
                )
            }

        }
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

    private fun createViewAnnotation(mapView: MapView, coordinate: Point) {

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

    private fun findRoute(destination: Point) {

        Toast.makeText(
            this@MapDriverActivity,
            "Navigation to Passenger",
            Toast.LENGTH_LONG
        ).show()

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
        Toast.makeText(
            this@MapDriverActivity,
            "Cancelled Navigation",
            Toast.LENGTH_LONG
        ).show()
        // clear
        mapboxNavigation.setNavigationRoutes(listOf())

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

    private fun loadPassengerLocationToMapFromDatabase() {
        var waitingPassengerCount = 0
        val locationReference = FirebaseDatabase.getInstance()
            .getReference(FirebaseMain.bookingCollection)

        locationReference.addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    var hasPassengersWaiting = false
                    for (locationSnapshot in snapshot.children) {
                        val locationData =
                            locationSnapshot.getValue(PassengerBookingModel::class.java)

                        if (locationData != null) {
                            if (locationData.bookingStatus == "Waiting") {

                                binding.bookingsImgBtn.setOnClickListener {
                                    showPassengerBookingsBottomSheetDialog(locationData.bookingID)
                                }

                                initializeBottomNavButtons(locationData.bookingID)

                                waitingPassengerCount++
                                hasPassengersWaiting = true
                                when (locationData.passengerUserType) {
                                    "Senior Citizen" -> {
                                        addSeniorAnnotationToMap(
                                            locationData.pickupLongitude,
                                            locationData.pickupLatitude,
                                            locationData.bookingID
                                        )
                                    }

                                    "Persons with Disability (PWD)" -> {
                                        addPWDAnnotationToMap(
                                            locationData.pickupLongitude,
                                            locationData.pickupLatitude,
                                            locationData.bookingID
                                        )
                                    }
                                }
                            } else if (locationData.bookingStatus == "Driver on the way"
                                && locationData.driverUserID == FirebaseMain.getUser().uid
                            ) {

                                binding.bookingsImgBtn.setOnClickListener {
                                    showPassengerBookingsBottomSheetDialog(locationData.bookingID)
                                }

                                initializeBottomNavButtons(locationData.bookingID)

                                when (locationData.passengerUserType) {
                                    "Senior Citizen" -> {
                                        addSeniorAnnotationToMap(
                                            locationData.pickupLongitude,
                                            locationData.pickupLatitude,
                                            locationData.bookingID
                                        )
                                    }

                                    "Persons with Disability (PWD)" -> {
                                        addPWDAnnotationToMap(
                                            locationData.pickupLongitude,
                                            locationData.pickupLatitude,
                                            locationData.bookingID
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (hasPassengersWaiting) {
                        binding.passengersInMapTextView.text =
                            "There are $waitingPassengerCount Passenger(s) waiting right now"
                    } else {
                        binding.passengersInMapTextView.text = "There are no Passengers right now"

                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, error.message)
            }

        })

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

    private fun generateRandomTripID(): String {
        val uuid = UUID.randomUUID()
        return uuid.toString()
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

                Toast.makeText(
                    this@MapDriverActivity,
                    "Booking Accepted", Toast.LENGTH_LONG
                ).show()

                findRoute(point)
            }
            .addOnFailureListener {
                Log.e(TAG, it.message.toString())

                Toast.makeText(
                    this@MapDriverActivity,
                    "Booking Failed to Accept", Toast.LENGTH_LONG
                ).show()
            }
    }

    //start the trip
    private fun storeTripToDatabase(
        generateTripID: String,
        bookingID: String,
        passengerID: String,
        pickupCoordinate: Point,
        destinationCoordinate: Point,
    ) {
        val documentReference = FirebaseMain.getFireStoreInstance()
            .collection(FirebaseMain.tripCollection)
            .document(generateTripID)

        val tripModel = TripModel(
            generateTripID,
            false,
            bookingID,
            "Passenger on board",
            FirebaseMain.getUser().uid,
            passengerID,
            getCurrentTimeAndDate(),
            pickupCoordinate.longitude(),
            pickupCoordinate.latitude(),
            destinationCoordinate.longitude(),
            destinationCoordinate.latitude()
        )

        documentReference.set(tripModel)
            .addOnSuccessListener {

                createDestinationAnnotationToMap(destinationCoordinate)
                findRoute(destinationCoordinate)
                loadPassengerLocationToMapFromDatabase()

                Toast.makeText(
                    this@MapDriverActivity,
                    "Navigating to destination",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this@MapDriverActivity,
                    "Failed to navigate to destination",
                    Toast.LENGTH_LONG
                ).show()
                Log.e(TAG, e.message.toString())
            }
    }

    private fun checkIfUserIsVerified() {
        if (FirebaseMain.getUser() != null) {
            documentReference = FirebaseMain.getFireStoreInstance()
                .collection(FirebaseMain.userCollection)
                .document(FirebaseMain.getUser().uid)

            documentReference.get().addOnSuccessListener {
                if (it != null && it.exists()) {
                    val getVerificationStatus = it.getBoolean("isVerified")

                    if (!getVerificationStatus!!) {
                        showUserNotVerifiedDialog()
                    } else {
                        checkLocationPermission()
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
        if (userNotVerifiedDialog.isShowing) {
            userNotVerifiedDialog.dismiss()
        }
    }

    private fun showCancelNavigationDialog() {
        builder = AlertDialog.Builder(this)
        val dialogView =
            layoutInflater.inflate(R.layout.dialog_cancel_navigation_to_passenger, null)

        val cancelBtn = dialogView.findViewById<Button>(R.id.cancelBtn)
        val closeBtn = dialogView.findViewById<Button>(R.id.closeBtn)

        cancelBtn.setOnClickListener {
            clearRouteAndStopNavigation()

            closeCancelNavigationDialog()
        }
        closeBtn.setOnClickListener {
            closeCancelNavigationDialog()
        }

        builder.setView(dialogView)
        cancelNavtigationDialog = builder.create()
        cancelNavtigationDialog.show()
    }

    private fun closeCancelNavigationDialog() {
        if (cancelNavtigationDialog.isShowing) {
            cancelNavtigationDialog.dismiss()
        }
    }

    private fun showExitMapDialog() {
        builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_exit_map, null)
        val exitBtn = dialogView.findViewById<Button>(R.id.exitBtn)
        val cancelBtn = dialogView.findViewById<Button>(R.id.cancelBtn)

        exitBtn.setOnClickListener {
            intent = Intent(this@MapDriverActivity, MainActivity::class.java)
            startActivity(intent)
            finish()

            closeExitMapDialog()
        }
        cancelBtn.setOnClickListener {
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
    override fun onDataReceived(bottomSheetData: BottomSheetData) {
        findRoute(bottomSheetData.destinationCoordinates)

        binding.passengerOnBoardBtn.setOnClickListener {
            storeTripToDatabase(
                generateRandomTripID(),
                bottomSheetData.bookingID,
                bottomSheetData.passengerID,
                bottomSheetData.pickupCoordinates,
                bottomSheetData.destinationCoordinates
            )
        }
    }

    override fun onDataReceivedFromPassengerBookingsBottomSheet(pickupPassengerBottomSheetData: PickupPassengerBottomSheetData) {
        findRoute(pickupPassengerBottomSheetData.destinationCoordinates)

        binding.passengerOnBoardBtn.setOnClickListener {
            storeTripToDatabase(
                generateRandomTripID(),
                pickupPassengerBottomSheetData.bookingID,
                pickupPassengerBottomSheetData.passengerID,
                pickupPassengerBottomSheetData.pickupCoordinates,
                pickupPassengerBottomSheetData.destinationCoordinates
            )
        }
    }

    private fun showBottomSheetDialog(bookingID: String) {
        val data = bookingID
        val modalBottomSheet = ModalBottomSheet.newInstance(data)
        modalBottomSheet.setBottomSheetListener(this) // Set the listener in the activity
        modalBottomSheet.show(supportFragmentManager, ModalBottomSheet.TAG)
    }

    private fun showPassengerBookingsBottomSheetDialog(bookingID: String) {
        val data = bookingID
        val passengerBookingsBottomSheet = PassengerBookingsBottomSheet.newInstance(data)
        passengerBookingsBottomSheet.setPassengerBookingsBottomSheetListener(this) // Set the listener in the activity
        passengerBookingsBottomSheet.show(supportFragmentManager, PassengerBookingsBottomSheet.TAG)
    }


    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onMapReady()
        } else {
            // Request permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
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
                // Permission granted, proceed to location retrieval
            } else {
                // Permission denied, handle accordingly (show error message, etc.)
            }
        }
    }
}



