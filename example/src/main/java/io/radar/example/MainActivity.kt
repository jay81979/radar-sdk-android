package io.radar.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.radar.sdk.Radar
import io.radar.sdk.RadarTrackingOptions
import io.radar.sdk.RadarTripOptions
import org.json.JSONObject
import java.util.EnumSet
import androidx.core.content.edit
import io.radar.sdk.model.RadarAddress
import io.radar.sdk.model.RadarCoordinate
import java.util.*

class MainActivity : AppCompatActivity() {

    val demoFunctions: ArrayList<() -> Unit> = ArrayList()
    private lateinit var listView: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val receiver = MyRadarReceiver()
        Radar.initialize(this, receiver, Radar.RadarLocationServicesProvider.GOOGLE, true)
        Radar.sdkVersion().let { Log.i("version", it) }

        listView = findViewById(R.id.buttonList)
        createButtons()
    }

    private fun requestForegroundPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            Log.v("example", "Foreground location permission already granted")
        }
    }

    private fun requestBackgroundPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 2)
        } else {
            Log.v("example", "Background location permission already granted")
        }
    }

     private fun requestActivityRecognitionPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 3)
        } else {
            Log.v("example", "Activity recognition permission already granted")
        }
    }

    fun createButton(text: String, function: () -> Unit) {
        val button = Button(this);
        button.text = text
        button.isAllCaps = false
        button.tag = demoFunctions.size
        button.setOnClickListener {
            function()
        }

        demoFunctions.add(function)
        listView.addView(button)
    }

    fun createButtons() {

        createButton("requestForegroundPermission") {
            requestForegroundPermission()
        }

        createButton("requestBackgroundPermission") {
            requestBackgroundPermission()
        }

        createButton("requestActivityRecognitionPermission") {
            requestActivityRecognitionPermission()
        }

        createButton("getLocation") {
            Radar.getLocation { status, location, stopped ->
                Log.v(
                    "example",
                    "Location: status = ${status}; location = $location; stopped = $stopped"
                )
            }
        }

        createButton("trackOnce") {
            Radar.trackOnce { status, location, events, user ->
                Log.v(
                    "example",
                    "Track once: status = ${status}; location = $location; events = $events; user = $user"
                )
            }
        }

        createButton("startTracking") {
            val options = RadarTrackingOptions.RESPONSIVE
            Radar.startTracking(options)
        }

        createButton("stopTracking") {
            Radar.stopTracking()
        }

        createButton("logConversion") {
            val conversionMetadata = JSONObject()
            conversionMetadata.put("one", "two")

            Radar.logConversion(
                "app_open_android",
                conversionMetadata
            ) { status, event ->
                Log.v(
                    "example",
                    "Conversion name = ${event?.conversionName}: status = $status; event = $event"
                )
            }
        }

        createButton("run all demo") {
            for (function in demoFunctions) {
                function()
            }
        }
        demoFunctions.removeAt(demoFunctions.size - 1)
    }
}
