package com.akspareparts.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val password: String,
    val fullName: String
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val city: String
)

/** Global parts catalog */
@Entity(tableName = "parts", indices = [Index(value = ["partNumber"], unique = true)])
data class Part(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val partNumber: String,
    val price: Double
)

/** Parts sold to a specific customer */
@Entity(
    tableName = "customer_parts",
    foreignKeys = [ForeignKey(
        entity = Customer::class,
        parentColumns = ["id"],
        childColumns = ["customerId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("customerId")]
)
data class CustomerPart(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val partNumber: String,
    val price: Double
)

@Entity(
    tableName = "bills",
    foreignKeys = [ForeignKey(
        entity = Customer::class,
        parentColumns = ["id"],
        childColumns = ["customerId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("customerId")]
)
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val date: String,
    val totalAmount: Double,
    val pdfPath: String?
)

@Entity(
    tableName = "bill_items",
    foreignKeys = [ForeignKey(
        entity = Bill::class,
        parentColumns = ["id"],
        childColumns = ["billId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("billId")]
)
data class BillItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val billId: Int,
    val partNumber: String,
    val qty: Int,
    val unitPrice: Double,
    val lineTotal: Double
)
