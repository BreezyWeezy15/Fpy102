package com.rick.fps_app

import android.annotation.SuppressLint
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
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
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException

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
        fpsMonitor = FPSMonitor(1000)
        calculateFPSPeriodically()


        // BATTERY
        Handler().postDelayed({},1000)
        runnable = object : Runnable {
            override fun run() {
                fpsMonitor.startMonitoring()
                getBatteryTemperature()
                try {
                    binding.cpuTempValue.text = buildString {
                        append(getCpuTemperature())
                        append(" °C")
                    }
                    binding.gpuTempValue.text = buildString {
                        append(getGpuTemperature())
                        append(" °C")
                    }
                    binding.gpuFanValue.text  = buildString {
                        append(getGpuFanSpeed())
                        append(" RPM")
                    }
                } catch (e : Exception){
                    Log.d("TAG","Exception " + e.message)
                }
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

    private fun listThermalZones(): List<String> {
        val zones = mutableListOf<String>()
        try {
            val dir = File("/sys/class/thermal/")
            if (dir.exists()) {
                val files = dir.listFiles()
                files?.forEach { file ->
                    if (file.isDirectory) {
                        zones.add(file.name)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return zones
    }

    fun getGpuTemperature(): String? {
        return try {
            val zones = listThermalZones()
            val gpuZone = zones.find { it.contains("gpu", ignoreCase = true) } ?: zones.firstOrNull()

            gpuZone?.let {
                val reader = BufferedReader(FileReader("/sys/class/thermal/$it/temp"))
                val tempStr = reader.readLine()
                reader.close()

                val tempInCelsius = tempStr?.toFloatOrNull()?.div(1000)
                if (tempInCelsius != null) {
                    val tempInFahrenheit = tempInCelsius * 9 / 5 + 32
                    "${tempInCelsius}°C / ${tempInFahrenheit}°F"
                } else {
                    null
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun getCpuTemperature(): String? {
        return try {
            val reader = BufferedReader(FileReader("/sys/class/thermal/thermal_zone0/temp"))
            val tempStr = reader.readLine()
            reader.close()

            val tempInCelsius = tempStr?.toFloatOrNull()?.div(1000)
            if (tempInCelsius != null) {
                val tempInFahrenheit = tempInCelsius * 9 / 5 + 32
                "${tempInCelsius}°C / ${tempInFahrenheit}°F"
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun getGpuFanSpeed(): String? {
        try {
            val zones = listThermalZones()
            println("Available Zones: $zones")
            val fanZone = zones.find { it.contains("fan", ignoreCase = true) }
            fanZone?.let {
                val fanSpeedFile = File("/sys/class/thermal/$it/fan_speed")
                if (fanSpeedFile.exists()) {
                    val reader = BufferedReader(FileReader(fanSpeedFile))
                    val fanSpeed = reader.readLine()
                    reader.close()
                    return fanSpeed
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return "Fan speed not available"
    }

    override fun onDestroy() {
        fpsMonitor.stopMonitoring()
        super.onDestroy()
    }

}