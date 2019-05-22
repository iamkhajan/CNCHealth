package com.example.mygraphapplication

import android.content.Context
import android.util.Log
import android.widget.Toast
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

class MyMqttClient(var mContext: Context, var mCallback: onMqttConnection) {

    var client: MqttAndroidClient? = null
    var callback: onMqttConnection? = null

    interface onMqttConnection {
        fun onTempDataReceived(value: Int)
        fun onHumidityDataReceived(value: Int)
        fun onLightReceived(value: Int)
        fun onHealthDataReceived(value: Int)
        fun onAccelerometerTopicDataReceived(value: Int)
        fun onPressureTopicDataReceived(value: Int)
    }

    companion object {
        val accelerometerTopic = "nissan/demo/accelerometer"
        val tempretureTopic = "nissan/demo/temperature"
        val humidityTopic = "nissan/demo/humidity"
        val lightTopic = "nissan/demo/light"
        val healthTopic = "nissan/demo/health"
        val vibrationTopic = "nissan/demo/vibration"
        val pressureTopic = "nissan/demo/pressure"
        val TAG = "MyMqttClient"
        val BROKER_URL = "tcp://broker.hivemq.com:1883"
    }

    init {
        callback = mCallback
        client()
    }

    fun client() {
        val clientId = MqttClient.generateClientId()
        client = MqttAndroidClient(
            mContext, BROKER_URL,
            clientId
        )
        val options = MqttConnectOptions()
        options.mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1
        client?.setCallback(MqttCallbackHandler())
        try {
            val token = client?.connect(options)
            token?.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    subsribeMessage(accelerometerTopic)
                    subsribeMessage(lightTopic)
                    subsribeMessage(tempretureTopic)
                    subsribeMessage(humidityTopic)
                    subsribeMessage(pressureTopic)
                    subsribeMessage(healthTopic)
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    print("onFailure")

                }
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun publish(topic: String, payload: String) {
        val message = MqttMessage(payload.toByteArray())
        client?.publish(topic, message)
    }

    fun subsribeMessage(topic: String) {
        val qos = 1
        try {
            val subToken = client?.subscribe(topic, qos)
            if (subToken?.actionCallback == null) {
                return
            }
            subToken?.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    print("subscribed successfully ")
                }

                override fun onFailure(
                    asyncActionToken: IMqttToken,
                    exception: Throwable
                ) {
                    print("Failure in subscribing")

                }
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }

    }

    fun unsubscribeMessage(topic: String) {
        try {
            val unsubToken = client?.unsubscribe(topic)
            if (unsubToken?.actionCallback == null) {
                return
            }
            unsubToken?.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                }

                override fun onFailure(
                    asyncActionToken: IMqttToken,
                    exception: Throwable
                ) {
                }
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }

    }

    inner class MqttCallbackHandler : MqttCallbackExtended {

        override fun connectComplete(b: Boolean, s: String) {
            Log.d(TAG, "connectComplete: $s")
        }

        override fun connectionLost(throwable: Throwable) {
            Log.d(TAG, "connectionLost: ")
        }

        @Throws(Exception::class)
        override fun messageArrived(topic: String, mqttMessage: MqttMessage) {
            //Log.d(TAG, topic + "---" + mqttMessage.toString())
            when (topic) {
                tempretureTopic -> mCallback.onTempDataReceived(mqttMessage.toString().toInt() / 1000)
                humidityTopic -> mCallback.onHumidityDataReceived(mqttMessage.toString().toInt())
                lightTopic -> mCallback.onLightReceived(mqttMessage.toString().toInt() / 1000)
                accelerometerTopic -> mCallback.onAccelerometerTopicDataReceived(mqttMessage.toString().toInt() / 100)
                pressureTopic -> mCallback.onPressureTopicDataReceived(mqttMessage.toString().toInt() / 100)
            }

        }

        override fun deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken) {
            Log.d(TAG, "deliveryComplete: ")
        }
    }
}