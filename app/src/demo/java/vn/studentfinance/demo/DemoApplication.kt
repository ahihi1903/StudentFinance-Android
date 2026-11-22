package vn.studentfinance.demo

import android.app.Application
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import vn.studentfinance.data.local.FinanceDatabase
import java.util.Calendar

class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FinanceDatabase.open(this, DemoData())
    }
}

class DemoData(private val today: Calendar = Calendar.getInstance()) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        fun monthOf(c: Calendar) = c.get(Calendar.YEAR) * 100 + c.get(Calendar.MONTH) + 1
        val month = monthOf(today)
        val previous = (today.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -1)
        }
        val previousMonth = monthOf(previous)
        fun day(n: Int) = month * 100 + minOf(n, today.get(Calendar.DAY_OF_MONTH))
        fun insert(sql: String, vararg args: Any?) = db.execSQL(sql, args)

        insert("INSERT INTO loans(id,name,limitVnd) VALUES (?,?,?)", "demo-study", "Vay học tập", 5_000_000)
        insert("INSERT INTO report_headers(loanId,author,lender) VALUES (?,?,?)",
            "demo-study", "Nguyễn Minh Anh (mẫu)", "Quỹ hỗ trợ sinh viên (mẫu)")
        val drawSql = "INSERT INTO disbursements(id,loanId,name,day,amount) VALUES (?,'demo-study',?,?,?)"
        insert(drawSql, "demo-first", "Đợt nhập học", previousMonth * 100 + 5, 2_000_000)
        insert(drawSql, "demo-second", "Đợt bổ sung", day(1), 700_000)

        fun entry(id: String, date: Int, category: String, amount: Long, note: String, draw: String? = null) {
            insert("INSERT INTO transactions(id,categoryId,source,disbursementId,day,amount,note) VALUES (?,?,?,?,?,?,?)",
                id, category, if (draw == null) "PERSONAL" else "LOAN", draw, date, amount, note)
        }
        entry("demo-fee", previousMonth * 100 + 9, "study", 300_000, "Lệ phí nhập học", "demo-first")
        entry("demo-income", day(1), "allowance", 1_500_000, "Gia đình gửi tiền sinh hoạt")
        entry("demo-books", day(3), "study", 120_000, "Mua giáo trình học kỳ mới", "demo-second")
        entry("demo-print", day(4), "study", 80_000, "In tài liệu ôn thi")
        entry("demo-lunch", day(5), "food", 45_000, "Ăn trưa tại căng tin")
        entry("demo-dinner", day(6), "food", 65_000, "Ăn tối sau buổi học nhóm")
        entry("demo-bus", day(7), "travel", 100_000, "Nạp thẻ xe buýt")
        entry("demo-tutor", day(8), "income-other", 600_000, "Tiền dạy kèm cuối tuần")
        entry("demo-dorm", day(9), "living", 350_000, "Phí ký túc xá")
        entry("demo-electricity", day(10), "living", 90_000, "Chia tiền điện phòng")
        entry("demo-wifi", day(11), "living", 50_000, "Góp tiền Internet")
        entry("demo-parking", day(12), "travel", 10_000, "Gửi xe tại thư viện")
        entry("demo-snack", day(13), "food", 25_000, "Bữa phụ trước giờ thực hành")
        entry("demo-stationery", day(14), "study", 35_000, "Vở và dụng cụ vẽ", "demo-second")
        entry("demo-lab", day(15), "study", 150_000, "Vật tư thực hành", "demo-first")
        insert("INSERT INTO receipts(id,transactionId,uri,displayName,mimeType) VALUES (?,?,?,?,?)",
            "demo-receipt", "demo-books", "file:///android_asset/phieu-giao-trinh.svg", "Phiếu mua giáo trình.svg", "image/svg+xml")
        val budgetSql = "INSERT INTO budgets(id,month,categoryId,limitVnd) VALUES (?,?,?,?)"
        insert(budgetSql, "demo-budget-study", month, "study", 300_000)
        insert(budgetSql, "demo-budget-food", month, "food", 100_000)
        insert(budgetSql, "demo-budget-travel", month, "travel", 150_000)
    }
}
