package io.radar.sdk

import android.content.Context
import android.location.Location
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.model.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@Config(sdk=[Build.VERSION_CODES.P])
class RadarTest {

    companion object {
        const val LATCH_TIMEOUT = 5L

        private val context: Context = ApplicationProvider.getApplicationContext()

        private val locationClientMock = RadarMockLocationProvider()
        private val permissionsHelperMock = RadarPermissionsHelperMock()
    }

    private fun assertGeofencesOk(geofences: Array<RadarGeofence>?) {
        assertNotNull(geofences)
        geofences?.let {
            for (geofence in geofences) {
                assertGeofenceOk(geofence)
            }
        }
    }

    private fun assertGeofenceOk(geofence: RadarGeofence?) {
        assertNotNull(geofence)
        assertNotNull(geofence?.description)
        assertNotNull(geofence?.tag)
        assertNotNull(geofence?.externalId)
        assertNotNull(geofence?.metadata)
        assertNotNull(geofence?.geometry)
    }

    private fun assertChainsOk(chains: Array<RadarChain>?) {
        assertNotNull(chains)
        chains?.let {
            for (chain in chains) {
                assertChainOk(chain)
            }
        }
    }

    private fun assertChainOk(chain: RadarChain?) {
        assertNotNull(chain)
        assertNotNull(chain?.slug)
        assertNotNull(chain?.name)
        assertNotNull(chain?.externalId)
        assertNotNull(chain?.metadata)
    }

    private fun assertPlacesOk(places: Array<RadarPlace>?) {
        assertNotNull(places)
        places?.let {
            for (place in places) {
                assertPlaceOk(place)
            }
        }
    }

    private fun assertPlaceOk(place: RadarPlace?) {
        assertNotNull(place)
        assertNotNull(place?._id)
        assertNotNull(place?.categories)
        place?.categories?.let {
            assertTrue(place.categories.count() > 0)
        }
        place?.chain?.let {
            assertChainOk(place.chain)
        }
        assertNotNull(place?.location)
    }

    private fun assertRegionOk(region: RadarRegion?) {
        assertNotNull(region)
        assertNotNull(region?._id)
        assertNotNull(region?.name)
        assertNotNull(region?.code)
        assertNotNull(region?.type)
    }

    private fun assertSegmentsOk(segments: Array<RadarSegment>?) {
        assertNotNull(segments)
        segments?.let {
            for (segment in segments) {
                assertSegmentOk(segment)
            }
        }
    }

    private fun assertSegmentOk(segment: RadarSegment?) {
        assertNotNull(segment)
        assertNotNull(segment?.description)
        assertNotNull(segment?.externalId)
    }

    private fun assertTripOk(trip: RadarTrip?) {
        assertNotNull(trip)
        assertNotNull(trip?.externalId)
        assertNotNull(trip?.metadata)
        assertNotNull(trip?.destinationGeofenceTag)
        assertNotNull(trip?.destinationGeofenceExternalId)
        assertNotNull(trip?.destinationLocation)
        assertNotNull(trip?.mode)
        assertNotNull(trip?.etaDistance)
        assertNotEquals(trip?.etaDistance, 0)
        assertNotNull(trip?.etaDuration)
        assertNotEquals(trip?.etaDuration, 0)
        assertEquals(trip?.status, RadarTrip.RadarTripStatus.STARTED)
    }

    private fun assertFraudOk(fraud: RadarFraud?) {
        assertNotNull(fraud)
        assertTrue(fraud!!.proxy)
        assertTrue(fraud.mocked)
        assertTrue(fraud.compromised)
        assertTrue(fraud.jumped)
    }

    private fun assertUserOk(user: RadarUser?) {
        assertNotNull(user)
        assertNotNull(user?._id)
        assertNotNull(user?.userId)
        assertNotNull(user?.deviceId)
        assertNotNull(user?.description)
        assertNotNull(user?.metadata)
        assertNotNull(user?.location)
        assertGeofencesOk(user?.geofences)
        assertPlaceOk(user?.place)
        assertRegionOk(user?.country)
        assertRegionOk(user?.state)
        assertRegionOk(user?.dma)
        assertRegionOk(user?.postalCode)
        assertChainsOk(user?.nearbyPlaceChains)
        assertSegmentsOk(user?.segments)
        assertChainsOk(user?.topChains)
        assertNotEquals(user?.source, Radar.RadarLocationSource.UNKNOWN)
        assertTripOk(user?.trip)
        assertFraudOk(user?.fraud)
    }

    private fun assertEventsOk(events: Array<RadarEvent>?) {
        assertNotNull(events)
        events?.let {
            for (event in events) {
                assertEventOk(event)
            }
        }
    }

    private fun assertEventOk(event: RadarEvent?) {
        assertNotNull(event)
        assertNotNull(event?._id)
        assertNotNull(event?.createdAt)
        assertNotNull(event?.actualCreatedAt)
        assertNotEquals(event?.type, RadarEvent.RadarEventType.UNKNOWN)
        assertNotEquals(event?.confidence, RadarEvent.RadarEventConfidence.NONE)
        assertNotNull(event?.location)
        if (event?.type == RadarEvent.RadarEventType.USER_ENTERED_GEOFENCE) {
            assertGeofenceOk(event.geofence)
        } else if (event?.type == RadarEvent.RadarEventType.USER_EXITED_GEOFENCE) {
            assertGeofenceOk(event.geofence)
            assertTrue(event.duration > 0)
        } else if (event?.type == RadarEvent.RadarEventType.USER_ENTERED_PLACE) {
            assertPlaceOk(event.place)
        } else if (event?.type == RadarEvent.RadarEventType.USER_EXITED_PLACE) {
            assertPlaceOk(event.place)
            assertTrue(event.duration > 0)
        } else if (event?.type == RadarEvent.RadarEventType.USER_NEARBY_PLACE_CHAIN) {
            assertPlaceOk(event.place)
        } else if (event?.type == RadarEvent.RadarEventType.USER_ENTERED_REGION_COUNTRY) {
            assertRegionOk(event.region)
        } else if (event?.type == RadarEvent.RadarEventType.USER_EXITED_REGION_COUNTRY) {
            assertRegionOk(event.region)
        } else if (event?.type == RadarEvent.RadarEventType.USER_ENTERED_REGION_STATE) {
            assertRegionOk(event.region)
        } else if (event?.type == RadarEvent.RadarEventType.USER_EXITED_REGION_STATE) {
            assertRegionOk(event.region)
        } else if (event?.type == RadarEvent.RadarEventType.USER_ENTERED_REGION_DMA) {
            assertRegionOk(event.region)
        } else if (event?.type == RadarEvent.RadarEventType.USER_EXITED_REGION_DMA) {
            assertRegionOk(event.region)
        }
    }

    private fun assertAddressesOk(addresses: Array<RadarAddress>?) {
        assertNotNull(addresses)
        addresses?.let {
            for (address in addresses) {
                assertAddressOk(address)
            }
        }
    }

    private fun assertAddressOk(address: RadarAddress?) {
        assertNotNull(address)
        assertNotEquals(address?.coordinate?.latitude, 0)
        assertNotEquals(address?.coordinate?.longitude, 0)
        assertNotNull(address?.formattedAddress)
        assertNotNull(address?.country)
        assertNotNull(address?.countryCode)
        assertNotNull(address?.countryFlag)
        assertNotNull(address?.state)
        assertNotNull(address?.stateCode)
        assertNotNull(address?.postalCode)
        assertNotNull(address?.city)
        assertNotNull(address?.borough)
        assertNotNull(address?.county)
        assertNotNull(address?.neighborhood)
        assertNotNull(address?.street)
        assertNotNull(address?.number)
        assertNotEquals(address?.confidence, RadarAddress.RadarAddressConfidence.NONE)
    }

    private fun assertContextOk(context: RadarContext?) {
        assertNotNull(context)
        assertGeofencesOk(context?.geofences)
        assertPlaceOk(context?.place)
        assertRegionOk(context?.country)
        assertRegionOk(context?.state)
        assertRegionOk(context?.dma)
        assertRegionOk(context?.postalCode)
    }

    private fun assertRoutesOk(routes: RadarRoutes?) {
        assertNotNull(routes)
        assertNotNull(routes?.geodesic)
        assertNotNull(routes?.geodesic?.distance?.text)
        assertNotEquals(routes?.geodesic?.distance?.value, 0)
        assertRouteOk(routes?.foot)
        assertRouteOk(routes?.bike)
        assertRouteOk(routes?.car)
    }

    private fun assertRouteOk(route: RadarRoute?) {
        assertNotNull(route)
        assertNotNull(route?.distance?.text)
        assertNotEquals(route?.distance?.value, 0)
        assertNotNull(route?.duration?.text)
        assertNotEquals(route?.duration?.value, 0)
    }

    @Before
    fun setUp() {
        Radar.logger = RadarLogger(context)

        Radar.initialize(context)

        Radar.locationManager.locationClient = locationClientMock
        Radar.locationManager.permissionsHelper = permissionsHelperMock
    }

    @Test
    fun test_Radar_setUserId() {
        val userId = "userId"
        Radar.setUserId(userId)
        assertEquals(userId, Radar.getUserId())
    }

    @Test
    fun test_Radar_setUserId_null() {
        Radar.setUserId(null)
        assertNull(Radar.getUserId())
    }

    @Test
    fun test_Radar_setDescription() {
        val description = "description"
        Radar.setDescription(description)
        assertEquals(description, Radar.getDescription())
    }

    @Test
    fun test_Radar_setDescription_null() {
        Radar.setDescription(null)
        assertNull(Radar.getDescription())
    }

    @Test
    fun test_Radar_setMetadata() {
        val metadata = JSONObject(mapOf("foo" to "bar", "baz" to true, "qux" to 1))
        Radar.setMetadata(metadata)
        assertEquals(metadata.toString(), Radar.getMetadata()?.toString())
    }

    @Test
    fun test_Radar_setNotificationsOptions() {
        val notificationOptions = RadarNotificationOptions(
            "foo",
            "red",
            "bar",
            "blue",
            "hello",
            "white")
        Radar.setNotificationOptions(notificationOptions)
        assertEquals(notificationOptions, RadarSettings.getNotificationOptions(context))
    }


    @Test
    fun test_Radar_notificationSettingDefaults() {
        Radar.setForegroundServiceOptions(RadarTrackingOptions.RadarTrackingOptionsForegroundService(
            text = "Text",
            title = "Title",
            icon = 1337,
            updatesOnly = true,
        ))
        // Radar.setNotificationOptions has side effects on foregroundServiceOptions.
        Radar.setNotificationOptions(RadarNotificationOptions(
            "foo",
            "red",
            "bar",
            "blue",
            "hello",
            "white"))
        assertEquals("bar", RadarSettings.getForegroundService(context).iconString)
        assertEquals("blue", RadarSettings.getForegroundService(context).iconColor)
        // We do not clear existing values of iconString and iconColor with null values.
        Radar.setForegroundServiceOptions(RadarTrackingOptions.RadarTrackingOptionsForegroundService(
            text = "Text",
            title = "Title",
            icon = 1337,
            updatesOnly = true,
        ))
        assertEquals("bar", RadarSettings.getForegroundService(context).iconString)
        assertEquals("blue", RadarSettings.getForegroundService(context).iconColor)
        Radar.setForegroundServiceOptions(RadarTrackingOptions.RadarTrackingOptionsForegroundService(
            text = "Text",
            title = "Title",
            iconString = "test",
            iconColor = "red",
            icon = 1337,
            updatesOnly = true,
        ))
        assertEquals("test", RadarSettings.getForegroundService(context).iconString)
        assertEquals("red", RadarSettings.getForegroundService(context).iconColor)
    }

    @Test
    fun test_Radar_setMetadata_null() {
        Radar.setMetadata(null)
        assertNull(Radar.getMetadata())
    }

    @Test
    fun test_Radar_getLocation_errorPermissions() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        locationClientMock.mockLocation = null

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null

        Radar.getLocation { status, _, _ ->
            callbackStatus = status
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_PERMISSIONS, callbackStatus)
    }

    @Test
    fun test_Radar_getLocation_errorLocation() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        locationClientMock.mockLocation = null

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null

        Radar.getLocation { status, _, _ ->
            callbackStatus = status
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_LOCATION, callbackStatus)
    }

    @Test
    fun test_Radar_getLocation_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.78382
        mockLocation.longitude = -73.97536
        mockLocation.accuracy = 65f
        mockLocation.time = System.currentTimeMillis()
        locationClientMock.mockLocation = mockLocation

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackLocation: Location? = null


        Radar.getLocation { status, location, _ ->
            callbackStatus = status
            callbackLocation = location
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertEquals(mockLocation, callbackLocation)
    }

    @Test
    fun test_Radar_trackOnce_errorPermissions() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        locationClientMock.mockLocation = null

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null

        Radar.trackOnce { status, _, _, _ ->
            callbackStatus = status
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_PERMISSIONS, callbackStatus)
    }

    @Test
    fun test_Radar_trackOnce_errorLocation() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        locationClientMock.mockLocation = null

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null

        Radar.trackOnce { status, _, _, _ ->
            callbackStatus = status
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_LOCATION, callbackStatus)
    }

    @Test
    fun test_Radar_trackOnce_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.78382
        mockLocation.longitude = -73.97536
        mockLocation.accuracy = 65f
        mockLocation.time = System.currentTimeMillis()
        locationClientMock.mockLocation = mockLocation

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackLocation: Location? = null
        var callbackEvents: Array<RadarEvent>? = null
        var callbackUser: RadarUser? = null

        Radar.trackOnce { status, location, events, user ->
            callbackStatus = status
            callbackLocation = location
            callbackEvents = events
            callbackUser = user
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertEquals(mockLocation, callbackLocation)
        assertEventsOk(callbackEvents)
        assertUserOk(callbackUser)
    }

    @Test
    fun test_Radar_startTracking_continuous() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true

        Radar.stopTracking()

        val options = RadarTrackingOptions.CONTINUOUS
        Radar.startTracking(options)
        assertEquals(options, Radar.getTrackingOptions())
        assertTrue(Radar.isTracking())
    }

    @Test
    fun test_Radar_startTracking_responsive() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true

        Radar.stopTracking()

        val options = RadarTrackingOptions.RESPONSIVE
        Radar.startTracking(options)
        assertEquals(options, Radar.getTrackingOptions())
        assertTrue(Radar.isTracking())
    }

    @Test
    fun test_Radar_startTracking_efficient() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true

        Radar.stopTracking()

        val options = RadarTrackingOptions.EFFICIENT
        Radar.startTracking(options)
        assertEquals(options, Radar.getTrackingOptions())
        assertTrue(Radar.isTracking())
    }

    @Test
    fun test_Radar_startTracking_custom() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true

        Radar.stopTracking()

        Radar.setForegroundServiceOptions(RadarTrackingOptions.RadarTrackingOptionsForegroundService(
            text="Text",
            title = "Title",
            icon = 1337,
            iconString = "test",
            updatesOnly = true,
            iconColor = "#FF0000"
        ))

        val options = RadarTrackingOptions.EFFICIENT
        options.desiredAccuracy = RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.LOW
        val now = Date()
        options.startTrackingAfter = now
        options.stopTrackingAfter = Date(now.time + 1000)
        options.sync = RadarTrackingOptions.RadarTrackingOptionsSync.NONE
        options.syncGeofences = true
        options.syncGeofencesLimit = 100
        Radar.startTracking(options)
        assertEquals(options, Radar.getTrackingOptions())
        assertTrue(Radar.isTracking())
    }

    @Test
    fun test_Radar_startTracking_custom_enum_int() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true

        Radar.stopTracking()

        val options = RadarTrackingOptions.CONTINUOUS
        options.desiredAccuracy = RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.LOW
        options.replay = RadarTrackingOptions.RadarTrackingOptionsReplay.STOPS
        options.sync = RadarTrackingOptions.RadarTrackingOptionsSync.NONE
        val json = options.toJson()
        json.put("desiredAccuracy", 1)
        json.put("replay", 1)
        json.put("sync", 0)
        val newOptions = RadarTrackingOptions.fromJson(json)
        Radar.startTracking(newOptions)
        assertEquals(newOptions, Radar.getTrackingOptions())
        assertEquals(newOptions, options)
        assertTrue(Radar.isTracking())
    }

    @Test
    fun test_Radar_startTracking_custom_enum_string() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true

        Radar.stopTracking()

        val options = RadarTrackingOptions.CONTINUOUS
        options.desiredAccuracy = RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.LOW
        options.replay = RadarTrackingOptions.RadarTrackingOptionsReplay.STOPS
        options.sync = RadarTrackingOptions.RadarTrackingOptionsSync.NONE
        val json = options.toJson()
        json.put("desiredAccuracy", "low")
        json.put("replay", "stops")
        json.put("sync", "none")
        val newOptions = RadarTrackingOptions.fromJson(json)
        Radar.startTracking(newOptions)
        assertEquals(newOptions, Radar.getTrackingOptions())
        assertEquals(newOptions, options)
        assertTrue(Radar.isTracking())
    }

    @Test
    fun test_Radar_stopTracking() {
        Radar.stopTracking()
        assertFalse(Radar.isTracking())
    }

    @Test
    fun test_Radar_startTrip_scheduledArrivalAt() {
        val tripOptions = getTestTripOptions()
        assertNull(tripOptions.scheduledArrivalAt)
        val tripOptionsJson = tripOptions.toJson()
        assertFalse(tripOptionsJson.has("scheduledArrivalAt"))

        val newScheduledArrivalAt = Date()
        tripOptions.scheduledArrivalAt = newScheduledArrivalAt
        val newTripOptionsJson = tripOptions.toJson()
        assertEquals(newTripOptionsJson.getString("scheduledArrivalAt"), RadarUtils.dateToISOString(tripOptions.scheduledArrivalAt))
    }


    @Test
    fun test_Radar_logConversionWithBlock_success() {

        val conversionType = "test_event" // has to match the property in the conversion_event.json file!
        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackEvent: RadarEvent? = null
        val metadata = JSONObject()
        metadata.put("foo", "bar")

        Radar.logConversion(conversionType, metadata) { status, event ->
            callbackStatus = status
            callbackEvent = event

            val conversionMetadata = event?.metadata
            assertNotNull(conversionMetadata)
            assertEquals("bar", conversionMetadata!!.get("foo"))

            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertConversionEvent(callbackEvent, conversionType, metadata)
    }

    @Test
    fun test_Radar_logConversionWithRevenueAndBlock_success() {

        val conversionType = "test_event" // has to match the property in the conversion_event.json file!
        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackEvent: RadarEvent? = null
        val revenue = 0.2
        val metadata = JSONObject()
        metadata.put("foo", "bar")

        Radar.logConversion(conversionType, revenue, metadata) { status, event ->
            callbackStatus = status
            callbackEvent = event

            val conversionMetadata = event?.metadata
            assertNotNull(conversionMetadata)
            assertEquals("bar", conversionMetadata!!.get("foo"))
            assertEquals(revenue, conversionMetadata!!.get("revenue"))

            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertConversionEvent(callbackEvent, conversionType, metadata)
    }

    private fun assertConversionEvent(
        event: RadarEvent?,
        conversionType: String,
        metadata: JSONObject
    ) {
        assertNotNull(event)
        assertEquals(conversionType, event!!.conversionName)
        assertNotNull(event!!.conversionName)

        val returnedMetadata = event!!.metadata
        assertNotNull(returnedMetadata)
        assertEquals(metadata.get("foo"), returnedMetadata!!["foo"])
    }

    private fun getTestTripOptions(): RadarTripOptions {
        val tripOptions = RadarTripOptions("tripExternalId")
        tripOptions.metadata = JSONObject(mapOf("foo" to "bar", "baz" to true, "qux" to 1))
        tripOptions.destinationGeofenceTag = "tripDestinationGeofenceTag"
        tripOptions.destinationGeofenceExternalId = "tripDestinationGeofenceExternalId"
        tripOptions.mode = Radar.RadarRouteMode.FOOT

        return tripOptions
    }
}
