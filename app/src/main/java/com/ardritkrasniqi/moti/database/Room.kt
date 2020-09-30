package com.ardritkrasniqi.moti.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.TypeConverters
import kotlinx.coroutines.selects.select

@Dao
interface WeatherDao {

    @Query("select * from WeatherEntity")
    fun getWeather(): LiveData<WeatherEntity>

    @Query("select * from weatherentity")
    fun getWeatherList(): LiveData<List<WeatherEntity>>

    @Query("select * from weatherentity where lat=:lat and lon=:lon")
    fun getWeatherByCityId(lat: Double, lon: Double): LiveData<WeatherEntity?>

    @Query("select * from weatherentity where name=:cityName")
    fun getWeatherByCityIdName(cityName: String): LiveData<WeatherEntity?>

    @Query("select name from weatherentity")
    fun getCityNames(): LiveData<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWeather(weather: WeatherEntity)

    @Query("DELETE from weatherentity where name=:cityName")
    fun deleteCity(cityName: String)
}

@Database(
    entities = [WeatherEntity::class],
    version = 8, exportSchema = false
)
@TypeConverters(value = [WeatherConverter::class])
abstract class WeatherDatabase : RoomDatabase() {
    abstract val WeatherDao: WeatherDao
}

/**
 * Creates a lateinit variable called Instance to hold the singleton object, The weather database should be singleton
so only one instance of it should exist due to expensive and timeconsuming initializations
Creates a function to initialize INSTANCE of database inside the synchriised block
ip::::: the .isInitialized Kotlin property returns true if the lateinit property is assigned a value othervise it returns false

 2020 Ardrit Krasniqi
 */

private lateinit var INSTANCE: WeatherDatabase

fun getDatabase(context: Context): WeatherDatabase {
    synchronized(WeatherDatabase::class.java) {
        if (!::INSTANCE.isInitialized) {
            INSTANCE = Room.databaseBuilder(
                context.applicationContext,
                WeatherDatabase::class.java,
                "weatherAll").fallbackToDestructiveMigration().build()
        }
    }
    return INSTANCE
}