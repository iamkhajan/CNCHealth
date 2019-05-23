package com.example.mygraphapplication

import android.content.Context
import android.media.MediaPlayer
import com.amazonaws.regions.Regions

class Utils {


    companion object {
        /*payload*/
        val alertPayload = "high"
        val lowPayload = "low"
        val notificationPayload = "Notification"
        val launchPayload = "LaunchAction"
        val stopAlert = "StopAlert"


        @JvmStatic
        val CUSTOMER_SPECIFIC_ENDPOINT = "axg7313dbj67t-ats.iot.us-west-2.amazonaws.com"
        // Name of the AWS IoT policy to attach to a newly created certificate
        @JvmStatic
        val AWS_IOT_POLICY_NAME = "bosch-xdk-policy"

        @JvmStatic
        val MY_REGION = Regions.US_WEST_2

        @JvmStatic
        val KEYSTORE_NAME = "iot_keystore"

        @JvmStatic
        val KEYSTORE_PASSWORD = "password"

        @JvmStatic
        val CERTIFICATE_ID = "default"


        //local chart limits

        //this for health
        val TOTAL_HEALTH_RANGE = 5.0f
        val LIMIT_MAX_HEALTH = 5.0f
        val LIMIT_MIN_HEALTH = 0f

        //max and limit for accelerometer
        val TOTAL_ACCELEROMETER_RANGE = 9000.0f
        val LIMIT_MAX_ACCELEROMETER = 4000.0f
        val LIMIT_MIN_ACCELEROMETER = 800f

        //max and limit for temperature
        val TOTAL_TEMP_RANGE = 80.0f
        val LIMIT_MAX_TEMP = 50.0f
        val LIMIT_MIN_TEMP = 0.0f

        //max and limit for temperature
        val TOTAL_HUMIDITY_RANGE = 80.0f
        val LIMIT_MAX_HUMIDITY = 50.0f
        val LIMIT_MIN_HUMIDITY = 0.0f

        //how many values will be shown
        val X_AIXIS_VISIBLE_MAX_VALUE = 6f
    }

    fun playAlertAlarm(context: Context) {
        val mPlayer: MediaPlayer = MediaPlayer.create(context, R.raw.alarm)
        mPlayer.start()
    }

    fun playInfoAlarm(context: Context) {
        val mPlayer: MediaPlayer = MediaPlayer.create(context, R.raw.information)
        mPlayer.start()
    }


    fun stopMediaPlayer(context: Context) {
        MediaPlayer().stop()
    }


    enum class ChartType {
        HEALTH, TEMPERATURE, HUMIDITY, PRESSURE, ACCELEROMETER
    }


}