package com.health_test

import android.app.Activity
import android.content.Context
import androidx.annotation.NonNull
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.*
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlin.random.Random

//import androidx.health.connect.client.permission.HealthPermission

const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1111
const val HEALTH_CONNECT_RESULT_CODE = 16969
const val CHANNEL_NAME = "flutter_health"
const val MMOLL_2_MGDL = 18.0 // 1 mmoll= 1

class MainActivity : FlutterActivity() {
    private var mResult: MethodChannel.Result? = null
    private var activity: Activity? = null
    private var useHealthConnectIfAvailable: Boolean = false
    private var context: Context? = null


    private var BODY_FAT_PERCENTAGE = "BODY_FAT_PERCENTAGE"
    private var HEIGHT = "HEIGHT"
    private var WEIGHT = "WEIGHT"
    private var STEPS = "STEPS"
    private var AGGREGATE_STEP_COUNT = "AGGREGATE_STEP_COUNT"
    private var ACTIVE_ENERGY_BURNED = "ACTIVE_ENERGY_BURNED"
    private var HEART_RATE = "HEART_RATE"
    private var BODY_TEMPERATURE = "BODY_TEMPERATURE"
    private var BLOOD_PRESSURE_SYSTOLIC = "BLOOD_PRESSURE_SYSTOLIC"
    private var BLOOD_PRESSURE_DIASTOLIC = "BLOOD_PRESSURE_DIASTOLIC"
    private var BLOOD_OXYGEN = "BLOOD_OXYGEN"
    private var BLOOD_GLUCOSE = "BLOOD_GLUCOSE"
    private var MOVE_MINUTES = "MOVE_MINUTES"
    private var DISTANCE_DELTA = "DISTANCE_DELTA"
    private var WATER = "WATER"

    // TODO support unknown?
    private var SLEEP_ASLEEP = "SLEEP_ASLEEP"
    private var SLEEP_AWAKE = "SLEEP_AWAKE"
    private var SLEEP_IN_BED = "SLEEP_IN_BED"
    private var SLEEP_SESSION = "SLEEP_SESSION"
    private var SLEEP_LIGHT = "SLEEP_LIGHT"
    private var SLEEP_DEEP = "SLEEP_DEEP"
    private var SLEEP_REM = "SLEEP_REM"
    private var SLEEP_OUT_OF_BED = "SLEEP_OUT_OF_BED"
    private var WORKOUT = "WORKOUT"

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
//        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
//        channel = MethodChannel(flutterPluginBinding.binaryMessenger, CHANNEL_NAME)
//        channel?.setMethodCallHandler(this)
        context = applicationContext
//        threadPoolExecutor = Executors.newFixedThreadPool(4)
        checkAvailability()
//        if (healthConnectAvailable) {
//            healthConnectClient =
//                    HealthConnectClient.getOrCreate(flutterPluginBinding.applicationContext)
//        }
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL_NAME).setMethodCallHandler { call, result ->
            when (call.method) {
                "getRandomNumber" -> getRandomNumber(call, result)
                "hasPermissions" -> hasPermissions(call, result)
                "useHealthConnectIfAvailable" -> useHealthConnectIfAvailable(call, result)
                "requestAuthorization" -> requestAuthorization(call, result)


                else -> result.notImplemented()
            }
        }
//        checkAvailability()
    }

    private fun getRandomNumber(call: MethodCall, result: MethodChannel.Result) {
        val rand = Random.nextInt(100)
        result.success(rand)
    }

    private fun keyToHealthDataType(type: String): DataType {
        return when (type) {
            BODY_FAT_PERCENTAGE -> DataType.TYPE_BODY_FAT_PERCENTAGE
            HEIGHT -> DataType.TYPE_HEIGHT
            WEIGHT -> DataType.TYPE_WEIGHT
            STEPS -> DataType.TYPE_STEP_COUNT_DELTA
            AGGREGATE_STEP_COUNT -> DataType.AGGREGATE_STEP_COUNT_DELTA
            ACTIVE_ENERGY_BURNED -> DataType.TYPE_CALORIES_EXPENDED
            HEART_RATE -> DataType.TYPE_HEART_RATE_BPM
            BODY_TEMPERATURE -> HealthDataTypes.TYPE_BODY_TEMPERATURE
            BLOOD_PRESSURE_SYSTOLIC -> HealthDataTypes.TYPE_BLOOD_PRESSURE
            BLOOD_PRESSURE_DIASTOLIC -> HealthDataTypes.TYPE_BLOOD_PRESSURE
            BLOOD_OXYGEN -> HealthDataTypes.TYPE_OXYGEN_SATURATION
            BLOOD_GLUCOSE -> HealthDataTypes.TYPE_BLOOD_GLUCOSE
            MOVE_MINUTES -> DataType.TYPE_MOVE_MINUTES
            DISTANCE_DELTA -> DataType.TYPE_DISTANCE_DELTA
            WATER -> DataType.TYPE_HYDRATION
            SLEEP_ASLEEP -> DataType.TYPE_SLEEP_SEGMENT
            SLEEP_AWAKE -> DataType.TYPE_SLEEP_SEGMENT
            SLEEP_IN_BED -> DataType.TYPE_SLEEP_SEGMENT
            WORKOUT -> DataType.TYPE_ACTIVITY_SEGMENT
            else -> throw IllegalArgumentException("Unsupported dataType: $type")
        }
    }

    private fun callToHealthTypes(call: MethodCall): FitnessOptions {
        val typesBuilder = FitnessOptions.builder()
        val args = call.arguments as HashMap<*, *>
        val types = (args["types"] as? ArrayList<*>)?.filterIsInstance<String>()
        val permissions = (args["permissions"] as? ArrayList<*>)?.filterIsInstance<Int>()

        assert(types != null)
        assert(permissions != null)
        assert(types!!.count() == permissions!!.count())

        for ((i, typeKey) in types.withIndex()) {
            val access = permissions[i]
            val dataType = keyToHealthDataType(typeKey)
            when (access) {
                0 -> typesBuilder.addDataType(dataType, FitnessOptions.ACCESS_READ)
                1 -> typesBuilder.addDataType(dataType, FitnessOptions.ACCESS_WRITE)
                2 -> {
                    typesBuilder.addDataType(dataType, FitnessOptions.ACCESS_READ)
                    typesBuilder.addDataType(dataType, FitnessOptions.ACCESS_WRITE)
                }

                else -> throw IllegalArgumentException("Unknown access type $access")
            }
            if (typeKey == SLEEP_ASLEEP || typeKey == SLEEP_AWAKE || typeKey == SLEEP_IN_BED) {
                typesBuilder.accessSleepSessions(FitnessOptions.ACCESS_READ)
                when (access) {
                    0 -> typesBuilder.accessSleepSessions(FitnessOptions.ACCESS_READ)
                    1 -> typesBuilder.accessSleepSessions(FitnessOptions.ACCESS_WRITE)
                    2 -> {
                        typesBuilder.accessSleepSessions(FitnessOptions.ACCESS_READ)
                        typesBuilder.accessSleepSessions(FitnessOptions.ACCESS_WRITE)
                    }

                    else -> throw IllegalArgumentException("Unknown access type $access")
                }
            }
            if (typeKey == WORKOUT) {
                when (access) {
                    0 -> typesBuilder.accessActivitySessions(FitnessOptions.ACCESS_READ)
                    1 -> typesBuilder.accessActivitySessions(FitnessOptions.ACCESS_WRITE)
                    2 -> {
                        typesBuilder.accessActivitySessions(FitnessOptions.ACCESS_READ)
                        typesBuilder.accessActivitySessions(FitnessOptions.ACCESS_WRITE)
                    }

                    else -> throw IllegalArgumentException("Unknown access type $access")
                }
            }
        }
        return typesBuilder.build()
    }

    private fun hasPermissions(call: MethodCall, result: MethodChannel.Result) {
        if (useHealthConnectIfAvailable && healthConnectAvailable) {
//            hasPermissionsHC(call, result)
            return
        }
        if (context == null) {
            println(true)
            result.success(false)
            return
        }

        val optionsToRegister = callToHealthTypes(call)

        val isGranted = GoogleSignIn.hasPermissions(
                GoogleSignIn.getLastSignedInAccount(context!!),
                optionsToRegister,
        )
        result?.success(isGranted)
    }

    private fun requestAuthorization(call: MethodCall, result: MethodChannel.Result) {
        if (context == null) {
            result.success(false)
            return
        }
        mResult = result

        if (useHealthConnectIfAvailable && healthConnectAvailable) {
//            requestAuthorizationHC(call, result)
            return
        }

        val optionsToRegister = callToHealthTypes(call)

        val isGranted = false

        if (!isGranted && activity != null) {
            GoogleSignIn.requestPermissions(
                    activity!!,
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(context!!),
                    optionsToRegister,
            )
        } else { // / Permission already granted
            result?.success(true)
        }
    }

    fun useHealthConnectIfAvailable(call: MethodCall, result: MethodChannel.Result) {
        useHealthConnectIfAvailable = true
        println(useHealthConnectIfAvailable)
        result.success(null)
    }

    var healthConnectAvailable = false

    fun checkAvailability() {
//        healthConnectStatus = HealthConnectClient.sdkStatus(context!!)
//        healthConnectAvailable = healthConnectStatus == HealthConnectClient.SDK_AVAILABLE
    }

//    private fun hasPermissionsHC(call: MethodCall, result: MethodChannel.Result) {
//        val args = call.arguments as java.util.HashMap<*, *>
//        val types = (args["types"] as? java.util.ArrayList<*>)?.filterIsInstance<String>()!!
//        val permissions = (args["permissions"] as? java.util.ArrayList<*>)?.filterIsInstance<Int>()!!
//
//        var permList = mutableListOf<String>()
//        for ((i, typeKey) in types.withIndex()) {
//            val access = permissions[i]!!
//            val dataType = MapToHCType[typeKey]!!
//            if (access == 0) {
//                permList.add(
//                    HealthPermission.getReadPermission(dataType),
//                )
//            } else {
//                permList.addAll(
//                    listOf(
//                        HealthPermission.getReadPermission(dataType),
//                        HealthPermission.getWritePermission(dataType),
//                    ),
//                )
//            }
//            // Workout also needs distance and total energy burned too
//            if (typeKey == WORKOUT) {
//                if (access == 0) {
//                    permList.addAll(
//                        listOf(
//                            HealthPermission.getReadPermission(DistanceRecord::class),
//                            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
//                        ),
//                    )
//                } else {
//                    permList.addAll(
//                        listOf(
//                            HealthPermission.getReadPermission(DistanceRecord::class),
//                            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
//                            HealthPermission.getWritePermission(DistanceRecord::class),
//                            HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
//                        ),
//                    )
//                }
//            }
//        }
//        scope.launch {
//            result.success(
//                healthConnectClient.permissionController.getGrantedPermissions()
//                    .containsAll(permList),
//            )
//        }
//    }

//        val MapToHCType = hashMapOf(
//            BODY_FAT_PERCENTAGE to BodyFatRecord::class,
//            HEIGHT to HeightRecord::class,
//            WEIGHT to WeightRecord::class,
//            STEPS to StepsRecord::class,
//            AGGREGATE_STEP_COUNT to StepsRecord::class,
//            ACTIVE_ENERGY_BURNED to ActiveCaloriesBurnedRecord::class,
//            HEART_RATE to HeartRateRecord::class,
//            BODY_TEMPERATURE to BodyTemperatureRecord::class,
//            BLOOD_PRESSURE_SYSTOLIC to BloodPressureRecord::class,
//            BLOOD_PRESSURE_DIASTOLIC to BloodPressureRecord::class,
//            BLOOD_OXYGEN to OxygenSaturationRecord::class,
//            BLOOD_GLUCOSE to BloodGlucoseRecord::class,
//            DISTANCE_DELTA to DistanceRecord::class,
//            WATER to HydrationRecord::class,
//            SLEEP_ASLEEP to SleepStageRecord::class,
//            SLEEP_AWAKE to SleepStageRecord::class,
//            SLEEP_LIGHT to SleepStageRecord::class,
//            SLEEP_DEEP to SleepStageRecord::class,
//            SLEEP_REM to SleepStageRecord::class,
//            SLEEP_OUT_OF_BED to SleepStageRecord::class,
//            SLEEP_SESSION to SleepSessionRecord::class,
//            WORKOUT to ExerciseSessionRecord::class,
//            // MOVE_MINUTES to TODO: Find alternative?
//            // TODO: Implement remaining types
//            // "ActiveCaloriesBurned" to ActiveCaloriesBurnedRecord::class,
//            // "BasalBodyTemperature" to BasalBodyTemperatureRecord::class,
//            // "BasalMetabolicRate" to BasalMetabolicRateRecord::class,
//            // "BloodGlucose" to BloodGlucoseRecord::class,
//            // "BloodPressure" to BloodPressureRecord::class,
//            // "BodyFat" to BodyFatRecord::class,
//            // "BodyTemperature" to BodyTemperatureRecord::class,
//            // "BoneMass" to BoneMassRecord::class,
//            // "CervicalMucus" to CervicalMucusRecord::class,
//            // "CyclingPedalingCadence" to CyclingPedalingCadenceRecord::class,
//            // "Distance" to DistanceRecord::class,
//            // "ElevationGained" to ElevationGainedRecord::class,
//            // "ExerciseSession" to ExerciseSessionRecord::class,
//            // "FloorsClimbed" to FloorsClimbedRecord::class,
//            // "HeartRate" to HeartRateRecord::class,
//            // "Height" to HeightRecord::class,
//            // "Hydration" to HydrationRecord::class,
//            // "LeanBodyMass" to LeanBodyMassRecord::class,
//            // "MenstruationFlow" to MenstruationFlowRecord::class,
//            // "MenstruationPeriod" to MenstruationPeriodRecord::class,
//            // "Nutrition" to NutritionRecord::class,
//            // "OvulationTest" to OvulationTestRecord::class,
//            // "OxygenSaturation" to OxygenSaturationRecord::class,
//            // "Power" to PowerRecord::class,
//            // "RespiratoryRate" to RespiratoryRateRecord::class,
//            // "RestingHeartRate" to RestingHeartRateRecord::class,
//            // "SexualActivity" to SexualActivityRecord::class,
//            // "SleepSession" to SleepSessionRecord::class,
//            // "SleepStage" to SleepStageRecord::class,
//            // "Speed" to SpeedRecord::class,
//            // "StepsCadence" to StepsCadenceRecord::class,
//            // "Steps" to StepsRecord::class,
//            // "TotalCaloriesBurned" to TotalCaloriesBurnedRecord::class,
//            // "Vo2Max" to Vo2MaxRecord::class,
//            // "Weight" to WeightRecord::class,
//            // "WheelchairPushes" to WheelchairPushesRecord::class,
//    )
}
