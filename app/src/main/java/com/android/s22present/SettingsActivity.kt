package com.android.s22present

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.slider.Slider
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.button.MaterialButton
import java.io.File

class SettingsActivity : AppCompatActivity() {

    private val pickGifLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                copyGifToInternalStorage(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sharedPrefs = getSharedPreferences("s22present_prefs", Context.MODE_PRIVATE)

        val spinnerFont = findViewById<Spinner>(R.id.spinnerfont)
        val sliderTitleScale = findViewById<Slider>(R.id.sliderTitleScale)
        val sliderSubScale = findViewById<Slider>(R.id.sliderSubScale)
        val sliderSpacing = findViewById<Slider>(R.id.sliderSpacing)
        val switchMarquee = findViewById<MaterialSwitch>(R.id.switchMarquee)
        val switchNotifications = findViewById<MaterialSwitch>(R.id.switchNotifications)
        val textGifStatus = findViewById<TextView>(R.id.textGifStatus)

        // Load current values
        val fontset = sharedPrefs.getString("font", "0") ?: "0"
        val titleFontScaleValue = sharedPrefs.getFloat("title_font_scale", 1.0f)
        val subFontScaleValue = sharedPrefs.getFloat("sub_font_scale", 1.0f)
        val lineSpacingValue = sharedPrefs.getFloat("line_spacing", 4.0f)
        val marqueeInfiniteValue = sharedPrefs.getBoolean("marquee_infinite", true)
        val showNotifications = sharedPrefs.getBoolean("show_notifications", true)
        
        spinnerFont.setSelection(fontset.toIntOrNull() ?: 0)
        sliderTitleScale.value = titleFontScaleValue.coerceIn(0.5f, 2.5f)
        sliderSubScale.value = subFontScaleValue.coerceIn(0.5f, 2.5f)
        sliderSpacing.value = lineSpacingValue.coerceIn(-10.0f, 20.0f)
        switchMarquee.isChecked = marqueeInfiniteValue
        switchNotifications.isChecked = showNotifications

        updateGifStatusText(textGifStatus)

        findViewById<MaterialButton>(R.id.buttonPickGif).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            pickGifLauncher.launch(intent)
        }

        findViewById<MaterialButton>(R.id.buttonClearGif).setOnClickListener {
            val destFile = File(filesDir, "custom_background.gif")
            if (destFile.exists()) destFile.delete()
            sharedPrefs.edit().remove("custom_gif_path").apply()
            Toast.makeText(this, "Media Cleared", Toast.LENGTH_SHORT).show()
            updateGifStatusText(textGifStatus)
        }

        findViewById<MaterialButton>(R.id.buttonReset).setOnClickListener {
            spinnerFont.setSelection(0)
            sliderTitleScale.value = 1.0f
            sliderSubScale.value = 1.0f
            sliderSpacing.value = 4.0f
            switchMarquee.isChecked = true
            switchNotifications.isChecked = true
        }

        findViewById<MaterialButton>(R.id.buttonsave).setOnClickListener {
            sharedPrefs.edit().apply {
                putString("font", spinnerFont.selectedItemPosition.toString())
                putFloat("title_font_scale", sliderTitleScale.value)
                putFloat("sub_font_scale", sliderSubScale.value)
                putFloat("line_spacing", sliderSpacing.value)
                putBoolean("marquee_infinite", switchMarquee.isChecked)
                putBoolean("show_notifications", switchNotifications.isChecked)
            }.apply()
            
            // Restart Listener Service
            val serviceintent = Intent(this, ListenerService::class.java)
            stopService(serviceintent)
            startService(serviceintent)
            Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun copyGifToInternalStorage(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val destFile = File(filesDir, "custom_background.gif")
                destFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                val sharedPrefs = getSharedPreferences("s22present_prefs", Context.MODE_PRIVATE)
                sharedPrefs.edit().putString("custom_gif_path", destFile.absolutePath).apply()
                Toast.makeText(this, "Media Selected Successfully", Toast.LENGTH_SHORT).show()
                updateGifStatusText(findViewById(R.id.textGifStatus))
            }
        } catch (e: Exception) {
            Log.e("S22PresSetting", "Error copying media", e)
            Toast.makeText(this, "Failed to load media", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateGifStatusText(textView: TextView) {
        val sharedPrefs = getSharedPreferences("s22present_prefs", Context.MODE_PRIVATE)
        val path = sharedPrefs.getString("custom_gif_path", "")
        if (path.isNullOrEmpty() || !File(path).exists()) {
            textView.text = "Custom Media: None"
        } else {
            textView.text = "Custom Media: Active"
        }
    }
}
