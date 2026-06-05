package com.akspareparts.app.data

import com.akspareparts.app.data.dao.BillDao
import com.akspareparts.app.data.dao.CustomerDao
import com.akspareparts.app.data.dao.CustomerPartDao
import com.akspareparts.app.data.dao.PartDao
import com.akspareparts.app.data.dao.UserDao
import kotlinx.coroutines.flow.Flow

class Repository(
    private val userDao: UserDao,
    private val customerDao: CustomerDao,
    private val partDao: PartDao,
    private val customerPartDao: CustomerPartDao,
    private val billDao: BillDao
) {
    // Auth
    suspend fun login(username: String, password: String): User? = userDao.login(username, password)

    // Customers
    fun customers(): Flow<List<Customer>> = customerDao.getAll()
    fun customer(id: Int): Flow<Customer?> = customerDao.getById(id)
    suspend fun addCustomer(name: String, city: String): Int =
        customerDao.insert(Customer(name = name, city = city)).toInt()

    // Global parts catalog
    fun allParts(): Flow<List<Part>> = partDao.getAll()
    fun searchParts(query: String): Flow<List<Part>> = partDao.search(query)
    suspend fun suggestParts(prefix: String): List<Part> =
        if (prefix.isBlank()) emptyList() else partDao.suggest(prefix)
    suspend fun addGlobalPart(partNumber: String, price: Double) =
        partDao.insert(Part(partNumber = partNumber, price = price))

    // Customer parts
    fun customerParts(customerId: Int): Flow<List<CustomerPart>> =
        customerPartDao.getForCustomer(customerId)
    fun searchCustomerParts(customerId: Int, query: String): Flow<List<CustomerPart>> =
        customerPartDao.searchForCustomer(customerId, query)

    /**
     * Saves a part to the customer AND mirrors it into the global catalog.
     * Returns false (and changes nothing) if the customer already has this part number.
     */
    suspend fun addCustomerPart(customerId: Int, partNumber: String, price: Double): Boolean {
        val pn = partNumber.trim()
        if (pn.isEmpty()) return false
        if (customerPartDao.countForCustomer(customerId, pn) > 0) return false
        customerPartDao.insert(CustomerPart(customerId = customerId, partNumber = pn, price = price))
        partDao.insert(Part(partNumber = pn, price = price))
        return true
    }

    suspend fun updateCustomerPart(part: CustomerPart) = customerPartDao.update(part)
    suspend fun deleteCustomerPart(part: CustomerPart) = customerPartDao.delete(part)

    // Bills
    fun bills(customerId: Int): Flow<List<Bill>> = billDao.getForCustomer(customerId)
    suspend fun getBill(billId: Int): Bill? = billDao.getBill(billId)
    suspend fun getBillItems(billId: Int): List<BillItem> = billDao.getItems(billId)
    suspend fun saveBill(bill: Bill, items: List<BillItem>): Int =
        billDao.saveBillWithItems(bill, items)
}
