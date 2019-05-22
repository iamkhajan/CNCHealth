package com.example.mygraphapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import android.speech.tts.TextToSpeech
import android.media.MediaPlayer
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), SensorEventListener, MyMqttClient.onMqttConnection {
    override fun onPressureTopicDataReceived(value: Int) {

    }

    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    private val sensors: Sensor? = null
    private var mChart: LineChart? = null
    private var mTempChart: LineChart? = null
    private var mHumidityChart: LineChart? = null
    private var mVibrationChart: LineChart? = null
    private var thread: Thread? = null
    private var plotData = true
    //var mqttClient: MyMqttClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var mqttClient = MyMqttClient(this, this)

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        val sensors = mSensorManager!!.getSensorList(Sensor.TYPE_ALL)

        for (i in sensors.indices) {
            Log.d(TAG, "onCreate: Sensor " + i + ": " + sensors[i].toString())
        }

        if (mAccelerometer != null) {
            mSensorManager!!.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME)
        }

        mChart = findViewById(R.id.health)
        mVibrationChart = findViewById(R.id.vibration)
        mTempChart = findViewById(R.id.temperature)
        mHumidityChart = findViewById(R.id.humidity)

        initChart(mChart!!)
        initChart(mVibrationChart!!)
        initChart(mTempChart!!)
        initChart(mHumidityChart!!)
        alert_button.setOnClickListener {
            print("play sound here")
            playMedia()
        }
        feedMultiple()

    }

    private fun initChart(lineChart: LineChart) {
        lineChart.description.isEnabled = true
        lineChart.description.text = "Graph"
        // enable touch gestures
        lineChart.setTouchEnabled(true)
        lineChart.isDoubleTapToZoomEnabled = false
        // enable scaling and dragging
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)
        lineChart.setDrawGridBackground(false)
        // if disabled, scaling can be done on x- and y-axis separately
        lineChart.setPinchZoom(true)
        // set an alternative background color
        lineChart.setBackgroundColor(Color.WHITE)
        val data = LineData()
        data.setValueTextColor(Color.WHITE)
        // add empty data
        lineChart.data = data

        // get the legend (only possible after setting data)
        val l = lineChart!!.legend

        // modify the legend ...
        l.form = Legend.LegendForm.LINE
        l.textColor = Color.WHITE

        val xl = lineChart!!.xAxis
        xl.textColor = Color.WHITE
        xl.setDrawGridLines(true)
        xl.setAvoidFirstLastClipping(true)
        xl.isEnabled = true

        val leftAxis = lineChart!!.axisLeft
        leftAxis.textColor = Color.WHITE
        leftAxis.setDrawGridLines(false)
        leftAxis.axisMaximum = 506880f
        leftAxis.axisMinimum = 0f
        leftAxis.setDrawGridLines(true)

        val rightAxis = lineChart!!.axisRight
        rightAxis.isEnabled = false

        lineChart.axisLeft.setDrawGridLines(false)
        lineChart.xAxis.setDrawGridLines(false)
        lineChart.setDrawBorders(false)
    }

    private fun playMedia() {
        val mPlayer: MediaPlayer = MediaPlayer.create(this, R.raw.alarm)
        mPlayer.start()
    }

    private fun addEntry(event: SensorEvent) {
        val data = mChart!!.data
        if (data != null) {
            var set: ILineDataSet? = data.getDataSetByIndex(0)
            if (set == null) {
                set = createSet()
                data.addDataSet(set)
            }
            if (event.values[0] > 4) {
                playMedia()
                createNotification()

            }
            // Log.d("Khajan values are", "" + event.values[0])
            //data.addEntry(new Entry(set.getEntryCount(), (float) (Math.random() * 80) + 10f), 0);
            data.addEntry(Entry(set.entryCount.toFloat(), event.values[0] + 5), 0)
            data.notifyDataChanged()
            // let the chart know it's data has changed
            mChart!!.notifyDataSetChanged()
            // limit the number of visible entries
            mChart!!.setVisibleXRangeMaximum(150f)
            mChart!!.setVisibleYRangeMaximum(30f, YAxis.AxisDependency.LEFT);
            // move to the latest entry
            mChart!!.moveViewToX(data.entryCount.toFloat())
        }
    }

    private fun addEntry(value: Float) {
        val data = mTempChart!!.data
        if (data != null) {
            var set: ILineDataSet? = data.getDataSetByIndex(0)
            if (set == null) {
                set = createSet()
                data.addDataSet(set)
            }
            data.addEntry(Entry(set.entryCount.toFloat(), value), 0)
            data.notifyDataChanged()
            // let the chart know it's data has changed
            mTempChart!!.notifyDataSetChanged()
            // limit the number of visible entries
            mTempChart!!.setVisibleXRangeMaximum(100f)
            mTempChart!!.setVisibleYRangeMaximum(1012f, YAxis.AxisDependency.LEFT);
            // move to the latest entry
            mTempChart!!.moveViewToX(data.entryCount.toFloat())
        }
    }

    private fun createSet(): LineDataSet {
        val set = LineDataSet(null, "Dynamic Data")
        set.axisDependency = YAxis.AxisDependency.LEFT
        set.lineWidth = 2f
        set.color = Color.BLUE
        set.isHighlightEnabled = false
        set.setDrawValues(false)
        set.setDrawCircles(false)
        set.mode = LineDataSet.Mode.CUBIC_BEZIER
        set.cubicIntensity = 0.2f
        return set
    }

    private fun feedMultiple() {
        if (thread != null) {
            thread!!.interrupt()
        }
        thread = Thread(Runnable {
            while (true) {
                plotData = true
                try {
                    Thread.sleep(10)
                } catch (e: InterruptedException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                }
            }
        })

        thread!!.start()
    }

    override fun onPause() {
        super.onPause()
        if (thread != null) {
            thread!!.interrupt()
        }
        mSensorManager!!.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do something here if sensor accuracy changes.
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (plotData) {
            addEntry(event)
            plotData = false
        }
    }


    override fun onResume() {
        super.onResume()
        mSensorManager!!.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onDestroy() {
        mSensorManager!!.unregisterListener(this@MainActivity)
        thread!!.interrupt()
        super.onDestroy()
    }

    companion object {
        private val TAG = "MainActivity"
    }

    fun createNotification() {
        createNotificationChannel()
        var builder = NotificationCompat.Builder(this, "12345")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Nissan")
            .setContentText("Bosch")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Much longer text that cannot fit one line...")
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


    override fun onTempDataReceived(value: Int) {

    }

    override fun onHumidityDataReceived(value: Int) {
    }

    override fun onLightReceived(value: Int) {
        addEntry(value.toFloat())
    }

    override fun onHealthDataReceived(value: Int) {
    }

    override fun onAccelerometerTopicDataReceived(value: Int) {
    }
}
