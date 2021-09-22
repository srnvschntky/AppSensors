package com.example.appsensors

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), SensorEventListener {


    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val bluetoothLeScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private lateinit var sensorManager: SensorManager
    private lateinit var leDeviceListAdapter: BleListAdapter
    private lateinit var locationManager: LocationManager
    private lateinit var objAccelerometer: Accelerometer
    private lateinit var objGyroscope: Gyroscope
    private lateinit var objMagnetometer: Magnetometer
    private lateinit var objModel: Model
    private lateinit var objGson: Gson


    private var listOfBleDiscovered: MutableSet<ListOfBleDiscovered> = mutableSetOf()
    private var magnetometerResultSet: MutableSet<Magnetometer> = mutableSetOf()
    private var accelerometerResultSet: MutableSet<Accelerometer> = mutableSetOf()
    private var gyroscopeResultSet: MutableSet<Gyroscope> = mutableSetOf()
    private var scanResultSet: MutableSet<ScanResult> = mutableSetOf()
    private var mAccelerometer: Sensor? = null
    private var mGyroscope: Sensor? = null
    private var mMagnetometer: Sensor? = null
    private var locationPermission: Int? = null
    private var scanning = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val permission1 = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
        val permission2 =
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
        locationPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        leDeviceListAdapter = BleListAdapter()
        recycler_view.adapter = leDeviceListAdapter

        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            val accSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER)
            mAccelerometer = accSensors.firstOrNull()
            Log.i("SENSOR_TYPE_ACCELEROMETER", "$mAccelerometer")
        }
        if (sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            val gyroSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_GYROSCOPE)
            mGyroscope = gyroSensors.firstOrNull()
            Log.i("SENSOR_TYPE_GYROSCOPE", "$mGyroscope")
        }
        if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {
            val magSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD)
            mMagnetometer = magSensors.firstOrNull()
            Log.i("SENSOR_TYPE_MAGNETIC_FIELD", "$mMagnetometer")
        }

        if (permission1 != PackageManager.PERMISSION_GRANTED
            || permission2 != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                ), REQUEST_CODE
            )
        }




        startScan.setOnClickListener {
            checkCredentials()
        }

        download.setOnClickListener {
            formattingDataToGson()
        }


    }

    private fun formattingDataToGson() {
        val devices = leDeviceListAdapter.getDevicesList()

        devices.forEach {
            var name: String =""
            name = if (it.device.name == null) {
                "N/A"
            } else it.device.name
            val obj = name?.let { it1 -> ListOfBleDiscovered(it1, it.device.address, it.rssi) }

            if (obj != null) {
                listOfBleDiscovered.add(obj)
            }
        }
        objModel = Model(accelerometerResultSet, gyroscopeResultSet, magnetometerResultSet, listOfBleDiscovered)
        objGson = Gson()
        val gsonString = objGson.toJson(objModel)
        val shareIntent = Intent().apply {
            this.action = Intent.ACTION_SEND
            this.putExtra(Intent.EXTRA_TEXT, gsonString.toString())
            this.type = "text/plain"

        }
        startActivity(shareIntent)

    }

    private fun checkCredentials() {
        try {
            if (startScan.text.equals(getString(R.string.startScanning))) {
                if (!bluetoothAdapter.isEnabled) {
                    turnOnBluetooth()
                } else if (locationPermission != PackageManager.PERMISSION_GRANTED
                    || !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                ) {
                    locationPermissions()
                } else {
                    magnetometerResultSet.clear()
                    gyroscopeResultSet.clear()
                    accelerometerResultSet.clear()
                    scanResultSet.clear()
                    listOfBleDiscovered.clear()
                    leDeviceListAdapter.receiveResult(scanResultSet)
                    leDeviceListAdapter.notifyDataSetChanged()
                    startScan.text = getString(R.string.startScanning)
                    scanLeDevice()
                }
            } else {
                scanning = false
                disableSensors()
                bluetoothLeScanner.stopScan(leScanCallback)
                progress_Linear.visibility = View.GONE
                startScan.text = getString(R.string.startScanning)
            }

        } catch (e: Exception) {
            Log.e("SENSOR_ERROR", "error = $e")
        }


        if (leDeviceListAdapter.getDevicesList().size > 0) {
            download.isClickable = true
            download.isEnabled = true
        } else {
            download.isClickable = false
            download.isEnabled = false
        }

    }


    override fun onSensorChanged(event: SensorEvent?) {

        Log.i("SENSOR_EVENT", "$event")


        when (event?.sensor?.stringType) {

            Type_Accelerometer -> {
                Log.i("SENSOR_EVENT_Type_Accelerometer", "$event")
                updateAccValues(event)

            }
            Type_Gyroscope -> {
                Log.i("SENSOR_EVENT_Type_Gyroscope", "$event")
                updateGyroValues(event)

            }
            Type_Magnetic_field -> {
                Log.i("SENSOR_EVENT_Type_Magnetic_field", "$event")
                updateMagValues(event)

            }
            else -> {
                Log.i("SENSOR_EVENT_UNKNOWN", "Unknown event $event")
                Toast.makeText(this, "Unknown event $event", Toast.LENGTH_SHORT).show()
            }


        }


    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {


        when (sensor?.stringType) {

            Type_Accelerometer -> {
                Log.i("SENSOR_EVENT_ACCURACY_CHANGED", "Sensor: $sensor ,Accuracy: $accuracy")


            }
            Type_Gyroscope -> {
                Log.i("SENSOR_EVENT_ACCURACY_CHANGED", "Sensor: $sensor ,Accuracy: $accuracy")

            }
            Type_Magnetic_field -> {
                Log.i("SENSOR_EVENT_ACCURACY_CHANGED", "Sensor: $sensor ,Accuracy: $accuracy")

            }
            else -> {
                Log.i(
                    "SENSOR_EVENT_ACCURACY_CHANGED_UNKNOWN",
                    "UnknownSensor: $sensor ,Accuracy: $accuracy"
                )

            }


        }

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    locationPermission = PackageManager.PERMISSION_GRANTED
                } else {
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )

                }
            }

        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    "Go to App settings and enable location permission to  Allow while using app",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    }


    override fun onResume() {
        super.onResume()


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            TURN_ON_BLUETOOTH_REQUEST_CODE -> {
                if (resultCode != Activity.RESULT_OK) {
                    turnOnBluetooth()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        scanning = false
        disableSensors()
        bluetoothLeScanner.stopScan(leScanCallback)
        progress_Linear.visibility = View.GONE
        startScan.text = getString(R.string.startScanning)

    }

    override fun onDestroy() {
        super.onDestroy()
        scanning = false
        disableSensors()
        bluetoothLeScanner.stopScan(leScanCallback)
        progress_Linear.visibility = View.GONE
        startScan.text = getString(R.string.startScanning)

    }


    //private functions
    private fun updateAccValues(event: SensorEvent) {

        value_A0.text = String.format("%.3f", event.values[0])
        value_A1.text = String.format("%.3f", event.values[1])
        value_A2.text = String.format("%.3f", event.values[2])
        accuracyValue.text = event.accuracy.toString()
        timeStamp.text = event.timestamp.toString().dropLast(8)

        objAccelerometer = Accelerometer(
            event.values[0],
            event.values[1],
            event.values[2],
            event.accuracy,
            event.timestamp
        )
        accelerometerResultSet.add(objAccelerometer)


    }

    private fun updateGyroValues(event: SensorEvent) {

        value_G0.text = String.format("%.3f", event.values[0])
        value_G1.text = String.format("%.3f", event.values[1])
        value_G2.text = String.format("%.3f", event.values[2])
        accuracyValue.text = event.accuracy.toString()
        timeStamp.text = event.timestamp.toString().dropLast(8)
        objGyroscope = Gyroscope(
            event.values[0],
            event.values[1],
            event.values[2],
            event.accuracy,
            event.timestamp
        )
        gyroscopeResultSet.add(objGyroscope)


    }

    private fun updateMagValues(event: SensorEvent) {
        value_M0.text = String.format("%.3f", event.values[0])
        value_M1.text = String.format("%.3f", event.values[1])
        value_M2.text = String.format("%.3f", event.values[2])
        accuracyValue.text = event.accuracy.toString()
        timeStamp.text = event.timestamp.toString().dropLast(8)
        objMagnetometer = Magnetometer(
            event.values[0],
            event.values[1],
            event.values[2],
            event.accuracy,
            event.timestamp
        )
        magnetometerResultSet.add(objMagnetometer)


    }


    private fun locationPermissions() {
        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_CODE
            )
        }else if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.title)
                .setIcon((R.drawable.ic_location_on_24))
                .setMessage(R.string.message)
                .setNeutralButton(R.string.cancel) { _, _ ->
                    // Respond to neutral button press
                }
                .setNegativeButton(R.string.decline) { _, _ ->
                    // Respond to negative button press
                }
                .setPositiveButton(R.string.accept) { _, _ ->
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }
                .show()

        }

    }


    private fun turnOnBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, TURN_ON_BLUETOOTH_REQUEST_CODE)
        }
    }


    private val leScanCallback: ScanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            sendScanResultToPopulate(result)
        }



    }


    private fun sendScanResultToPopulate(result: ScanResult) {
        val mAddress = result.device.address.toString()
        var count:Int? = null
        if (scanResultSet.size > 0) {
            var flag = false
            for (i in 0 until scanResultSet.size) {
                val srs = scanResultSet.elementAt(i).device.address.toString()
                if (srs == mAddress) {
                    flag = true
                    val list:MutableList<ScanResult> = scanResultSet.toMutableList()
                    list[i] = result
                    scanResultSet.clear()
                    scanResultSet = list.toMutableSet()
                }
            }
            if (!flag) {
                scanResultSet.add(result)
            }

        } else {
            scanResultSet.add(result)
        }
        Log.i("TAG_rssi", "$scanResultSet")
        sortResultAccordingToRssi(scanResultSet)

    }

    private fun sortResultAccordingToRssi(scanResultSet: MutableSet<ScanResult>) {


        scanResultSet.sortedBy { it.rssi }
//        scanResultSet.reversed()
        Log.i("TAG_rssi", "$scanResultSet")
        leDeviceListAdapter.receiveResult(scanResultSet)
        leDeviceListAdapter.notifyDataSetChanged()
    }


    private fun scanLeDevice() {
        if (!scanning) {
            scanning = true
            enableSensors()
            progress_Linear.visibility = View.VISIBLE
            startScan.text = getString(R.string.stopScanning)
            bluetoothLeScanner.startScan(leScanCallback)
        } else {
            scanning = false
            disableSensors()
            bluetoothLeScanner.stopScan(leScanCallback)
            progress_Linear.visibility = View.GONE
            startScan.text = getString(R.string.startScanning)
        }
    }

    private fun enableSensors(){
        mAccelerometer?.also { light ->
            sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL)
        }
        mGyroscope?.also { light ->
            sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL)
        }
        mMagnetometer?.also { light ->
            sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL)
        }

    }

    private fun disableSensors(){
        sensorManager.unregisterListener(this)
    }


    companion object {
        const val Type_Accelerometer: String            = "android.sensor.accelerometer"
        const val Type_Gyroscope: String                = "android.sensor.gyroscope"
        const val Type_Magnetic_field: String           = "android.sensor.magnetic_field"
        const val PERMISSION_CODE: Int                  = 1
        const val TURN_ON_BLUETOOTH_REQUEST_CODE: Int   = 2
        const val REQUEST_CODE: Int                     = 3


    }
}
