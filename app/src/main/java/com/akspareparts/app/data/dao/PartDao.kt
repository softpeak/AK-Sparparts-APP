package com.akspareparts.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akspareparts.app.data.Part
import kotlinx.coroutines.flow.Flow

@Dao
interface PartDao {
    @Query("SELECT * FROM parts ORDER BY partNumber ASC")
    fun getAll(): Flow<List<Part>>

    @Query("SELECT * FROM parts WHERE partNumber LIKE '%' || :query || '%' ORDER BY partNumber ASC")
    fun search(query: String): Flow<List<Part>>

    @Query("SELECT * FROM parts WHERE partNumber LIKE :prefix || '%' ORDER BY partNumber ASC LIMIT 8")
    suspend fun suggest(prefix: String): List<Part>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(part: Part)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(parts: List<Part>)

    @Query("SELECT COUNT(*) FROM parts")
    suspend fun count(): Int
}
