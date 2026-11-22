package vn.studentfinance.ui.ledger

import android.view.View
import vn.studentfinance.R
import vn.studentfinance.ui.*

class TransactionsFragment : FinanceFragment(R.layout.fragment_transactions) {
    override fun render(view: View, data: ScreenData) {
        view.findViewById<View>(R.id.create).setOnClickListener { EditorDialog.open(childFragmentManager, vn.studentfinance.data.repository.RecordType.ENTRY) }
        entries(view, data.entries, data, editable = true)
        empty(view, data.entries.isEmpty(), R.string.empty_ledger)
    }
}
