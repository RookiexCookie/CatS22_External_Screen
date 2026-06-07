package com.android.s22present

import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Typeface
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        window?.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
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

    private fun updateUI() {
        if (Globals.musicPlaying) {
            // Walkman Mode
            textClock.visibility = View.GONE
            textCenter.visibility = View.VISIBLE
            
            textCenter.text = Globals.musicTitle.ifEmpty { "Playing Music" }
            textSub.text = Globals.musicArtist
            textSub.visibility = if (Globals.musicArtist.isNotEmpty()) View.VISIBLE else View.GONE

            if (Globals.musicArtwork != null) {
                imageViewBackground.setImageBitmap(Globals.musicArtwork)
                gifDrawable?.stop()
                
                // Dynamic Palette
                Palette.from(Globals.musicArtwork!!).generate { palette ->
                    if (palette != null) {
                        val vibrant = palette.getVibrantColor(Color.WHITE)
                        val lightVibrant = palette.getLightVibrantColor(Color.WHITE)
                        val dominant = palette.getDominantColor(Color.BLACK)
                        
                        // Decide on text color based on background luminance
                        val isDark = isColorDark(dominant)
                        val primaryTextColor = if (isDark) Color.WHITE else Color.BLACK
                        val secondaryTextColor = if (isDark) lightVibrant else palette.getDarkVibrantColor(Color.DKGRAY)

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
                    val drawable = ImageDecoder.decodeDrawable(source)
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
        val digifont = context.resources.getFont(R.font.digital7)
        val pixelfont = context.resources.getFont(R.font.dogica)
        val orbitronfont = context.resources.getFont(R.font.orbitron)
        val vt323font = context.resources.getFont(R.font.vt323)

        val selectedFont = when (Globals.font) {
            "1" -> digifont
            "2" -> pixelfont
            "3" -> orbitronfont
            "4" -> vt323font
            else -> Typeface.DEFAULT
        }

        textClock.typeface = selectedFont
        textCenter.typeface = selectedFont
        textSub.typeface = selectedFont
    }

    private fun isColorDark(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }
}
