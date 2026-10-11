package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupPayload(
    val version: Int = 1,
    val monthlyBudget: Long = 5000000L,
    val transactions: List<TransactionEntity> = emptyList(),
    val exportDate: String = "",
    val transactionsCount: Int = 0
)

object BackupManager {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun getDefaultBackupFileName(): String {
        val stamp = fileTimestampFormat.format(Date())
        return "backup_keuangan_$stamp.json"
    }

    fun generateBackupJson(transactions: List<TransactionEntity>, monthlyBudget: Long): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "Catatan Keuangan")
        root.put("exportTimestamp", System.currentTimeMillis())
        root.put("exportDate", dateFormat.format(Date()))
        root.put("monthlyBudget", monthlyBudget)
        root.put("transactionsCount", transactions.size)

        val txArray = JSONArray()
        transactions.forEach { tx ->
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("title", tx.title)
            obj.put("amount", tx.amount)
            obj.put("type", tx.type)
            obj.put("categoryId", tx.categoryId)
            obj.put("wallet", tx.wallet)
            obj.put("dateMillis", tx.dateMillis)
            obj.put("note", tx.note)
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        return root.toString(2)
    }

    fun parseBackupJson(jsonString: String): BackupPayload {
        val root = JSONObject(jsonString)
        val version = root.optInt("version", 1)
        val budget = root.optLong("monthlyBudget", 5000000L)
        val exportDate = root.optString("exportDate", "")

        val list = mutableListOf<TransactionEntity>()
        if (root.has("transactions")) {
            val array = root.getJSONArray("transactions")
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                list.add(
                    TransactionEntity(
                        id = item.optLong("id", 0L),
                        title = item.optString("title", "Tanpa Judul"),
                        amount = item.optLong("amount", 0L),
                        type = item.optString("type", "EXPENSE"),
                        categoryId = item.optString("categoryId", "lainnya_keluar"),
                        wallet = item.optString("wallet", "Tunai"),
                        dateMillis = item.optLong("dateMillis", System.currentTimeMillis()),
                        note = item.optString("note", "")
                    )
                )
            }
        }
        return BackupPayload(
            version = version,
            monthlyBudget = budget,
            transactions = list,
            exportDate = exportDate,
            transactionsCount = list.size
        )
    }

    fun writeJsonToUri(context: Context, uri: Uri, jsonString: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(jsonString.toByteArray(Charsets.UTF_8))
                stream.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun readJsonFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun prepareShareIntent(context: Context, jsonString: String): Intent? {
        return try {
            val backupDir = File(context.cacheDir, "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            val fileName = getDefaultBackupFileName()
            val file = File(backupDir, fileName)
            FileOutputStream(file).use { out ->
                out.write(jsonString.toByteArray(Charsets.UTF_8))
            }

            val authority = "${context.packageName}.fileprovider"
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Cadangan Database Catatan Keuangan ($fileName)")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "File cadangan database aplikasi Catatan Keuangan ($fileName). Simpan file ini untuk memulihkan (restore) data Anda kapan saja."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Intent.createChooser(shareIntent, "Bagikan Cadangan Database")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
