package vn.studentfinance.ui

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import vn.studentfinance.data.local.FinanceDatabase
import java.util.concurrent.Executors
import vn.studentfinance.data.repository.*
import vn.studentfinance.report.LoanReport
import vn.studentfinance.report.loanFigures
import vn.studentfinance.report.MonthlyReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.util.Calendar

fun Calendar.dayKey() = get(Calendar.YEAR) * 10000 + (get(Calendar.MONTH) + 1) * 100 + get(Calendar.DAY_OF_MONTH)
private fun Iterable<Long>.total() = fold(BigInteger.ZERO) { sum, value -> sum + value.toBigInteger() }
private fun share(value: BigInteger, total: BigInteger) =
    if (total.signum() == 0) 0 else (value * 100.toBigInteger() / total).coerceAtMost(Int.MAX_VALUE.toBigInteger()).toInt()

data class Slice(val category: String, val amount: BigInteger, val millionths: Int)
data class BudgetLine(val id: String, val category: String, val spent: BigInteger, val limit: BigInteger, val percent: Int) {
    val remaining get() = limit - spent
}

data class ScreenData(val ledger: Ledger, val entries: List<Entry>, val income: BigInteger,
                      val spent: BigInteger, val capital: BigInteger, val slices: List<Slice>,
                      val budgets: List<BudgetLine>, val budgetPercent: Int,
                      val reports: Map<String, LoanReport>) {
    val disbursed = reports.values.fold(BigInteger.ZERO) { sum, row -> sum + row.received }
    val net = income + disbursed - spent
}

data class WriteResult(val token: String, val error: String?)

class FinanceViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = FinanceRepository(FinanceDatabase.open(app))
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    val state = MutableLiveData<LoadState>(LoadState.Loading)
    val result = MutableLiveData<WriteResult>()
    private val writes = mutableSetOf<String>()

    init { refresh() }

    fun refresh() {
        io.execute {
            val next = try { LoadState.Ready(repository.read()) } catch (e: Exception) { LoadState.Failure }
            main.post { state.value = next }
        }
    }

    fun pending(token: String) = token in writes
    fun submit(token: String, change: FinanceRepository.() -> Unit) {
        if (!writes.add(token)) return
        io.execute {
            val error = try { repository.change(); null }
                catch (e: IllegalArgumentException) { e.message ?: "Dữ liệu không hợp lệ." }
                catch (e: Exception) { "Không thể lưu thay đổi vào cơ sở dữ liệu." }
            val next = try { LoadState.Ready(repository.read()) } catch (e: Exception) { LoadState.Failure }
            main.post {
                writes.remove(token)
                state.value = next
                result.value = WriteResult(token, error)
            }
        }
    }
    override fun onCleared() { io.shutdown() }

    private val calendar = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
    val month get() = calendar.dayKey() / 100


    fun changeMonth(delta: Int) {
        calendar.add(Calendar.MONTH, delta)
        state.value = state.value
    }

    suspend fun report(loan: String, month: Int): MonthlyReport = withContext(Dispatchers.IO) {
        repository.report(loan, month)
    }

    suspend fun saveReportHeader(loan: String, author: String, lender: String) = withContext(Dispatchers.IO) {
        repository.saveReportHeader(loan, author, lender)
    }

    fun project(ledger: Ledger) = monthlySummary(ledger, month)
}

fun monthlySummary(ledger: Ledger, month: Int): ScreenData {
    val entries = ledger.entries.filter { it.day / 100 == month }.sortedByDescending { it.day }
    val costs = entries.filterNot { it.income }
    val spent = costs.map { it.amount }.total()
    val byCategory = costs.groupBy { it.category }.mapValues { (_, rows) -> rows.map { it.amount }.total() }
    val slices = byCategory.entries.sortedByDescending { it.value }.map { (label, amount) ->
        Slice(label, amount, (amount * 1_000_000.toBigInteger() / spent).toInt())
    }
    val budgets = ledger.budgets.filter { it.month == month }.map {
        val used = byCategory[it.category] ?: BigInteger.ZERO
        BudgetLine(it.id, it.category, used, it.limit.toBigInteger(), share(used, it.limit.toBigInteger()))
    }
    val drawsByLoan = ledger.draws.groupBy { it.loanId }
    val usesByDraw = ledger.entries.filter { it.sourceId != null && !it.income }.groupBy { it.sourceId }
    val reports = ledger.loans.associate { loan ->
        val draws = drawsByLoan[loan.id].orEmpty()
        val uses = draws.flatMap { usesByDraw[it.id].orEmpty() }
        loan.id to loanFigures(draws, uses, month)
    }
    return ScreenData(ledger, entries, entries.filter { it.income }.map { it.amount }.total(),
        spent, reports.values.fold(BigInteger.ZERO) { sum, report -> sum + report.closing },
        slices, budgets, share(budgets.fold(BigInteger.ZERO) { a,b -> a+b.spent },
            budgets.fold(BigInteger.ZERO) { a,b -> a+b.limit }), reports)
}
