package io.radar.sdk

import android.content.Context
import android.location.Location
import io.radar.sdk.model.RadarCircleGeometry
import io.radar.sdk.model.RadarConfig
import io.radar.sdk.model.RadarCoordinate
import io.radar.sdk.model.RadarPolygonGeometry
import org.json.JSONObject

class RadarOfflineManager {
    interface RadarOfflineCallback {
        fun onComplete(config: RadarConfig?)
    }
    internal fun contextualizeLocation(context: Context, location: Location, callback: RadarOfflineCallback) {
        var newGeofenceIds = mutableSetOf<String>()
        var newGeofenceTags = mutableSetOf<String>()
        val nearbyGeofences = RadarState.getNearbyGeofences(context)
        if (nearbyGeofences == null) {
            callback.onComplete(null)
            return
        }
        for (geofence in nearbyGeofences) {
            var center: RadarCoordinate? = null
            var radius = 100.0
            if (geofence.geometry is RadarCircleGeometry) {
                center = geofence.geometry.center
                radius = geofence.geometry.radius
            } else if (geofence.geometry is RadarPolygonGeometry) {
                center = geofence.geometry.center
                radius = geofence.geometry.radius
            } else {
                Radar.logger.e("Unsupported geofence geometry type")
                continue
            }
            if (isPointInsideCircle(center, radius, location)) {
                newGeofenceIds.add(geofence._id)
                if (geofence.tag != null) {
                    newGeofenceTags.add(geofence.tag)
                }
            }
        }
        RadarState.setGeofenceIds(context,newGeofenceIds)
        val sdkConfiguration = RadarSettings.getSdkConfiguration(context)
        val rampUpGeofenceTags = sdkConfiguration.inGeofenceTrackingOptionsTags
        var isRampedUp = false
        if (!rampUpGeofenceTags.isNullOrEmpty()) {
            for (tag in rampUpGeofenceTags) {
                if (newGeofenceTags.contains(tag)) {
                    isRampedUp = true
                    break
                }
            }
        }
        var newTrackingOptions: RadarTrackingOptions? = null
        if (isRampedUp) {
            // ramp up
            newTrackingOptions = sdkConfiguration.inGeofenceTrackingOptions
        } else {
            val tripOptions = RadarSettings.getTripOptions(context)
            val onTripTrackingOptions = sdkConfiguration.onTripTrackingOptions
            newTrackingOptions = if (tripOptions != null && onTripTrackingOptions != null){
                onTripTrackingOptions
            } else {
                sdkConfiguration.defaultTrackingOptions
            }
        }
        if (newTrackingOptions != null) {
            val metaDict = JSONObject()
            metaDict.put("trackingOptions", newTrackingOptions.toJson())
            val configDict = JSONObject()
            configDict.put("meta", metaDict)
            callback.onComplete(RadarConfig.fromJson(configDict))
            return 
        }
        callback.onComplete(null)
        return
    }

    private fun isPointInsideCircle(center: RadarCoordinate, radius: Double, point: Location): Boolean {
        val distance = Math.sqrt(Math.pow(center.latitude - point.latitude, 2.0) + Math.pow(center.longitude - point.longitude, 2.0))
        return distance <= radius
    }

}