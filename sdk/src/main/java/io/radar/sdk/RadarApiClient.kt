package io.radar.sdk

import android.content.Context
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import io.radar.sdk.model.RadarEvent.RadarEventVerification
import io.radar.sdk.Radar.RadarLocationSource
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.model.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URL
import java.util.*

internal class RadarApiClient(
    private val context: Context,
    private var logger: RadarLogger,
    internal var apiHelper: RadarApiHelper = RadarApiHelper(logger)
) {

    interface RadarTrackApiCallback {
        fun onComplete(
            status: RadarStatus,
            res: JSONObject? = null,
            events: Array<RadarEvent>? = null,
            user: RadarUser? = null,
            nearbyGeofences: Array<RadarGeofence>? = null,
            config: RadarConfig? = null
        )
    }

    interface RadarGetConfigApiCallback {
        fun onComplete(config: RadarConfig)
    }

    interface RadarTripApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, trip: RadarTrip? = null, events: Array<RadarEvent>? = null)
    }

    interface RadarContextApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, context: RadarContext? = null)
    }

    interface RadarSearchPlacesApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, places: Array<RadarPlace>? = null)
    }

    interface RadarSearchGeofencesApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, geofences: Array<RadarGeofence>? = null)
    }

    interface RadarSearchBeaconsApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, beacons: Array<RadarBeacon>? = null, uuids: Array<String>? = null, uids: Array<String>? = null)
    }

    interface RadarGeocodeApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, addresses: Array<RadarAddress>? = null)
    }

    interface RadarIpGeocodeApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, address: RadarAddress? = null, proxy: Boolean = false)
    }

    interface RadarDistanceApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, routes: RadarRoutes? = null)
    }

    interface RadarMatrixApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, matrix: RadarRouteMatrix? = null)
    }

    interface RadarSendEventApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, events: Array<RadarEvent>? = null)
    }

    internal interface RadarLogCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null)
    }

    private fun headers(publishableKey: String): Map<String, String> {
        return mapOf(
            "Authorization" to publishableKey,
            "Content-Type" to "application/json",
            "X-Radar-Config" to "true",
            "X-Radar-Device-Make" to RadarUtils.deviceMake,
            "X-Radar-Device-Model" to RadarUtils.deviceModel,
            "X-Radar-Device-OS" to RadarUtils.deviceOS,
            "X-Radar-Device-Type" to RadarUtils.deviceType,
            "X-Radar-SDK-Version" to RadarUtils.sdkVersion
        )
    }

    internal fun getConfig(callback: RadarGetConfigApiCallback? = null) {
        val publishableKey = RadarSettings.getPublishableKey(context) ?: return

        val queryParams = StringBuilder()
        queryParams.append("installId=${RadarSettings.getInstallId(context)}")
        queryParams.append("&sessionId=${RadarSettings.getSessionId(context)}")
        queryParams.append("&locationAuthorization=${RadarUtils.getLocationAuthorization(context)}")
        queryParams.append("&locationAccuracyAuthorization=${RadarUtils.getLocationAccuracyAuthorization(context)}")

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/config?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", url, headers, null, false, object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status == RadarStatus.SUCCESS) {
                    Radar.flushLogs()
                }
                callback?.onComplete(RadarConfig.fromJson(res))
            }
        })
    }

    internal fun log(logs: List<RadarLog>, callback: RadarLogCallback?) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback?.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)
            return
        }
        val params = JSONObject()
        try {
            params.putOpt("id", RadarSettings.getId(context))
            params.putOpt("deviceId", RadarUtils.getDeviceId(context))
            params.putOpt("installId", RadarSettings.getInstallId(context))
            params.putOpt("sessionId", RadarSettings.getSessionId(context))
            val array = JSONArray()
            logs.forEach { log -> array.put(log.toJson()) }
            params.putOpt("logs", array)
        } catch (e: JSONException) {
            callback?.onComplete(RadarStatus.ERROR_BAD_REQUEST)
            return
        }
        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/logs")
            .build()
        apiHelper.request(
            context = context,
            method = "POST",
            url = URL(uri.toString()),
            headers = headers(publishableKey),
            params = params,
            sleep = false,
            callback = object : RadarApiHelper.RadarApiCallback {
                override fun onComplete(status: RadarStatus, res: JSONObject?) {
                    callback?.onComplete(status, res)
                }
            },
            stream = true,
            // Do not log the saved log events. If the logs themselves were logged it would create a redundancy and
            // eventually lead to a crash when creating a downstream log request, since these will log to memory as a
            // single log entry. Then each time after, this log entry would contain more and more logs, eventually
            // causing an out of memory exception.
            logPayload = false
        )
    }

    internal fun track(location: Location, stopped: Boolean, foreground: Boolean, source: RadarLocationSource, replayed: Boolean, beacons: Array<RadarBeacon>?, callback: RadarTrackApiCallback? = null) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback?.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val params = JSONObject()
        val options = Radar.getTrackingOptions()
        val tripOptions = RadarSettings.getTripOptions(context)
        val anonymous = RadarSettings.getAnonymousTrackingEnabled(context)
        try {
            params.putOpt("anonymous", anonymous)
            if (anonymous) {
                params.putOpt("geofenceIds", RadarState.getGeofenceIds(context)?.toTypedArray())
                params.putOpt("placeId", RadarState.getPlaceId(context))
                params.putOpt("regionIds", RadarState.getRegionIds(context)?.toTypedArray())
                params.putOpt("beaconIds", RadarState.getBeaconIds(context)?.toTypedArray())
            } else {
                params.putOpt("id", RadarSettings.getId(context))
                params.putOpt("installId", RadarSettings.getInstallId(context))
                params.putOpt("userId", RadarSettings.getUserId(context))
                params.putOpt("deviceId", RadarUtils.getDeviceId(context))
                params.putOpt("description", RadarSettings.getDescription(context))
                params.putOpt("metadata", RadarSettings.getMetadata(context))
                if (RadarSettings.getAdIdEnabled(context)) {
                    params.putOpt("adId", RadarUtils.getAdId(context))
                }
                params.putOpt("sessionId", RadarSettings.getSessionId(context))
            }
            params.putOpt("latitude", location.latitude)
            params.putOpt("longitude", location.longitude)
            var accuracy = location.accuracy
            if (!location.hasAccuracy() || location.accuracy.isNaN() || accuracy <= 0) {
                accuracy = 1F
            }
            params.putOpt("accuracy", accuracy)
            if (location.hasSpeed() && !location.speed.isNaN()) {
                params.putOpt("speed", location.speed)
            }
            if (location.hasBearing() && !location.bearing.isNaN()) {
                params.putOpt("course", location.bearing)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (location.hasVerticalAccuracy() && !location.verticalAccuracyMeters.isNaN()) {
                    params.putOpt("verticalAccuracy", location.verticalAccuracyMeters)
                }
                if (location.hasSpeedAccuracy() && !location.speedAccuracyMetersPerSecond.isNaN()) {
                    params.putOpt("speedAccuracy", location.speedAccuracyMetersPerSecond)
                }
                if (location.hasBearingAccuracy() && !location.bearingAccuracyDegrees.isNaN()) {
                    params.putOpt("courseAccuracy", location.bearingAccuracyDegrees)
                }
            }
            if (!foreground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                val updatedAtMsDiff = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1000000
                params.putOpt("updatedAtMsDiff", updatedAtMsDiff)
            }
            params.putOpt("foreground", foreground)
            params.putOpt("stopped", stopped)
            params.putOpt("replayed", replayed)
            params.putOpt("deviceType", "Android")
            params.putOpt("deviceMake", RadarUtils.deviceMake)
            params.putOpt("sdkVersion", RadarUtils.sdkVersion)
            params.putOpt("deviceModel", RadarUtils.deviceModel)
            params.putOpt("deviceOS", RadarUtils.deviceOS)
            params.putOpt("deviceType", RadarUtils.deviceType)
            params.putOpt("deviceMake", RadarUtils.deviceMake)
            params.putOpt("country", RadarUtils.country)
            params.putOpt("timeZoneOffset", RadarUtils.timeZoneOffset)
            params.putOpt("source", Radar.stringForSource(source))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                val mocked = location.isFromMockProvider
                params.putOpt("mocked", mocked)
            }
            if (tripOptions != null) {
                val tripOptionsObj = JSONObject()
                tripOptionsObj.putOpt("externalId", tripOptions.externalId)
                tripOptionsObj.putOpt("metadata", tripOptions.metadata)
                tripOptionsObj.putOpt("destinationGeofenceTag", tripOptions.destinationGeofenceTag)
                tripOptionsObj.putOpt("destinationGeofenceExternalId", tripOptions.destinationGeofenceExternalId)
                tripOptionsObj.putOpt("mode", Radar.stringForMode(tripOptions.mode))
                params.putOpt("tripOptions", tripOptionsObj)
            }
            if (options.syncGeofences) {
                params.putOpt("nearbyGeofences", true)
                params.putOpt("nearbyGeofencesLimit", options.syncGeofencesLimit)
            }
            if (beacons != null) {
                params.putOpt("beacons", RadarBeacon.toJson(beacons))
            }
            params.putOpt("locationAuthorization", RadarUtils.getLocationAuthorization(context))
            params.putOpt("locationAccuracyAuthorization", RadarUtils.getLocationAccuracyAuthorization(context))
            params.putOpt("trackingOptions", Radar.getTrackingOptions().toJson())
            val usingRemoteTrackingOptions = RadarSettings.getTracking(context) && RadarSettings.getRemoteTrackingOptions(context) != null
            params.putOpt("usingRemoteTrackingOptions", usingRemoteTrackingOptions)
            params.putOpt("locationServicesProvider", RadarSettings.getLocationServicesProvider(context))
            params.putOpt("rooted", RadarUtils.isRooted())
        } catch (e: JSONException) {
            callback?.onComplete(RadarStatus.ERROR_BAD_REQUEST)

            return
        }

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/track")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "POST", url, headers, params, true, object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    if (options.replay == RadarTrackingOptions.RadarTrackingOptionsReplay.STOPS && stopped && !(source == RadarLocationSource.FOREGROUND_LOCATION || source == RadarLocationSource.BACKGROUND_LOCATION)) {
                        RadarState.setLastFailedStoppedLocation(context, location)
                    }

                    Radar.sendError(status)

                    callback?.onComplete(status)

                    return
                }
                Radar.flushLogs()

                RadarState.setLastFailedStoppedLocation(context, null)

                val config = RadarConfig.fromJson(res)

                val events = res.optJSONArray("events")?.let { eventsArr ->
                    RadarEvent.fromJson(eventsArr)
                }
                val user = res.optJSONObject("user")?.let { userObj ->
                    RadarUser.fromJson(userObj)
                }
                val nearbyGeofences = res.optJSONArray("nearbyGeofences")?.let { nearbyGeofencesArr ->
                    RadarGeofence.fromJson(nearbyGeofencesArr)
                }

                if (user != null) {
                    val inGeofences = user.geofences != null && user.geofences.isNotEmpty()
                    val atPlace = user.place != null
                    val canExit = inGeofences || atPlace
                    RadarState.setCanExit(context, canExit)

                    val geofenceIds = mutableSetOf<String>()
                    user.geofences?.forEach { geofence -> geofenceIds.add(geofence._id) }
                    RadarState.setGeofenceIds(context, geofenceIds)

                    val placeId = user.place?._id
                    RadarState.setPlaceId(context, placeId)

                    val regionIds = mutableSetOf<String>()
                    user.country?.let { country -> regionIds.add(country._id) }
                    user.state?.let { state -> regionIds.add(state._id) }
                    user.dma?.let { dma -> regionIds.add(dma._id) }
                    user.postalCode?.let { postalCode -> regionIds.add(postalCode._id) }
                    RadarState.setRegionIds(context, regionIds)

                    val beaconIds = mutableSetOf<String>()
                    user.beacons?.forEach { beacon -> beacon._id?.let { _id -> beaconIds.add(_id) } }
                    RadarState.setBeaconIds(context, beaconIds)
                }

                if (events != null && user != null) {
                    RadarSettings.setId(context, user._id)

                    if (user.trip == null) {
                        RadarSettings.setTripOptions(context, null)
                    }

                    Radar.sendLocation(location, user)

                    if (events.isNotEmpty()) {
                        Radar.sendEvents(events, user)
                    }

                    callback?.onComplete(RadarStatus.SUCCESS, res, events, user, nearbyGeofences, config)

                    return
                }

                Radar.sendError(status)

                callback?.onComplete(RadarStatus.ERROR_SERVER)
            }
        })
    }

    internal fun verifyEvent(eventId: String, verification: RadarEventVerification, verifiedPlaceId: String? = null) {
        val publishableKey = RadarSettings.getPublishableKey(context) ?: return

        val params = JSONObject()
        params.putOpt("verification", verification)
        params.putOpt("verifiedPlaceId", verifiedPlaceId)

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/events/")
            .appendEncodedPath(eventId)
            .appendEncodedPath("/verification")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "PUT", url, headers, params, false)
    }

    internal fun updateTrip(options: RadarTripOptions?, status: RadarTrip.RadarTripStatus?, callback: RadarTripApiCallback?) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback?.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val externalId = options?.externalId
        if (externalId == null) {
            callback?.onComplete(RadarStatus.ERROR_BAD_REQUEST)

            return
        }

        val params = JSONObject()
        if (status != null && status != RadarTrip.RadarTripStatus.UNKNOWN) {
            params.putOpt("status", Radar.stringForTripStatus(status))
        }
        if (options.metadata != null) {
            params.putOpt("metadata", options.metadata)
        }
        if (options.destinationGeofenceTag != null) {
            params.putOpt("destinationGeofenceTag", options.destinationGeofenceTag)
        }
        if (options.destinationGeofenceExternalId != null) {
            params.putOpt("destinationGeofenceExternalId", options.destinationGeofenceExternalId)
        }
        params.putOpt("mode", Radar.stringForMode(options.mode))
        params.putOpt("scheduledArrivalAt", RadarUtils.dateToISOString(options.scheduledArrivalAt))
        if (options.approachingThreshold > 0) {
            params.put("approachingThreshold", options.approachingThreshold)
        }

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/trips/")
            .appendEncodedPath(externalId)
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "PATCH", url, headers, params, false, object: RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback?.onComplete(status)

                    return
                }

                val trip = res.optJSONObject("trip")?.let { tripObj ->
                    RadarTrip.fromJson(tripObj)
                }
                val events = res.optJSONArray("events")?.let { eventsArr ->
                    RadarEvent.fromJson(eventsArr)
                }

                if (events != null && events.isNotEmpty()) {
                    Radar.sendEvents(events)
                }

                callback?.onComplete(RadarStatus.SUCCESS, res, trip, events)
            }
        })
    }

    internal fun getContext(
        location: Location,
        callback: RadarContextApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("coordinates=${location.latitude},${location.longitude}")

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/context?${queryParams}")
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", url, headers, null, false, object: RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val context = res.optJSONObject("context")?.let { contextObj ->
                    RadarContext.fromJson(contextObj)
                }
                if (context != null) {
                    callback.onComplete(RadarStatus.SUCCESS, res, context)

                    return
                }

                callback.onComplete(RadarStatus.ERROR_SERVER)
            }
        })

    }

    internal fun searchPlaces(
        location: Location,
        radius: Int,
        chains: Array<String>?,
        chainMetadata: Map<String, String>?,
        categories: Array<String>?,
        groups: Array<String>?,
        limit: Int?,
        callback: RadarSearchPlacesApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("near=${location.latitude},${location.longitude}")
        queryParams.append("&radius=${radius}")
        queryParams.append("&limit=${limit}")
        if (chains?.isNotEmpty() == true) {
            queryParams.append("&chains=${chains.joinToString(separator = ",")}")
        }
        if (categories?.isNotEmpty() == true) {
            queryParams.append("&categories=${categories.joinToString(separator = ",")}")
        }
        if (groups?.isNotEmpty() == true) {
            queryParams.append("&groups=${groups.joinToString(separator = ",")}")
        }

        chainMetadata?.entries?.forEach {
            queryParams.append("&chainMetadata[${it.key}]=\"${it.value}\"");
        }

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/search/places?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", url, headers, null, false, object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val places = res.optJSONArray("places")?.let { placesArr ->
                    RadarPlace.fromJson(placesArr)
                }
                if (places != null) {
                    callback.onComplete(RadarStatus.SUCCESS, res, places)

                    return
                }

                callback.onComplete(RadarStatus.ERROR_SERVER)
            }
        })
    }

    internal fun searchGeofences(
        location: Location,
        radius: Int,
        tags: Array<String>?,
        metadata: JSONObject?,
        limit: Int?,
        callback: RadarSearchGeofencesApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("near=${location.latitude},${location.longitude}")
        queryParams.append("&radius=${radius}")
        queryParams.append("&limit=${limit}")
        if (tags?.isNotEmpty() == true) {
            queryParams.append("&tags=${tags.joinToString(separator = ",")}")
        }
        metadata?.keys()?.forEach { key ->
            val value = metadata.get(key)
            queryParams.append("&metadata[${key}]=${value}")
        }

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/search/geofences?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", url, headers, null, false, object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val geofences = res.optJSONArray("geofences")?.let { geofencesArr ->
                    RadarGeofence.fromJson(geofencesArr)
                }
                if (geofences != null) {
                    callback.onComplete(RadarStatus.SUCCESS, res, geofences)

                    return
                }

                callback.onComplete(RadarStatus.ERROR_SERVER)
            }
        })
    }

    internal fun searchBeacons(
        location: Location,
        radius: Int,
        limit: Int?,
        callback: RadarSearchBeaconsApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("near=${location.latitude},${location.longitude}")
        queryParams.append("&radius=${radius}")
        queryParams.append("&limit=${limit}")

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/search/beacons?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", url, headers, null, false, object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val beacons = res.optJSONArray("beacons")?.let { beaconsArr ->
                    RadarBeacon.fromJson(beaconsArr)
                }

                val uuids = res.optJSONObject("meta")?.optJSONObject("settings")?.optJSONObject("beacons")?.optJSONArray("uuids")?.let { uuids ->
                    Array(uuids.length()) { index ->
                        uuids.getString(index)
                    }.filter { uuid -> uuid.isNotEmpty() }.toTypedArray()
                }

                val uids = res.optJSONObject("meta")?.optJSONObject("settings")?.optJSONObject("beacons")?.optJSONArray("uids")?.let { uids ->
                    Array(uids.length()) { index ->
                        uids.getString(index)
                    }.filter { uid -> uid.isNotEmpty() }.toTypedArray()
                }

                callback.onComplete(RadarStatus.SUCCESS, res, beacons, uuids, uids)
            }
        })
    }

    internal fun autocomplete(
        query: String,
        near: Location? = null,
        layers: Array<String>? = null,
        limit: Int? = null,
        country: String? = null,
        callback: RadarGeocodeApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("query=${query}")
        if (near != null) {
            queryParams.append("&near=${near.latitude},${near.longitude}")
        }
        if (layers?.isNotEmpty() == true) {
            queryParams.append("&layers=${layers.joinToString(separator = ",")}")
        }
        queryParams.append("&limit=${limit}")
        if (country != null) {
            queryParams.append("&country=${country}")
        }

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/search/autocomplete?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", url, headers, null, false, object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val addresses = res.optJSONArray("addresses")?.let { addressesArr ->
                    RadarAddress.fromJson(addressesArr)
                }
                if (addresses != null) {
                    callback.onComplete(RadarStatus.SUCCESS, res, addresses)

                    return
                }

                callback.onComplete(RadarStatus.ERROR_SERVER)
            }
        })
    }

    internal fun geocode(
        query: String,
        callback: RadarGeocodeApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("query=${query}")

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/geocode/forward?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", url, headers, null, false, object: RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val addresses = res.optJSONArray("addresses")?.let { addressesArr ->
                    RadarAddress.fromJson(addressesArr)
                }
                if (addresses != null) {
                    callback.onComplete(RadarStatus.SUCCESS, res, addresses)

                    return
                }

                callback.onComplete(RadarStatus.ERROR_SERVER)
            }
        })
    }

    internal fun reverseGeocode(
        location: Location,
        callback: RadarGeocodeApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("coordinates=${location.latitude},${location.longitude}")

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/geocode/reverse?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", url, headers, null, false, object: RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val addresses = res.optJSONArray("addresses")?.let { addressesArr ->
                    RadarAddress.fromJson(addressesArr)
                }
                if (addresses != null) {
                    callback.onComplete(RadarStatus.SUCCESS, res, addresses)

                    return
                }

                callback.onComplete(RadarStatus.ERROR_SERVER)
            }
        })
    }

    internal fun ipGeocode(
        callback: RadarIpGeocodeApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/geocode/ip")
            .build()

        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", url, headers, null, false, object: RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val address: RadarAddress? = res.optJSONObject("address")?.let { addressObj ->
                    RadarAddress.fromJson(addressObj)
                }
                val proxy = res.optBoolean("proxy")

                if (address != null) {
                    callback.onComplete(RadarStatus.SUCCESS, res, address, proxy)

                    return
                }

                callback.onComplete(RadarStatus.ERROR_SERVER)
            }
        })
    }

    internal fun getDistance(
        origin: Location,
        destination: Location,
        modes: EnumSet<Radar.RadarRouteMode>,
        units: Radar.RadarRouteUnits,
        geometryPoints: Int,
        callback: RadarDistanceApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("origin=${origin.latitude},${origin.longitude}")
        queryParams.append("&destination=${destination.latitude},${destination.longitude}")
        val modesList = mutableListOf<String>()
        if (modes.contains(Radar.RadarRouteMode.FOOT)) {
            modesList.add("foot")
        }
        if (modes.contains(Radar.RadarRouteMode.BIKE)) {
            modesList.add("bike")
        }
        if (modes.contains(Radar.RadarRouteMode.CAR)) {
            modesList.add("car")
        }
        if (modes.contains(Radar.RadarRouteMode.TRUCK)) {
            modesList.add("truck")
        }
        if (modes.contains(Radar.RadarRouteMode.MOTORBIKE)) {
            modesList.add("motorbike")
        }
        queryParams.append("&modes=${modesList.joinToString(",")}")
        if (units == Radar.RadarRouteUnits.METRIC) {
            queryParams.append("&units=metric")
        } else {
            queryParams.append("&units=imperial")
        }
        if (geometryPoints > 1) {
            queryParams.append("&geometryPoints=${geometryPoints}")
        }
        queryParams.append("&geometry=linestring")

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/route/distance?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", url, headers, null, false, object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val routes = res.optJSONObject("routes")?.let { routesObj ->
                    RadarRoutes.fromJson(routesObj)
                }
                if (routes != null) {
                    callback.onComplete(RadarStatus.SUCCESS, res, routes)

                    return
                }

                callback.onComplete(RadarStatus.ERROR_SERVER)
            }
        })
    }

    internal fun getMatrix(
        origins: Array<Location>,
        destinations: Array<Location>,
        mode: Radar.RadarRouteMode,
        units: Radar.RadarRouteUnits,
        callback: RadarMatrixApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("origins=")
        for (i in origins.indices) {
            queryParams.append("${origins[i].latitude},${origins[i].longitude}")
            if (i < origins.size - 1) {
                queryParams.append("|")
            }
        }
        queryParams.append("&destinations=")
        for (i in destinations.indices) {
            queryParams.append("${destinations[i].latitude},${destinations[i].longitude}")
            if (i < destinations.size - 1) {
                queryParams.append("|")
            }
        }
        if (mode == Radar.RadarRouteMode.FOOT) {
            queryParams.append("&mode=foot")
        } else if (mode == Radar.RadarRouteMode.BIKE) {
            queryParams.append("&mode=bike")
        } else if (mode == Radar.RadarRouteMode.CAR) {
            queryParams.append("&mode=car")
        } else if (mode == Radar.RadarRouteMode.TRUCK) {
            queryParams.append("&mode=truck")
        } else if (mode == Radar.RadarRouteMode.MOTORBIKE) {
            queryParams.append("&mode=motorbike")
        }
        if (units == Radar.RadarRouteUnits.METRIC) {
            queryParams.append("&units=metric")
        } else {
            queryParams.append("&units=imperial")
        }

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/route/matrix?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", url, headers, null, false, object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val matrix = res.optJSONArray("matrix")?.let { matrixObj ->
                    RadarRouteMatrix.fromJson(matrixObj)
                }
                if (matrix != null) {
                    callback.onComplete(RadarStatus.SUCCESS, res, matrix)

                    return
                }

                callback.onComplete(RadarStatus.ERROR_SERVER)
            }
        })
    }

    internal fun sendEvent(
        name: String,
        metadata: JSONObject?,
        user: RadarUser?,
        trackingEvents: Array<RadarEvent>?,
        callback: RadarSendEventApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val params = JSONObject()
        try {
            params.putOpt("id", RadarSettings.getId(context))
            params.putOpt("installId", RadarSettings.getInstallId(context))
            params.putOpt("userId", RadarSettings.getUserId(context))
            params.putOpt("deviceId", RadarUtils.getDeviceId(context))

            params.putOpt("type", name)
            params.putOpt("metadata", metadata)

        } catch (e: JSONException) {
            callback?.onComplete(RadarStatus.ERROR_BAD_REQUEST)

            return
        }

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon().appendEncodedPath("v1/events").build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)
        apiHelper.request(context, "POST", url, headers, params, false, object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val customEvent = res.optJSONObject("event")?.let { eventObj ->
                    RadarEvent.fromJson(eventObj)
                }

                val allEvents: MutableList<RadarEvent> = mutableListOf()

                if (trackingEvents != null) {
                    allEvents.addAll(trackingEvents)
                }

                if (customEvent != null) {
                    allEvents.add(0, customEvent)
                } else {
                    callback.onComplete(RadarStatus.ERROR_SERVER)
                }

                callback.onComplete(RadarStatus.SUCCESS, res, allEvents.toTypedArray())
            }
        })
    }

}
