package com.tamapoke.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [PetEntity::class], version = 2, exportSchema = false)
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
            )
                // v1 -> v2 added battle/catch/daily-goal columns; no released users yet,
                // so a destructive wipe on schema change is simpler than writing a migration.
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
        }
    }
}
