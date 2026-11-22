package vn.studentfinance.ui.dashboard

import android.view.View
import android.widget.LinearLayout
import vn.studentfinance.R
import vn.studentfinance.ui.*

class DashboardFragment : FinanceFragment(R.layout.fragment_dashboard) {
    override fun render(view: View, data: ScreenData) {
        view.label(R.id.balanceValue, money(data.capital))
        view.label(R.id.incomeValue, money(data.income))
        view.label(R.id.expenseValue, money(data.spent))
        view.label(R.id.drawValue, money(data.disbursed))
        view.label(R.id.netValue, money(data.net))
        val chart = view.findViewById<SpendingChart>(R.id.chart)
        chart.slices = data.slices
        val legend = view.findViewById<LinearLayout>(R.id.legend)
        legend.removeAllViews()
        if (data.slices.isEmpty()) record(legend, getString(R.string.empty_chart), "", "")
        data.slices.forEachIndexed { index, slice ->
            val row = record(legend, getString(R.string.chart_label, slice.category, money(slice.amount)), "", "")
            row.findViewById<android.widget.TextView>(R.id.name).setTextColor(requireContext().getColor(chart.palette[index % chart.palette.size]))
        }
        entries(view, data.entries.take(5), data)
        empty(view, data.entries.isEmpty(), R.string.empty_ledger)
    }
}
