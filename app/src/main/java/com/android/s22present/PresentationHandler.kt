package com.android.s22present

import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Typeface
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import androidx.palette.graphics.Palette
import java.io.File

class PresentationHandler(context: Context, display: Display?) : Presentation(context, display) {

    private lateinit var textClock: TextClock
    private lateinit var textCenter: TextView
    private lateinit var textSub: TextView
    private lateinit var imageViewBackground: ImageView

    private var gifDrawable: AnimatedImageDrawable? = null
    private val handler = Handler(Looper.getMainLooper())
    private val stopGifRunnable = Runnable { gifDrawable?.stop() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Window flags intentionally removed to allow the display to sleep
        setContentView(R.layout.presentation)

        textClock = findViewById(R.id.textClock)
        textCenter = findViewById(R.id.textCenter)
        textSub = findViewById(R.id.textSub)
        imageViewBackground = findViewById(R.id.imageViewBackground)

        applySelectedFont()

        Globals.onStateChanged = {
            findViewById<View>(android.R.id.content).post {
                updateUI()
            }
        }

        updateUI()
        Log.i("S22PresHandlerInit", "Presentation displayed")
    }

    override fun onStart() {
        super.onStart()
        // Restart GIF animation when screen turns on
        gifDrawable?.start()
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(stopGifRunnable)
    }

    private fun updateUI() {
        if (Globals.musicPlaying) {
            // Walkman Mode
            textClock.visibility = View.GONE
            textCenter.visibility = View.VISIBLE
            
            // Apply infinite scrolling toggle
            val repeatLimit = if (Globals.marqueeInfinite) -1 else 1
            textCenter.marqueeRepeatLimit = repeatLimit
            textSub.marqueeRepeatLimit = repeatLimit
            
            // Enable marquee
            textCenter.isSelected = true
            textSub.isSelected = true
            
            textCenter.text = Globals.musicTitle.ifEmpty { "Playing Music" }
            textSub.text = Globals.musicArtist
            textSub.visibility = if (Globals.musicArtist.isNotEmpty()) View.VISIBLE else View.GONE

            if (Globals.musicArtwork != null) {
                imageViewBackground.setImageBitmap(Globals.musicArtwork)
                gifDrawable?.stop()
                
                // Dynamic Palette
                Palette.from(Globals.musicArtwork!!).generate { palette ->
                    if (palette != null) {
                        val primaryTextColor = palette.getLightVibrantColor(Color.WHITE)
                        val secondaryTextColor = palette.getLightMutedColor(Color.LTGRAY)

                        textCenter.setTextColor(primaryTextColor)
                        textSub.setTextColor(secondaryTextColor)
                    }
                }
            } else {
                loadDefaultBackground()
                textCenter.setTextColor(Color.WHITE)
                textSub.setTextColor(Color.LTGRAY)
            }
        } else {
            // Normal Mode
            textClock.visibility = View.VISIBLE
            textCenter.visibility = View.GONE
            
            // Disable marquee to allow screen sleep
            textCenter.isSelected = false
            textSub.isSelected = false
            
            if (Globals.showNotifications && Globals.currentNotification.isNotEmpty()) {
                textSub.text = Globals.currentNotification
                textSub.visibility = View.VISIBLE
            } else {
                textSub.visibility = View.GONE
            }

            loadDefaultBackground()
            textClock.setTextColor(Color.WHITE)
            textSub.setTextColor(Color.LTGRAY)
        }
    }

    private fun loadDefaultBackground() {
        if (Globals.customGifPath.isNotEmpty()) {
            val gifFile = File(Globals.customGifPath)
            if (gifFile.exists()) {
                try {
                    val source = ImageDecoder.createSource(gifFile)
                    val drawable = ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
                        // Optimize GIF RAM Usage by downsampling to screen resolution (max 320px)
                        val maxDim = 320
                        val size = info.size
                        if (size.width > maxDim || size.height > maxDim) {
                            val scale = java.lang.Math.min(maxDim.toFloat() / size.width, maxDim.toFloat() / size.height)
                            val newWidth = (size.width * scale).toInt()
                            val newHeight = (size.height * scale).toInt()
                            decoder.setTargetSize(newWidth, newHeight)
                        }
                    }
                    imageViewBackground.setImageDrawable(drawable)
                    if (drawable is AnimatedImageDrawable) {
                        gifDrawable = drawable
                        drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                        drawable.start()
                    }
                    return
                } catch (e: Exception) {
                    Log.e("S22PresHandler", "Error loading custom GIF", e)
                }
            }
        }
        // Fallback
        imageViewBackground.setImageResource(R.drawable.moto_razr_v3_wallpaper_by_willcomb_d12rdmr_fullview)
        gifDrawable?.stop()
        gifDrawable = null
    }

    private fun applySelectedFont() {
        val vt323font = context.resources.getFont(R.font.vt323)

        val selectedFont = when (Globals.font) {
            "1" -> vt323font
            else -> Typeface.DEFAULT
        }

        textClock.typeface = selectedFont
        textCenter.typeface = selectedFont
        textSub.typeface = selectedFont
        
        // Convert dp to px for the margin offset
        val displayMetrics = context.resources.displayMetrics
        val spacingPx = (Globals.lineSpacing * displayMetrics.density).toInt()
        
        val layoutParams = textSub.layoutParams as LinearLayout.LayoutParams
        layoutParams.setMargins(layoutParams.leftMargin, spacingPx, layoutParams.rightMargin, layoutParams.bottomMargin)
        textSub.layoutParams = layoutParams

        textClock.textSize = 22f * Globals.titleFontScale
        textCenter.textSize = 22f * Globals.titleFontScale
        textSub.textSize = 14f * Globals.subFontScale
    }
}
