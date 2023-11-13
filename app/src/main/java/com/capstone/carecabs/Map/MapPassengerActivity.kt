package com.capstone.carecabs.Map

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.capstone.carecabs.BookingsActivity
import com.capstone.carecabs.BottomSheetModal.ConfirmBookingBottomSheet
import com.capstone.carecabs.FavoritesActivity
import com.capstone.carecabs.Firebase.FirebaseMain
import com.capstone.carecabs.HelpActivity
import com.capstone.carecabs.LoginOrRegisterActivity
import com.capstone.carecabs.MainActivity
import com.capstone.carecabs.Model.PassengerBookingModel
import com.capstone.carecabs.R
import com.capstone.carecabs.Utility.NotificationHelper
import com.capstone.carecabs.Utility.StaticDataPasser
import com.capstone.carecabs.Utility.VoiceAssistant
import com.capstone.carecabs.databinding.ActivityMapPassengerBinding
import com.capstone.carecabs.databinding.DialogHasActiveBookingBinding
import com.capstone.carecabs.databinding.DialogMapInstructionsBinding
import com.capstone.carecabs.databinding.DialogPassengerOwnBookingInfoBinding
import com.capstone.carecabs.databinding.MapboxItemViewAnnotationBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.messaging.FirebaseMessaging
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.Utils.dpToPx
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.CameraAnimatorOptions.Companion.cameraAnimatorOptions
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.addOnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.gestures.removeOnMapLongClickListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import com.mapbox.search.ResponseInfo
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchEngineSettings
import com.mapbox.search.ServiceProvider
import com.mapbox.search.autocomplete.PlaceAutocomplete
import com.mapbox.search.autocomplete.PlaceAutocompleteOptions
import com.mapbox.search.autocomplete.PlaceAutocompleteSuggestion
import com.mapbox.search.autocomplete.PlaceAutocompleteType
import com.mapbox.search.common.CompletionCallback
import com.mapbox.search.offline.OfflineResponseInfo
import com.mapbox.search.offline.OfflineSearchEngine
import com.mapbox.search.offline.OfflineSearchEngineSettings
import com.mapbox.search.offline.OfflineSearchResult
import com.mapbox.search.record.HistoryRecord
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import com.mapbox.search.ui.adapter.autocomplete.PlaceAutocompleteUiAdapter
import com.mapbox.search.ui.adapter.engines.SearchEngineUiAdapter
import com.mapbox.search.ui.view.CommonSearchViewConfiguration
import com.mapbox.search.ui.view.DistanceUnitType
import com.mapbox.search.ui.view.SearchResultAdapterItem
import com.mapbox.search.ui.view.SearchResultsView
import com.mapbox.search.ui.view.UiError
import com.mapbox.search.ui.view.place.SearchPlace
import java.util.Calendar
import java.util.UUID


class MapPassengerActivity : AppCompatActivity(), OnMapClickListener, OnMapLongClickListener {

    private val TAG: String = "MapPassengerActivity"
    private lateinit var binding: ActivityMapPassengerBinding
    private lateinit var documentReference: DocumentReference
    private lateinit var builder: AlertDialog.Builder
    private lateinit var userNotVerifiedDialog: AlertDialog
    private lateinit var passengerOwnBookingInfoDialog: AlertDialog
    private lateinit var hasActiveBookingDialog: AlertDialog
    private lateinit var mapInstructionsDialog: AlertDialog
    private lateinit var exitMapDialog: AlertDialog
    private val voiceAssistantState = StaticDataPasser.storeVoiceAssistantState
    private lateinit var voiceAssistant: VoiceAssistant
    private var hasActiveBooking = false
    private var currentLongitude: Double = 0.0
    private var currentLatitude: Double = 0.0

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        binding.mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        binding.mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())
        binding.mapView.gestures.focalPoint = binding.mapView.getMapboxMap().pixelForCoordinate(it)
    }

    private val onMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {
            onCameraTrackingDismissed()
        }

        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {}
    }

    private val viewAnnotationMap = mutableMapOf<Point, View>()
    private lateinit var mapboxMap: MapboxMap

    //search
    private lateinit var placeAutocomplete: PlaceAutocomplete
    private lateinit var placeAutocompleteUiAdapter: PlaceAutocompleteUiAdapter
    private lateinit var mapMarkersManager: MapMarkersManager
    private var ignoreNextQueryUpdate = false

    override fun onDestroy() {
        super.onDestroy()

        binding.mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        binding.mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        binding.mapView.gestures.removeOnMoveListener(onMoveListener)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapPassengerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        placeAutocomplete = PlaceAutocomplete.create(getString(R.string.mapbox_access_token))

        checkIfUserIsVerified()
        initializeBottomNavButtons()

        binding.mapStyleSwitch.setOnCheckedChangeListener { compoundButton, isChecked ->
            if (isChecked) {
                binding.mapView.getMapboxMap().apply {
                    loadStyleUri(Style.SATELLITE_STREETS) {
                        showToast("Changed Map style to Streets")
                    }
                }
            } else {
                binding.mapView.getMapboxMap().apply {
                    loadStyleUri(Style.MAPBOX_STREETS) {
                        showToast("Changed Map style to Satellite")
                    }
                }
            }
        }

        binding.fullscreenImgBtn.setOnClickListener {
            showToast("Entered fullscreen")

            if (voiceAssistantState.equals("enabled")) {
                voiceAssistant = VoiceAssistant.getInstance(this)
                voiceAssistant.speak("Entered fullscreen")
            }

            binding.fullscreenImgBtn.visibility = View.GONE
            binding.minimizeScreenImgBtn.visibility = View.VISIBLE

            binding.bottomNavigationView.visibility = View.GONE
        }

        binding.minimizeScreenImgBtn.setOnClickListener {
            showToast("Exited fullscreen")

            if (voiceAssistantState.equals("enabled")) {
                voiceAssistant = VoiceAssistant.getInstance(this)
                voiceAssistant.speak("Exited fullscreen")
            }

            binding.minimizeScreenImgBtn.visibility = View.GONE
            binding.fullscreenImgBtn.visibility = View.VISIBLE

            binding.bottomNavigationView.visibility = View.VISIBLE
        }

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

    private fun onMapReady() {

        mapboxMap = binding.mapView.getMapboxMap().apply {
            loadStyleUri(Style.MAPBOX_STREETS) {

                initializeLocationComponent()
                initializeSearchEngine()
                loadBookingsToMapFromDatabase()

//                binding.searchDestinationEditText.addTextChangedListener(object : TextWatcher {
//                    override fun beforeTextChanged(
//                        s: CharSequence?,
//                        start: Int,
//                        count: Int,
//                        after: Int
//                    ) {
//                    }
//
//                    override fun onTextChanged(
//                        s: CharSequence?,
//                        start: Int,
//                        before: Int,
//                        count: Int
//                    ) {
//                    }
//
//                    override fun afterTextChanged(s: Editable?) {
//                        if (s.isNullOrEmpty()) {
//                            binding.searchContainerView.visibility = View.GONE
//                        } else {
//                            binding.searchContainerView.visibility = View.VISIBLE
//                        }
//                    }
//
//                })

                binding.mapView.camera.apply {
                    val bearing = createBearingAnimator(cameraAnimatorOptions(-45.0)) {
                        duration = 4000
                        interpolator = AccelerateDecelerateInterpolator()
                    }
                    val zoom = createZoomAnimator(
                        cameraAnimatorOptions(14.0) {
                            startValue(3.0)
                        }
                    ) {
                        duration = 4000
                        interpolator = AccelerateDecelerateInterpolator()
                    }
                    val pitch = createPitchAnimator(
                        cameraAnimatorOptions(55.0) {
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
    }

    private fun zoomInCamera(coordinate: Point) {
        binding.zoomInImgBtn.visibility = View.GONE
        binding.zoomOutImgBtn.visibility = View.VISIBLE
        binding.mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .zoom(16.0)
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

    private fun recenterCamera(coordinate: Point) {
        binding.mapView.location
            .addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        binding.mapView.location
            .addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        binding.mapView.gestures.addOnMoveListener(onMoveListener)

        binding.mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .center(coordinate)
                .build()
        )
    }

    private fun getUserProfilePictureFromFireStore() {

        documentReference = FirebaseMain.getFireStoreInstance()
            .collection(FirebaseMain.userCollection)
            .document(FirebaseMain.getUser().uid)

        documentReference.get().addOnSuccessListener {
            if (it != null && it.exists()) {
                val getProfilePicture = it.getString("profilePicture")

                if (!getProfilePicture.isNullOrEmpty()) {
                    // Load the profile picture using Glide
                    Glide.with(this)
                        .load(getProfilePicture)
                        .into(object : CustomTarget<Drawable>() {
                            override fun onResourceReady(
                                resource: Drawable,
                                transition: Transition<in Drawable>?
                            ) {
                                val imageResource = drawableToIntResource(resource)
                                initializeLocationComponent()
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                                // Handle case when the image load is cleared
                            }
                        })
                }
            }

        }.addOnFailureListener {

        }
    }

    @SuppressLint("DiscouragedApi")
    private fun drawableToIntResource(drawable: Drawable): Int {
        val resources = resources

        // Use the unique identifier of the Drawable to get its resource ID
        val resourceId = resources.getIdentifier(
            resources.getResourceEntryName(drawable.hashCode()), // Unique identifier
            "drawable", // Assuming the Drawable is stored in the "drawable" folder
            packageName // Your app's package name
        )

        return resourceId
    }

    //get current location
    @SuppressLint("SetTextI18n")
    private fun initializeLocationComponent() {

        val locationComponentPlugin = binding.mapView.location
        locationComponentPlugin.addOnIndicatorPositionChangedListener {

            //store the current location
            currentLongitude = it.longitude()
            currentLatitude = it.latitude()

            StaticDataPasser.storePickupLatitude = currentLatitude
            StaticDataPasser.storePickupLongitude = currentLongitude

            createViewAnnotation(
                binding.mapView,
                Point.fromLngLat(currentLongitude, currentLatitude)
            )

//            val passengerBookingModel = PassengerBookingModel(
//                pickupLatitude = it.latitude(),
//                pickupLongitude = it.longitude()
//            )

//            val pickupLocationGeocode = MapboxGeocoding.builder()
//                .accessToken(getString(R.string.mapbox_access_token))
//                .query(
//                    Point.fromLngLat(
//                        StaticDataPasser.storeLongitude,
//                        StaticDataPasser.storeLatitude
//                    )
//                )
//                .geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
//                .build()
//
//            pickupLocationGeocode.enqueueCall(object : Callback<GeocodingResponse> {
//                override fun onResponse(
//                    call: Call<GeocodingResponse>,
//                    response: Response<GeocodingResponse>
//                ) {
//                    if (response.body() != null && response.body()!!.features() != null) {
//                        val feature: CarmenFeature = response.body()!!.features()!![0]
//                        val locationName: String? = feature.placeName()
//
//                        // Display the location name in your TextView
//                        binding.currentCoordinatesTextView.text = locationName
//                    } else {
//                        // Handle no results
//                    }
//                }
//
//                override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
//                    // Handle failure
//                }
//            })

        }

//        binding.currentCoordinatesTextView.text =
//            "${StaticDataPasser.storeLatitude}\n${StaticDataPasser.storeLongitude}"

        locationComponentPlugin.updateSettings {
            this.enabled = true
            this.locationPuck = LocationPuck2D(
                bearingImage = AppCompatResources.getDrawable(
                    this@MapPassengerActivity,
                    R.drawable.mapbox_navigation_puck_icon,
                ),
                scaleExpression = interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(0.0)
                        literal(0.6)
                    }
                    stop {
                        literal(20.0)
                        literal(1.0)
                    }
                }.toJson()
            )
        }
        locationComponentPlugin.addOnIndicatorPositionChangedListener(
            onIndicatorPositionChangedListener
        )
        locationComponentPlugin.addOnIndicatorBearingChangedListener(
            onIndicatorBearingChangedListener
        )
    }

    private fun recenterCamera(location: Location) {
        val mapAnimationOptions = MapAnimationOptions.Builder().duration(1500L).build()
        binding.mapView.camera.easeTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(location.longitude, location.latitude))
                .zoom(12.0)
                .padding(EdgeInsets(500.0, 0.0, 0.0, 0.0))
                .build(),
            mapAnimationOptions
        )
    }

    private fun initializeBottomNavButtons() {
        binding.bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {

                R.id.bookings -> {
                    intent = Intent(this, BookingsActivity::class.java)
                    startActivity(intent)
                }

                R.id.help -> {
                    intent = Intent(this, HelpActivity::class.java)
                    startActivity(intent)
                }

                R.id.favorites -> {
                    intent = Intent(this, FavoritesActivity::class.java)
                    startActivity(intent)
                }
            }
            true
        }
    }

    private fun initializeSearchEngine() {
        binding.searchResultsView.initialize(
            SearchResultsView.Configuration(
                CommonSearchViewConfiguration(DistanceUnitType.IMPERIAL)
            )
        )
        val searchEngine = SearchEngine.createSearchEngineWithBuiltInDataProviders(
            SearchEngineSettings(getString(R.string.mapbox_access_token))
        )

        val offlineSearchEngine = OfflineSearchEngine.create(
            OfflineSearchEngineSettings(getString(R.string.mapbox_access_token))
        )

        val searchEngineUiAdapter = SearchEngineUiAdapter(
            view = binding.searchResultsView,
            searchEngine = searchEngine,
            offlineSearchEngine = offlineSearchEngine,
        )

        searchEngineUiAdapter.addSearchListener(object : SearchEngineUiAdapter.SearchListener {
            override fun onError(e: Exception) {
                TODO("Not yet implemented")
            }

            override fun onFeedbackItemClick(responseInfo: ResponseInfo) {
                TODO("Not yet implemented")
            }

            override fun onHistoryItemClick(historyRecord: HistoryRecord) {
                TODO("Not yet implemented")
            }

            override fun onOfflineSearchResultSelected(
                searchResult: OfflineSearchResult,
                responseInfo: OfflineResponseInfo
            ) {
                TODO("Not yet implemented")
            }

            override fun onOfflineSearchResultsShown(
                results: List<OfflineSearchResult>,
                responseInfo: OfflineResponseInfo
            ) {
                TODO("Not yet implemented")
            }

            override fun onPopulateQueryClick(
                suggestion: SearchSuggestion,
                responseInfo: ResponseInfo
            ) {
                TODO("Not yet implemented")
            }

            override fun onSearchResultSelected(
                searchResult: SearchResult,
                responseInfo: ResponseInfo
            ) {
                TODO("Not yet implemented")
            }

            override fun onSearchResultsShown(
                suggestion: SearchSuggestion,
                results: List<SearchResult>,
                responseInfo: ResponseInfo
            ) {
                TODO("Not yet implemented")
            }

            override fun onSuggestionSelected(searchSuggestion: SearchSuggestion): Boolean {
                TODO("Not yet implemented")
            }

            override fun onSuggestionsShown(
                suggestions: List<SearchSuggestion>,
                responseInfo: ResponseInfo
            ) {
                TODO("Not yet implemented")
            }
        })

        showSearchHistory()
    }

    private fun showSearchHistory() {
        val historyDataProvider = ServiceProvider.INSTANCE.historyDataProvider()

        // Show `loading` item that indicates the progress of `search history` loading operation.
        binding.searchResultsView.setAdapterItems(listOf(SearchResultAdapterItem.Loading))

        // Load `search history`
        var loadingTask =
            historyDataProvider.getAll(object : CompletionCallback<List<HistoryRecord>> {
                override fun onComplete(result: List<HistoryRecord>) {
                    val viewItems = mutableListOf<SearchResultAdapterItem>().apply {
                        // Add `Recent searches` header
                        add(SearchResultAdapterItem.RecentSearchesHeader)

                        // Add history record items
                        addAll(result.map { history ->
                            SearchResultAdapterItem.History(
                                history,
                                isFavorite = false
                            )
                        })
                    }

                    // Show prepared items
                    binding.searchResultsView.setAdapterItems(viewItems)
                }

                override fun onError(e: Exception) {
                    // Show error in case of failure
                    val errorItem = SearchResultAdapterItem.Error(UiError.createFromException(e))
                    binding.searchResultsView.setAdapterItems(listOf(errorItem))
                }
            })
    }

    private fun searchDestination(destination: String) {
        val placeAutocomplete = PlaceAutocomplete
            .create(getString(R.string.mapbox_access_token))

        lifecycleScope.launchWhenCreated {
            val response = placeAutocomplete.suggestions(
                query = destination,
            )

            if (response.isValue) {
                val suggestions = requireNotNull(response.value)

                Log.i("SearchApiExample", "Place Autocomplete suggestions: $suggestions")

                if (suggestions.isNotEmpty()) {
// Supposing that a user has selected (clicked in UI) the first suggestion
                    val selectedSuggestion = suggestions.first()

                    Log.i("SearchApiExample", "Selecting first suggestion...")

                    val selectionResponse = placeAutocomplete.select(selectedSuggestion)
                    selectionResponse.onValue { result ->
                        Log.i("SearchApiExample", "Place Autocomplete result: $result")
                    }.onError { e ->
                        Log.i("SearchApiExample", "An error occurred during selection", e)
                    }
                }
            } else {
                Log.i("SearchApiExample", "Place Autocomplete error", response.error)
            }
        }
    }

    //destination marker
    private fun addDestinationAnnotationToMap(
        destinationLongitude: Double,
        destinationLatitude: Double,
        bookingID: String
    ) {
        bitmapFromDrawableRes(
            this@MapPassengerActivity,
            R.drawable.location_pin_128
        ).let {
            val annotationApi = binding.mapView.annotations
            val pointAnnotationManager = annotationApi.createPointAnnotationManager()
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(Point.fromLngLat(destinationLongitude, destinationLatitude))
                .withIconImage(it)

            pointAnnotationManager.create(pointAnnotationOptions)

            pointAnnotationManager.apply {
                addClickListener(
                    OnPointAnnotationClickListener {

                        showOwnBookingInfoDialog(bookingID)

                        true
                    }
                )
            }
        }
    }

    //driver pinged location
    private fun addDriverPingedLocationToMap(
        driverLongitude: Double,
        driverLatitude: Double,
    ) {
        bitmapFromDrawableRes(
            this@MapPassengerActivity,
            R.drawable.car_100_2
        ).let {
            val annotationApi = binding.mapView.annotations
            val pointAnnotationManager = annotationApi.createPointAnnotationManager()
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(Point.fromLngLat(driverLongitude, driverLatitude))
                .withIconImage(it)

            pointAnnotationManager.create(pointAnnotationOptions)
        }
    }

    //displays the the "you"
    private fun createViewAnnotation(mapView: MapView, coordinate: Point) {

        binding.zoomInImgBtn.setOnClickListener {
            zoomInCamera(coordinate)
        }

        binding.zoomOutImgBtn.setOnClickListener {
            zoomOutCamera(coordinate)
        }

        binding.recenterBtn.setOnClickListener {
            recenterCamera(coordinate)
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


    private fun setupGesturesListener() {
        binding.mapView.gestures.addOnMoveListener(onMoveListener)

    }

    @SuppressLint("SetTextI18n")
    override fun onMapClick(point: Point): Boolean {

        return false
    }

    override fun onMapLongClick(point: Point): Boolean {

        return if (hasActiveBooking) {
            showHasActiveBookingDialog()

            true
        } else {
            showConfirmBookingBottomSheet(point)

            false
        }
    }

    private fun onCameraTrackingDismissed() {
        binding.mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        binding.mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        binding.mapView.gestures.removeOnMoveListener(onMoveListener)
    }

    private fun loadBookingsToMapFromDatabase() {
        if (FirebaseMain.getUser() != null) {

            setupGesturesListener()
//            checkBookingStatus()

            val bookingReference = FirebaseDatabase.getInstance()
                .getReference(FirebaseMain.bookingCollection)

            bookingReference.addValueEventListener(object : ValueEventListener {
                @SuppressLint("SetTextI18n")
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {

                        val userID = FirebaseMain.getUser().uid
                        for (locationSnapshot in snapshot.children) {
                            val passengerBookingData =
                                locationSnapshot.getValue(PassengerBookingModel::class.java)

                            if (passengerBookingData != null) {
                                if (passengerBookingData.bookingStatus == "Waiting"
                                    && passengerBookingData.passengerUserID == userID
                                ) {

                                    hasActiveBooking = true

                                    binding.searchDestinationLayout.visibility = View.GONE
                                    binding.longTapTextView.text =
                                        "You are currently waiting for a Driver"

                                    val destinationLatitude =
                                        passengerBookingData.destinationLatitude
                                    val destinationLongitude =
                                        passengerBookingData.destinationLongitude
                                    val getBookingID = passengerBookingData.bookingID

                                    addDestinationAnnotationToMap(
                                        destinationLongitude,
                                        destinationLatitude,
                                        getBookingID
                                    )
                                } else if (passengerBookingData.bookingStatus == "Driver on the way"
                                    && passengerBookingData.passengerUserID == userID
                                ) {

                                    hasActiveBooking = true

                                    binding.searchDestinationLayout.visibility = View.GONE
                                    binding.longTapTextView.text = "Your Driver is on the way!"

                                    val driverLongitude = passengerBookingData.driverPingedLongitude
                                    val driverLatitude = passengerBookingData.driverPingedLatitude

                                    val destinationLatitude =
                                        passengerBookingData.destinationLatitude
                                    val destinationLongitude =
                                        passengerBookingData.destinationLongitude
                                    val getBookingID = passengerBookingData.bookingID

                                    addDriverPingedLocationToMap(driverLongitude, driverLatitude)

                                    addDestinationAnnotationToMap(
                                        destinationLongitude,
                                        destinationLatitude,
                                        getBookingID
                                    )
                                } else if (passengerBookingData.bookingStatus == "Passenger Onboard"
                                    && passengerBookingData.passengerUserID == userID
                                ) {

                                    hasActiveBooking = true

                                    binding.searchDestinationLayout.visibility = View.GONE
                                    binding.longTapTextView.text =
                                        "You are currently Onboard"

                                } else if (passengerBookingData.bookingStatus == "Transported to destination"
                                    && passengerBookingData.passengerUserID == userID
                                ) {
                                    hasActiveBooking = false

                                    binding.longTapTextView.text = "Long tap/click on the Map you wish to go"
                                    binding.searchDestinationLayout.visibility = View.VISIBLE
                                }

                                mapboxMap = if (hasActiveBooking) {
                                    binding.mapView.getMapboxMap().apply {
                                        removeOnMapLongClickListener(this@MapPassengerActivity)
                                    }
                                } else {
                                    binding.mapView.getMapboxMap().apply {
                                        addOnMapLongClickListener(this@MapPassengerActivity)
                                    }
                                }
                            }
                        }
                    } else {
                        Log.e(
                            TAG,
                            "loadCurrentCoordinatesToMapFromDatabase: addValueEventListener is null"
                        )
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "loadCurrentCoordinatesToMapFromDatabase: " + error.message)
                }
            })

        } else {
            intent = Intent(this@MapPassengerActivity, LoginOrRegisterActivity::class.java)
            startActivity(intent)
            finish()
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

    private fun generateRandomBookingID(): String {
        val uuid = UUID.randomUUID()
        return uuid.toString()
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
                        val isFirstTimeUser = it.getBoolean("isFirstTimeUser")

                        if (isVerified == false) {
                            showUserNotVerifiedDialog()
                        } else {
                            if (isFirstTimeUser == true) {
                                showMapInstructionsDialog()
                            }
                            onMapReady()
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, "checkIfUserIsVerified: " + it.message)
                }

        } else {
            intent = Intent(this, LoginOrRegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun retrieveAndStoreFCMToken(point: Point) {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token: String ->

                storeCoordinatesInFireStore(point, token)
            }
            .addOnFailureListener { e: java.lang.Exception ->
                Log.e(
                    TAG,
                    e.message!!
                )
            }
    }

    private fun storeCoordinatesInFireStore(point: Point, fcmToken: String) {
        if (FirebaseMain.getUser() != null) {

            documentReference = FirebaseMain.getFireStoreInstance()
                .collection(FirebaseMain.userCollection)
                .document(FirebaseMain.getUser().uid)

            documentReference.get()
                .addOnSuccessListener {
                    if (it != null && it.exists()) {
                        val getUserType = it.getString("userType")!!
                        val getProfilePicture = it.getString("profilePicture")!!
                        val getFirstName = it.getString("firstname")!!
                        val getLastName = it.getString("lastname")!!
                        val fullName = "$getFirstName $getLastName"

                        intent = Intent(this@MapPassengerActivity, BookingsActivity::class.java)
                        intent.putExtra("dataSent", "dummy")
                        startActivity(intent)

                        when (getUserType) {
                            "Senior Citizen" -> {

                                storeSeniorCitizenBookingToDatabase(
                                    fcmToken,
                                    point,
                                    fullName,
                                    getUserType,
                                    getProfilePicture,
                                    generateRandomBookingID()
                                )
                            }

                            "Person with Disabilities (PWD)" -> {
                                val getDisability = it.getString("disability")!!

                                storePWDBookingToDatabase(
                                    fcmToken,
                                    point,
                                    fullName,
                                    getUserType,
                                    getProfilePicture,
                                    getDisability,
                                    generateRandomBookingID()
                                )
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, "storeCoordinatesInFireStore: " + it.message)
                }

        } else {
            intent = Intent(this@MapPassengerActivity, LoginOrRegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun storePWDBookingToDatabase(
        fcmToken: String,
        point: Point,
        fullName: String,
        userType: String,
        profilePicture: String,
        disability: String,
        generateBookingID: String
    ) {
        val database = FirebaseDatabase.getInstance()
        val locationReference = database.getReference(FirebaseMain.tripCollection)
            .child(generateBookingID)

        val passengerBookingModel = PassengerBookingModel(
            fcmToken = fcmToken,
            passengerUserID = FirebaseMain.getUser().uid,
            bookingID = generateBookingID,
            bookingStatus = "Waiting",
            pickupLongitude = StaticDataPasser.storePickupLongitude,
            pickupLatitude = StaticDataPasser.storePickupLatitude,
            destinationLongitude = point.longitude(),
            destinationLatitude = point.latitude(),
            bookingDate = getCurrentTimeAndDate(),
            passengerName = fullName,
            passengerProfilePicture = profilePicture,
            passengerType = userType,
            passengerDisability = disability,
        )
        locationReference.setValue(passengerBookingModel)
            .addOnSuccessListener {

                showToast("Booking success")
                loadBookingsToMapFromDatabase()

            }.addOnFailureListener {
                showToast("Booking failed")
                Log.e(TAG, "storePWDBookingToDatabase: " + it.message)
            }
    }

    private fun storeSeniorCitizenBookingToDatabase(
        fcmToken: String,
        point: Point,
        fullName: String,
        userType: String,
        profilePicture: String,
        generateBookingID: String
    ) {

        val database = FirebaseDatabase.getInstance()
        val locationReference = database.getReference(FirebaseMain.bookingCollection)
            .child(generateBookingID)

        val passengerBookingModel = PassengerBookingModel(
            fcmToken = fcmToken,
            passengerUserID = FirebaseMain.getUser().uid,
            bookingID = generateBookingID,
            bookingStatus = "Waiting",
            pickupLongitude = StaticDataPasser.storePickupLongitude,
            pickupLatitude = StaticDataPasser.storePickupLatitude,
            destinationLongitude = point.longitude(),
            destinationLatitude = point.latitude(),
            bookingDate = getCurrentTimeAndDate(),
            passengerName = fullName,
            passengerProfilePicture = profilePicture,
            passengerType = userType,
        )

        locationReference.setValue(passengerBookingModel)
            .addOnSuccessListener {

//                showToast("Booking success")
                loadBookingsToMapFromDatabase()

            }.addOnFailureListener {

                showToast("Booking failed")
                Log.e(TAG, "storeSeniorCitizenBookingToDatabase: " + it.message)

            }
    }

    private fun checkBookingStatus() {
        val bookingReference = FirebaseDatabase.getInstance()
            .getReference(FirebaseMain.bookingCollection)

        bookingReference.addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {

                    val userID = FirebaseMain.getUser().uid
                    for (locationSnapshot in snapshot.children) {
                        val passengerBookingData =
                            locationSnapshot.getValue(PassengerBookingModel::class.java)
                        if (passengerBookingData != null) {

                            if (passengerBookingData.passengerUserID == userID &&
                                passengerBookingData.bookingStatus == "Waiting"
                            ) {
                                hasActiveBooking = true

                            } else if (passengerBookingData.passengerUserID == userID &&
                                passengerBookingData.bookingStatus == "Driver on the way"
                            ) {
                                hasActiveBooking = true

                                binding.longTapTextView.text = "Your Driver on the way!"
                            }
                        }
                    }

                    if (hasActiveBooking) {
                        mapboxMap = binding.mapView.getMapboxMap().apply {
                            removeOnMapLongClickListener(this@MapPassengerActivity)
                        }
                    } else {
                        mapboxMap = binding.mapView.getMapboxMap().apply {
                            addOnMapLongClickListener(this@MapPassengerActivity)
                        }
                    }
                } else {
                    mapboxMap = binding.mapView.getMapboxMap().apply {
                        addOnMapLongClickListener(this@MapPassengerActivity)
                    }

                    Log.e(TAG, "checkBookingStatus: addValueEventListener is null")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "checkBookingStatus: " + error.message)
            }
        }
        )
    }

    private fun showBookingIsAcceptedNotification() {
        val notificationHelper = NotificationHelper(this)
        notificationHelper.showBookingIsAcceptedNotificationNotification(
            "CareCabs",
            "A Driver has accepted your Booking and is on the way to your location"
        )
    }

    private fun showMapInstructionsDialog() {
        val binding: DialogMapInstructionsBinding =
            DialogMapInstructionsBinding.inflate(layoutInflater)
        builder = AlertDialog.Builder(this)
        val dialogView = binding.root

        if (voiceAssistantState.equals("enabled")) {
            voiceAssistant = VoiceAssistant.getInstance(this)
            voiceAssistant.speak("Do you need help?")
        }

        binding.closeBtn.setOnClickListener {
            closeMapInstructionsDialog()
        }

        builder.setView(dialogView)
        mapInstructionsDialog = builder.create()
        mapInstructionsDialog.show()
    }

    private fun closeMapInstructionsDialog() {
        if (mapInstructionsDialog.isShowing) {
            mapInstructionsDialog.dismiss()
        }
    }

    private fun showHasActiveBookingDialog() {
        val binding: DialogHasActiveBookingBinding =
            DialogHasActiveBookingBinding.inflate(layoutInflater)
        builder = AlertDialog.Builder(this)
        val dialogView = binding.root

        binding.okayBtn.setOnClickListener {
            closeHasActiveBookingDialog()
        }

        builder.setView(dialogView)
        hasActiveBookingDialog = builder.create()
        hasActiveBookingDialog.show()
    }

    private fun closeHasActiveBookingDialog() {
        if (hasActiveBookingDialog.isShowing) {
            hasActiveBookingDialog.dismiss()
        }
    }

    private fun showOwnBookingInfoDialog(bookingID: String) {

        val binding: DialogPassengerOwnBookingInfoBinding =
            DialogPassengerOwnBookingInfoBinding.inflate(layoutInflater)
        builder = AlertDialog.Builder(this)
        val dialogView = binding.root

        var isDriverOnTheWay = false
        val bookingReference = FirebaseDatabase.getInstance()
            .getReference(FirebaseMain.bookingCollection)

        bookingReference.addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (bookingData in snapshot.children) {
                        val passengerBookingModel =
                            bookingData.getValue(PassengerBookingModel::class.java)

                        if (passengerBookingModel != null) {
                            if (passengerBookingModel.bookingID == bookingID &&
                                passengerBookingModel.passengerUserID == FirebaseMain.getUser().uid
                            ) {
                                val destination = passengerBookingModel.destination
                                val pickupLocation = passengerBookingModel.pickupLocation

                                binding.loading1.visibility = View.GONE
                                binding.loading2.visibility = View.GONE

                                binding.pickupLocationTextView.text = pickupLocation
                                binding.destinationTextView.text = destination

                                val message = "Pickup location: $pickupLocation." +
                                        " Destination: $destination"

                                if (voiceAssistantState == "enabled") {
                                    voiceAssistant =
                                        VoiceAssistant.getInstance(this@MapPassengerActivity)
                                    voiceAssistant.speak(message)
                                }

                                if (passengerBookingModel.bookingStatus == "Driver on the way") {
                                    isDriverOnTheWay = true
                                    binding.cancelBookingBtn.visibility = View.GONE
                                }
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "onCancelled: " + error.message)
            }

        })

        binding.cancelBookingBtn.setOnClickListener {
            if (isDriverOnTheWay) {
                val updateBookingStatus = mapOf("bookingStatus" to "Cancelled")
                bookingReference.child(bookingID).updateChildren(updateBookingStatus)
                    .addOnSuccessListener {
                        showToast("Booking cancelled")

                        hasActiveBooking = false
                        loadBookingsToMapFromDatabase()
                        closeOwnBookingInfoDialog()
                    }
                    .addOnFailureListener {
                        showToast("Booking failed to cancel")

                        Log.e(TAG, "showPassengerOwnBookingInfoDialog: " + it.message)
                    }
            }
        }

        binding.closeBtn.setOnClickListener {
            closeOwnBookingInfoDialog()
        }

        builder.setView(dialogView)
        passengerOwnBookingInfoDialog = builder.create()
        passengerOwnBookingInfoDialog.show()
    }

    private fun closeOwnBookingInfoDialog() {
        if (passengerOwnBookingInfoDialog.isShowing) {
            passengerOwnBookingInfoDialog.dismiss()
        }
    }

    private fun showUserNotVerifiedDialog() {
        builder = AlertDialog.Builder(this)
        builder.setCancelable(false)

        val dialogView = layoutInflater.inflate(R.layout.dialog_user_not_verified, null)
        val okBtn = dialogView.findViewById<Button>(R.id.okBtn)

        okBtn.setOnClickListener {
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

    private fun showExitMapDialog() {
        builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_exit_map, null)
        val exitBtn = dialogView.findViewById<Button>(R.id.exitBtn)
        val cancelBtn = dialogView.findViewById<Button>(R.id.cancelBtn)

        if (voiceAssistantState == "enabled") {
            voiceAssistant =
                VoiceAssistant.getInstance(this@MapPassengerActivity)
            voiceAssistant.speak("Are you sure you want to exit Map?")
        }

        exitBtn.setOnClickListener {
            intent = Intent(this@MapPassengerActivity, MainActivity::class.java)
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

    private fun showConfirmBookingBottomSheet(destination: Point) {
        val destinationJson = destination.toJson()
        val confirmBookingBottomSheet = ConfirmBookingBottomSheet.newInstance(destinationJson)
        confirmBookingBottomSheet.show(supportFragmentManager, confirmBookingBottomSheet.tag)
    }

    private fun reverseGeocoding(point: Point) {
        val types: List<PlaceAutocompleteType> = when (mapboxMap.cameraState.zoom) {
            in 0.0..4.0 -> REGION_LEVEL_TYPES
            in 4.0..6.0 -> DISTRICT_LEVEL_TYPES
            in 6.0..12.0 -> LOCALITY_LEVEL_TYPES
            else -> ALL_TYPES
        }

        lifecycleScope.launchWhenStarted {
            val response =
                placeAutocomplete.suggestions(point, PlaceAutocompleteOptions(types = types))
            response.onValue { suggestions ->
                if (suggestions.isEmpty()) {
                    Toast.makeText(this@MapPassengerActivity, "yeah", Toast.LENGTH_LONG).show()
                } else {
                    openPlaceCard(suggestions.first())
                }
            }.onError { error ->
                Log.d(LOG_TAG, "Reverse geocoding error", error)
                Toast.makeText(this@MapPassengerActivity, error.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openPlaceCard(suggestion: PlaceAutocompleteSuggestion) {
        ignoreNextQueryUpdate = true
//        binding.searchDestinationEditText.setText("")

        lifecycleScope.launchWhenStarted {
            placeAutocomplete.select(suggestion).onValue { result ->
                mapMarkersManager.showMarker(suggestion.coordinate)
                binding.searchPlaceView.open(SearchPlace.createFromPlaceAutocompleteResult(result))
//                binding.searchDestinationEditText.hideKeyboard()
                binding.searchResultsView.isVisible = false
            }.onError { error ->
                Log.d(LOG_TAG, "Suggestion selection error", error)
                Toast.makeText(this@MapPassengerActivity, error.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun closePlaceCard() {
        binding.searchPlaceView.hide()
        mapMarkersManager.clearMarkers()
    }

    private class MapMarkersManager(mapView: MapView) {

        private val mapboxMap = mapView.getMapboxMap()
        private val circleAnnotationManager =
            mapView.annotations.createCircleAnnotationManager(null)
        private val markers = mutableMapOf<Long, Point>()

        fun clearMarkers() {
            markers.clear()
            circleAnnotationManager.deleteAll()
        }

        fun showMarker(coordinate: Point) {
            clearMarkers()

            val circleAnnotationOptions: CircleAnnotationOptions = CircleAnnotationOptions()
                .withPoint(coordinate)
                .withCircleRadius(8.0)
                .withCircleColor("#ee4e8b")
                .withCircleStrokeWidth(2.0)
                .withCircleStrokeColor("#ffffff")

            val annotation = circleAnnotationManager.create(circleAnnotationOptions)
            markers[annotation.id] = coordinate

            CameraOptions.Builder()
                .center(coordinate)
                .padding(MARKERS_INSETS_OPEN_CARD)
                .zoom(14.0)
                .build().also {
                    mapboxMap.setCamera(it)
                }
        }
    }

    private fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }

    private companion object {

        const val PERMISSIONS_REQUEST_LOCATION = 0

        const val LOG_TAG = "AutocompleteUiActivity"

        val MARKERS_EDGE_OFFSET = dpToPx(64F).toDouble()
        val PLACE_CARD_HEIGHT = dpToPx(300F).toDouble()
        val MARKERS_TOP_OFFSET = dpToPx(88F).toDouble()

        val MARKERS_INSETS_OPEN_CARD = EdgeInsets(
            MARKERS_TOP_OFFSET, MARKERS_EDGE_OFFSET, PLACE_CARD_HEIGHT, MARKERS_EDGE_OFFSET
        )

        val REGION_LEVEL_TYPES = listOf(
            PlaceAutocompleteType.AdministrativeUnit.Country,
            PlaceAutocompleteType.AdministrativeUnit.Region
        )

        val DISTRICT_LEVEL_TYPES = REGION_LEVEL_TYPES + listOf(
            PlaceAutocompleteType.AdministrativeUnit.Postcode,
            PlaceAutocompleteType.AdministrativeUnit.District
        )

        val LOCALITY_LEVEL_TYPES = DISTRICT_LEVEL_TYPES + listOf(
            PlaceAutocompleteType.AdministrativeUnit.Place,
            PlaceAutocompleteType.AdministrativeUnit.Locality
        )

        private val ALL_TYPES = listOf(
            PlaceAutocompleteType.Poi,
            PlaceAutocompleteType.AdministrativeUnit.Country,
            PlaceAutocompleteType.AdministrativeUnit.Region,
            PlaceAutocompleteType.AdministrativeUnit.Postcode,
            PlaceAutocompleteType.AdministrativeUnit.District,
            PlaceAutocompleteType.AdministrativeUnit.Place,
            PlaceAutocompleteType.AdministrativeUnit.Locality,
            PlaceAutocompleteType.AdministrativeUnit.Neighborhood,
            PlaceAutocompleteType.AdministrativeUnit.Street,
            PlaceAutocompleteType.AdministrativeUnit.Address,
        )
    }

}