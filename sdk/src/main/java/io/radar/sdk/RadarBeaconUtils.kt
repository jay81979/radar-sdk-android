package io.radar.sdk

import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.os.Build
import android.os.ParcelUuid
import androidx.annotation.RequiresApi
import io.radar.sdk.model.RadarBeacon
import java.nio.ByteBuffer
import java.util.*

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal object RadarBeaconUtils {

    private val EDDYSTONE_SERVICE_UUID = ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB")
    private const val IBEACON_MANUFACTURER_ID = 76

    fun getScanFilterForBeaconUUID(beacon: RadarBeacon): ScanFilter? {
        if (beacon.type == RadarBeacon.RadarBeaconType.EDDYSTONE) {
            val uid = beacon.uuid
            val identifier = beacon.major

            val serviceData = ByteBuffer.allocate(18)
                .put(ByteArray(2) { 0x00.toByte() })
                .put(uid.toByteArray())
                .put(identifier.toByteArray())
                .array()

            val serviceDataMask = ByteBuffer.allocate(18)
                .put(ByteArray(1) { 0xFF.toByte() } )
                .put(ByteArray(1) { 0x00.toByte() } )
                .put(ByteArray(16) { 0xFF.toByte() } )
                .array()

            return ScanFilter.Builder()
                .setServiceUuid(EDDYSTONE_SERVICE_UUID)
                .setServiceData(EDDYSTONE_SERVICE_UUID, serviceData, serviceDataMask)
                .build()
        } else if (beacon.type == RadarBeacon.RadarBeaconType.IBEACON) {
            val uuid = UUID.fromString(beacon.uuid)
            val major = beacon.major.toInt()
            val minor = beacon.minor.toInt()

            val manufacturerData = ByteBuffer.allocate(23)
                .put(ByteArray(2) { 0x00.toByte() })
                .putLong(uuid.mostSignificantBits)
                .putLong(uuid.leastSignificantBits)
                .put((major / 256).toByte())
                .put((major % 256).toByte())
                .put((minor / 256).toByte())
                .put((minor % 256).toByte())
                .put(ByteArray(1) { 0x00.toByte() })
                .array()

            val manufacturerDataMask = ByteBuffer.allocate(23)
                .put(ByteArray(2) { 0x00.toByte() })
                .put(ByteArray(20) { 0xFF.toByte() })
                .put(ByteArray(1) { 0x00.toByte() })
                .array()

            return ScanFilter.Builder()
                .setManufacturerData(IBEACON_MANUFACTURER_ID, manufacturerData, manufacturerDataMask)
                .build()
        }

        return null
    }

    fun getScanFilterForBeaconUUID(beaconUUID: String): ScanFilter? {
        val uuid = UUID.fromString(beaconUUID.lowercase())

        val manufacturerData = ByteBuffer.allocate(23)
            .put(ByteArray(2) { 0x00.toByte() })
            .putLong(uuid.mostSignificantBits)
            .putLong(uuid.leastSignificantBits)
            .put(ByteArray(5) { 0x00.toByte() })
            .array()

        val manufacturerDataMask = ByteBuffer.allocate(23)
            .put(ByteArray(2) { 0x00.toByte() })
            .put(ByteArray(16) { 0xFF.toByte() })
            .put(ByteArray(5) { 0x00.toByte() })
            .array()

        return ScanFilter.Builder()
            .setManufacturerData(IBEACON_MANUFACTURER_ID, manufacturerData, manufacturerDataMask)
            .build()
    }

    fun getScanFilterForBeaconUID(beaconUID: String): ScanFilter? {
        val serviceData = ByteBuffer.allocate(18)
            .put(ByteArray(2) { 0x00.toByte() })
            .put(beaconUID.toByteArray())
            .put(ByteArray(6) { 0x00.toByte() })
            .array()

        val serviceDataMask = ByteBuffer.allocate(18)
            .put(ByteArray(1) { 0xFF.toByte() } )
            .put(ByteArray(1) { 0x00.toByte() } )
            .put(ByteArray(10) { 0xFF.toByte() } )
            .put(ByteArray(6) { 0x00.toByte() } )
            .array()

        return ScanFilter.Builder()
            .setServiceUuid(EDDYSTONE_SERVICE_UUID)
            .setServiceData(EDDYSTONE_SERVICE_UUID, serviceData, serviceDataMask)
            .build()
    }

    fun getBeacon(result: ScanResult, scanRecord: ScanRecord): RadarBeacon? {
        val bytes = scanRecord.bytes

        val eddystone = scanRecord.serviceUuids.contains(EDDYSTONE_SERVICE_UUID)

        if (eddystone) {
            val uid = ByteBuffer.wrap(bytes, 2, 10).toString()
            val identifier = ByteBuffer.wrap(bytes, 12, 6).toString()

            return RadarBeacon(
                uuid = uid,
                major = identifier,
                minor = "0",
                rssi = result.rssi,
                type = RadarBeacon.RadarBeaconType.EDDYSTONE
            )
        } else {
            var startByte = 2
            var iBeacon = false
            while (startByte <= 5) {
                if ((bytes[startByte + 2].toInt() and 0xFF) == 0x02 &&
                    (bytes[startByte + 3].toInt() and 0xFF) == 0x15
                ) {
                    iBeacon = true
                    break
                }
                startByte++
            }

            if (!iBeacon) {
                return null
            }

            val buf = ByteBuffer.wrap(bytes, startByte + 4, 20)
            val uuid = UUID(buf.long, buf.long)
            val major = ((buf.get().toInt() and 0xFF) * 0x100 + (buf.get().toInt() and 0xFF)).toString()
            val minor = ((buf.get().toInt() and 0xFF) * 0x100 + (buf.get().toInt() and 0xFF)).toString()

            return RadarBeacon(
                uuid = uuid.toString(),
                major = major,
                minor = minor,
                rssi = result.rssi,
                type = RadarBeacon.RadarBeaconType.IBEACON
            )
        }
    }

}