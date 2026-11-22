package vn.studentfinance.ui.loan

import android.view.View
import android.widget.LinearLayout
import vn.studentfinance.R
import vn.studentfinance.data.repository.RecordType
import com.google.android.material.button.MaterialButton
import vn.studentfinance.ui.*

class LoansFragment : FinanceFragment(R.layout.fragment_loans) {
    override fun render(view: View, data: ScreenData) {
        view.findViewById<View>(R.id.create).setOnClickListener { EditorDialog.open(childFragmentManager, RecordType.LOAN) }
        val rows = view.findViewById<LinearLayout>(R.id.rows)
        rows.removeAllViews()
        data.ledger.loans.forEach { loan ->
            val row = record(rows, loan.name, getString(R.string.loan_limit, money(loan.limit)),
                getString(R.string.remaining_capital, money(data.reports.getValue(loan.id).available)))
            actions(row, RecordType.LOAN, loan.id)
            val draws = data.ledger.draws.filter { it.loanId == loan.id }
            if (draws.isEmpty()) record(rows, "", getString(R.string.no_draw), "")
            draws.forEach { actions(record(rows, it.name, dayLabel(it.day), money(it.amount)), RecordType.DRAW, it.id) }
            rows.addView(MaterialButton(requireContext()).apply {
                setText(R.string.new_draw)
                setOnClickListener { EditorDialog.open(childFragmentManager, RecordType.DRAW, parent = loan.id) }
            })
        }
        empty(view, data.ledger.loans.isEmpty(), R.string.empty_loans)
    }
}
