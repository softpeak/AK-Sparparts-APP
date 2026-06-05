package com.akspareparts.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.akspareparts.app.data.CustomerPart
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerPartDao {
    @Query("SELECT * FROM customer_parts WHERE customerId = :customerId ORDER BY id DESC")
    fun getForCustomer(customerId: Int): Flow<List<CustomerPart>>

    @Query("""SELECT * FROM customer_parts WHERE customerId = :customerId
              AND partNumber LIKE '%' || :query || '%' ORDER BY id DESC""")
    fun searchForCustomer(customerId: Int, query: String): Flow<List<CustomerPart>>

    /** How many times this customer already has the given part number. */
    @Query("""SELECT COUNT(*) FROM customer_parts
              WHERE customerId = :customerId AND partNumber = :partNumber""")
    suspend fun countForCustomer(customerId: Int, partNumber: String): Int

    @Insert
    suspend fun insert(part: CustomerPart): Long

    @Insert
    suspend fun insertAll(parts: List<CustomerPart>)

    @Update
    suspend fun update(part: CustomerPart)

    @Delete
    suspend fun delete(part: CustomerPart)
}
