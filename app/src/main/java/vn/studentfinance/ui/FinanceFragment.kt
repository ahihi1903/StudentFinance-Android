package vn.studentfinance.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import vn.studentfinance.R
import vn.studentfinance.data.repository.RecordType
import vn.studentfinance.data.repository.Entry
import vn.studentfinance.data.repository.LoadState
import java.text.NumberFormat
import java.util.Locale

fun View.label(id: Int, value: CharSequence) { findViewById<TextView>(id).text = value }
fun View.show(visible: Boolean) { visibility = if (visible) View.VISIBLE else View.GONE }
fun dayLabel(day: Int) = String.format(Locale.ROOT, "%02d/%02d/%04d", day%100, day/100%100, day/10000)

abstract class FinanceFragment(layout: Int) : Fragment(layout) {
    protected val model get() = ViewModelProvider(requireActivity())[FinanceViewModel::class.java]
    protected fun money(number: Number): String = getString(R.string.money,
        NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(number))

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View?>(R.id.prev)?.setOnClickListener { model.changeMonth(-1) }
        view.findViewById<View?>(R.id.next)?.setOnClickListener { model.changeMonth(1) }
        view.findViewById<View>(R.id.retry).setOnClickListener { model.refresh() }
        model.state.observe(viewLifecycleOwner) { state ->
            view.findViewById<TextView?>(R.id.period)?.text = getString(R.string.month,
                String.format(Locale.ROOT, "%02d/%04d", model.month%100, model.month/100))
            val ready = state is LoadState.Ready
            val status = view.findViewById<View>(R.id.state)
            // Keep the status panel reachable even when its enclosing card is the last child.
            var branch = status
            while (branch !== view) {
                val parent = branch.parent as ViewGroup
                for (i in 0 until parent.childCount) {
                    val sibling = parent.getChildAt(i)
                    if (sibling !== branch) sibling.show(ready)
                }
                branch = parent
            }
            status.show(true)
            status.findViewById<View>(R.id.spinner).show(state == LoadState.Loading)
            status.findViewById<View>(R.id.retry).show(state == LoadState.Failure)
            when (state) {
                LoadState.Loading -> status.label(R.id.message, getString(R.string.loading))
                LoadState.Failure -> status.label(R.id.message, getString(R.string.error))
                is LoadState.Ready -> render(view, model.project(state.ledger))
            }
        }
    }

    protected abstract fun render(view: View, data: ScreenData)

    protected fun empty(view: View, visible: Boolean, message: Int) {
        view.findViewById<View>(R.id.state).show(visible)
        view.label(R.id.message, getString(message))
    }

    protected fun record(parent: LinearLayout, name: String, detail: String, value: String): View {
        val row = layoutInflater.inflate(R.layout.item_record, parent, false)
        row.label(R.id.name, name)
        row.findViewById<View>(R.id.name).show(name.isNotEmpty())
        row.label(R.id.detail, detail)
        row.label(R.id.value, value)
        row.findViewById<View>(R.id.detail).show(detail.isNotEmpty())
        row.findViewById<View>(R.id.value).show(value.isNotEmpty())
        parent.addView(row)
        return row
    }

    protected fun actions(row: View, type: RecordType, id: String) {
        row.findViewById<View>(R.id.actions).show(true)
        row.findViewById<View>(R.id.edit).setOnClickListener { EditorDialog.open(childFragmentManager, type, id) }
        row.findViewById<View>(R.id.delete).setOnClickListener { EditorDialog.open(childFragmentManager, type, id, delete = true) }
    }

    protected fun entries(view: View, list: List<Entry>, data: ScreenData, editable: Boolean = false) {
        val rows = view.findViewById<LinearLayout>(R.id.rows)
        rows.removeAllViews()
        list.forEach { entry ->
            val draw = data.ledger.draws.find { it.id == entry.sourceId }
            val source = if (draw == null) getString(R.string.individual) else
                getString(R.string.row_detail, data.ledger.loans.first { it.id == draw.loanId }.name, draw.name)
            val row = record(rows, entry.note.ifBlank { entry.category },
                getString(R.string.row_detail, dayLabel(entry.day), source),
                getString(if (entry.income) R.string.signed_income else R.string.signed_expense, money(entry.amount)))
            row.findViewById<TextView>(R.id.value).setTextColor(requireContext().getColor(
                if (entry.income) R.color.teal else R.color.red))
            if (editable) actions(row, RecordType.ENTRY, entry.id)
        }
    }
}
