package vn.studentfinance

import android.content.Context
import androidx.room.Room
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode
import vn.studentfinance.data.local.FinanceDatabase as Db
import vn.studentfinance.data.repository.*
import vn.studentfinance.ui.monthlySummary
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class FinanceRepositoryTest {
    private lateinit var db: Db
    private lateinit var repo: FinanceRepository
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val day = 20261112
    private val month = 202611

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, Db::class.java)
            .addCallback(Db.SEED).allowMainThreadQueries().build()
        repo = FinanceRepository(db)
    }
    @After fun close() { db.close() }

    private fun loan(limit: Long = 500) = repo.saveLoan(Db.Loan("loan", "Học tập", limit), false)
    private fun draw(amount: Long = 300) =
        repo.saveDraw(Db.Disbursement("draw", "loan", "Đợt 1", day, amount), false)
    private fun entry(id: String = "t", amount: Long = 50, source: String = "LOAN",
                      draw: String? = "draw", category: String = "food", date: Int = day) =
        Db.Transaction(id, category, source, draw, date, amount, "")
    private fun summary(period: Int = month) = monthlySummary(repo.read(), period)
    private fun invalid(block: () -> Unit) { assertThrows(IllegalArgumentException::class.java, block) }

    @Test fun createReadUpdateDeleteAllModules() {
        loan(); draw()
        repo.saveLoan(Db.Loan("loan","Vay A",600),true)
        repo.saveDraw(Db.Disbursement("draw","loan","Đợt A",day,400),true)
        repo.saveEntry(entry(),false)
        repo.saveEntry(entry(amount=100),true, listOf(Db.Receipt("r","t","content://example/test","Phiếu thu","image/png")))
        repo.saveBudget(Db.Budget("b",month,"food",90),false)
        repo.saveBudget(Db.Budget("b",month,"food",150),true)
        val ledger = repo.read()
        assertEquals("Vay A",ledger.loans.single().name)
        assertEquals(400L,ledger.draws.single().amount)
        assertEquals(100L,ledger.entries.single().amount)
        assertEquals(150L,ledger.budgets.single().limit)
        assertEquals("Phiếu thu",ledger.entries.single().receipts.single().displayName)
        assertEquals("t",db.dao().receipts().single().transactionId)
        repo.delete(RecordType.ENTRY,"t")
        assertTrue(db.dao().receipts().isEmpty())
        repo.delete(RecordType.BUDGET,"b")
        repo.delete(RecordType.DRAW,"draw")
        repo.delete(RecordType.LOAN,"loan")
        assertTrue(repo.read().let { it.entries.isEmpty() && it.budgets.isEmpty() && it.loans.isEmpty() && it.draws.isEmpty() })
    }

    @Test fun individualIncomeAndExpensesDoNotConsumeLoanCapital() {
        loan(); draw()
        repo.saveEntry(entry("income",90,"PERSONAL",null,"allowance"),false)
        repo.saveEntry(entry("personal",40,"PERSONAL",null),false)
        assertEquals(300.toBigInteger(),summary().capital)
        assertEquals(90.toBigInteger(),summary().income)
        repo.saveEntry(entry(amount=80),false)
        assertEquals(220.toBigInteger(),summary().capital)
        assertEquals(120.toBigInteger(),summary().spent)
        repo.saveEntry(entry(amount=300),true)
        assertEquals(0.toBigInteger(),summary().capital)
        repo.saveEntry(entry(amount=300, source="PERSONAL",draw=null),true)
        assertEquals(300.toBigInteger(),summary().capital)
        repo.saveEntry(entry(amount=50),true)
        repo.delete(RecordType.ENTRY,"t")
        assertEquals(300.toBigInteger(),summary().capital)
    }

    @Test fun budgetUsesBothSourcesOnlyInItsMonthAndCategory() {
        loan(); draw()
        repo.saveBudget(Db.Budget("b",month,"food",100),false)
        repo.saveEntry(entry("one",70),false)
        repo.saveEntry(entry("two",40,"PERSONAL",null),false)
        repo.saveEntry(entry("other",20,category="study"),false)
        repo.saveEntry(entry("next",15,date=20261201),false)
        val budget = summary().budgets.single()
        assertEquals(110.toBigInteger(),budget.spent)
        assertEquals((-10).toBigInteger(),budget.remaining)
        assertTrue(summary(202612).budgets.isEmpty())
        invalid { repo.saveBudget(Db.Budget("duplicate",month,"food",200),false) }
        invalid { repo.saveBudget(Db.Budget("income",month,"allowance",1),false) }
    }

    @Test fun rejectsInvalidMoneySourceDateAndOverdraftAtomically() {
        loan(); draw()
        listOf(entry(amount=-1),entry(amount=0),entry(source="",draw=null),entry(source="LOAN",draw=null),
            entry(draw="unknown"),entry(source="PERSONAL"),entry(category="allowance"),
            entry(date=20260230),entry(date=20261111),entry(amount=301)).forEach {
            invalid { repo.saveEntry(it,false) }
        }
        assertTrue(repo.read().entries.isEmpty())
        repo.saveEntry(entry(amount=200),false)
        invalid { repo.saveEntry(entry(amount=301),true) }
        assertEquals(200L,repo.read().entries.single().amount)
        assertEquals(100.toBigInteger(),summary().capital)
    }

    @Test fun invalidReceiptRollsBackTheEntry() {
        loan(); draw()
        invalid { repo.saveEntry(entry(), false, listOf(Db.Receipt("r","other","content://example/test","Ảnh","image/png"))) }
        assertTrue(repo.read().entries.isEmpty())
        assertTrue(db.dao().receipts().isEmpty())
    }

    @Test fun enforcesParentLimitsAndDeleteRestrictions() {
        loan(); draw()
        invalid { repo.saveLoan(Db.Loan("loan","A",299),true) }
        invalid { repo.saveDraw(Db.Disbursement("another","loan","B",day,201),false) }
        repo.saveEntry(entry(amount=150),false)
        invalid { repo.saveDraw(Db.Disbursement("draw","loan","A",day,149),true) }
        invalid { repo.saveDraw(Db.Disbursement("draw","loan","A",day+1,300),true) }
        invalid { repo.delete(RecordType.LOAN,"loan") }
        invalid { repo.delete(RecordType.DRAW,"draw") }
        assertEquals(150.toBigInteger(),summary().capital)
    }

    @Test fun reportsSeparateLoansAndCarryOpeningIntoNextMonth() {
        loan(); draw()
        repo.saveLoan(Db.Loan("other","B",200),false)
        repo.saveDraw(Db.Disbursement("d2","other","Đợt 2",day,200),false)
        repo.saveEntry(entry(amount=60),false)
        repo.saveEntry(entry("dec",30,date=20261201),false)
        repo.saveEntry(entry("other",20,draw="d2",date=20261201),false)
        val report = summary(202612).reports.getValue("loan")
        assertEquals(240.toBigInteger(),report.opening)
        assertEquals(0.toBigInteger(),report.received)
        assertEquals(30.toBigInteger(),report.spent)
        assertEquals(210.toBigInteger(),report.closing)
        assertEquals(listOf("dec"),report.entries.map { it.id })
    }

    @Test fun preservesDongPrecisionAboveDoubleAndAtLongBoundary() {
        loan(Long.MAX_VALUE); draw(Long.MAX_VALUE)
        repo.saveEntry(entry(amount=Long.MAX_VALUE-1),false)
        assertEquals(1.toBigInteger(),summary().capital)
        invalid { repo.saveEntry(entry("extra",2),false) }
        repo.saveEntry(entry("personal",Long.MAX_VALUE,"PERSONAL",null),false)
        assertEquals(Long.MAX_VALUE.toBigInteger()*2.toBigInteger()-1.toBigInteger(),summary().spent)
    }

    @Test fun concurrentWritersCannotSpendSameFundsTwice() {
        loan(); draw()
        val gate = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val writes = listOf("a","b").map { id ->
                pool.submit(Callable { gate.await(); runCatching { repo.saveEntry(entry(id,200),false) }.isSuccess })
            }
            gate.countDown()
            assertEquals(1,writes.count { it.get() })
            assertEquals(100.toBigInteger(),summary().capital)
        } finally { pool.shutdownNow() }
    }

    @Test fun reopensFileWithSameRelationsAndBalances() {
        val name = "persistence-test.db"
        fun open() = Room.databaseBuilder(context,Db::class.java,name)
            .addCallback(Db.SEED).allowMainThreadQueries().build()
        db.close()
        db = open(); repo = FinanceRepository(db)
        loan(); draw(); repo.saveEntry(entry(),false)
        repo.saveBudget(Db.Budget("b",month,"food",80),false)
        db.close()
        db = open(); repo = FinanceRepository(db)
        assertEquals(250.toBigInteger(),summary().capital)
        assertEquals(30.toBigInteger(),summary().budgets.single().remaining)
        assertEquals("draw",repo.read().entries.single().sourceId)
    }
}
