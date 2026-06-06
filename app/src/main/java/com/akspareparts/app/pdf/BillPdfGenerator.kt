package com.akspareparts.app.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import android.net.Uri
import com.akspareparts.app.data.BillItem
import java.io.File
import java.io.FileOutputStream

/**
 * Renders a styled bill to a PDF using the native PdfDocument API (no external deps).
 * Page size: A4 at 72dpi (595 x 842 pt).
 */
object BillPdfGenerator {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 40f
    private val BLUE = Color.parseColor("#1565C0")

    data class BillData(
        val customerName: String,
        val city: String,
        val date: String,
        val items: List<BillItem>,
        val grandTotal: Double,
        val deliveryCharge: Double = 0.0
    )

    /** Writes the PDF into <files>/bills and returns the File. */
    fun generate(context: Context, data: BillData, fileName: String): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val title = Paint().apply {
            color = BLUE; textSize = 30f; isFakeBoldText = true; isAntiAlias = true
        }
        val subtitle = Paint().apply { color = Color.DKGRAY; textSize = 12f; isAntiAlias = true }
        val label = Paint().apply { color = Color.BLACK; textSize = 12f; isAntiAlias = true }
        val labelBold = Paint().apply {
            color = Color.BLACK; textSize = 12f; isFakeBoldText = true; isAntiAlias = true
        }
        val headerText = Paint().apply {
            color = Color.WHITE; textSize = 12f; isFakeBoldText = true; isAntiAlias = true
        }
        val headerBg = Paint().apply { color = BLUE }
        val line = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

        // Header band
        canvas.drawRect(0f, 0f, PAGE_W.toFloat(), 70f, headerBg)
        val whiteTitle = Paint(title).apply { color = Color.WHITE }
        canvas.drawText("AK Spareparts", MARGIN, 46f, whiteTitle)
        canvas.drawText("Auto Parts Invoice", PAGE_W - MARGIN - 130f, 46f,
            Paint(headerText).apply { isFakeBoldText = false })

        // Customer + date block
        var y = 100f
        canvas.drawText("Bill To:", MARGIN, y, labelBold)
        canvas.drawText(data.customerName, MARGIN + 50f, y, label)
        y += 18f
        canvas.drawText("City:", MARGIN, y, labelBold)
        canvas.drawText(data.city, MARGIN + 50f, y, label)
        canvas.drawText("Date: ${data.date}", PAGE_W - MARGIN - 160f, 100f, label)

        // Table header
        y += 30f
        val colPart = MARGIN + 6f
        val colQty = MARGIN + 250f
        val colUnit = MARGIN + 330f
        val colTotal = MARGIN + 440f
        canvas.drawRect(MARGIN, y - 14f, PAGE_W - MARGIN, y + 6f, headerBg)
        canvas.drawText("Part No", colPart, y, headerText)
        canvas.drawText("Qty", colQty, y, headerText)
        canvas.drawText("Unit (AED)", colUnit, y, headerText)
        canvas.drawText("Total (AED)", colTotal, y, headerText)

        // Rows
        y += 24f
        for (item in data.items) {
            canvas.drawText(item.partNumber, colPart, y, label)
            canvas.drawText(item.qty.toString(), colQty, y, label)
            canvas.drawText(fmt(item.unitPrice), colUnit, y, label)
            canvas.drawText(fmt(item.lineTotal), colTotal, y, label)
            canvas.drawLine(MARGIN, y + 6f, PAGE_W - MARGIN, y + 6f, line)
            y += 24f
            if (y > PAGE_H - 120f) break // single-page guard
        }

        // Subtotal + delivery (only when a delivery charge is present)
        y += 8f
        if (data.deliveryCharge > 0.0) {
            val subtotal = data.items.sumOf { it.lineTotal }
            canvas.drawText("Subtotal", colUnit, y, labelBold)
            canvas.drawText("AED ${fmt(subtotal)}", colTotal, y, label)
            y += 20f
            canvas.drawText("Delivery", colUnit, y, labelBold)
            canvas.drawText("AED ${fmt(data.deliveryCharge)}", colTotal, y, label)
            y += 18f
        }

        // Grand total band
        y += 8f
        canvas.drawRect(colUnit - 10f, y - 16f, PAGE_W - MARGIN, y + 8f, headerBg)
        canvas.drawText("Grand Total", colUnit, y, headerText)
        canvas.drawText("AED ${fmt(data.grandTotal)}", colTotal, y, Paint(headerText))

        // Footer
        canvas.drawText("Thank you for your business — AK Spareparts",
            MARGIN, PAGE_H - 40f, subtitle)

        doc.finishPage(page)

        val dir = File(context.filesDir, "bills").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private fun fmt(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.2f", v)
}
