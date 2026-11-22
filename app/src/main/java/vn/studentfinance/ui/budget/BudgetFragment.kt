package vn.studentfinance.ui.budget

import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import vn.studentfinance.R
import vn.studentfinance.data.repository.RecordType
import vn.studentfinance.ui.*

class BudgetFragment : FinanceFragment(R.layout.fragment_budget) {
    override fun render(view: View, data: ScreenData) {
        view.findViewById<View>(R.id.create).setOnClickListener {
            EditorDialog.open(childFragmentManager, RecordType.BUDGET, month = model.month)
        }
        view.label(R.id.overall, if (data.budgets.isEmpty()) getString(R.string.budget_empty_value)
            else getString(R.string.percent, data.budgetPercent))
        view.findViewById<ProgressBar>(R.id.progress).progress = data.budgetPercent
        val rows = view.findViewById<LinearLayout>(R.id.rows)
        rows.removeAllViews()
        data.budgets.forEach {
            val row = layoutInflater.inflate(R.layout.item_budget, rows, false)
            row.label(R.id.name, it.category)
            row.label(R.id.detail, getString(R.string.budget_details, money(it.spent), money(it.limit)))
            row.findViewById<ProgressBar>(R.id.progress).progress = it.percent
            row.label(R.id.remaining, getString(if (it.remaining.signum() < 0) R.string.budget_over else R.string.budget_left, money(it.remaining.abs())))
            actions(row, RecordType.BUDGET, it.id)
            rows.addView(row)
        }
        empty(view, data.budgets.isEmpty(), R.string.empty_budget)
    }
}
