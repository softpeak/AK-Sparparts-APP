package com.akspareparts.app.data

/** A line on the bill-generation screen (selection + quantity). */
data class BillDraftItem(
    val partNumber: String,
    val unitPrice: Double,
    val selected: Boolean = false,
    val qty: Int = 1
) {
    val lineTotal: Double get() = unitPrice * qty
}
