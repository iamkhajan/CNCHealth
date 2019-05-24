package com.example.mygraphapplication

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.widget.Toast
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlinx.android.synthetic.main.activity_main.*
import android.view.MenuItem
import com.example.mygraphapplication.Utils.Companion.LIMIT_MAX_ACCELEROMETER
import com.example.mygraphapplication.Utils.Companion.LIMIT_MAX_HEALTH
import com.example.mygraphapplication.Utils.Companion.LIMIT_MAX_HUMIDITY
import com.example.mygraphapplication.Utils.Companion.LIMIT_MAX_TEMP
import com.example.mygraphapplication.Utils.Companion.LIMIT_MIN_ACCELEROMETER
import com.example.mygraphapplication.Utils.Companion.LIMIT_MIN_TEMP
import com.example.mygraphapplication.Utils.Companion.TOTAL_ACCELEROMETER_RANGE
import com.example.mygraphapplication.Utils.Companion.TOTAL_HEALTH_RANGE
import com.example.mygraphapplication.Utils.Companion.TOTAL_HUMIDITY_RANGE
import com.example.mygraphapplication.Utils.Companion.TOTAL_TEMP_RANGE
import com.example.mygraphapplication.Utils.Companion.X_AIXIS_VISIBLE_MAX_VALUE
import com.example.mygraphapplication.Utils.Companion.alertPayload
import com.example.mygraphapplication.Utils.Companion.launchPayload
import com.example.mygraphapplication.Utils.Companion.lowPayload
import com.example.mygraphapplication.Utils.Companion.notificationPayload
import com.example.mygraphapplication.Utils.Companion.stopAlert


class GraphActivity : AppCompatActivity(), MyMqttClient.onMqttConnection, AWSPubSubModule.OnAWSEvents {


    override fun onClientConnected(status: String) {
        runOnUiThread {
            status_text.text = status
        }
    }

    override fun onMessageReceived(message: String?) {
        runOnUiThread {
            if (message.equals(alertPayload, true)) {
                utils?.playAlertAlarm(this)
            }

            if (message.equals(lowPayload, true)) {
                utils?.playInfoAlarm(this)
            }

            if (message.equals(notificationPayload, true)) {
                if (!isNotificationVisible(123)) {
                    createNotification("This is an alert notification", 123)
                }
            }

            if (message.equals(launchPayload, true)) {
                mqttClient?.subscribeAllTopics()
            }
            if (message.equals(stopAlert, true)) {
                utils?.stopMediaPlayer(this)
                mqttClient?.unSubscribeAllTopics()
                createNotification("Process stopped", 567)
            }
        }
    }

    override fun onSubscribe() {
        runOnUiThread { Toast.makeText(this, "getInstance success ", Toast.LENGTH_LONG).show() }
    }

    private var mHealthChart: LineChart? = null
    private var mTempChart: LineChart? = null
    private var mHumidityChart: LineChart? = null
    private var mVibrationChart: LineChart? = null

    var mqttClient: MyMqttClient? = null
    var awsPubSubModule: AWSPubSubModule? = null
    var chartUtils: ChartUtils? = null
    var utils: Utils? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mHealthChart = findViewById<LineChart>(R.id.health)
        mVibrationChart = findViewById(R.id.vibration)
        mTempChart = findViewById(R.id.temperature)
        mHumidityChart = findViewById(R.id.humidity)

        utils = Utils()
        chartUtils = ChartUtils()
        initMqttClient()
        initCharts()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        // Handle item selection
        when (item?.getItemId()) {
            R.id.action_settings_connect -> {
                initMqttClient()
//                Handler().postDelayed({
//                    mqttClient?.subscribeAllTopics()
//                }, 2000)

                return true
            }
            R.id.action_alexa_connect -> {
                awsPubSubModule?.connectMqtt()
                return true
            }
            R.id.action_disconnect -> {
                mqttClient?.unSubscribeAllTopics()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun initMqttClient() {
        mqttClient = MyMqttClient(this, this)
        mqttClient?.connectMqttclient()
        //aws
        awsPubSubModule = AWSPubSubModule(this, this)
        awsPubSubModule?.inception()
        Handler().postDelayed({ awsPubSubModule?.connectMqtt() }, 2000)
    }

    private fun initCharts() {
        mHealthChart?.let {
            it.setBorderColor(resources.getColor(R.color.grayColor))
            chartUtils?.chartInception(it, LIMIT_MAX_HEALTH, TOTAL_HEALTH_RANGE)
        }

        mVibrationChart?.let {
            it.setBorderColor(resources.getColor(R.color.grayColor))
            chartUtils?.chartInception(it, LIMIT_MAX_ACCELEROMETER, TOTAL_ACCELEROMETER_RANGE, LIMIT_MIN_ACCELEROMETER)
        }

        mTempChart?.let {
            it.setBorderColor(resources.getColor(R.color.grayColor))
            chartUtils?.chartInception(it, LIMIT_MAX_TEMP, TOTAL_TEMP_RANGE, LIMIT_MIN_TEMP)
        }

        mHumidityChart?.let {
            it.setBorderColor(resources.getColor(R.color.grayColor))
            chartUtils?.chartInception(it, LIMIT_MAX_HUMIDITY, TOTAL_HUMIDITY_RANGE)
        }
    }

    private fun addEntry(lineChart: LineChart?, value: Float, type: Utils.ChartType = Utils.ChartType.TEMPERATURE) {
        val data = lineChart?.data

        if (data != null) {
            var set: ILineDataSet? = data.getDataSetByIndex(0)

            if (set == null) {
                set = chartUtils!!.createSet()
                data.addDataSet(set)
            }

            if (type == Utils.ChartType.HEALTH && value > 600f) {
                utils?.playInfoAlarm(this)
            }

            data.addEntry(Entry(set.entryCount.toFloat(), value), 0)
            // let the chart know it's data has changed
            data.notifyDataChanged()
            lineChart.notifyDataSetChanged()

            // limit the number of visible entries
            lineChart.setVisibleXRangeMaximum(X_AIXIS_VISIBLE_MAX_VALUE)
            // move to the latest entry
            lineChart.moveViewToX(data.entryCount.toFloat())
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
    }

    override fun onTempDataReceived(value: Int) {
        Log.d(MyMqttClient.TAG, "Temp data " + value)
        addEntry(mTempChart, value.toFloat())
    }

    override fun onHumidityDataReceived(value: Int) {
        addEntry(mHumidityChart, value.toFloat(), Utils.ChartType.HUMIDITY)
    }

    override fun onLightReceived(value: Int) {
        //addEntry(mHealthChart, value.toFloat(), Utils.ChartType.HEALTH)
    }

    override fun onHealthDataReceived(value: Int) {
        if (value > 2) {
            mHealthChart?.setBorderColor(resources.getColor(R.color.redColor))
            if (!isNotificationVisible(345)) {
                createNotification("Health Alert notification", 345)
            }
        } else {
            mHealthChart?.setBorderColor(resources.getColor(R.color.greenColor))
        }
        addEntry(mHealthChart, value.toFloat(), Utils.ChartType.HEALTH)
    }

    override fun onAccelerometerTopicDataReceived(value: Int) {
        addEntry(mVibrationChart, value.toFloat(), Utils.ChartType.ACCELEROMETER)
    }

    override fun onPressureTopicDataReceived(value: Int) {

    }

    override fun onConnectionSuccess(status: Boolean) {
       // mqttConnectionStatus.text = "Success Status is " + status
    }

    override fun onConnectionLost(message: String) {
     //   mqttConnectionStatus.text = "Failure Message is " + message
        Handler().postDelayed({ initMqttClient() }, 3000)
    }


    fun createNotification(message: String?, notificationId: Int?) {
        createNotificationChannel()
        var builder = NotificationCompat.Builder(this, "12345")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setBadgeIconType(R.mipmap.ic_launcher)
            .setContentTitle("Bosch")
            .setContentText("Bosch")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        with(NotificationManagerCompat.from(this)) {
            notify(notificationId!!, builder.build())
        }
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Bosch"
            val descriptionText = "Bosch"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("12345", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    private fun isNotificationVisible(MY_ID: Int): Boolean {
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifications = mNotificationManager.activeNotifications
        for (notification in notifications) {
            if (notification?.id == MY_ID) {
                return true
            }
        }
        return false
    }

}