package jareddefour.example.comp3606a2publisherapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.Builder.IMPLICIT_MIN_UPDATE_INTERVAL
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.snackbar.Snackbar
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import org.json.JSONObject
import org.w3c.dom.Text
import java.lang.System.currentTimeMillis


class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private val requestCode = 4321
    private var requestingLocationUpdates = false
    private var client: Mqtt5BlockingClient? = null
    private var studentID: String = "823456789"


//    private val intentFilter = IntentFilter().apply {
//        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
//        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
//        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
//        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
//    }

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(R.layout.publisher_activity)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)


        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))

        client = Mqtt5Client.builder()
            .identifier(studentID)
            .serverHost("broker.sundaebytestt.com")
            .serverPort(1883)
            .build()
            .toBlocking()




        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                p0 ?: return
                for (location in p0.locations){
                    Log.e("Loc","$location")
                    val locationPayload = JSONObject()
                    locationPayload.put("timestamp",currentTimeMillis())
                    locationPayload.put("id", studentID)
                    locationPayload.put( "longitude","${location.longitude}")
                    locationPayload.put( "latitude","${location.latitude}")

                    client?.publishWith()?.topic("assignment/location")?.payload(locationPayload.toString().toByteArray())?.send()
                    Log.e("LOC","${locationPayload}")
//                    Toast.makeText(this,locationPayload,Toast.LENGTH_SHORT).show()
                }
            }
            }



    }


    val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) || permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)-> {
                Toast.makeText(this,"Location access granted",Toast.LENGTH_SHORT).show()

                if(isLocationEnabled()){
                     locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                        .apply {
                            setWaitForAccurateLocation(false)
                            setMinUpdateIntervalMillis(100)
                            setMaxUpdateDelayMillis(100000)
                        }.build()

                }
                // Precise location access granted.
            }
             else -> {
            // No location access granted.
        }
        }




    }


     fun startUpdates(view: View){
        if(!isLocationEnabled()){
            Snackbar.make(this,view,"Location updates failed to start",Snackbar.LENGTH_SHORT).show()
            return

        }
         try{
             val textView = findViewById<TextView>(R.id.etStudentID)

             disableEditText(textView)
             studentID = textView.text.toString()
//             textView.text=""

             val imm = getSystemService(
                 INPUT_METHOD_SERVICE
             ) as InputMethodManager
             imm.hideSoftInputFromWindow(textView.windowToken, 0)



             client?.connect()
             startLocationUpdates()
             requestingLocationUpdates=true
             Snackbar.make(this,view,"Location updates have successfully begun",Snackbar.LENGTH_SHORT).show()
         }catch (e:Exception){
             Log.e("ERROR","$e")
         }


    }

    fun stopUpdates(view: View){
        if (!requestingLocationUpdates){
            Snackbar.make(this,view,"Updates are not currently enables",Snackbar.LENGTH_SHORT).show()
            return

        }

        client?.disconnect()
        stopLocationUpdates()
        Snackbar.make(this,view,"Updates have been disabled",Snackbar.LENGTH_SHORT).show()
        enableEditText(findViewById<TextView>(R.id.etStudentID))
        requestingLocationUpdates=false

    }
    private fun isLocationEnabled(): Boolean{
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return true
    }

    private fun disableEditText(editText: TextView) {
        editText.isFocusable = false
        editText.isEnabled = false
        editText.isCursorVisible = false
    }

    private fun enableEditText(editText: TextView) {
        editText.isFocusable = true
        editText.isEnabled = true
        editText.isCursorVisible = true

    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,
            locationCallback,
            Looper.getMainLooper())

    }


    override fun onSaveInstanceState(outState: Bundle) {
//        outState?.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, requestingLocationUpdates)
        super.onSaveInstanceState(outState)
    }


    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        if (requestingLocationUpdates) startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }




}