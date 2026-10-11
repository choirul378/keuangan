package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.CatBillsColor
import com.example.ui.theme.CatBonusColor
import com.example.ui.theme.CatBusinessColor
import com.example.ui.theme.CatEducationColor
import com.example.ui.theme.CatEntertainmentColor
import com.example.ui.theme.CatFoodColor
import com.example.ui.theme.CatHealthColor
import com.example.ui.theme.CatInvestColor
import com.example.ui.theme.CatOtherExpenseColor
import com.example.ui.theme.CatOtherIncomeColor
import com.example.ui.theme.CatSalaryColor
import com.example.ui.theme.CatShoppingColor
import com.example.ui.theme.CatTransportColor

enum class TransactionType(val label: String) {
    EXPENSE("Pengeluaran"),
    INCOME("Pemasukan")
}

data class TransactionCategory(
    val id: String,
    val name: String,
    val type: TransactionType,
    val icon: ImageVector,
    val color: Color
)

object CategoryRepository {
    val expenseCategories = listOf(
        TransactionCategory("makanan", "Makanan & Minuman", TransactionType.EXPENSE, Icons.Default.Restaurant, CatFoodColor),
        TransactionCategory("transportasi", "Transportasi", TransactionType.EXPENSE, Icons.Default.DirectionsCar, CatTransportColor),
        TransactionCategory("belanja", "Belanja", TransactionType.EXPENSE, Icons.Default.ShoppingBag, CatShoppingColor),
        TransactionCategory("tagihan", "Tagihan & Utilitas", TransactionType.EXPENSE, Icons.Default.Bolt, CatBillsColor),
        TransactionCategory("hiburan", "Hiburan & Hobi", TransactionType.EXPENSE, Icons.Default.SportsEsports, CatEntertainmentColor),
        TransactionCategory("kesehatan", "Kesehatan", TransactionType.EXPENSE, Icons.Default.HealthAndSafety, CatHealthColor),
        TransactionCategory("pendidikan", "Pendidikan", TransactionType.EXPENSE, Icons.Default.School, CatEducationColor),
        TransactionCategory("lainnya_keluar", "Lain-lain", TransactionType.EXPENSE, Icons.Default.MoreHoriz, CatOtherExpenseColor)
    )

    val incomeCategories = listOf(
        TransactionCategory("gaji", "Gaji Pokok", TransactionType.INCOME, Icons.Default.Payments, CatSalaryColor),
        TransactionCategory("bonus", "Bonus & Tunjangan", TransactionType.INCOME, Icons.Default.CardGiftcard, CatBonusColor),
        TransactionCategory("bisnis", "Bisnis / Usaha", TransactionType.INCOME, Icons.Default.Storefront, CatBusinessColor),
        TransactionCategory("investasi", "Investasi & Bunga", TransactionType.INCOME, Icons.Default.TrendingUp, CatInvestColor),
        TransactionCategory("lainnya_masuk", "Lain-lain", TransactionType.INCOME, Icons.Default.AccountBalanceWallet, CatOtherIncomeColor)
    )

    fun getAllCategories(): List<TransactionCategory> = expenseCategories + incomeCategories

    fun findCategory(id: String, type: TransactionType): TransactionCategory {
        val list = if (type == TransactionType.EXPENSE) expenseCategories else incomeCategories
        return list.find { it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) }
            ?: getAllCategories().find { it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) }
            ?: if (type == TransactionType.EXPENSE) expenseCategories.last() else incomeCategories.last()
    }
}
