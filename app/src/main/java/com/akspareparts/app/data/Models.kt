package com.akspareparts.app.data

/** A part extracted from an image by the Claude vision API, awaiting user review. */
data class ExtractedPart(
    val partNumber: String,
    val price: Double
)

/** A line on the bill-generation screen (selection + quantity). */
data class BillDraftItem(
    val partNumber: String,
    val unitPrice: Double,
    val selected: Boolean = false,
    val qty: Int = 1
) {
    val lineTotal: Double get() = unitPrice * qty
}
