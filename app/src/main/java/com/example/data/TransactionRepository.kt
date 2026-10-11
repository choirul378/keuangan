package com.example.data

import com.example.model.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class TransactionRepository(private val dao: TransactionDao) {

    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()

    fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> {
        return dao.getTransactionsBetween(startMillis, endMillis)
    }

    suspend fun insert(transaction: TransactionEntity): Long {
        return dao.insert(transaction)
    }

    suspend fun update(transaction: TransactionEntity) {
        dao.update(transaction)
    }

    suspend fun delete(transaction: TransactionEntity) {
        dao.delete(transaction)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun getAllTransactionsList(): List<TransactionEntity> {
        return dao.getAllTransactionsList()
    }

    suspend fun restoreTransactions(transactions: List<TransactionEntity>, replaceExisting: Boolean) {
        if (replaceExisting) {
            dao.deleteAllTransactions()
        }
        val toInsert = transactions.map { it.copy(id = 0L) }
        dao.insertAll(toInsert)
    }

    suspend fun checkAndSeedIfEmpty() {
        if (dao.getCount() == 0) {
            val now = Calendar.getInstance()
            val year = now.get(Calendar.YEAR)
            val month = now.get(Calendar.MONTH)

            fun getTime(day: Int, hour: Int, minute: Int): Long {
                val cal = Calendar.getInstance()
                cal.set(year, month, day.coerceIn(1, 28), hour, minute, 0)
                return cal.timeInMillis
            }

            val samples = listOf(
                TransactionEntity(
                    title = "Gaji Bulanan",
                    amount = 7500000L,
                    type = "INCOME",
                    categoryId = "gaji",
                    wallet = "Bank",
                    dateMillis = getTime(1, 9, 0),
                    note = "Transfer gaji kantor utama"
                ),
                TransactionEntity(
                    title = "Belanja Bulanan Supermarket",
                    amount = 850000L,
                    type = "EXPENSE",
                    categoryId = "belanja",
                    wallet = "E-Wallet",
                    dateMillis = getTime(2, 14, 30),
                    note = "Kebutuhan pokok & sabun"
                ),
                TransactionEntity(
                    title = "Makan Siang & Kopi",
                    amount = 65000L,
                    type = "EXPENSE",
                    categoryId = "makanan",
                    wallet = "Tunai",
                    dateMillis = getTime(3, 12, 15),
                    note = "Ayam bakar + es teh"
                ),
                TransactionEntity(
                    title = "Bensin Kendaraan",
                    amount = 100000L,
                    type = "EXPENSE",
                    categoryId = "transportasi",
                    wallet = "E-Wallet",
                    dateMillis = getTime(4, 8, 30),
                    note = "Pertalite penuh"
                ),
                TransactionEntity(
                    title = "Tagihan Listrik & Air",
                    amount = 450000L,
                    type = "EXPENSE",
                    categoryId = "tagihan",
                    wallet = "Bank",
                    dateMillis = getTime(5, 10, 0),
                    note = "PLN Pascabayar & PDAM"
                ),
                TransactionEntity(
                    title = "Hasil Jual Online Shop",
                    amount = 1200000L,
                    type = "INCOME",
                    categoryId = "bisnis",
                    wallet = "Bank",
                    dateMillis = getTime(6, 16, 45),
                    note = "Pesanan marketplace batch 1"
                ),
                TransactionEntity(
                    title = "Langganan Internet & Wi-Fi",
                    amount = 330000L,
                    type = "EXPENSE",
                    categoryId = "tagihan",
                    wallet = "Bank",
                    dateMillis = getTime(7, 11, 20),
                    note = "Internet fiber optik bulanan"
                ),
                TransactionEntity(
                    title = "Nonton Bioskop & Snack",
                    amount = 140000L,
                    type = "EXPENSE",
                    categoryId = "hiburan",
                    wallet = "E-Wallet",
                    dateMillis = getTime(8, 19, 0),
                    note = "Tiket bioskop akhir pekan"
                ),
                TransactionEntity(
                    title = "Makan Malam Keluarga",
                    amount = 275000L,
                    type = "EXPENSE",
                    categoryId = "makanan",
                    wallet = "Tunai",
                    dateMillis = getTime(9, 19, 30),
                    note = "Resto seafood"
                ),
                TransactionEntity(
                    title = "Vitamin & Suplemen",
                    amount = 120000L,
                    type = "EXPENSE",
                    categoryId = "kesehatan",
                    wallet = "Tunai",
                    dateMillis = getTime(10, 15, 0),
                    note = "Beli di apotek"
                )
            )
            dao.insertAll(samples)
        }
    }
}
