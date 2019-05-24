package com.example.mygraphapplication

import android.content.Context
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import kotlin.math.absoluteValue
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.IMqttActionListener


class MyMqttClient(var mContext: Context, var mCallback: onMqttConnection) {

    var mqttClient: MqttAndroidClient? = null
//    var callback: onMqttConnection? = null

    interface onMqttConnection {
        fun onTempDataReceived(value: Int)
        fun onHumidityDataReceived(value: Int)
        fun onLightReceived(value: Int)
        fun onHealthDataReceived(value: Int)
        fun onAccelerometerTopicDataReceived(value: Int)
        fun onPressureTopicDataReceived(value: Int)

        fun onConnectionSuccess(status: Boolean)
        fun onConnectionLost(message: String)
    }

    companion object {
        val accelerometerTopic = "nissan/demo/accelerometer"
        val tempretureTopic = "nissan/demo/temperature"
        val humidityTopic = "nissan/demo/humidity"
        val healthTopic = "nissan/demo/health"
        val pressureTopic = "nissan/demo/pressure"
        val TAG = "MyMqttClient"
        val BROKER_URL = "tcp://broker.hivemq.com:1883"
    }

    init {
        connectMqttclient()
    }

    fun mqttDisconnect() {
        try {
            var disconToken = mqttClient?.disconnect()
            if (disconToken == null)
                return
            disconToken.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    // we are now successfully disconnected
                }

                override fun onFailure(
                    asyncActionToken: IMqttToken,
                    exception: Throwable
                ) {
                }
            }
        } catch (e: MqttException) {

        }

    }

    fun connectMqttclient() {
        val clientId = MqttClient.generateClientId()
        mqttClient = MqttAndroidClient(
            mContext, BROKER_URL,
            clientId
        )
        val options = MqttConnectOptions()
        options.keepAliveInterval = 10
        options.mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1
        mqttClient?.setCallback(MqttCallbackHandler())
        try {
            val token = mqttClient?.connect(options)
            token?.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    subscribeAllTopics()
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {

                }
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun publish(topic: String, payload: String) {
        val message = MqttMessage(payload.toByteArray())
        mqttClient?.publish(topic, message)
    }

    fun subscribeAllTopics() {
        subsribeMessage(healthTopic)
        subsribeMessage(tempretureTopic)
        subsribeMessage(accelerometerTopic)
        subsribeMessage(humidityTopic)
        subsribeMessage(tempretureTopic)
    }

    fun disconnectCNCMqtt() {
        unSubscribeAllTopics()
        //mqttDisconnect()
        mqttClient = null
    }


    fun unSubscribeAllTopics() {
        unsubscribeMessage(healthTopic)
        unsubscribeMessage(tempretureTopic)
        unsubscribeMessage(accelerometerTopic)
        unsubscribeMessage(humidityTopic)
        unsubscribeMessage(tempretureTopic)
    }

    fun subsribeMessage(topic: String) {
        val qos = 1
        try {
            val subToken = mqttClient?.subscribe(topic, qos)
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
            val unsubToken = mqttClient?.unsubscribe(topic)
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
            mCallback.onConnectionSuccess(b)
        }

        override fun connectionLost(throwable: Throwable) {
            Log.d(TAG, "connectionLost: ")
            mCallback.onConnectionLost(throwable.message!!)
        }

        @Throws(Exception::class)
        override fun messageArrived(topic: String, mqttMessage: MqttMessage) {

            when (topic) {
                tempretureTopic -> mCallback?.onTempDataReceived(mqttMessage.toString().toFloat().absoluteValue.toInt())
                humidityTopic -> mCallback?.onHumidityDataReceived(mqttMessage.toString().toInt())
                accelerometerTopic -> mCallback?.onAccelerometerTopicDataReceived(mqttMessage.toString().toInt())
                pressureTopic -> mCallback?.onPressureTopicDataReceived(mqttMessage.toString().toInt())
                healthTopic -> mCallback?.onHealthDataReceived(mqttMessage.toString().toInt())
            }

        }

        override fun deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken) {
            Log.d(TAG, "deliveryComplete: ")
        }
    }
}