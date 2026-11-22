package vn.studentfinance.data.repository

import vn.studentfinance.data.local.FinanceDatabase
import vn.studentfinance.data.local.FinanceDatabase as Db
import vn.studentfinance.report.*
import java.util.GregorianCalendar
import java.util.concurrent.Callable

data class Entry(val id: String, val income: Boolean, val amount: Long, val day: Int,
                 val category: String, val sourceId: String?, val note: String, val categoryId: String,
                 val receipts: List<Db.Receipt>)
data class Loan(val id: String, val name: String, val limit: Long)
data class Draw(val id: String, val loanId: String, val name: String, val day: Int, val amount: Long)
data class Budget(val id: String, val month: Int, val category: String, val categoryId: String, val limit: Long)
data class Ledger(val entries: List<Entry>, val loans: List<Loan>, val draws: List<Draw>,
                  val budgets: List<Budget>, val categories: List<Db.Category>)
sealed interface LoadState {
    data object Loading : LoadState
    data object Failure : LoadState
    data class Ready(val ledger: Ledger) : LoadState
}

enum class RecordType { ENTRY, LOAN, DRAW, BUDGET }
enum class Funding { PERSONAL, LOAN }

// Blocking methods. The ViewModel dispatches them onto its serial I/O executor.
class FinanceRepository(private val db: FinanceDatabase) {
    private val dao = db.dao()

    fun saveReportHeader(id: String, author: String, lender: String) {
        require(author.isNotBlank() && lender.isNotBlank()) { "Nhập người lập và đơn vị cho vay." }
        dao.saveHeader(Db.ReportHeader(id, author.trim(), lender.trim()))
    }

    fun report(id: String, month: Int): MonthlyReport = db.runInTransaction(Callable {
        require(month / 100 in 1000..9999 && month % 100 in 1..12)
        val loan = requireNotNull(dao.loan(id)) { "Khoản vay không còn trong sổ." }
        val header = dao.reportHeader(id)
        val start = month * 100 + 1
        val end = month * 100 + 31
        val categories = dao.categories().associateBy { it.id }
        val receipts = dao.reportReceipts(id, start, end).groupBy { it.transactionId }
        val draws = dao.reportDraws(id, end).map { Draw(it.id, id, it.name, it.day, it.amount) }
        val expenses = dao.reportExpenses(id, end).map {
            Entry(it.id, false, it.amount, it.day, categories.getValue(it.categoryId).name, it.disbursementId,
                it.note, it.categoryId, receipts[it.id].orEmpty())
        }
        val figures = loanFigures(draws, expenses, month)
        val names = draws.associate { it.id to it.name }
        val lines = draws.filter { it.day / 100 == month }.map {
            ReportLine(it.id, it.day, false, it.name, "", "", it.amount, emptyList())
        } + figures.entries.map {
            ReportLine(it.id, it.day, true, names.getValue(it.sourceId!!), it.category, it.note, it.amount,
                it.receipts.map { receipt -> receipt.displayName })
        }
        MonthlyReport(id, loan.name, month, header?.author.orEmpty(), header?.lender.orEmpty(), figures,
            lines.sortedWith(compareBy({ it.day }, { it.expense }, { it.id })))
    })


    fun read(): Ledger = db.runInTransaction(Callable {
        val categories = dao.categories()
        val byId = categories.associateBy { it.id }
        val receipts = dao.receipts().groupBy { it.transactionId }
        Ledger(dao.transactions().map {
            val category = byId.getValue(it.categoryId)
            Entry(it.id, category.income, it.amount, it.day, category.name, it.disbursementId, it.note, category.id, receipts[it.id].orEmpty())
        }, dao.loans().map { Loan(it.id, it.name, it.limitVnd) },
            dao.disbursements().map { Draw(it.id, it.loanId, it.name, it.day, it.amount) },
            dao.budgets().map { Budget(it.id, it.month, byId.getValue(it.categoryId).name, it.categoryId, it.limitVnd) },
            categories)
    })

    private fun positive(amount: Long) = require(amount > 0) { "Số tiền phải lớn hơn 0 đồng." }
    private fun validDay(day: Int) {
        require(day in 10000101..99991231 && runCatching {
            GregorianCalendar(day / 10000, day / 100 % 100 - 1, day % 100).apply { isLenient = false }.time
        }.isSuccess) { "Ngày giao dịch không hợp lệ." }
    }
    private fun changed(count: Int) = require(count == 1) { "Bản ghi đã bị xóa. Vui lòng mở lại danh sách." }

    fun saveLoan(row: Db.Loan, edit: Boolean) = db.runInTransaction {
        require(row.id.isNotBlank()) { "Thiếu mã bản ghi." }
        positive(row.limitVnd)
        require(row.name.isNotBlank()) { "Nhập tên khoản vay." }
        require(row.limitVnd >= dao.drawn(row.id, "")) { "Hạn mức thấp hơn tổng đã giải ngân." }
        if (edit) changed(dao.update(row)) else dao.insert(row)
    }

    fun saveDraw(row: Db.Disbursement, edit: Boolean) = db.runInTransaction {
        require(row.id.isNotBlank()) { "Thiếu mã bản ghi." }
        positive(row.amount)
        validDay(row.day)
        require(row.name.isNotBlank()) { "Nhập tên đợt giải ngân." }
        val loan = requireNotNull(dao.loan(row.loanId)) { "Chọn khoản vay hợp lệ." }
        require(!edit || dao.draw(row.id)?.loanId == row.loanId) { "Đợt giải ngân phải thuộc khoản vay ban đầu." }
        require(row.amount <= loan.limitVnd - dao.drawn(row.loanId, row.id)) { "Giải ngân vượt hạn mức khoản vay." }
        require(row.amount >= dao.used(row.id, "")) { "Số tiền giải ngân thấp hơn tổng đã sử dụng." }
        require(dao.earliestUse(row.id)?.let { row.day <= it } != false) { "Đã có khoản chi trước ngày giải ngân này." }
        if (edit) changed(dao.update(row)) else dao.insert(row)
    }

    fun saveEntry(row: Db.Transaction, edit: Boolean, newReceipts: List<Db.Receipt> = emptyList()) = db.runInTransaction {
        require(row.id.isNotBlank()) { "Thiếu mã giao dịch." }
        positive(row.amount)
        validDay(row.day)
        val category = requireNotNull(dao.category(row.categoryId)) { "Chọn danh mục hợp lệ." }
        when (row.source) {
            Funding.PERSONAL.name -> require(row.disbursementId == null) { "Tiền cá nhân không gắn với đợt giải ngân." }
            Funding.LOAN.name -> {
                require(!category.income) { "Tiền giải ngân không phải là thu nhập cá nhân." }
                val draw = requireNotNull(row.disbursementId?.let(dao::draw)) { "Chọn khoản vay và đợt giải ngân." }
                require(row.day >= draw.day) { "Ngày chi phải từ ngày giải ngân trở đi." }
                require(row.amount <= draw.amount - dao.used(draw.id, row.id)) { "Khoản chi vượt vốn còn lại của đợt giải ngân." }
            }
            else -> throw IllegalArgumentException("Chọn nguồn tiền cho giao dịch.")
        }
        if (edit) changed(dao.update(row)) else dao.insert(row)
        newReceipts.forEach { receipt ->
            require(receipt.transactionId == row.id && receipt.uri.isNotBlank() && receipt.displayName.isNotBlank()) {
                "Thông tin chứng từ không hợp lệ."
            }
            dao.insert(receipt)
        }
    }

    fun saveBudget(row: Db.Budget, edit: Boolean) = db.runInTransaction {
        require(row.id.isNotBlank()) { "Thiếu mã bản ghi." }
        positive(row.limitVnd)
        require(row.month / 100 in 1000..9999 && row.month % 100 in 1..12) { "Tháng không hợp lệ." }
        require(dao.category(row.categoryId)?.income == false) { "Chọn danh mục chi cho ngân sách." }
        require(!dao.hasBudget(row.month, row.categoryId, row.id)) { "Danh mục đã có ngân sách trong tháng này." }
        if (edit) changed(dao.update(row)) else dao.insert(row)
    }

    fun delete(type: RecordType, id: String) = db.runInTransaction {
        val deleted = when (type) {
            RecordType.ENTRY -> dao.deleteTransaction(id)
            RecordType.BUDGET -> dao.deleteBudget(id)
            RecordType.LOAN -> {
                require(dao.drawn(id, "") == 0L) { "Khoản vay đã có đợt giải ngân." }
                dao.deleteLoan(id)
            }
            RecordType.DRAW -> {
                require(dao.earliestUse(id) == null) { "Đợt giải ngân đã có khoản chi." }
                dao.deleteDisbursement(id)
            }
        }
        changed(deleted)
    }
}
