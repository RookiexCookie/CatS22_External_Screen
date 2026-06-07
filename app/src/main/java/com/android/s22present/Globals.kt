package com.android.s22present

import android.annotation.SuppressLint
import android.widget.ProgressBar
import android.widget.TextView

import android.graphics.Bitmap

// This class stores global variables.
class Globals
{
    @SuppressLint("StaticFieldLeak")
    companion object
    {
        // UI Element storage.
        lateinit var loading : ProgressBar
        lateinit var loadingtext : TextView
        
        var fontSizeScale = 1.0f
        var font = "0"
        var customGifPath = ""
        var showNotifications = true

        // Media & Notification State
        var musicPlaying = false
        var musicTitle = ""
        var musicArtist = ""
        var musicArtwork: Bitmap? = null
        var currentNotification = ""
        
        // Callback to notify PresentationHandler of changes
        var onStateChanged: (() -> Unit)? = null
    }
}