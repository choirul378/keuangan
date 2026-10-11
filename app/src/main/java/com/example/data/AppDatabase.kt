package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(entities = [TransactionEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "keuangan_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed initial realistic starter data for current month
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database.transactionDao())
                    }
                }
            }
        }

        private suspend fun populateInitialData(dao: TransactionDao) {
            val now = Calendar.getInstance()
            val currentYear = now.get(Calendar.YEAR)
            val currentMonth = now.get(Calendar.MONTH)

            fun getTime(day: Int, hour: Int, minute: Int): Long {
                val cal = Calendar.getInstance()
                cal.set(currentYear, currentMonth, day.coerceIn(1, 28), hour, minute, 0)
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
                    note = "Tiket akhir pekan + popcorn"
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
