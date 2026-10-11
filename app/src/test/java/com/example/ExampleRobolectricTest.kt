package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.BackupManager
import com.example.export.ReportExportManager
import com.example.model.CategoryRepository
import com.example.model.TransactionEntity
import com.example.model.TransactionType
import com.example.printer.ThermalPaperSize
import com.example.printer.ThermalPrinterManager
import com.example.security.SecurityManager
import com.example.ui.components.CurrencyUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Catatan Keuangan", appName)
  }

  @Test
  fun `currency utils formatting test`() {
    val formatted = CurrencyUtils.formatRupiah(1500000L)
    assertTrue(formatted.contains("1.500.000") || formatted.contains("1,500,000"))
    val compact = CurrencyUtils.formatCompact(2500000L)
    assertTrue(compact.contains("jt"))
  }

  @Test
  fun `category repository default categories test`() {
    val expenseCategories = CategoryRepository.expenseCategories
    assertTrue(expenseCategories.isNotEmpty())
    val foodCat = CategoryRepository.findCategory("makanan", TransactionType.EXPENSE)
    assertNotNull(foodCat)
    assertEquals("Makanan & Minuman", foodCat.name)
  }

  @Test
  fun `backup and restore manager serialization test`() {
    val samples = listOf(
      TransactionEntity(
        id = 1L,
        title = "Gaji",
        amount = 5000000L,
        type = "INCOME",
        categoryId = "gaji",
        wallet = "Bank",
        dateMillis = 1700000000000L,
        note = "Catatan gaji"
      ),
      TransactionEntity(
        id = 2L,
        title = "Makan Siang",
        amount = 50000L,
        type = "EXPENSE",
        categoryId = "makanan",
        wallet = "Tunai",
        dateMillis = 1700000010000L,
        note = "Nasi padang"
      )
    )

    val json = BackupManager.generateBackupJson(samples, 6000000L)
    assertTrue(json.contains("Catatan Keuangan"))
    assertTrue(json.contains("5000000"))

    val parsed = BackupManager.parseBackupJson(json)
    assertEquals(2, parsed.transactions.size)
    assertEquals(6000000L, parsed.monthlyBudget)
    assertEquals("Gaji", parsed.transactions[0].title)
    assertEquals(50000L, parsed.transactions[1].amount)
  }

  @Test
  fun `security manager pin and biometric test`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sec = SecurityManager(context)
    sec.disableSecurity()

    assertFalse(sec.isPinEnabled)
    sec.setPin("1234")
    assertTrue(sec.isPinEnabled)
    assertTrue(sec.verifyPin("1234"))
    assertFalse(sec.verifyPin("9999"))

    sec.disableSecurity()
    assertFalse(sec.isPinEnabled)
  }

  @Test
  fun `report export csv generation test`() {
    val samples = listOf(
      TransactionEntity(
        id = 1L,
        title = "Gaji Bulanan",
        amount = 7500000L,
        type = "INCOME",
        categoryId = "gaji",
        wallet = "Bank",
        dateMillis = 1700000000000L,
        note = "Gaji kantor"
      )
    )
    val csv = ReportExportManager.generateCsvContent(samples)
    assertTrue(csv.contains("Gaji Bulanan"))
    assertTrue(csv.contains("Pemasukan"))
    assertTrue(csv.contains("7500000"))
  }

  @Test
  fun `thermal printer manager receipt test`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val printer = ThermalPrinterManager(context)

    val test58 = printer.buildTestReceipt(ThermalPaperSize.MM_58)
    assertTrue(test58.isNotEmpty())

    val test80 = printer.buildTestReceipt(ThermalPaperSize.MM_80)
    assertTrue(test80.isNotEmpty())
  }
}
