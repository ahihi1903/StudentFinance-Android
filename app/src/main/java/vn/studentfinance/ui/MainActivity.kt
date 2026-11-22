package vn.studentfinance.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import vn.studentfinance.R
import vn.studentfinance.ui.budget.BudgetFragment
import vn.studentfinance.ui.dashboard.DashboardFragment
import vn.studentfinance.ui.ledger.TransactionsFragment
import vn.studentfinance.ui.loan.LoansFragment
import vn.studentfinance.ui.report.ReportsFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        val nav = findViewById<BottomNavigationView>(R.id.nav)
        val title = findViewById<TextView>(R.id.pageTitle)
        nav.setOnItemSelectedListener { item ->
            val tag = item.itemId.toString()
            if (supportFragmentManager.findFragmentById(R.id.host)?.tag != tag) {
                val fragment = when (item.itemId) {
                    R.id.tab_dashboard -> DashboardFragment()
                    R.id.tab_ledger -> TransactionsFragment()
                    R.id.tab_loans -> LoansFragment()
                    R.id.tab_budget -> BudgetFragment()
                    else -> ReportsFragment()
                }
                supportFragmentManager.beginTransaction().replace(R.id.host, fragment, tag).commitNow()
            }
            title.text = item.title
            true
        }
        nav.selectedItemId = savedInstanceState?.getInt("tab", R.id.tab_dashboard) ?: R.id.tab_dashboard
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("tab", findViewById<BottomNavigationView>(R.id.nav).selectedItemId)
        super.onSaveInstanceState(outState)
    }
}
