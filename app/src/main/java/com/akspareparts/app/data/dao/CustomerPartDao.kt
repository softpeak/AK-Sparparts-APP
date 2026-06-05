package com.akspareparts.app.data.dao

import androidx.room.Dao
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

    @Insert
    suspend fun insert(part: CustomerPart): Long

    @Insert
    suspend fun insertAll(parts: List<CustomerPart>)

    @Update
    suspend fun update(part: CustomerPart)
}
