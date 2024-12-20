package io.radar.sdk.model

import io.radar.sdk.RadarUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

/**
 * Represents a user's verified location.
 *
 * @see [](https://radar.com/documentation/fraud)
 */
class RadarVerifiedLocationToken(
    /**
     * The user.
     */
    val user: RadarUser,

    /**
     * An array of events.
     */
    val events: Array<RadarEvent>,

    /**
     * A signed JSON Web Token (JWT) containing the user and array of events. Verify the token server-side using your secret key.
     */
    val token: String,

    /**
     * The datetime when the token expires.
     */
    val expiresAt: Date,

    /**
     * The number of seconds until the token expires.
     */
    val expiresIn: Int,

    /**
     * A boolean indicating whether the user passed all jurisdiction and fraud detection checks.
     */
    val passed: Boolean,

    /**
     * An array of failure reasons for jurisdiction and fraud detection checks.
     */
    val failureReasons: Array<String>,

    /**
     * The Radar ID of the location check.
     */
    val _id: String,

    /**
     * The full JSON value of the token.
     */
    val fullJson: JSONObject,
) {
    internal companion object {
        private const val FIELD_USER = "user"
        private const val FIELD_EVENTS = "events"
        private const val FIELD_TOKEN = "token"
        private const val FIELD_EXPIRES_AT = "expiresAt"
        private const val FIELD_EXPIRES_IN = "expiresIn"
        private const val FIELD_PASSED = "passed"
        private const val FIELD_FAILURE_REASONS = "failureReasons"
        private const val FIELD_ID = "_id"

        fun fromJson(obj: JSONObject?): RadarVerifiedLocationToken? {
            if (obj == null) {
                return null
            }

            val user: RadarUser? = RadarUser.fromJson(obj.optJSONObject(FIELD_USER))
            val events: Array<RadarEvent>? = RadarEvent.fromJson(obj.optJSONArray(FIELD_EVENTS))
            val token: String? = obj.optString(FIELD_TOKEN)
            val expiresAt: Date? = RadarUtils.isoStringToDate(obj.optString(FIELD_EXPIRES_AT))
            val expiresIn: Int = obj.optInt(FIELD_EXPIRES_IN)
            val passed: Boolean = obj.optBoolean(FIELD_PASSED)
            val failureReasons = obj.optJSONArray(FIELD_FAILURE_REASONS)?.let { failureReasons ->
                Array<String>(failureReasons.length()) {
                    failureReasons.optString(it)
                }
            } ?: emptyArray()
            val id = obj.optString(FIELD_ID) ?: ""

            if (user == null || events == null || token == null || expiresAt == null) {
                return null
            }

            return RadarVerifiedLocationToken(user, events, token, expiresAt, expiresIn, passed, failureReasons, id, obj)
        }
    }

    fun toJson(): JSONObject {
        return fullJson
    }

}