package vn.studentfinance.ui.report

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import vn.studentfinance.R
import vn.studentfinance.report.*
import vn.studentfinance.ui.*
import java.io.File
import java.io.IOException

class ReportsFragment : FinanceFragment(R.layout.fragment_reports) {
    private var selected: String? = null
    private var loaded: MonthlyReport? = null
    private var readJob: Job? = null
    private var headerLoan: String? = null
    private var staged: String? = null
    private var preparing = false
    private val pdf = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf"), ::finishExport)
    private val csv = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv"), ::finishExport)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selected = savedInstanceState?.getString("loan")
        headerLoan = savedInstanceState?.getString("headerLoan")
        staged = savedInstanceState?.getString("staged")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("loan", selected)
        outState.putString("headerLoan", headerLoan)
        outState.putString("staged", staged)
    }

    override fun render(view: View, data: ScreenData) {
        view.label(R.id.disclosure, DECLARATION)
        val loans = data.ledger.loans
        val selector = view.findViewById<Spinner>(R.id.selector)
        selector.onItemSelectedListener = null
        selector.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, loans.map { it.name })
        selector.isEnabled = loans.isNotEmpty()
        val pos = loans.indexOfFirst { it.id == selected }.coerceAtLeast(0)
        selector.setSelection(pos)
        load(view, loans.getOrNull(pos)?.id)
        selector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, item: View?, pos: Int, id: Long) {
                val key = loans.getOrNull(pos)?.id
                if (key != selected) load(view, key)
            }
        }
        view.findViewById<View>(R.id.exportPdf).setOnClickListener { prepare(view, ReportFormat.PDF) }
        view.findViewById<View>(R.id.exportCsv).setOnClickListener { prepare(view, ReportFormat.CSV) }
    }

    private fun buttons() {
        val enabled = loaded != null && staged == null && !preparing
        view?.findViewById<View>(R.id.exportPdf)?.isEnabled = enabled
        view?.findViewById<View>(R.id.exportCsv)?.isEnabled = enabled
    }

    private fun load(view: View, id: String?) {
        readJob?.cancel()
        selected = id
        loaded = null
        buttons()
        val summary = view.findViewById<LinearLayout>(R.id.summary)
        val rows = view.findViewById<LinearLayout>(R.id.rows)
        summary.removeAllViews()
        rows.removeAllViews()
        if (id != headerLoan) {
            headerLoan = null
            view.findViewById<EditText>(R.id.author).setText("")
            view.findViewById<EditText>(R.id.lender).setText("")
        }
        empty(view, true, if (id == null) R.string.empty_reports else R.string.loading)
        if (id == null) return
        val month = model.month
        readJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val report = model.report(id, month)
                if (headerLoan != id) {
                    view.findViewById<EditText>(R.id.author).setText(report.author)
                    view.findViewById<EditText>(R.id.lender).setText(report.lender)
                    headerLoan = id
                }
                report.totals.forEach { (label, value) -> record(summary, label, "", money(value)) }
                report.lines.forEach {
                    val detail = listOf(it.draw, it.category, it.note).filter(String::isNotBlank).joinToString(" · ") +
                        if (it.expense) "\nChứng từ: " + it.receipts.ifEmpty { listOf("Chưa đính kèm") }.joinToString(", ") else ""
                    record(rows, it.date + " · " + it.kind, detail, money(it.amount))
                }
                empty(view, report.lines.isEmpty(), R.string.report_empty)
                loaded = report
                buttons()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                empty(view, true, R.string.error)
                view.findViewById<View>(R.id.retry).show(true)
            }
        }
    }

    private fun prepare(view: View, format: ReportFormat) {
        val report = loaded ?: return
        val person = view.findViewById<EditText>(R.id.author)
        val org = view.findViewById<EditText>(R.id.lender)
        for (field in listOf(person, org)) {
            if (field.text.isBlank()) {
                field.error = getString(R.string.required)
                field.requestFocus()
                return
            }
        }
        val snapshot = report.copy(author = person.text.toString().trim(), lender = org.text.toString().trim())
        val cache = requireContext().cacheDir
        preparing = true
        buttons()
        viewLifecycleOwner.lifecycleScope.launch {
            var file: File? = null
            try {
                model.saveReportHeader(snapshot.loanId, snapshot.author, snapshot.lender)
                withContext(Dispatchers.IO) {
                    val temp = File.createTempFile("monthly-", "." + format.extension, cache)
                    file = temp
                    temp.outputStream().use { ReportWriter.write(snapshot, format, it) }
                }
                staged = file!!.name
                (if (format == ReportFormat.PDF) pdf else csv).launch("StudentFinance-" + snapshot.month + "." + format.extension)
                file = null
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                staged = null
                Toast.makeText(requireContext(), R.string.export_failed, Toast.LENGTH_LONG).show()
            } finally {
                file?.delete()
                preparing = false
                buttons()
            }
        }
    }

    private fun finishExport(uri: Uri?) {
        val context = requireContext().applicationContext
        val name = staged ?: return
        val file = File(context.cacheDir, name)
        staged = null
        buttons()
        model.viewModelScope.launch {
            try {
                if (uri != null) {
                    withContext(Dispatchers.IO) {
                        file.inputStream().use { input ->
                            val stream = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException()
                            stream.use { input.copyTo(it) }
                        }
                    }
                    Toast.makeText(context, R.string.export_saved, Toast.LENGTH_SHORT).show()
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Toast.makeText(context, R.string.export_failed, Toast.LENGTH_LONG).show() }
            finally { file.delete() }
        }
    }
}
