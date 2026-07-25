package com.tamapoke.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Query("SELECT * FROM pet WHERE id = ${PetEntity.SINGLETON_ID}")
    fun observe(): Flow<PetEntity?>

    @Query("SELECT * FROM pet WHERE id = ${PetEntity.SINGLETON_ID}")
    suspend fun getOnce(): PetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PetEntity)
}
