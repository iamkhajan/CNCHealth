package com.example.mygraphapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.View
import android.widget.Toast
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlinx.android.synthetic.main.activity_main.*
import android.view.MenuItem
import com.example.mygraphapplication.Utils.Companion.LIMIT_MAX_HEALTH
import com.example.mygraphapplication.Utils.Companion.LIMIT_MAX_HUMIDITY
import com.example.mygraphapplication.Utils.Companion.TOTAL_HEALTH_RANGE
import com.example.mygraphapplication.Utils.Companion.TOTAL_HUMIDITY_RANGE
import com.example.mygraphapplication.Utils.Companion.X_AIXIS_VISIBLE_MAX_VALUE


class GraphActivity : AppCompatActivity(), MyMqttClient.onMqttConnection, View.OnClickListener,
    PubSubModule.OnAWSEvents {

    override fun onClientConnected(status: String) {
        runOnUiThread {
            status_text.text = status
            //Toast.makeText(this, status, Toast.LENGTH_LONG).show() }
        }
    }

    override fun onMessageReceived(message: String?) {
        runOnUiThread {
            subscribeAllTopics()
            // Toast.makeText(this, "Message " + message, Toast.LENGTH_LONG).show()
        }

    }

    override fun onSubscribe() {
        runOnUiThread { Toast.makeText(this, "getInstance success ", Toast.LENGTH_LONG).show() }

    }

    private var mChart: LineChart? = null
    private var mTempChart: LineChart? = null
    private var mHumidityChart: LineChart? = null
    private var mVibrationChart: LineChart? = null

    var mqttClient: MyMqttClient? = null
    var pubSubActivity: PubSubModule? = null
    var chartUtils: ChartUtils? = null
    var utils: Utils? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mChart = findViewById<LineChart>(R.id.health)
        mVibrationChart = findViewById(R.id.vibration)
        mTempChart = findViewById(R.id.temperature)
        mHumidityChart = findViewById(R.id.humidity)

        chartUtils = ChartUtils()
        initMqttClient()
        setButtonClickListener()
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
            R.id.action_settings -> {
                pubSubActivity?.connectMqtt()
                return true
            }
            R.id.action_disconnect -> {
                pubSubActivity?.disconnectConnection()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun initMqttClient() {
        mqttClient = MyMqttClient(this, this)
        //aws
        pubSubActivity = PubSubModule(this, this)
        pubSubActivity?.inception()
        Handler().postDelayed({ pubSubActivity?.connectMqtt() }, 1000)
    }

    private fun initCharts() {
        mChart?.let {
            chartUtils?.chartInception(it, LIMIT_MAX_HEALTH, TOTAL_HEALTH_RANGE)
        }

        mVibrationChart?.let {
            chartUtils?.chartInception(it, LIMIT_MAX_HEALTH, TOTAL_HEALTH_RANGE)
        }

        mTempChart?.let {
            chartUtils?.chartInception(it, LIMIT_MAX_HEALTH, TOTAL_HEALTH_RANGE)
        }

        mHumidityChart?.let {
            chartUtils?.chartInception(it, LIMIT_MAX_HUMIDITY, TOTAL_HUMIDITY_RANGE)
        }
    }

    fun setButtonClickListener() {
        startHealth.setOnClickListener(this)
        stopHealth.setOnClickListener(this)
        startTemperature.setOnClickListener(this)
        stopTemperature.setOnClickListener(this)
        startVibration.setOnClickListener(this)
        stopvVbration.setOnClickListener(this)
        startHumidity.setOnClickListener(this)
        stopHumidity.setOnClickListener(this)
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
        addEntry(mTempChart, value.toFloat())
    }

    override fun onHumidityDataReceived(value: Int) {
        addEntry(mHumidityChart, value.toFloat(), Utils.ChartType.HUMIDITY)
    }

    override fun onLightReceived(value: Int) {
        addEntry(mChart, value.toFloat(), Utils.ChartType.HEALTH)
    }

    override fun onHealthDataReceived(value: Int) {
    }

    override fun onAccelerometerTopicDataReceived(value: Int) {

    }

    override fun onPressureTopicDataReceived(value: Int) {
        addEntry(mVibrationChart, value.toFloat(), Utils.ChartType.PRESSURE)
    }


    override fun onClick(p0: View?) {
        when (p0) {
            startHealth -> mqttClient?.subsribeMessage(MyMqttClient.lightTopic)
            stopHealth -> mqttClient?.unsubscribeMessage(MyMqttClient.lightTopic)
            startTemperature -> mqttClient?.subsribeMessage(MyMqttClient.tempretureTopic)
            stopTemperature -> mqttClient?.unsubscribeMessage(MyMqttClient.tempretureTopic)
            startVibration -> mqttClient?.subsribeMessage(MyMqttClient.vibrationTopic)
            stopvVbration -> mqttClient?.unsubscribeMessage(MyMqttClient.vibrationTopic)
            startHumidity -> mqttClient?.subsribeMessage(MyMqttClient.humidityTopic)
            stopHumidity -> mqttClient?.unsubscribeMessage(MyMqttClient.humidityTopic)
        }
    }

    fun subscribeAllTopics() {
        mqttClient?.subsribeMessage(MyMqttClient.lightTopic)
        mqttClient?.subsribeMessage(MyMqttClient.tempretureTopic)
        mqttClient?.subsribeMessage(MyMqttClient.vibrationTopic)
        mqttClient?.subsribeMessage(MyMqttClient.humidityTopic)

        createNotification()
    }

    fun unsubscribeAllTopics() {
        mqttClient?.unsubscribeMessage(MyMqttClient.lightTopic)
        mqttClient?.unsubscribeMessage(MyMqttClient.tempretureTopic)
        mqttClient?.unsubscribeMessage(MyMqttClient.vibrationTopic)
        mqttClient?.unsubscribeMessage(MyMqttClient.humidityTopic)
    }


    fun createNotification() {
        createNotificationChannel()
        var builder = NotificationCompat.Builder(this, "12345")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setBadgeIconType(R.mipmap.ic_launcher)
            .setContentTitle("Nissan")
            .setContentText("Bosch")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Health checkup warning")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        with(NotificationManagerCompat.from(this)) {
            notify(123, builder.build())
        }
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Nissan"
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

}