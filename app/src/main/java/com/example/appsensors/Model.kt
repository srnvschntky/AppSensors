package com.example.appsensors

data class Model (
    var accelerometer:Accelerometer,
    var gyroscope:Gyroscope,
    var magnetometer:Magnetometer,
    var bleDevices:MutableSet<ListOfBleDiscovered>
)


data class Accelerometer (
    var valueA0:Float,
    var valueA1:Float,
    var valueA2:Float,
    var accurate:Int,
    var timeStamp:Long,

)



data class Gyroscope(
    var valueG0:Float,
    var valueG1:Float,
    var valueG2:Float,
    var accurate:Int,
    var timeStamp:Long,

)



data class Magnetometer (
    var valueM0:Float,
    var valueM1:Float,
    var valueM2:Float,
    var accurate:Int,
    var timeStamp:Long,

)

data class ListOfBleDiscovered(
    var deviceName:String,
    var deviceMacAddress:String,
    var rssiValue:Int
)
