package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val amount: Long,
    val type: String, // "EXPENSE" or "INCOME"
    val categoryId: String,
    val wallet: String = "Tunai", // "Tunai", "Bank", "E-Wallet"
    val dateMillis: Long,
    val note: String = ""
) {
    val transactionType: TransactionType
        get() = if (type == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
}
