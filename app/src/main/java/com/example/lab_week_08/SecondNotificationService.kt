package com.example.lab_week_08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.os.Handler
import android.os.Looper

class SecondNotificationService : Service() {
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var serviceHandler: Handler
    override fun onBind(intent: Intent): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        //Create the notification with all of its contents and configurations
        //in the startForegroundService() custom function
        notificationBuilder = startForegroundService()
        val handlerThread = HandlerThread("SecondThread")
            .apply { start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    private fun startForegroundService(): NotificationCompat.Builder {
        //Create a pending Intent which is used to be executed
        //when the user clicks the notification
        //A pending Intent is the same as a regular Intent,
        //The difference is that pending Intent will be
        //executed "Later On" and not "Immediately"
        val pendingIntent = getPendingIntent()
        //To make a notification, you should know the keyword 'channel'
        //Notification uses channels that'll be used to
        //set up the required configurations
        val channelId = createNotificationChannel()
        //Combine both the pending Intent and the channel
        //into a notification builder
        //Remember that getNotificationBuilder() is not a built-in function!
        val notificationBuilder = getNotificationBuilder(
            pendingIntent, channelId
        )
        //After all has been set and the notification builder is ready,
        //start the foreground service and the notification
        //will appear on the user's device
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        return notificationBuilder
    }

    private fun getPendingIntent(): PendingIntent {
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            FLAG_IMMUTABLE else 0
        //Here, we're setting MainActivity into the pending Intent
        //When the user clicks the notification, they will be
        //redirected to the Main Activity of the app
        return PendingIntent.getActivity(
            this, 0, Intent(
                this,
                MainActivity::class.java
            ), flag
        )
    }

    private fun createNotificationChannel(): String =
    //Unfortunately notification channel exists only for API 26 and above,
    //therefore we need to check for the SDK version of the device.
        //"Build.VERSION_CODES.O" stands for 'Oreo' which is the API 26 release name
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Create the channel id
            val channelId = "002"
            //Create the channel name
            val channelName = "002 Channel"

            //IMPORTANCE_LOW - silent and doesn't appear as heads-up notification
            val channelPriority = NotificationManager.IMPORTANCE_DEFAULT
            //Build the channel notification based on all 3 previous attributes
            val channel = NotificationChannel(
                channelId,
                channelName,
                channelPriority
            )
            //Get the NotificationManager class
            val service = requireNotNull(
                ContextCompat.getSystemService(this,
                    NotificationManager::class.java)
            )
            //Binds the channel into the NotificationManager
            //NotificationManager will trigger the notification later on
            service.createNotificationChannel(channel)

            //Return the channel id
            channelId
        } else { "" }
    //Build the notification with all of its contents and configurations
    private fun getNotificationBuilder(pendingIntent: PendingIntent, channelId:
    String) =
        NotificationCompat.Builder(this, channelId)
            //Sets the title
            .setContentTitle("Third worker process is done")
            //Sets the content
            .setContentText("Check it out!")
            //Sets the notification icon
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            //Sets the ticker message (brief message on top of your device)
            .setTicker("Third worker process is done, check it out!")
            .setOngoing(true)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        val returnValue = super.onStartCommand(intent,
            flags, startId)
        //Gets the channel id passed from the MainActivity through the Intent
        val Id = intent?.getStringExtra(EXTRA_ID)
            ?: throw IllegalStateException("Channel ID must be provided")
        //Posts the notification task to the handler,
        //which will be executed on a different thread
        serviceHandler.post {
            //Sets up what happens after the notification is posted
            //Here, we're counting down from 10 to 0 in the notification
            countDownFromTenToZero(notificationBuilder)
            //by returning the channel ID through LiveData
            notifyCompletion(Id)
            //Stops the foreground service, which closes the notification
            //but the service still goes on
            stopForeground(STOP_FOREGROUND_REMOVE)
            //Stop and destroy the service
            stopSelf()
        }
        return returnValue
    }

    private fun countDownFromTenToZero(notificationBuilder:
                                       NotificationCompat.Builder) {
        //Gets the notification manager
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as
                NotificationManager
        //Count down from 10 to 0
        for (i in 10 downTo 0) {
            Thread.sleep(1000L)
            //Updates the notification content text
            notificationBuilder.setContentText("$i seconds until last warning")
                .setSilent(true)
            //Notify the notification manager about the content update
            notificationManager.notify(
                NOTIFICATION_ID,
                notificationBuilder.build()
            )
        }
    }

    private fun notifyCompletion(Id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableID.value = Id
        }
    }


    companion object {
        const val NOTIFICATION_ID = 0xCA8
        const val EXTRA_ID = "Id"
        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}
