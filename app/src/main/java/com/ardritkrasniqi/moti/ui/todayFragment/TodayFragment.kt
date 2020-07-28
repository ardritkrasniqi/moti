package com.ardritkrasniqi.moti.ui.todayFragment

import android.content.Context.SENSOR_SERVICE
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_ACCELEROMETER
import android.hardware.Sensor.TYPE_MAGNETIC_FIELD
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.SENSOR_DELAY_GAME
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation.RELATIVE_TO_SELF
import android.view.animation.RotateAnimation
import androidx.core.content.ContextCompat
import androidx.core.util.rangeTo
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.ardritkrasniqi.moti.MainActivity
import com.ardritkrasniqi.moti.R
import com.ardritkrasniqi.moti.databinding.TodayFragmentBinding
import com.ardritkrasniqi.moti.ui.mainFragment.MainFragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.LineData
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.main_fragment.*
import java.lang.Math.toDegrees

/*
Brewed with love by Ardrit Krasniqi 2020
 */

class TodayFragment : Fragment(), SensorEventListener {

    lateinit var binding: TodayFragmentBinding
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var magnetometer: Sensor

    var currentDegree = 0.0f
    var lastAccelerometer = FloatArray(3)
    var lastMagnetometer = FloatArray(3)
    var lastAccelerometerSet = false
    var lastMagnetometerSet = false

    companion object {
        fun newInstance() = TodayFragment()
    }


    // viewmodeli initializohet me lazy qe do te thote kur te krijohet aktiviteti i cili e hoston fragmentin
    private val viewModel: TodayViewModel by lazy {
        val activity = requireNotNull(this.activity) {}
        ViewModelProvider(
            this,
            TodayViewModel.Factory(activity.application)
        ).get(TodayViewModel::class.java)
    }

    private lateinit var tempChart: LineChart


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = TodayFragmentBinding.inflate(inflater)
        // Allows Data Binding to Observe LiveData with the lifecycle of this Fragment
        // Lejon DataBinding qe te observoje liveDatan me (jeten) e ketij fragmneti
        binding.lifecycleOwner = this
        // Giving the binding access to the viewmodel
        // I jep akces bindit qe te perdore viewmodelin
        binding.viewModel = viewModel
        // instantiating sensors
        sensorManager = (requireActivity().getSystemService(SENSOR_SERVICE) as SensorManager?)!!
        accelerometer = sensorManager.getDefaultSensor(TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(TYPE_MAGNETIC_FIELD)
        // instantiates lineData from viewModel
        val lineData: LineData = viewModel.getData(5, 5f, requireContext())
        viewModel.setupChart(
            binding.tempChart,
            lineData,
            ContextCompat.getColor(requireContext(), R.color.whiteColor)
        )
        tempChart = binding.tempChart

        // Sets the icon in weatherDescriptionIcon
        viewModel.weather.observe(viewLifecycleOwner, Observer {
            binding.weatherConditionIcon.setImageResource(
                when(viewModel.weather.value?.weatherList?.get(0)?.weatherId){
                    200,299 -> R.drawable.ic_thunderstorm_colored
                    300,399 -> R.drawable.ic_drizzle_colored_3xx
                    500, 599 -> R.drawable.ic_lightrain_colored
                    600, 699 -> R.drawable.ic_snow_colored
                    700, 799 -> R.drawable.ic_cloud_mist_7xx
                    800 -> R.drawable.ic_sunny_colored
                    801,802 -> R.drawable.ic_fewclouds_colored_801
                    803, 900 -> R.drawable.ic_moreclouds_colored
                    else -> R.drawable.ic_sunny_colored
                }
            )
        })


        binding.selectCity.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_cities)
        }

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>("key")?.observe(
            viewLifecycleOwner) { result ->
            viewModel.getWeather(result)
        }
        return binding.root
    }



    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //nuk na duhet
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor === accelerometer) {
            lowPass(event.values, lastAccelerometer)
            lastAccelerometerSet = true
        } else if (event?.sensor === magnetometer) {
            lowPass(event.values, lastMagnetometer)
            lastMagnetometerSet = true
        }

        if (lastAccelerometerSet && lastMagnetometerSet) {
            val r = FloatArray(9)
            if (SensorManager.getRotationMatrix(r, null, lastAccelerometer, lastMagnetometer)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(r, orientation)
                val degree = (toDegrees(orientation[0].toDouble()) + 360).toFloat() % 360

                val rotateAnimation = RotateAnimation(
                    currentDegree,
                    -degree,
                    RELATIVE_TO_SELF, 0.5f,
                    RELATIVE_TO_SELF, 0.5f
                )
                rotateAnimation.duration = 1000
                rotateAnimation.fillAfter = true

                binding.compassImage.startAnimation(rotateAnimation)
                currentDegree = -degree
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, magnetometer, SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this, accelerometer)
        sensorManager.unregisterListener(this, magnetometer)
    }

    private fun lowPass(input: FloatArray, output: FloatArray) {
        val alpha = 0.05f

        for (i in input.indices) {
            output[i] = output[i] + alpha * (input[i] - output[i])
        }
    }

}
