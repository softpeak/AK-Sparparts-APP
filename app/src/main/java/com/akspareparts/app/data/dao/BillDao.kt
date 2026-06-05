package com.akspareparts.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.akspareparts.app.data.Bill
import com.akspareparts.app.data.BillItem
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Query("SELECT * FROM bills WHERE customerId = :customerId ORDER BY id DESC")
    fun getForCustomer(customerId: Int): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE id = :billId LIMIT 1")
    suspend fun getBill(billId: Int): Bill?

    @Query("SELECT * FROM bill_items WHERE billId = :billId")
    suspend fun getItems(billId: Int): List<BillItem>

    @Insert
    suspend fun insertBill(bill: Bill): Long

    @Insert
    suspend fun insertItems(items: List<BillItem>)

    @Transaction
    suspend fun saveBillWithItems(bill: Bill, items: List<BillItem>): Int {
        val billId = insertBill(bill).toInt()
        insertItems(items.map { it.copy(billId = billId) })
        return billId
    }
}
