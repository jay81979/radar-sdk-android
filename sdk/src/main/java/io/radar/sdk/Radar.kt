package io.radar.sdk

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import io.radar.sdk.model.*
import io.radar.sdk.model.RadarEvent.RadarEventVerification
import io.radar.sdk.util.RadarLogBuffer
import io.radar.sdk.util.RadarReplayBuffer
import io.radar.sdk.util.RadarSimpleLogBuffer
import io.radar.sdk.util.RadarSimpleReplayBuffer
import org.json.JSONObject
import java.util.*

/**
 * The main class used to interact with the Radar SDK.
 *
 * @see [](https://radar.com/documentation/sdk)
 */
@SuppressLint("StaticFieldLeak")
object Radar {

    /**
     * Called when a location request succeeds, fails, or times out.
     */
    interface RadarLocationCallback {

        /**
         * Called when a location request succeeds, fails, or times out. Receives the request status and, if successful, the location.
         *
         * @param[status] RadarStatus The request status.
         * @param[location] Location? If successful, the location.
         * @param[stopped] Boolean A boolean indicating whether the device is stopped.
         */
        fun onComplete(
            status: RadarStatus,
            location: Location? = null,
            stopped: Boolean = false
        )

    }

    /**
     * Called when a beacon ranging request succeeds, fails, or times out.
     */
    interface RadarBeaconCallback {

        /**
         * Called when a beacon ranging request succeeds, fails, or times out. Receives the request status and, if successful, the nearby beacons.
         *
         * @param[status] RadarStatus The request status.
         * @param[beacons] Array<String>? If successful, the nearby beacons.
         */
        fun onComplete(
            status: RadarStatus,
            beacons: Array<RadarBeacon>? = null
        )

    }

    /**
     * Called when a track request succeeds, fails, or times out.
     */
    interface RadarTrackCallback {

        /**
         * Called when a track request succeeds, fails, or times out. Receives the request status and, if successful, the user's location, an array of the events generated, and the user.
         *
         * @param[status] RadarStatus The request status.
         * @param[location] Location? If successful, the user's location.
         * @param[events] Array<RadarEvent>? If successful, an array of the events generated.
         * @param[user] RadarUser? If successful, the user.
         */
        fun onComplete(
            status: RadarStatus,
            location: Location? = null,
            events: Array<RadarEvent>? = null,
            user: RadarUser? = null
        )
    }

    /**
     * Called when a trip update succeeds, fails, or times out.
     */
    interface RadarTripCallback {

        /**
         * Called when a trip update succeeds, fails, or times out. Receives the request status and, if successful, the trip and an array of the events generated.
         *
         * @param[status] RadarStatus The request status.
         * @param[trip] RadarTrip? If successful, the trip.
         * @param[events] Array<RadarEvent>? If successful, an array of the events generated.
         */
        fun onComplete(
            status: RadarStatus,
            trip: RadarTrip? = null,
            events: Array<RadarEvent>? = null
        )

    }

    /**
     * Called when a context request succeeds, fails, or times out.
     */
    interface RadarContextCallback {

        /**
         * Called when a context request succeeds, fails, or times out. Receives the request status and, if successful, the location and the context.
         *
         * @param[status] RadarStatus The request status.
         * @param[location] Location? If successful, the location.
         * @param[context] RadarContext? If successful, the context.
         */
        fun onComplete(
            status: RadarStatus,
            location: Location? = null,
            context: RadarContext? = null
        )
    }

    /**
     * Called when a place search request succeeds, fails, or times out.
     */
    interface RadarSearchPlacesCallback {
        /**
         * Called when a place search request succeeds, fails, or times out. Receives the request status and, if successful, the location and an array of places sorted by distance.
         *
         * @param[status] RadarStatus The request status.
         * @param[location] Location? If successful, the location.
         * @param[places] Array<RadarPlace>? If successful, an array of places sorted by distance.
         */
        fun onComplete(
            status: RadarStatus,
            location: Location? = null,
            places: Array<RadarPlace>? = null
        )
    }

    /**
     * Called when a geofence search request succeeds, fails, or times out.
     */
    interface RadarSearchGeofencesCallback {
        /**
         * Called when a geofence search request succeeds, fails, or times out. Receives the request status and, if successful, the location and an array of geofences sorted by distance.
         *
         * @param[status] RadarStatus The request status.
         * @param[location] Location? If successful, the location.
         * @param[geofences] Array<RadarGeofence>? If successful, an array of geofences sorted by distance.
         */
        fun onComplete(
            status: RadarStatus,
            location: Location? = null,
            geofences: Array<RadarGeofence>? = null
        )
    }

    /**
     * Called when a geocoding request succeeds, fails, or times out.
     */
    interface RadarGeocodeCallback {
        /**
         * Called when a geocoding request succeeds, fails, or times out. Receives the request status and, if successful, the geocoding results (an array of addresses).
         *
         * @param[status] RadarStatus The request status.
         * @param[addresses] Array<RadarAddress>? If successful, the geocoding results (an array of addresses).
         */
        fun onComplete(
            status: RadarStatus,
            addresses: Array<RadarAddress>? = null
        )
    }

    /**
     * Called when a validateAddress request succeeds, fails, or times out.
     * Receives the request status and, if successful, the address populated with a verification status.
     */

    interface RadarValidateAddressCallback {
        fun onComplete(
            status: RadarStatus,
            address: RadarAddress? = null,
            verificationStatus: RadarAddressVerificationStatus? = null
        )
    }

    /**
     * Called when an IP geocoding request succeeds, fails, or times out.
     */
    interface RadarIpGeocodeCallback {
        /**
         * Called when an IP geocoding request succeeds, fails, or times out. Receives the request status and, if successful, the geocoding result (a partial address) and a boolean indicating whether the IP address is a known proxy.
         *
         * @param[status] RadarStatus The request status.
         * @param[address] RadarAddress? If successful, the geocoding result (a partial address).
         * @param[proxy] Boolean A boolean indicating whether the IP address is a known proxy.
         */
        fun onComplete(
            status: RadarStatus,
            address: RadarAddress? = null,
            proxy: Boolean = false
        )
    }

    /**
     * Called when a distance request succeeds, fails, or times out.
     */
    interface RadarRouteCallback {
        /**
         * Called when a distance request succeeds, fails, or times out. Receives the request status and, if successful, the routes.
         *
         * @param[status] RadarStatus The request status.
         * @param[routes] RadarRoutes? If successful, the routes.
         */
        fun onComplete(
            status: RadarStatus,
            routes: RadarRoutes? = null
        )
    }

    /**
     * Called when a matrix request succeeds, fails, or times out.
     */
    interface RadarMatrixCallback {
        /**
         * Called when a matrix request succeeds, fails, or times out. Receives the request status and, if successful, the matrix.
         *
         * @param[status] RadarStatus The request status.
         * @param[matrix] RadarRoutesMatrix? If successful, the matrix.
         */
        fun onComplete(
            status: RadarStatus,
            matrix: RadarRouteMatrix? = null
        )
    }

    /**
     * Called when a request to log a conversion succeeds, fails, or times out.
     */
    interface RadarLogConversionCallback {
        /**
         * Called when a request to log a conversion succeeds, fails, or times out. Receives the request status and, if successful, the conversion event generated.
         *
         * @param[status] RadarStatus The request status.
         * @param[event] RadarEvent? If successful, the conversion event.
         *
         */
        fun onComplete(
            status: RadarStatus,
            event: RadarEvent? = null
        )
    }

    /**
     * The status types for a request. See [](https://radar.com/documentation/sdk/android#foreground-tracking).
     */
    enum class RadarStatus {
        /** Success */
        SUCCESS,

        /** SDK not initialized */
        ERROR_PUBLISHABLE_KEY,

        /** Location permissions not granted */
        ERROR_PERMISSIONS,

        /** Location services error or timeout (20 seconds) */
        ERROR_LOCATION,

        /** Beacon ranging error or timeout (5 seconds) */
        ERROR_BLUETOOTH,

        /** Network error or timeout (10 seconds) */
        ERROR_NETWORK,

        /** Bad request (missing or invalid params) */
        ERROR_BAD_REQUEST,

        /** Unauthorized (invalid API key) */
        ERROR_UNAUTHORIZED,

        /** Payment required (organization disabled or usage exceeded) */
        ERROR_PAYMENT_REQUIRED,

        /** Forbidden (insufficient permissions or no beta access) */
        ERROR_FORBIDDEN,

        /** Not found */
        ERROR_NOT_FOUND,

        /** Too many requests (rate limit exceeded) */
        ERROR_RATE_LIMIT,

        /** Internal server error */
        ERROR_SERVER,

        /** Unknown error */
        ERROR_UNKNOWN
    }

    /**
     * The sources for location updates.
     */
    enum class RadarLocationSource {
        /** Foreground */
        FOREGROUND_LOCATION,

        /** Background */
        BACKGROUND_LOCATION,

        /** Manual */
        MANUAL_LOCATION,

        /** Geofence enter */
        GEOFENCE_ENTER,

        /** Geofence dwell */
        GEOFENCE_DWELL,

        /** Geofence exit */
        GEOFENCE_EXIT,

        /** Mock */
        MOCK_LOCATION,

        /** Beacon enter */
        BEACON_ENTER,

        /** Beacon exit */
        BEACON_EXIT,

        /** Unknown */
        UNKNOWN
    }

    /**
     * The levels for debug logs.
     */
    enum class RadarLogLevel(val value: Int) {
        /** None */
        NONE(0),

        /** Error */
        ERROR(1),

        /** Warning */
        WARNING(2),

        /** Info */
        INFO(3),

        /** Debug */
        DEBUG(4);

        companion object {
            @JvmStatic
            fun fromInt(value: Int): RadarLogLevel {
                return values().first { it.value == value }
            }
        }
    }

    /**
     * The classification type for debug logs.
     */
    enum class RadarLogType(val value: Int) {
        NONE(0),
        SDK_CALL(1),
        SDK_ERROR(2),
        SDK_EXCEPTION(3),
        APP_LIFECYCLE_EVENT(4),
        PERMISSION_EVENT(5);

        companion object {
            @JvmStatic
            fun fromInt(value: Int): RadarLogType {
                return values().first { it.value == value }
            }
        }
    }

    /**
     * The travel modes for routes. See [](https://radar.com/documentation/api#routing).
     */
    enum class RadarRouteMode {
        /** Foot */
        FOOT,

        /** Bike */
        BIKE,

        /** Car */
        CAR,

        /** Truck */
        TRUCK,

        /** Motorbike */
        MOTORBIKE
    }

    /**
     * The distance units for routes. See [](https://radar.com/documentation/api#routing).
     */
    enum class RadarRouteUnits {
        /** Imperial (feet) */
        IMPERIAL,

        /** Metric (meters) */
        METRIC
    }

    /**
     * The verification status of an address.
     */
    enum class RadarAddressVerificationStatus {
        VERIFIED,
        PARTIALLY_VERIFIED,
        AMBIGUOUS,
        UNVERIFIED,
        NONE
    }

    enum class RadarActivityType {
        UNKNOWN,
        STATIONARY,
        FOOT,
        RUN,
        BIKE,
        CAR;

        companion object {
            @JvmStatic
            fun fromString(value: String): RadarActivityType {
                return when (value) {
                    "unknown" -> UNKNOWN
                    "stationary" -> STATIONARY
                    "foot" -> FOOT
                    "run" -> RUN
                    "bike" -> BIKE
                    "car" -> CAR
                    else -> UNKNOWN
                }
            }
        }

        override fun toString(): String {
            return when (this) {
                UNKNOWN -> "unknown"
                STATIONARY -> "stationary"
                FOOT -> "foot"
                RUN -> "run"
                BIKE -> "bike"
                CAR -> "car"
            }
        }
    }

    /**
     * The location services providers.
     */
    enum class RadarLocationServicesProvider {
        /** Google Play Services Location (default) */
        GOOGLE,

        /** Huawei Mobile Services Location */
        HUAWEI
    }

    internal var initialized = false
    internal var isFlushingReplays = false
    private lateinit var context: Context
    private var activity: Activity? = null
    internal lateinit var handler: Handler
    private var receiver: RadarReceiver? = null
    internal lateinit var logger: RadarLogger
    internal lateinit var locationManager: RadarLocationManager
    internal lateinit var beaconManager: RadarBeaconManager
    private lateinit var logBuffer: RadarLogBuffer
    private lateinit var replayBuffer: RadarReplayBuffer
    internal lateinit var batteryManager: RadarBatteryManager

    /**
     * Initializes the Radar SDK. Call this method from the main thread in `Application.onCreate()` before calling any other Radar methods.
     *
     * @see [](https://radar.com/documentation/sdk/android#initialize-sdk)
     *
     * @param[context] The context.
     */
    @JvmStatic
    fun initialize(context: Context?) {
        initialize(context, null)
    }

    /**
     * Initializes the Radar SDK. Call this method from the main thread in `Application.onCreate()` before calling any other Radar methods.
     *
     * @see [](https://radar.com/documentation/sdk/android#initialize-sdk)
     *
     * @param[context] The context.
     * @param[receiver] An optional receiver for the client-side delivery of events.
     * @param[provider] The location services provider.
     * @param[fraud] A boolean indicating whether to enable additional fraud detection signals for location verification.
     */
    @JvmStatic
    fun initialize(
        context: Context?,
        receiver: RadarReceiver? = null,
        provider: RadarLocationServicesProvider = RadarLocationServicesProvider.GOOGLE,
        fraud: Boolean = false
    ) {
        if (context == null) {
            return
        }

        this.context = context.applicationContext
        this.handler = Handler(this.context.mainLooper)

        if (context is Activity) {
            this.activity = context
        }

        if (receiver != null) {
            this.receiver = receiver
        }

        if (!this::logBuffer.isInitialized) {
            this.logBuffer = RadarSimpleLogBuffer(this.context)
        }

        if (!this::replayBuffer.isInitialized) {
            this.replayBuffer = RadarSimpleReplayBuffer(this.context)
        }

        if (!this::logger.isInitialized) {
            this.logger = RadarLogger(this.context)
        }

        if (RadarActivityLifecycleCallbacks.foreground) {
            this.logger.d("App is foregrounded")
            RadarSettings.updateSessionId(this.context)
        } else {
            this.logger.d("App is backgrounded, not updating session ID")
        }

        if (!this::batteryManager.isInitialized) {
            this.batteryManager = RadarBatteryManager(this.context)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!this::beaconManager.isInitialized) {
                this.beaconManager = RadarBeaconManager(this.context, logger)
            }
        }
        if (!this::locationManager.isInitialized) {
            this.locationManager =
                RadarLocationManager(this.context, logger, batteryManager, provider)
            RadarSettings.setLocationServicesProvider(this.context, provider)
            this.locationManager.updateTracking()
        }

        this.logger.i("initialize()", RadarLogType.SDK_CALL)

        if (provider == RadarLocationServicesProvider.GOOGLE) {
            this.logger.d("Using Google location services")
        } else if (provider == RadarLocationServicesProvider.HUAWEI) {
            this.logger.d("Using Huawei location services")
        }

        val application = this.context as? Application
        if (fraud) {
            RadarSettings.setSharing(this.context, false)
        }
        application?.registerActivityLifecycleCallbacks(RadarActivityLifecycleCallbacks(fraud))

        var sdkConfiguration = RadarSettings.getSdkConfiguration(this.context)
        if (sdkConfiguration.usePersistence) {
            Radar.loadReplayBufferFromSharedPreferences()
        }

        val usage = "initialize"
        val config = RadarConfig(
            meta = RadarMeta(
                remoteTrackingOptions = null,
                sdkConfiguration = RadarSdkConfiguration(
                    maxConcurrentJobs = 1,
                    schedulerRequiresNetwork = false,
                    usePersistence = false,
                    extendFlushReplays = false,
                    useLogPersistence = false,
                    useRadarModifiedBeacon = false,
                    logLevel = Radar.RadarLogLevel.INFO,
                    startTrackingOnInitialize = false,
                    trackOnceOnAppOpen = false,
                    useLocationMetadata = false,
                    useOpenedAppConversion = true
                ),
            ),
            googlePlayProjectNumber = 0,
            nonce = ""
        )

        locationManager.updateTrackingFromMeta(config.meta)
        RadarSettings.setSdkConfiguration(context, config.meta.sdkConfiguration)

        sdkConfiguration = RadarSettings.getSdkConfiguration(context)
        if (sdkConfiguration.startTrackingOnInitialize && !RadarSettings.getTracking(context)) {
            Radar.startTracking(Radar.getTrackingOptions())
        }
        if (sdkConfiguration.trackOnceOnAppOpen) {
            Radar.trackOnce()
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            this.logger.logPastTermination()
        }

        this.initialized = true

        logger.i("📍️ Radar initialized")
    }

    /**
     * Identifies the user. Until you identify the user, Radar will automatically identify the user by `deviceId` (Android ID).
     *
     * @see [](https://radar.com/documentation/sdk/android#identify-user)
     *
     * @param[userId] A stable unique ID for the user. If null, the previous `userId` will be cleared.
     */
    @JvmStatic
    fun setUserId(userId: String?) {
        if (!initialized) {
            return
        }

        RadarSettings.setUserId(context, userId)
    }

    /**
     * Returns the current `userId`.
     *
     * @see [](https://radar.com/documentation/sdk/android#identify-user)
     *
     * @return The current `userId`.
     */
    @JvmStatic
    fun getUserId(): String? {
        if (!initialized) {
            return null
        }

        return RadarSettings.getUserId(context)
    }

    /**
     * Sets an optional description for the user, displayed in the dashboard.
     *
     * @see [](https://radar.com/documentation/sdk/android#identify-user)
     *
     * @param[description] A description for the user. If null, the previous `description` will be cleared.
     */
    @JvmStatic
    fun setDescription(description: String?) {
        if (!initialized) {
            return
        }

        RadarSettings.setDescription(context, description)
    }

    /**
     * Returns the current `description`.
     *
     * @see [](https://radar.com/documentation/sdk/android#identify-user)
     *
     * @return The current `description`.
     */
    @JvmStatic
    fun getDescription(): String? {
        if (!initialized) {
            return null
        }

        return RadarSettings.getDescription(context)
    }

    /**
     * Sets an optional set of custom key-value pairs for the user.
     *
     * @see [](https://radar.com/documentation/sdk/android#identify-user)
     *
     * @param[metadata] A set of custom key-value pairs for the user. Must have 16 or fewer keys and values of type string, boolean, or number. If `null`, the previous `metadata` will be cleared.
     */
    @JvmStatic
    fun setMetadata(metadata: JSONObject?) {
        if (!initialized) {
            return
        }

        RadarSettings.setMetadata(context, metadata)
    }

    /**
     * Returns the current `metadata`.
     *
     * @see [](https://radar.com/documentation/sdk/android#identify-user)
     *
     * @return The current `metadata`.
     */
    @JvmStatic
    fun getMetadata(): JSONObject? {
        if (!initialized) {
            return null
        }

        return RadarSettings.getMetadata(context)
    }

    /**
     * Enables anonymous tracking for privacy reasons. Avoids creating user records on the server and avoids sending any stable device IDs, user IDs, and user metadata
     * to the server when calling `trackOnce()` or `startTracking()`. Disabled by default.
     *
     * @param[enabled] A boolean indicating whether anonymous tracking should be enabled.
     */
    @JvmStatic
    fun setAnonymousTrackingEnabled(enabled: Boolean) {
        RadarSettings.setAnonymousTrackingEnabled(context, enabled)
    }

    /**
     * Gets the device's current location.
     *
     * @see [](https://radar.com/documentation/sdk/android#get-location)
     *
     * @param[callback] An optional callback.
     */
    @JvmStatic
    fun getLocation(callback: RadarLocationCallback? = null) {
        if (!initialized) {
            callback?.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }
        this.logger.i("getLocation()", RadarLogType.SDK_CALL)

        locationManager.getLocation(object : RadarLocationCallback {
            override fun onComplete(status: RadarStatus, location: Location?, stopped: Boolean) {
                handler.post {
                    callback?.onComplete(status, location, stopped)
                }
            }
        })
    }

    /**
     * Gets the device's current location.
     *
     * @see [](https://radar.com/documentation/sdk/android#get-location)
     *
     * @param[block] A block callback.
     */
    fun getLocation(block: (status: RadarStatus, location: Location?, stopped: Boolean) -> Unit) {
        getLocation(object : RadarLocationCallback {
            override fun onComplete(status: RadarStatus, location: Location?, stopped: Boolean) {
                block(status, location, stopped)
            }
        })
    }

    /**
     * Gets the device's current location with the desired accuracy.
     *
     * @see [](https://radar.com/documentation/sdk/android#get-location)
     *
     * @param[desiredAccuracy] The desired accuracy.
     * @param[callback] An optional callback.
     */
    @JvmStatic
    fun getLocation(
        desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy,
        callback: RadarLocationCallback? = null
    ) {
        if (!initialized) {
            callback?.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }
        this.logger.i("getLocation()", RadarLogType.SDK_CALL)

        locationManager.getLocation(
            desiredAccuracy,
            RadarLocationSource.FOREGROUND_LOCATION,
            object : RadarLocationCallback {
                override fun onComplete(
                    status: RadarStatus,
                    location: Location?,
                    stopped: Boolean
                ) {
                    handler.post {
                        callback?.onComplete(status, location, stopped)
                    }
                }
            })
    }

    /**
     * Gets the device's current location with the desired accuracy.
     *
     * @see [](https://radar.com/documentation/sdk/android#get-location)
     *
     * @param[desiredAccuracy] The desired accuracy.
     * @param[block] A block callback.
     */
    fun getLocation(
        desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy,
        block: (status: RadarStatus, location: Location?, stopped: Boolean) -> Unit
    ) {
        getLocation(desiredAccuracy, object : RadarLocationCallback {
            override fun onComplete(status: RadarStatus, location: Location?, stopped: Boolean) {
                block(status, location, stopped)
            }
        })
    }

    /**
     * Tracks the user's location once in the foreground.
     *
     * @see [](https://radar.com/documentation/sdk/android#foreground-tracking)
     *
     * @param[callback] An optional callback.
     */
    @JvmStatic
    fun trackOnce(callback: RadarTrackCallback? = null) {
        var desiredAccuracy = RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.MEDIUM
        if (RadarUtils.isEmulator()) {
            desiredAccuracy = RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.HIGH
        }
        trackOnce(desiredAccuracy, false, callback)
    }

    /**
     * Tracks the user's location once in the foreground.
     *
     * @see [](https://radar.com/documentation/sdk/android#foreground-tracking)
     *
     * @param[block] A block callback.
     */
    fun trackOnce(block: (status: RadarStatus, location: Location?, events: Array<RadarEvent>?, user: RadarUser?) -> Unit) {
        var desiredAccuracy = RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.MEDIUM
        if (RadarUtils.isEmulator()) {
            desiredAccuracy = RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.HIGH
        }
        trackOnce(desiredAccuracy, false, block)
    }

    /**
     * Tracks the user's location once with the desired accuracy and optionally ranges beacons in the foreground.
     *
     * @see [](https://radar.com/documentation/sdk/android#foreground-tracking)
     *
     * @param[desiredAccuracy] The desired accuracy.
     * @param[beacons] A boolean indicating whether to range beacons.
     * @param[callback] An optional callback.
     */
    @JvmStatic
    fun trackOnce(
        desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy,
        beacons: Boolean,
        callback: RadarTrackCallback? = null
    ) {
        if (!initialized) {
            callback?.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }
        this.logger.i("trackOnce()", RadarLogType.SDK_CALL)

        locationManager.getLocation(
            desiredAccuracy,
            RadarLocationSource.FOREGROUND_LOCATION,
            object : RadarLocationCallback {
                override fun onComplete(
                    status: RadarStatus,
                    location: Location?,
                    stopped: Boolean
                ) {
                    if (status != RadarStatus.SUCCESS || location == null) {
                        handler.post {
                            callback?.onComplete(status)
                        }

                        return
                    }
                }
            })
    }

    /**
     * Tracks the user's location once with the desired accuracy and optionally ranges beacons in the foreground.
     *
     * @see [](https://radar.com/documentation/sdk/android#foreground-tracking)
     *
     * @param[desiredAccuracy] The desired accuracy.
     * @param[beacons] A boolean indicating whether to range beacons.
     * @param[block] A block callback.
     */
    @JvmStatic
    fun trackOnce(
        desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy,
        beacons: Boolean,
        block: (status: RadarStatus, location: Location?, events: Array<RadarEvent>?, user: RadarUser?) -> Unit
    ) {
        trackOnce(desiredAccuracy, beacons, object : RadarTrackCallback {
            override fun onComplete(
                status: RadarStatus,
                location: Location?,
                events: Array<RadarEvent>?,
                user: RadarUser?
            ) {
                block(status, location, events, user)
            }
        })
    }

    /**
     * Starts tracking the user's location in the background.
     *
     * @see [](https://radar.com/documentation/sdk/android#background-tracking-for-geofencing)
     *
     * @param[options] Configurable tracking options.
     */
    @JvmStatic
    fun startTracking(options: RadarTrackingOptions) {
        if (!initialized) {
            return
        }
        this.logger.i("startTracking()", RadarLogType.SDK_CALL)

        locationManager.startTracking(options)
    }

    /**
     * Stops tracking the user's location in the background.
     *
     * @see [](https://radar.com/documentation/sdk/android#background-tracking-for-geofencing)
     */
    @JvmStatic
    fun stopTracking() {
        if (!initialized) {
            return
        }
        this.logger.i("stopTracking()", RadarLogType.SDK_CALL)

        locationManager.stopTracking()
    }

    /**
     * Returns a boolean indicating whether tracking has been started.
     *
     * @see [](https://radar.com/documentation/sdk/android#background-tracking-for-geofencing)
     *
     * @return A boolean indicating whether tracking has been started.
     */
    @JvmStatic
    fun isTracking(): Boolean {
        if (!initialized) {
            return false
        }

        return RadarSettings.getTracking(context)
    }




    /**
     * Returns the current tracking options.
     *
     * @see [](https://radar.com/documentation/sdk/tracking)
     *
     * @return The current tracking options.
     */
    @JvmStatic
    fun getTrackingOptions() = RadarSettings.getTrackingOptions(context)

    /**
     * Settings for the foreground notification when the foregroundServiceEnabled parameter
     * is true on Radar tracking options.
     *
     * @see [](https://radar.com/documentation/sdk/tracking)
     *
     * @param[options] Foreground service options
     */
    @JvmStatic
    fun setForegroundServiceOptions(options: RadarTrackingOptions.RadarTrackingOptionsForegroundService) {
        if (!initialized) {
            return
        }

        RadarSettings.setForegroundService(context, options)
    }

    /**
     * Settings for the all notifications.
     *
     * @see [](https://radar.com/documentation/sdk)
     *
     * @param[options] Notifications options
     */
    @JvmStatic
    fun setNotificationOptions(options: RadarNotificationOptions) {
        if (!initialized) {
            return
        }

        RadarSettings.setNotificationOptions(context, options)
    }


    /**
     * Sets a receiver for client-side delivery of events, location updates, and debug logs.
     *
     * @see [](https://radar.com/documentation/sdk/android#listening-for-events-with-a-receiver)
     *
     * @param[receiver] A delegate for client-side delivery of events, location updates, and debug logs. If `null`, the previous receiver will be cleared.
     */
    @JvmStatic
    fun setReceiver(receiver: RadarReceiver?) {
        if (!initialized) {
            return
        }

        this.receiver = receiver
    }

    /**
     * Logs a conversion.
     *
     * @see [](https://radar.com/documentation/api#send-a-custom-event)
     *
     * @param[name] The name of the conversion.
     * @param[metadata] The metadata associated with the conversion.
     * @param[callback] A callback.
     */
    @JvmStatic
    fun logConversion(
        name: String,
        metadata: JSONObject? = null,
        callback: RadarLogConversionCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        // if track() has been returned in the last 60 seconds, don't call it again
        val timestampSeconds = System.currentTimeMillis() / 1000
        val lastTrackedTime = RadarSettings.getLastTrackedTime(context)
        val isLastTrackRecent = timestampSeconds - lastTrackedTime < 60
        val doesNotHaveLocationPermissions =
            !locationManager.permissionsHelper.fineLocationPermissionGranted(context)
                    && !locationManager.permissionsHelper.coarseLocationPermissionGranted(context)

        if (isLastTrackRecent || doesNotHaveLocationPermissions) {
            return
        }

        trackOnce(object : RadarTrackCallback {
            override fun onComplete(
                status: RadarStatus,
                location: Location?,
                events: Array<RadarEvent>?,
                user: RadarUser?
            ) {
                if (status != RadarStatus.SUCCESS || location == null) {
                    handler.post {
                        callback.onComplete(status)
                    }

                    return
                }
            }
        })
    }

    /**
     * Logs a conversion.
     *
     * @see [](https://radar.com/documentation/api#send-a-custom-event)
     *
     * @param[name] The name of the conversion.
     * @param[metadata] The metadata associated with the conversion.
     * @param[block] A block callback
     */
    @JvmStatic
    fun logConversion(
        name: String,
        metadata: JSONObject? = null,
        block: (status: RadarStatus, event: RadarEvent?) -> Unit
    ) {
        logConversion(name, metadata, object : RadarLogConversionCallback {
            override fun onComplete(
                status: RadarStatus,
                event: RadarEvent?
            ) {
                block(status, event)
            }
        })
    }

    /**
     * Logs a conversion with revenue.
     *
     * @see [](https://radar.com/documentation/api#send-a-custom-event)
     *
     * @param[name] The name of the conversion.
     * @param[revenue] The revenue generated by the conversion.
     * @param[metadata] The metadata associated with the conversion.
     * @param[callback] A callback.
     */
    @JvmStatic
    fun logConversion(
        name: String,
        revenue: Double,
        metadata: JSONObject? = null,
        callback: RadarLogConversionCallback
    ) {
        val nonNullMetadata = metadata ?: JSONObject()
        nonNullMetadata.put("revenue", revenue);

        logConversion(name, nonNullMetadata, callback)
    }

    /**
     * Logs a conversion with revenue.
     *
     * @see [](https://radar.com/documentation/api#send-a-custom-event)
     *
     * @param[name] The name of the conversion.
     * @param[revenue] The revenue generated by the conversion.
     * @param[metadata] The metadata associated with the conversion.
     * @param[block] A block callback.
     */
    @JvmStatic
    fun logConversion(
        name: String,
        revenue: Double,
        metadata: JSONObject? = null,
        block: (status: RadarStatus, RadarEvent?) -> Unit
    ) {
        logConversion(name, revenue, metadata, object : RadarLogConversionCallback {
            override fun onComplete(
                status: RadarStatus,
                event: RadarEvent?
            ) {
                block(status, event)
            }
        })
    }

    internal fun logOpenedAppConversion() {
        if (!RadarSettings.getSdkConfiguration(context).useOpenedAppConversion) {
            return
        }
        // if opened_app has been logged in the last 1000 milliseconds, don't log it again
        val timestamp = System.currentTimeMillis()
        val lastAppOpenTime = RadarSettings.getLastAppOpenTimeMillis(context)
        if (timestamp - lastAppOpenTime > 1000) {
            RadarSettings.updateLastAppOpenTimeMillis(context)
        }
    }

    /**
     * Sets the log level for debug logs.
     *
     * @param[level] The log level.
     */
    @JvmStatic
    fun setLogLevel(level: RadarLogLevel) {
        if (!initialized) {
            return
        }
        // update clientSdkConfiguration if the new level is different, otherwise no-op
        val sdkConfiguration = RadarSettings.getClientSdkConfiguration(context)
        if (sdkConfiguration.optString("logLevel") == level.toString().lowercase()) {
            return;
        }
        sdkConfiguration.put("logLevel", level.toString().lowercase())
        RadarSettings.setClientSdkConfiguration(context, sdkConfiguration)
        // if the current log level is already the target log level, no-op
        if (RadarSettings.getLogLevel(context) == level) {
            return;
        }
    }

    /**
    Log application resigning active.
     */
    @JvmStatic
    fun logResigningActive() {
        if (!initialized) {
            return
        }
        this.logger.logResigningActive()
    }

    /**
    Log application entering background and flush logs in memory buffer into persistent buffer.
     */
    @JvmStatic
    fun logBackgrounding() {
        if (!initialized) {
            return
        }
        this.logger.logBackgrounding()
        this.logBuffer.persistLogs()
    }

    /**
     * Flushes debug logs to the server.
     */
    @JvmStatic
    internal fun flushLogs() {
        if (!initialized) {
            return
        }

        val flushable = logBuffer.getFlushableLogs()
        val logs = flushable.get()
        if (logs.isNotEmpty()) {
            flushable.onFlush(true)
        }
    }

    /**
     * Flushes replays to the server.
     */
    @JvmStatic
    internal fun flushReplays(
        replayParams: JSONObject? = null,
        callback: RadarTrackCallback? = null
    ) {
        if (!initialized) {
            return
        }

        if (isFlushingReplays) {
            this.logger.d("Already flushing replays")
            callback?.onComplete(RadarStatus.ERROR_SERVER)
            return
        }

        // check if any replays to flush
        if (!hasReplays() && replayParams == null) {
            this.logger.d("No replays to flush")
            return
        }

        this.isFlushingReplays = true

        // get a copy of the replays so we can safely clear what was synced up
        val replaysStash = replayBuffer.getFlushableReplaysStash()
        val replays = replaysStash.get().toMutableList()

        // if we have a current track update, mark it as replayed and add to local list
        if (replayParams != null) {
            replayParams.putOpt("replayed", true)
            replayParams.putOpt("updatedAtMs", System.currentTimeMillis())
            replayParams.remove("updatedAtMsDiff")

            replays.add(RadarReplay(replayParams))
        }

        val replayCount = replays.size
        this.logger.d("Flushing $replayCount replays")
    }

    @JvmStatic
    internal fun hasReplays(): Boolean {
        val replayCount = replayBuffer.getSize()
        return replayCount > 0
    }

    @JvmStatic
    internal fun addReplay(replayParams: JSONObject) {
        replayBuffer.write(replayParams)
    }

    @JvmStatic
    internal fun loadReplayBufferFromSharedPreferences() {
        replayBuffer.loadFromSharedPreferences()
        val replayCount = replayBuffer.getSize()
        logger.d("Loaded replays | replayCount = $replayCount")
    }

    /**
     * Returns a display string for a location source value.
     *
     * @param[source] A location source value.
     *
     * @return A display string for the location source value.
     */
    @JvmStatic
    fun stringForSource(source: RadarLocationSource): String {
        return when (source) {
            RadarLocationSource.FOREGROUND_LOCATION -> "FOREGROUND_LOCATION"
            RadarLocationSource.BACKGROUND_LOCATION -> "BACKGROUND_LOCATION"
            RadarLocationSource.MANUAL_LOCATION -> "MANUAL_LOCATION"
            RadarLocationSource.GEOFENCE_ENTER -> "GEOFENCE_ENTER"
            RadarLocationSource.GEOFENCE_DWELL -> "GEOFENCE_DWELL"
            RadarLocationSource.GEOFENCE_EXIT -> "GEOFENCE_EXIT"
            RadarLocationSource.MOCK_LOCATION -> "MOCK_LOCATION"
            RadarLocationSource.BEACON_ENTER -> "BEACON_ENTER"
            RadarLocationSource.BEACON_EXIT -> "BEACON_EXIT"
            else -> "UNKNOWN"
        }
    }

    /**
     * Returns a display string for a travel mode value.
     *
     * @param[mode] A travel mode value.
     *
     * @return A display string for the travel mode value.
     */
    @JvmStatic
    fun stringForMode(mode: RadarRouteMode): String {
        return when (mode) {
            RadarRouteMode.FOOT -> "foot"
            RadarRouteMode.BIKE -> "bike"
            RadarRouteMode.CAR -> "car"
            RadarRouteMode.TRUCK -> "truck"
            RadarRouteMode.MOTORBIKE -> "motorbike"
            else -> "car"
        }
    }

    /**
     * Returns a display string for a verification status value.
     *
     * @param[verificationStatus] A verification status value.
     *
     * @return A display string for the address verification status value.
     */
    @JvmStatic
    fun stringForVerificationStatus(verificationStatus: RadarAddressVerificationStatus? = null): String {
        if (verificationStatus == null) {
            return "UNKNOWN"
        }
        return when (verificationStatus) {
            RadarAddressVerificationStatus.VERIFIED -> "VERIFIED"
            RadarAddressVerificationStatus.PARTIALLY_VERIFIED -> "PARTIALLY_VERIFIED"
            RadarAddressVerificationStatus.AMBIGUOUS -> "AMBIGUOUS"
            RadarAddressVerificationStatus.UNVERIFIED -> "UNVERIFIED"
            else -> "UNKNOWN"
        }
    }

    /**
     * Returns a display string for a trip status value.
     *
     * @param[status] A trip status value.
     *
     * @return A display string for the trip status value.
     */
    @JvmStatic
    fun stringForTripStatus(status: RadarTrip.RadarTripStatus): String {
        return when (status) {
            RadarTrip.RadarTripStatus.STARTED -> "started"
            RadarTrip.RadarTripStatus.APPROACHING -> "approaching"
            RadarTrip.RadarTripStatus.ARRIVED -> "arrived"
            RadarTrip.RadarTripStatus.EXPIRED -> "expired"
            RadarTrip.RadarTripStatus.COMPLETED -> "completed"
            RadarTrip.RadarTripStatus.CANCELED -> "canceled"
            else -> "unknown"
        }
    }

    /**
     * Returns a JSON object for a location.
     *
     * @param[location] A location.
     *
     * @return A JSON object for the location.
     */
    @JvmStatic
    fun jsonForLocation(location: Location): JSONObject {
        val obj = JSONObject()
        obj.put("latitude", location.latitude)
        obj.put("longitude", location.longitude)
        obj.put("accuracy", location.accuracy)
        obj.put("altitude", location.altitude)
        obj.put("speed", location.speed)
        obj.put("course", location.bearing)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            obj.put("verticalAccuracy", location.verticalAccuracyMeters)
            obj.put("speedAccuracy", location.speedAccuracyMetersPerSecond)
            obj.put("courseAccuracy", location.bearingAccuracyDegrees)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            obj.put("mocked", location.isFromMockProvider)
        }
        return obj
    }

    /**
     * Gets the version number of the Radar SDK, such as "3.5.1" or "3.5.1-beta.2".
     *
     * @return The current `sdkVersion`.
     */
    @JvmStatic
    fun sdkVersion(): String {

        return RadarUtils.sdkVersion

    }


    internal fun handleLocation(context: Context, location: Location, source: RadarLocationSource) {
        if (!initialized) {
            initialize(context)
        }

        locationManager.handleLocation(location, source)
    }

    internal fun handleBeacons(
        context: Context,
        beacons: Array<RadarBeacon>?,
        source: RadarLocationSource
    ) {
        if (!initialized) {
            initialize(context)
        }

        locationManager.handleBeacons(beacons, source)
    }

    internal fun handleBootCompleted(context: Context) {
        if (!initialized) {
            initialize(context)
        }

        locationManager.handleBootCompleted()
    }

    internal fun sendEvents(events: Array<RadarEvent>, user: RadarUser? = null) {
        if (events.isEmpty()) {
            return
        }

        receiver?.onEventsReceived(context, events, user)

        RadarNotificationHelper.showNotifications(context, events)

        for (event in events) {
            logger.i("📍 Radar event received | type = ${RadarEvent.stringForType(event.type)}; replayed = ${event.replayed}; link = https://radar.com/dashboard/events/${event._id}")
        }
    }

    internal fun sendLocation(location: Location, user: RadarUser) {
        receiver?.onLocationUpdated(context, location, user)

        logger.i("📍 Radar location updated | coordinates = (${location.latitude}, ${location.longitude}); accuracy = ${location.accuracy} meters; link = https://radar.com/dashboard/users/${user._id}")
    }

    internal fun sendClientLocation(
        location: Location,
        stopped: Boolean,
        source: RadarLocationSource
    ) {
        receiver?.onClientLocationUpdated(context, location, stopped, source)
    }

    internal fun sendError(status: RadarStatus) {
        receiver?.onError(context, status)

        logger.e("📍️ Radar error received | status = $status", RadarLogType.SDK_ERROR)
    }

    internal fun sendLog(
        level: RadarLogLevel,
        message: String,
        type: RadarLogType?,
        createdAt: Date = Date()
    ) {
        receiver?.onLog(context, message)
        logBuffer.write(level, type, message, createdAt)
    }

    internal fun setLogPersistenceFeatureFlag(enabled: Boolean) {
        this.logBuffer.setPersistentLogFeatureFlag(enabled)
    }

}
