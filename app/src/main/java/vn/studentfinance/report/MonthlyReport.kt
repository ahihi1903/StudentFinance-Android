package vn.studentfinance.report

import vn.studentfinance.data.repository.Draw
import vn.studentfinance.data.repository.Entry
import java.math.BigInteger
import java.text.NumberFormat
import java.util.Locale

const val DECLARATION = "Dữ liệu do người dùng tự khai. Báo cáo không có xác nhận của ngân hàng."
data class LoanReport(val opening: BigInteger, val received: BigInteger, val spent: BigInteger,
                      val closing: BigInteger, val available: BigInteger, val entries: List<Entry>)
private fun Iterable<Long>.total() = fold(BigInteger.ZERO) { a, b -> a + b.toBigInteger() }

fun loanFigures(draws: List<Draw>, uses: List<Entry>, month: Int): LoanReport {
    val opening = draws.filter { it.day / 100 < month }.map { it.amount }.total() -
        uses.filter { it.day / 100 < month }.map { it.amount }.total()
    val received = draws.filter { it.day / 100 == month }.map { it.amount }.total()
    val rows = uses.filter { it.day / 100 == month }.sortedWith(compareBy({ it.day }, { it.id }))
    val spent = rows.map { it.amount }.total()
    return LoanReport(opening, received, spent, opening + received - spent,
        draws.map { it.amount }.total() - uses.map { it.amount }.total(), rows)
}
data class ReportLine(val id: String, val day: Int, val expense: Boolean, val draw: String, val category: String,
                      val note: String, val amount: Long, val receipts: List<String>) {
    val kind get() = if (expense) "Chi vốn vay" else "Giải ngân"
    val date get() = String.format(Locale.ROOT, "%02d/%02d/%04d", day % 100, day / 100 % 100, day / 10000)
}
data class MonthlyReport(val loanId: String, val loan: String, val month: Int, val author: String, val lender: String,
                         val figures: LoanReport, val lines: List<ReportLine>) {
    val period get() = String.format(Locale.ROOT, "%02d/%04d", month % 100, month / 100)
    val totals get() = listOf("Số dư đầu kỳ" to figures.opening, "Giải ngân trong kỳ" to figures.received,
        "Chi vốn vay trong kỳ" to figures.spent, "Số dư cuối kỳ" to figures.closing)
    fun requireHeader() = require(author.isNotBlank() && lender.isNotBlank()) { "Nhập người lập và đơn vị cho vay." }
}
fun vnd(value: Number): String = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(value) + " đ"
