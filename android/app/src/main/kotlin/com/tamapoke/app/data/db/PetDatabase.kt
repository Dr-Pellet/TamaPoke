package com.tamapoke.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [PetEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class PetDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao

    companion object {
        @Volatile private var instance: PetDatabase? = null

        fun get(context: Context): PetDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                PetDatabase::class.java,
                "tamapoke.db",
            ).build().also { instance = it }
        }
    }
}
