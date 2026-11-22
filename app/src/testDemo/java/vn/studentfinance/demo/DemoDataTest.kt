package vn.studentfinance.demo

import androidx.room.Room
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode
import vn.studentfinance.data.local.FinanceDatabase
import vn.studentfinance.data.repository.FinanceRepository
import vn.studentfinance.data.repository.RecordType
import vn.studentfinance.ui.monthlySummary
import java.util.Calendar
import java.util.GregorianCalendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class DemoDataTest {
    @Test fun seedsFifteenEntriesAcrossYearsAndPreservesEdits() {
        fun open() = Room.databaseBuilder(RuntimeEnvironment.getApplication(), FinanceDatabase::class.java, "demo-check.db")
            .addCallback(FinanceDatabase.SEED)
            .addCallback(DemoData(GregorianCalendar(2027, Calendar.JANUARY, 1)))
            .allowMainThreadQueries().build()
        var db = open()
        try {
            val repo = FinanceRepository(db)
            val ledger = repo.read()
            assertEquals(15, ledger.entries.size)
            assertTrue(ledger.entries.all { it.day <= 20270101 })
            val month = monthlySummary(ledger, 202701)
            assertEquals(14, month.entries.size)
            assertEquals(2_100_000.toBigInteger(), month.income)
            assertEquals(1_120_000.toBigInteger(), month.spent)
            val report = repo.report("demo-study", 202701)
            assertEquals(listOf(1_700_000L, 700_000L, 305_000L, 2_095_000L), report.totals.map { it.second.toLong() })
            assertEquals((-85_000).toBigInteger(), month.budgets.single { it.category == "Học tập" }.remaining)
            repo.delete(RecordType.ENTRY, "demo-books")
        } finally { db.close() }
        db = open()
        try { assertEquals(14, FinanceRepository(db).read().entries.size) } finally { db.close() }
    }
}
