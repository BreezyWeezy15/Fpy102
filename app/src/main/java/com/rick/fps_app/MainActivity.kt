package com.rick.fps_app

import android.annotation.SuppressLint
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.rick.fps_app.databinding.ActivityMainBinding
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private lateinit var fpsMonitor : FPSMonitor
    private lateinit var binding : ActivityMainBinding
    private val handler = Handler()
    private var runnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        // FPS
        fpsMonitor = FPSMonitor(binding.fpsValues,1000)
        calculateFPSPeriodically()


        // BATTERY
        Handler().postDelayed({},1000)
        runnable = object : Runnable {
            override fun run() {
                fpsMonitor.startMonitoring()
                getBatteryTemperature()
                handler.postDelayed(this, 1000)
            }
        }
        handler.postDelayed(runnable as Runnable, 1000)

    }

    @SuppressLint("SetTextI18n")
    fun getBatteryTemperature() {
        val intent = registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val temperature = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)

        val tempInCelsius = temperature?.div(10.0)
        val tempInFahrenheit = tempInCelsius?.let {
            (it * 9 / 5) + 32
        }

        tempInCelsius?.let {
            val tempCelsius = it.toInt()
            val tempFahrenheit = tempInFahrenheit?.toInt() ?: 0
            binding.batteryValue.text = "$tempCelsius°C / $tempFahrenheit°F"
        } ?: run {
            binding.batteryValue.text = "0°C / 0°F"
        }
    }

    private fun calculateFPSPeriodically() {
        lifecycleScope.launch {
            while (isActive) {
                delay(1000)
                val fps = fpsMonitor.calculateFPS()
                val roundedFps = fps.toInt()
                binding.fpsValues.text = roundedFps.toString()
            }
        }
    }

    override fun onDestroy() {
        fpsMonitor.stopMonitoring()
        super.onDestroy()
    }
}