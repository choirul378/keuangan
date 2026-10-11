package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.CategoryRepository
import com.example.model.TransactionEntity
import com.example.model.TransactionType
import com.example.ui.components.CategorySlice
import com.example.ui.components.CurrencyUtils
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExportManager {

    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID"))
    private val dateShortFormat = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))

    fun getPdfFileName(periodTitle: String): String {
        val cleanPeriod = periodTitle.replace(" ", "_")
        return "Laporan_Keuangan_${cleanPeriod}_${fileTimestampFormat.format(Date())}.pdf"
    }

    fun getCsvFileName(periodTitle: String): String {
        val cleanPeriod = periodTitle.replace(" ", "_")
        return "Transaksi_${cleanPeriod}_${fileTimestampFormat.format(Date())}.csv"
    }

    fun generateCsvContent(transactions: List<TransactionEntity>): String {
        val sb = StringBuilder()
        // CSV Header
        sb.append("\"ID\",\"Tanggal\",\"Judul\",\"Tipe Transaksi\",\"Kategori\",\"Sumber Dana\",\"Nominal (Rp)\",\"Catatan\"\n")

        transactions.forEach { tx ->
            val dateStr = dateShortFormat.format(Date(tx.dateMillis))
            val category = CategoryRepository.findCategory(
                tx.categoryId,
                if (tx.type == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
            )
            val typeLabel = if (tx.type == "INCOME") "Pemasukan" else "Pengeluaran"
            val escapedTitle = tx.title.replace("\"", "\"\"")
            val escapedNote = tx.note.replace("\"", "\"\"")

            sb.append("\"${tx.id}\",")
            sb.append("\"$dateStr\",")
            sb.append("\"$escapedTitle\",")
            sb.append("\"$typeLabel\",")
            sb.append("\"${category.name}\",")
            sb.append("\"${tx.wallet}\",")
            sb.append("${tx.amount},")
            sb.append("\"$escapedNote\"\n")
        }

        return sb.toString()
    }

    fun generatePdfFile(
        context: Context,
        periodTitle: String,
        totalIncome: Long,
        totalExpense: Long,
        netBalance: Long,
        monthlyBudget: Long,
        expenseSlices: List<CategorySlice>,
        transactions: List<TransactionEntity>
    ): File {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val paint = Paint().apply { isAntiAlias = true }
        var pageNumber = 1

        // Page 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        // Header Background Banner
        paint.color = Color.rgb(13, 148, 136) // Emerald Primary
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 95f, paint)

        // App & Document Title
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CATATAN KEUANGAN PRIBADI", 32f, 40f, paint)

        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Laporan Bulanan Periode: $periodTitle", 32f, 62f, paint)

        paint.textSize = 9f
        paint.color = Color.rgb(204, 251, 241)
        canvas.drawText("Dicetak otomatis pada: ${dateTimeFormat.format(Date())}", 32f, 80f, paint)

        var y = 120f

        // 3 Summary Cards
        val cardWidth = 160f
        val cardHeight = 60f
        val gap = 15f
        var startX = 32f

        // Card 1: Pemasukan
        paint.color = Color.rgb(209, 250, 229) // Light Green
        canvas.drawRoundRect(startX, y, startX + cardWidth, y + cardHeight, 10f, 10f, paint)
        paint.color = Color.rgb(4, 120, 87)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOTAL PEMASUKAN", startX + 12f, y + 20f, paint)
        paint.textSize = 14f
        canvas.drawText(CurrencyUtils.formatRupiah(totalIncome), startX + 12f, y + 44f, paint)

        // Card 2: Pengeluaran
        startX += cardWidth + gap
        paint.color = Color.rgb(254, 226, 226) // Light Red
        canvas.drawRoundRect(startX, y, startX + cardWidth, y + cardHeight, 10f, 10f, paint)
        paint.color = Color.rgb(185, 28, 28)
        paint.textSize = 10f
        canvas.drawText("TOTAL PENGELUARAN", startX + 12f, y + 20f, paint)
        paint.textSize = 14f
        canvas.drawText(CurrencyUtils.formatRupiah(totalExpense), startX + 12f, y + 44f, paint)

        // Card 3: Saldo Bersih
        startX += cardWidth + gap
        paint.color = Color.rgb(241, 245, 249) // Slate Light
        canvas.drawRoundRect(startX, y, startX + cardWidth, y + cardHeight, 10f, 10f, paint)
        paint.color = if (netBalance >= 0) Color.rgb(13, 148, 136) else Color.rgb(185, 28, 28)
        paint.textSize = 10f
        canvas.drawText("SISA SALDO KAS", startX + 12f, y + 20f, paint)
        paint.textSize = 14f
        canvas.drawText(CurrencyUtils.formatRupiah(netBalance), startX + 12f, y + 44f, paint)

        y += cardHeight + 25f

        // Category Breakdown Section
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Proporsi Pengeluaran per Kategori", 32f, y, paint)

        y += 14f
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawLine(32f, y, pageWidth - 32f, y, paint)

        y += 18f
        paint.textSize = 9f
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText("KATEGORI", 32f, y, paint)
        canvas.drawText("JUMLAH NOMINAL", 280f, y, paint)
        canvas.drawText("PERSENTASE", 450f, y, paint)

        y += 8f
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawLine(32f, y, pageWidth - 32f, y, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(15, 23, 42)

        if (expenseSlices.isEmpty()) {
            y += 18f
            canvas.drawText("Belum ada transaksi pengeluaran pada periode ini.", 32f, y, paint)
        } else {
            expenseSlices.take(6).forEach { slice ->
                y += 18f
                canvas.drawText(slice.category.name, 32f, y, paint)
                canvas.drawText(CurrencyUtils.formatRupiah(slice.amount), 280f, y, paint)
                canvas.drawText(String.format(Locale.getDefault(), "%.1f%%", slice.percentage), 450f, y, paint)
            }
        }

        y += 28f

        // Transaction Log Section Table
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Daftar Rincian Transaksi", 32f, y, paint)

        y += 14f
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawLine(32f, y, pageWidth - 32f, y, paint)

        y += 18f
        paint.textSize = 9f
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText("TANGGAL", 32f, y, paint)
        canvas.drawText("JUDUL / KETERANGAN", 110f, y, paint)
        canvas.drawText("KATEGORI", 270f, y, paint)
        canvas.drawText("DOMPET", 380f, y, paint)
        canvas.drawText("NOMINAL", 460f, y, paint)

        y += 8f
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawLine(32f, y, pageWidth - 32f, y, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        transactions.forEachIndexed { index, tx ->
            // Check if we need to start a new page
            if (y > pageHeight - 50f) {
                // Footer on current page
                paint.color = Color.rgb(148, 163, 184)
                paint.textSize = 8f
                canvas.drawText("Halaman $pageNumber", pageWidth / 2f - 20f, pageHeight - 20f, paint)

                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas

                // Header for next page
                y = 40f
                paint.color = Color.rgb(15, 23, 42)
                paint.textSize = 10f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("Lanjutan Transaksi - $periodTitle (Hal $pageNumber)", 32f, y, paint)

                y += 15f
                paint.textSize = 9f
                paint.color = Color.rgb(100, 116, 139)
                canvas.drawText("TANGGAL", 32f, y, paint)
                canvas.drawText("JUDUL / KETERANGAN", 110f, y, paint)
                canvas.drawText("KATEGORI", 270f, y, paint)
                canvas.drawText("DOMPET", 380f, y, paint)
                canvas.drawText("NOMINAL", 460f, y, paint)

                y += 8f
                paint.color = Color.rgb(226, 232, 240)
                canvas.drawLine(32f, y, pageWidth - 32f, y, paint)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            y += 18f
            val dateStr = dateShortFormat.format(Date(tx.dateMillis))
            val isIncome = tx.type == "INCOME"
            val category = CategoryRepository.findCategory(
                tx.categoryId,
                if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
            )

            paint.color = Color.rgb(71, 85, 105)
            canvas.drawText(dateStr, 32f, y, paint)

            paint.color = Color.rgb(15, 23, 42)
            val titleTrimmed = if (tx.title.length > 24) tx.title.take(22) + ".." else tx.title
            canvas.drawText(titleTrimmed, 110f, y, paint)

            paint.color = Color.rgb(71, 85, 105)
            canvas.drawText(category.name.take(18), 270f, y, paint)
            canvas.drawText(tx.wallet, 380f, y, paint)

            paint.color = if (isIncome) Color.rgb(5, 150, 105) else Color.rgb(220, 38, 38)
            val nominalStr = "${if (isIncome) "+" else "-"} ${CurrencyUtils.formatRupiah(tx.amount)}"
            canvas.drawText(nominalStr, 460f, y, paint)
        }

        // Final page footer
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 8f
        canvas.drawText("Halaman $pageNumber", pageWidth / 2f - 20f, pageHeight - 20f, paint)

        document.finishPage(page)

        // Write to cache file
        val outputDir = File(context.cacheDir, "exports")
        if (!outputDir.exists()) outputDir.mkdirs()
        val pdfFile = File(outputDir, getPdfFileName(periodTitle))

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }

    fun prepareSharePdfIntent(context: Context, pdfFile: File): Intent {
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, pdfFile)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Keuangan Bulanan - ${pdfFile.name}")
            putExtra(Intent.EXTRA_TEXT, "Berikut adalah lampiran berkas laporan keuangan PDF dari aplikasi Catatan Keuangan.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(intent, "Bagikan Laporan PDF")
    }

    fun prepareShareCsvIntent(context: Context, csvFile: File): Intent {
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, csvFile)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Ekspor Transaksi CSV - ${csvFile.name}")
            putExtra(Intent.EXTRA_TEXT, "Berikut adalah lampiran berkas data transaksi CSV untuk Excel atau Spreadsheet.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(intent, "Bagikan Data CSV")
    }
}
