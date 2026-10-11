package com.example.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.example.model.CategoryRepository
import com.example.model.TransactionEntity
import com.example.model.TransactionType
import com.example.ui.components.CategorySlice
import com.example.ui.components.CurrencyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class ThermalPaperSize(val label: String, val charsPerLine: Int) {
    MM_58("58 mm (32 Karakter)", 32),
    MM_80("80 mm (48 Karakter)", 48)
}

data class BluetoothPrinterDevice(
    val name: String,
    val address: String,
    val device: BluetoothDevice? = null
)

class ThermalPrinterManager(private val context: Context) {

    companion object {
        // Standard Bluetooth Serial Port Profile (SPP) UUID
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        // ESC/POS Command Constants
        val ESC_INIT = byteArrayOf(0x1B, 0x40)
        val ESC_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
        val ESC_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
        val ESC_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
        val ESC_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
        val ESC_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
        val ESC_DOUBLE_SIZE = byteArrayOf(0x1B, 0x21, 0x30)
        val ESC_NORMAL_SIZE = byteArrayOf(0x1B, 0x21, 0x00)
        val ESC_CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x41, 0x10)
    }

    @SuppressLint("MissingPermission")
    fun getPairedBluetoothPrinters(): List<BluetoothPrinterDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()

        return try {
            val bonded = adapter.bondedDevices ?: emptySet()
            bonded.map { device ->
                BluetoothPrinterDevice(
                    name = device.name ?: "Printer Tanpa Nama",
                    address = device.address,
                    device = device
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun buildTestReceipt(paperSize: ThermalPaperSize): ByteArray {
        val bos = ByteArrayOutputStream()
        val width = paperSize.charsPerLine

        bos.write(ESC_INIT)
        bos.write(ESC_ALIGN_CENTER)
        bos.write(ESC_DOUBLE_SIZE)
        bos.write("CATATAN KEUANGAN\n".toByteArray(Charsets.ISO_8859_1))
        bos.write(ESC_NORMAL_SIZE)
        bos.write("Uji Coba Printer Thermal\n".toByteArray(Charsets.ISO_8859_1))
        bos.write(makeDivider('=', width).toByteArray(Charsets.ISO_8859_1))

        bos.write(ESC_ALIGN_LEFT)
        bos.write(formatTwoColumns("Tipe Kertas:", paperSize.label, width).toByteArray(Charsets.ISO_8859_1))
        bos.write(formatTwoColumns("Lebar Karakter:", "$width Karakter", width).toByteArray(Charsets.ISO_8859_1))
        bos.write(formatTwoColumns("Status:", "SIAP DIGUNAKAN", width).toByteArray(Charsets.ISO_8859_1))
        bos.write(makeDivider('-', width).toByteArray(Charsets.ISO_8859_1))

        bos.write(ESC_ALIGN_CENTER)
        bos.write("Printer Bluetooth Terhubung!\n".toByteArray(Charsets.ISO_8859_1))
        bos.write("\n\n\n".toByteArray(Charsets.ISO_8859_1))
        bos.write(ESC_CUT_PAPER)

        return bos.toByteArray()
    }

    fun buildMonthlyReportReceipt(
        periodTitle: String,
        totalIncome: Long,
        totalExpense: Long,
        netBalance: Long,
        monthlyBudget: Long,
        expenseSlices: List<CategorySlice>,
        transactions: List<TransactionEntity>,
        paperSize: ThermalPaperSize
    ): ByteArray {
        val bos = ByteArrayOutputStream()
        val width = paperSize.charsPerLine
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        bos.write(ESC_INIT)

        // Header
        bos.write(ESC_ALIGN_CENTER)
        bos.write(ESC_DOUBLE_SIZE)
        bos.write("CATATAN KEUANGAN\n".toByteArray(Charsets.ISO_8859_1))
        bos.write(ESC_NORMAL_SIZE)
        bos.write(ESC_BOLD_ON)
        bos.write("LAPORAN ARUS KAS BULANAN\n".toByteArray(Charsets.ISO_8859_1))
        bos.write(ESC_BOLD_OFF)
        bos.write("Periode: $periodTitle\n".toByteArray(Charsets.ISO_8859_1))
        bos.write("Dicetak: ${sdf.format(Date())}\n".toByteArray(Charsets.ISO_8859_1))
        bos.write(makeDivider('=', width).toByteArray(Charsets.ISO_8859_1))

        // Ringkasan Finansial
        bos.write(ESC_ALIGN_LEFT)
        bos.write(ESC_BOLD_ON)
        bos.write("RINGKASAN SALDO KAS:\n".toByteArray(Charsets.ISO_8859_1))
        bos.write(ESC_BOLD_OFF)
        bos.write(formatTwoColumns("Pemasukan (+):", CurrencyUtils.formatRupiah(totalIncome), width).toByteArray(Charsets.ISO_8859_1))
        bos.write(formatTwoColumns("Pengeluaran (-):", CurrencyUtils.formatRupiah(totalExpense), width).toByteArray(Charsets.ISO_8859_1))
        bos.write(makeDivider('-', width).toByteArray(Charsets.ISO_8859_1))
        bos.write(ESC_BOLD_ON)
        bos.write(formatTwoColumns("Sisa Saldo Kas:", CurrencyUtils.formatRupiah(netBalance), width).toByteArray(Charsets.ISO_8859_1))
        bos.write(ESC_BOLD_OFF)

        if (monthlyBudget > 0) {
            val budgetUsedPct = ((totalExpense.toFloat() / monthlyBudget) * 100).toInt()
            bos.write(formatTwoColumns("Target Anggaran:", CurrencyUtils.formatRupiah(monthlyBudget), width).toByteArray(Charsets.ISO_8859_1))
            bos.write(formatTwoColumns("Anggaran Terpakai:", "$budgetUsedPct%", width).toByteArray(Charsets.ISO_8859_1))
        }

        bos.write(makeDivider('=', width).toByteArray(Charsets.ISO_8859_1))

        // Rincian Kategori Pengeluaran
        if (expenseSlices.isNotEmpty()) {
            bos.write(ESC_BOLD_ON)
            bos.write("PENGELUARAN PER KATEGORI:\n".toByteArray(Charsets.ISO_8859_1))
            bos.write(ESC_BOLD_OFF)
            expenseSlices.take(8).forEach { slice ->
                val label = "${slice.category.name} (${String.format(Locale.getDefault(), "%.0f%%", slice.percentage)})"
                bos.write(formatTwoColumns(label, CurrencyUtils.formatRupiah(slice.amount), width).toByteArray(Charsets.ISO_8859_1))
            }
            bos.write(makeDivider('-', width).toByteArray(Charsets.ISO_8859_1))
        }

        // Daftar Transaksi Utama
        if (transactions.isNotEmpty()) {
            bos.write(ESC_BOLD_ON)
            bos.write("CATATAN TRANSAKSI BULAN INI:\n".toByteArray(Charsets.ISO_8859_1))
            bos.write(ESC_BOLD_OFF)
            transactions.take(15).forEach { tx ->
                val dateStr = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(tx.dateMillis))
                val sign = if (tx.type == "INCOME") "(+)" else "(-)"
                val titleLeft = "$dateStr ${tx.title}"
                val amountRight = "$sign ${CurrencyUtils.formatCompact(tx.amount)}"
                bos.write(formatTwoColumns(titleLeft, amountRight, width).toByteArray(Charsets.ISO_8859_1))
            }
            if (transactions.size > 15) {
                bos.write("... dan ${transactions.size - 15} transaksi lainnya\n".toByteArray(Charsets.ISO_8859_1))
            }
        }

        bos.write(makeDivider('=', width).toByteArray(Charsets.ISO_8859_1))

        // Footer
        bos.write(ESC_ALIGN_CENTER)
        bos.write("Keuangan Sehat, Masa Depan Cerah!\n".toByteArray(Charsets.ISO_8859_1))
        bos.write("--- Simpan Struk Sebagai Bukti ---\n".toByteArray(Charsets.ISO_8859_1))
        bos.write("\n\n\n".toByteArray(Charsets.ISO_8859_1))
        bos.write(ESC_CUT_PAPER)

        return bos.toByteArray()
    }

    private fun makeDivider(char: Char, width: Int): String {
        return char.toString().repeat(width) + "\n"
    }

    private fun formatTwoColumns(left: String, right: String, totalWidth: Int): String {
        val maxLeft = (totalWidth - right.length - 1).coerceAtLeast(4)
        val truncatedLeft = if (left.length > maxLeft) left.take(maxLeft - 1) + "." else left
        val spacesNeeded = (totalWidth - truncatedLeft.length - right.length).coerceAtLeast(1)
        return truncatedLeft + " ".repeat(spacesNeeded) + right + "\n"
    }

    @SuppressLint("MissingPermission")
    suspend fun printBytesToDevice(device: BluetoothDevice, data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null
        try {
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            outputStream = socket.outputStream
            outputStream.write(data)
            outputStream.flush()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        } finally {
            try {
                outputStream?.close()
                socket?.close()
            } catch (ignored: Exception) {}
        }
    }
}
