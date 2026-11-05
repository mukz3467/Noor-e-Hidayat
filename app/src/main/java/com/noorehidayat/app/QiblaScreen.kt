package com.noorehidayat.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val KAABA_LATITUDE = 21.4225
private const val KAABA_LONGITUDE = 39.8262
private val QiblaBackground = Color(0xFF061F17)
private val QiblaPanel = Color(0xFF103B2B)
private val QiblaGold = Color(0xFFE7C879)
private val QiblaCream = Color(0xFFFFF4DE)
private val QiblaMuted = Color(0xFFB7C9BC)

private fun normalizeDegrees(value: Float): Float = ((value % 360f) + 360f) % 360f

internal fun calculateQiblaBearing(latitude: Double, longitude: Double): Float {
    val lat1 = Math.toRadians(latitude)
    val lat2 = Math.toRadians(KAABA_LATITUDE)
    val deltaLongitude = Math.toRadians(KAABA_LONGITUDE - longitude)
    val y = sin(deltaLongitude) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLongitude)
    return normalizeDegrees(Math.toDegrees(atan2(y, x)).toFloat())
}

@Composable
internal fun QiblaScreen() {
    val context = LocalContext.current
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val rotationSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    var coordinates by remember { mutableStateOf<Location?>(null) }
    var heading by remember { mutableStateOf<Float?>(null) }
    var locationError by remember { mutableStateOf("") }

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun requestLocationUpdates() {
        if (!hasLocationPermission()) return
        locationError = ""
        try {
            locationManager.getProviders(true).forEach { provider ->
                try {
                    val last = locationManager.getLastKnownLocation(provider)
                    if (last != null && (coordinates == null || last.time > (coordinates?.time ?: 0L))) coordinates = last
                } catch (_: SecurityException) { }
            }
        } catch (_: Exception) {
            locationError = "Location is unavailable. Check device location settings."
        }
    }

    var permissionGranted by remember { mutableStateOf(hasLocationPermission()) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        permissionGranted = results.values.any { it }
        if (permissionGranted) requestLocationUpdates()
        else locationError = "Location permission is needed to calculate your Qibla direction."
    }

    LaunchedEffect(Unit) {
        permissionGranted = hasLocationPermission()
        if (permissionGranted) requestLocationUpdates()
    }

    DisposableEffect(permissionGranted) {
        if (!permissionGranted) onDispose { }
        else {
            val listener = LocationListener { location -> coordinates = location; locationError = "" }
            try {
                listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
                    try {
                        if (locationManager.isProviderEnabled(provider)) {
                            locationManager.requestLocationUpdates(provider, 3000L, 5f, listener, Looper.getMainLooper())
                        }
                    } catch (_: SecurityException) {
                        locationError = "Location permission was revoked. Please enable it again."
                    } catch (_: IllegalArgumentException) { }
                }
            } catch (_: Exception) {
                locationError = "Unable to start location updates on this device."
            }
            onDispose { try { locationManager.removeUpdates(listener) } catch (_: Exception) { } }
        }
    }

    DisposableEffect(rotationSensor) {
        if (rotationSensor == null) onDispose { }
        else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val matrix = FloatArray(9)
                    val orientation = FloatArray(3)
                    SensorManager.getRotationMatrixFromVector(matrix, event.values)
                    SensorManager.getOrientation(matrix, orientation)
                    heading = normalizeDegrees(Math.toDegrees(orientation[0].toDouble()).toFloat())
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }

    val bearing = coordinates?.let { calculateQiblaBearing(it.latitude, it.longitude) }
    val relativeDirection = if (bearing != null && heading != null) normalizeDegrees(bearing - heading!!) else null

    Column(Modifier.fillMaxSize().background(QiblaBackground).padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("DIRECTION OF PRAYER", color = QiblaGold, fontSize = 10.sp, letterSpacing = 2.sp)
        Text("Qibla Compass", color = QiblaCream, fontSize = 27.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp, bottom = 8.dp))
        Text("Point your phone flat and follow the arrow toward the Kaaba.", color = QiblaMuted, fontSize = 13.sp)
        Spacer(Modifier.height(24.dp))
        Box(Modifier.size(238.dp).background(QiblaPanel, CircleShape).padding(12.dp).background(QiblaBackground, CircleShape), contentAlignment = Alignment.Center) {
            Text("N", color = QiblaGold, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
            if (relativeDirection != null) {
                Icon(Icons.Default.Navigation, contentDescription = "Arrow pointing toward Qibla", tint = QiblaGold, modifier = Modifier.size(104.dp).rotate(relativeDirection))
            } else {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = QiblaMuted, modifier = Modifier.size(58.dp))
            }
        }
        Spacer(Modifier.height(22.dp))
        if (bearing != null) {
            Text("QIBLA BEARING", color = QiblaMuted, fontSize = 10.sp, letterSpacing = 1.5.sp)
            Text("${bearing.toInt()}°", color = QiblaGold, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            if (relativeDirection == null) Text("Waiting for compass sensor…", color = QiblaMuted, fontSize = 12.sp)
            else Text("Turn your phone ${relativeDirection.toInt()}° clockwise from north", color = QiblaCream, fontSize = 12.sp)
            coordinates?.let { Text("Location accuracy: ${it.accuracy.toInt()} m", color = QiblaMuted, fontSize = 11.sp) }
        } else {
            Text("Find Qibla direction", color = QiblaGold, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Text("Allow location access so the app can calculate the direction from your current position.", color = QiblaMuted, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Button(onClick = {
                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }, colors = ButtonDefaults.buttonColors(containerColor = QiblaGold, contentColor = QiblaBackground)) { Text("Enable location") }
        }
        if (locationError.isNotBlank()) Text(locationError, color = QiblaGold, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
        if (rotationSensor == null) Text("This device does not provide a rotation-vector compass sensor. The bearing is still shown when location is available.", color = QiblaMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(18.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = QiblaPanel), border = BorderStroke(1.dp, QiblaGold.copy(alpha = 0.3f))) {
            Text("For better accuracy, keep the phone away from magnets and metal objects and calibrate the compass if the device requests it.", color = QiblaMuted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(14.dp))
        }
    }
}
