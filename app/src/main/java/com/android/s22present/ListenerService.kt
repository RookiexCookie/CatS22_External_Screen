package com.android.s22present

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class ListenerService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("S22PresListServInit", "Hello!")
        val displaymanager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display1 = displaymanager.displays.getOrNull(1)

        val sharedPrefs = getSharedPreferences("s22present_prefs", Context.MODE_PRIVATE)
        Globals.style = sharedPrefs.getString("style", "0") ?: "0"
        Globals.font = sharedPrefs.getString("font", "0") ?: "0"
        Globals.customGifPath = sharedPrefs.getString("custom_gif_path", "") ?: ""
        Globals.showNotifications = sharedPrefs.getBoolean("show_notifications", true)

        if (display1 != null) {
            val present = PresentationHandler(this, display1)
            present.show()
            try {
                Globals.loading.progress = 3
                Globals.loadingtext.text = "Done!"
            } catch (e: Exception) {
                // Ignore if not initialized
            }
        } else {
            Log.w("S22PresListServInit", "Display 1 not found!")
        }

        Log.i("S22PresListServInit", "Listening...")
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

class NotificationService : NotificationListenerService() {
    
    private val activeNotifications = mutableMapOf<String, String>()

    override fun onCreate() {
        Log.i("S22PresNotifServInit", "Listening for notifications...")
        super.onCreate()
    }

    private fun processMediaPlayback(sbn: StatusBarNotification): Boolean {
        val extras = sbn.notification.extras
        val template = extras.getString(Notification.EXTRA_TEMPLATE)
        val token = extras.getParcelable<MediaSession.Token>(Notification.EXTRA_MEDIA_SESSION)
        
        val isMedia = template?.contains("MediaStyle") == true || token != null
        var isPlaying = false

        if (token != null) {
            try {
                val controller = MediaController(this, token)
                val state = controller.playbackState
                if (state != null && state.state == PlaybackState.STATE_PLAYING) {
                    isPlaying = true
                    
                    val metadata = controller.metadata
                    Globals.musicTitle = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: extras.getString("android.title") ?: ""
                    Globals.musicArtist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: extras.getString("android.text") ?: ""
                    Globals.musicArtwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) 
                        ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                }
            } catch (e: Exception) {
                Log.e("S22PresNotifServ", "Error checking media state", e)
            }
        } else if (isMedia) {
            // Fallback for media players that don't correctly pass the MediaSession token
            isPlaying = true
            Globals.musicTitle = extras.getString("android.title") ?: ""
            Globals.musicArtist = extras.getString("android.text") ?: ""
            Globals.musicArtwork = null // Can't easily get artwork without a token
        }

        return isPlaying
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString("android.title")
        val text = extras.getString("android.text")

        // Handle Media
        if (processMediaPlayback(sbn)) {
            Globals.musicPlaying = true
            Globals.onStateChanged?.invoke()
            return
        }

        // Handle Normal Notifications
        if (!title.isNullOrEmpty() || !text.isNullOrEmpty()) {
            val notifText = "${title ?: ""}: ${text ?: ""}"
            activeNotifications[sbn.key] = notifText
            
            // Set current notification to the latest one
            Globals.currentNotification = notifText
            Globals.onStateChanged?.invoke()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // If the removed notification was the media player, stop music mode
        val extras = sbn.notification.extras
        val token = extras.getParcelable<MediaSession.Token>(Notification.EXTRA_MEDIA_SESSION)
        val template = extras.getString(Notification.EXTRA_TEMPLATE)
        if (template?.contains("MediaStyle") == true || token != null) {
            Globals.musicPlaying = false
            Globals.musicArtwork = null
            Globals.onStateChanged?.invoke()
        }

        // Remove from our active notifications queue
        if (activeNotifications.containsKey(sbn.key)) {
            activeNotifications.remove(sbn.key)
            // Update to the next active notification, or clear if none
            Globals.currentNotification = activeNotifications.values.lastOrNull() ?: ""
            Globals.onStateChanged?.invoke()
        }
    }
}
