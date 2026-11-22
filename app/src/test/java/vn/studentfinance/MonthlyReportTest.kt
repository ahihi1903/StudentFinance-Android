package vn.studentfinance

import android.content.Context
import androidx.room.Room
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode
import vn.studentfinance.data.local.FinanceDatabase as Db
import vn.studentfinance.data.repository.FinanceRepository
import vn.studentfinance.report.*
import java.io.ByteArrayOutputStream
import java.io.File
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class MonthlyReportTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private lateinit var db: Db
    private lateinit var repo: FinanceRepository
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, Db::class.java).addCallback(Db.SEED).allowMainThreadQueries().build()
        repo = FinanceRepository(db)
        repo.saveLoan(Db.Loan("a","Vay học tập",2_000_000),false)
        repo.saveLoan(Db.Loan("b","Khoản khác",1_000_000),false)
        repo.saveDraw(Db.Disbursement("old","a","Đợt đầu",20261001,900000),false)
        repo.saveDraw(Db.Disbursement("new","a","Đợt mới",20261101,500000),false)
        repo.saveDraw(Db.Disbursement("other","b","Đợt khác",20261101,400000),false)
        fun cost(id: String, day: Int, amount: Long, draw: String?) =
            Db.Transaction(id,"study",if(draw==null) "PERSONAL" else "LOAN",draw,day,amount,"Học phí, \"kỳ 1\"\nđợt hai")
        repo.saveEntry(cost("oct",20261031,200000,"old"),false)
        repo.saveEntry(cost("first",20261101,120000,"old"),false,listOf(
            Db.Receipt("r1","first","content://qa/1","Hóa đơn.pdf","application/pdf"),
            Db.Receipt("r2","first","content://qa/2","Ảnh.jpg","image/jpeg")))
        repo.saveEntry(cost("last",20261130,80000,"new"),false)
        repo.saveEntry(cost("dec",20261201,10000,"new"),false)
        repo.saveEntry(cost("personal",20261108,60000,null),false)
        repo.saveEntry(cost("otherLoan",20261108,30000,"other"),false)
        repo.saveEntry(Db.Transaction("income","allowance","PERSONAL",null,20261108,100000,"Trợ cấp"),false)
        repo.saveReportHeader("a","Nguyễn Ánh","Quỹ học tập")
    }
    @After fun close() = db.close()
    private fun report(month: Int = 202611) = repo.report("a",month)

    @Test fun R01_openingAndClosing() {
        assertEquals(listOf(700000L,500000L,200000L,1000000L),report().totals.map { it.second.longValueExact() })
    }
    @Test fun R02_scopeAndOrder() {
        assertEquals(listOf("new","first","last"),report().lines.map { it.id })
        assertEquals(listOf("other","otherLoan"),repo.report("b",202611).lines.map { it.id })
    }
    @Test fun R03_changeMonth() {
        assertEquals(listOf(1000000L,0L,10000L,990000L),report(202612).totals.map { it.second.longValueExact() })
    }
    @Test fun R04_emptyMonthCarryForward() {
        val empty = report(202701)
        assertTrue(empty.lines.isEmpty())
        assertEquals(990000.toBigInteger(),empty.figures.closing)
        assertEquals(empty.figures.opening,empty.figures.closing)
    }
    @Test fun R05_receiptsDoNotMultiplyMoney() {
        val row = report().lines.first { it.id=="first" }
        assertEquals(listOf("Hóa đơn.pdf","Ảnh.jpg"),row.receipts)
        assertEquals("Học tập",row.category)
        assertEquals(120000L,row.amount)
    }
    @Test fun R06_metadataByLoan() {
        assertEquals("Nguyễn Ánh",report().author)
        assertEquals("Quỹ học tập",report().lender)
        assertEquals("",repo.report("b",202611).lender)
    }
    @Test fun R07_missingMetadata() {
        assertThrows(IllegalArgumentException::class.java) { repo.saveReportHeader("a"," ","Q") }
        assertThrows(IllegalArgumentException::class.java) {
            ReportWriter.write(repo.report("b",202611),ReportFormat.CSV,ByteArrayOutputStream())
        }
    }
    @Test fun R08_csvEncodingAndEscapes() {
        val out = ByteArrayOutputStream()
        ReportWriter.write(report(),ReportFormat.CSV,out)
        val csv = out.toString("UTF-8")
        assertTrue(csv.startsWith("\uFEFF\"Loại dòng\""))
        assertTrue(csv.contains("\"Học phí, \"\"kỳ 1\"\"\nđợt hai\""))
        assertTrue(csv.contains("\"Hóa đơn.pdf\nẢnh.jpg\""))
        assertTrue(csv.contains(DECLARATION))
    }
    @Test fun R09_frozenPreview() {
        val old = report()
        repo.saveEntry(Db.Transaction("first","study","PERSONAL",null,20261101,120000,"Chuyển nguồn"),true)
        assertEquals(200000.toBigInteger(),old.figures.spent)
        assertEquals(80000.toBigInteger(),report().figures.spent)
    }
    @Test fun R10_invalidPeriodAndMissingLoan() {
        assertThrows(IllegalArgumentException::class.java) { report(202613) }
        assertThrows(IllegalArgumentException::class.java) { repo.report("missing",202611) }
    }
    @Test fun R11_writeFailurePropagates() {
        assertThrows(java.io.IOException::class.java) {
            ReportWriter.write(report(),ReportFormat.CSV,object: java.io.OutputStream() {
                override fun write(b: Int) { throw java.io.IOException("qa") }
            })
        }
    }
    @Test fun R12_migrateV1WithData() {
        val name = "report-migration.db"
        val schema = JSONObject(File("schemas/vn.studentfinance.data.local.FinanceDatabase/1.json").readText())
            .getJSONObject("database")
        context.openOrCreateDatabase(name,Context.MODE_PRIVATE,null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                fun sql(s: String) = old.execSQL(s.replace("$" + "{TABLE_NAME}",entity.getString("tableName")))
                sql(entity.getString("createSql"))
                val indices = entity.getJSONArray("indices")
                for (j in 0 until indices.length()) sql(indices.getJSONObject(j).getString("createSql"))
            }
            old.execSQL("INSERT INTO loans VALUES ('legacy','Khoản cũ',500)")
            old.execSQL("INSERT INTO disbursements VALUES ('d','legacy','Đợt cũ',20261001,300)")
            old.version = 1
        }
        val upgraded = Room.databaseBuilder(context,Db::class.java,name).addMigrations(Db.MIGRATION_1_2).allowMainThreadQueries().build()
        try {
            val migrated = FinanceRepository(upgraded)
            assertEquals(300.toBigInteger(),migrated.report("legacy",202611).figures.opening)
            migrated.saveReportHeader("legacy","An","Quỹ")
            assertEquals("Quỹ",migrated.report("legacy",202611).lender)
        } finally { upgraded.close() }
    }
}
