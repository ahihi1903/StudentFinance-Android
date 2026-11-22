package vn.studentfinance.ui

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import vn.studentfinance.R
import vn.studentfinance.data.local.FinanceDatabase as Db
import vn.studentfinance.data.repository.*
import java.util.Calendar
import java.util.UUID

private data class Choice(val key: String, val label: String) {
    override fun toString() = label
}

class EditorDialog : DialogFragment() {
    private val model get() = ViewModelProvider(requireActivity())[FinanceViewModel::class.java]
    private val args get() = requireArguments()
    private val type get() = RecordType.valueOf(args.getString("type")!!)
    private val id get() = args.getString("id")!!
    private val token get() = args.getString("token")!!
    private val edit get() = args.getBoolean("edit")
    private val deleting get() = args.getBoolean("delete")
    private lateinit var form: View
    private lateinit var save: View
    private var draft: Bundle? = null
    private var day = Calendar.getInstance().dayKey()
    private var initialized = false
    private var attached: Db.Receipt? = null
    private var readingReceipt = false
    private var savedReceipts = emptyList<Db.Receipt>()
    private val chooseReceipt = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        readingReceipt = true
        save.isEnabled = false
        val resolver = requireContext().contentResolver
        val entryId = id
        lifecycleScope.launch {
            val metadata = withContext(Dispatchers.IO) {
                runCatching {
                    val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                        if (it.moveToFirst()) it.getString(0) else null
                    }
                    Db.Receipt(UUID.randomUUID().toString(), entryId, uri.toString(),
                        name ?: uri.lastPathSegment.orEmpty(), resolver.getType(uri) ?: "application/octet-stream")
                }.getOrNull()
            }
            readingReceipt = false
            save.isEnabled = model.state.value is LoadState.Ready && !model.pending(token)
            if (metadata == null) Toast.makeText(requireContext(), R.string.receipt_error, Toast.LENGTH_LONG).show()
            else { attached = metadata; receiptLabel() }
        }
    }

    private fun receiptLabel() {
        if (!::form.isInitialized) return
        val names = savedReceipts.map { it.displayName } + listOfNotNull(attached?.displayName)
        form.label(R.id.receiptNames, names.joinToString("\n"))
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        draft = savedInstanceState
        attached = savedInstanceState?.getStringArray("receipt")?.let { Db.Receipt(it[0], id, it[1], it[2], it[3]) }
        initialized = false
        if (deleting) return MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (type == RecordType.ENTRY) R.string.confirm else R.string.confirm_record)
            .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.delete, null).create()
        val editor = layoutInflater.inflate(R.layout.dialog_editor, null)
        form = layoutInflater.inflate(if (type == RecordType.ENTRY) R.layout.form_transaction else R.layout.form_loan,
            editor.findViewById<FrameLayout>(R.id.fields), true)
        val title = when (type) {
            RecordType.ENTRY -> if (edit) R.string.edit_entry else R.string.new_entry
            RecordType.LOAN -> if (edit) R.string.edit_loan else R.string.new_loan
            RecordType.DRAW -> if (edit) R.string.edit_draw else R.string.new_draw
            RecordType.BUDGET -> if (edit) R.string.edit_budget else R.string.new_budget
        }
        editor.label(R.id.editorTitle, getString(title))
        editor.findViewById<View>(android.R.id.button2).setOnClickListener { dismiss() }
        save = editor.findViewById(android.R.id.button1)
        save.isEnabled = false
        return Dialog(requireContext(), R.style.Theme_StudentFinance).apply {
            setContentView(editor)
            window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, false)
                it.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
            ViewCompat.setOnApplyWindowInsetsListener(editor) { root, insets ->
                val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
                    or WindowInsetsCompat.Type.displayCutout())
                root.setPadding(safe.left, safe.top, safe.right, safe.bottom)
                WindowInsetsCompat.CONSUMED
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (deleting) {
            save = (requireDialog() as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            val recordType = type
            val recordId = id
            save.setOnClickListener { submit { delete(recordType, recordId) } }
        }
        model.result.removeObservers(this)
        model.result.observe(this) { result ->
            if (result.token != token) return@observe
            if (result.error == null) dismissAllowingStateLoss()
            else {
                save.isEnabled = true
                Toast.makeText(requireContext(), result.error, Toast.LENGTH_LONG).show()
            }
        }
        model.state.removeObservers(this)
        model.state.observe(this) { state ->
            if (!deleting && !initialized && state is LoadState.Ready) {
                bind(state.ledger)
                initialized = true
            }
            save.isEnabled = state is LoadState.Ready && !readingReceipt && !model.pending(token)
        }
    }

    private fun bind(ledger: Ledger) {
        val loan = ledger.loans.find { it.id == id }
        val draw = ledger.draws.find { it.id == id }
        val entry = ledger.entries.find { it.id == id }
        val budget = ledger.budgets.find { it.id == id }
        val amount = entry?.amount ?: loan?.limit ?: draw?.amount ?: budget?.limit
        field(R.id.amount).setText(draft?.getString("amount") ?: amount?.toString().orEmpty())
        day = draft?.getInt("day") ?: entry?.day ?: draw?.day ?: if (type == RecordType.BUDGET)
            (budget?.month ?: args.getInt("month")) * 100 + 1 else day

        if (type == RecordType.ENTRY) {
            savedReceipts = entry?.receipts.orEmpty()
            receiptLabel()
            form.findViewById<View>(R.id.chooseReceipt).setOnClickListener { chooseReceipt.launch(arrayOf("image/*", "application/pdf")) }
            field(R.id.note).setText(draft?.getString("note") ?: entry?.note.orEmpty())
            val kind = spinner(R.id.kind)
            bindOptions(kind, listOf(Choice("expense", getString(R.string.expense_kind)),
                Choice("income", getString(R.string.income_kind))), draft?.getString("kind") ?: if (entry?.income == true) "income" else "expense")
            var lastKind = key(R.id.kind)
            fun choices(income: Boolean, category: String?, source: String?) {
                categories(ledger, income, category)
                val options = mutableListOf<Choice>()
                if (!income) options.add(Choice("", getString(R.string.choose_source)))
                options.add(Choice(PERSONAL, getString(R.string.individual)))
                if (!income) ledger.draws.forEach { d ->
                    val parent = ledger.loans.first { it.id == d.loanId }
                    options.add(Choice(d.id, getString(R.string.row_detail, parent.name, d.name)))
                }
                bindOptions(spinner(R.id.source), options, source)
            }
            choices(lastKind == "income", draft?.getString("category") ?: entry?.categoryId,
                draft?.getString("source") ?: entry?.let { it.sourceId ?: PERSONAL })
            kind.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                override fun onItemSelected(parent: AdapterView<*>?, item: View?, pos: Int, rowId: Long) {
                    val selected = key(R.id.kind)
                    if (selected != lastKind) { lastKind = selected; choices(selected == "income", null, null) }
                }
            }
        } else {
            val nameField = form.findViewById<TextInputLayout>(R.id.nameField)
            nameField.show(type != RecordType.BUDGET)
            nameField.hint = getString(if (type == RecordType.DRAW) R.string.draw_name else R.string.loan_name)
            field(R.id.name).setText(draft?.getString("name") ?: loan?.name ?: draw?.name.orEmpty())
            if (type == RecordType.DRAW) form.findViewById<TextInputLayout>(R.id.amountField).hint = getString(R.string.amount)
            if (type == RecordType.BUDGET) {
                spinner(R.id.category).show(true)
                categories(ledger, false, draft?.getString("category") ?: budget?.categoryId)
            }
        }
        if (type != RecordType.LOAN) {
            val date = form.findViewById<Button>(R.id.date)
            fun label() { date.text = if (type == RecordType.BUDGET) getString(R.string.month, dayLabel(day).substring(3)) else dayLabel(day) }
            date.show(true)
            label()
            date.setOnClickListener {
                DatePickerDialog(requireContext(), { _, y, m, d -> day = y * 10000 + (m + 1) * 100 + d; label() },
                    day / 10000, day / 100 % 100 - 1, day % 100).show()
            }
        }
        save.setOnClickListener { saveForm(draw?.loanId ?: args.getString("parent")) }
    }

    private fun saveForm(parentId: String?) {
        val amount = field(R.id.amount).text.toString().trim().toLongOrNull()
        if (amount == null || amount <= 0) {
            field(R.id.amount).error = getString(R.string.positive)
            field(R.id.amount).requestFocus()
            return
        }
        val rowId = id
        val editing = edit
        val whenDay = day
        when (type) {
            RecordType.ENTRY -> {
                val source = key(R.id.source)
                if (source.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.choose_source, Toast.LENGTH_SHORT).show()
                    return
                }
                val row = Db.Transaction(rowId, key(R.id.category), if (source == PERSONAL) Funding.PERSONAL.name else Funding.LOAN.name,
                    source.takeUnless { it == PERSONAL }, whenDay, amount, field(R.id.note).text.toString().trim())
                val receipts = listOfNotNull(attached)
                submit { saveEntry(row, editing, receipts) }
            }
            RecordType.BUDGET -> {
                val row = Db.Budget(rowId, whenDay / 100, key(R.id.category), amount)
                submit { saveBudget(row, editing) }
            }
            else -> {
                val name = field(R.id.name).text.toString().trim()
                if (name.isBlank()) { field(R.id.name).error = getString(R.string.required); return }
                if (type == RecordType.LOAN) submit { saveLoan(Db.Loan(rowId, name, amount), editing) }
                else submit { saveDraw(Db.Disbursement(rowId, parentId.orEmpty(), name, whenDay, amount), editing) }
            }
        }
    }

    private fun submit(change: FinanceRepository.() -> Unit) {
        save.isEnabled = false
        model.submit(token, change)
    }
    private fun field(id: Int) = form.findViewById<EditText>(id)
    private fun spinner(id: Int) = form.findViewById<Spinner>(id)
    private fun key(id: Int) = (spinner(id).selectedItem as? Choice)?.key.orEmpty()
    private fun categories(ledger: Ledger, income: Boolean, selected: String?) =
        bindOptions(spinner(R.id.category), ledger.categories.filter { it.income == income }.map { Choice(it.id, it.name) }, selected)

    private fun bindOptions(spinner: Spinner, options: List<Choice>, selected: String?) {
        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, options)
        spinner.setSelection(options.indexOfFirst { it.key == selected }.coerceAtLeast(0))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (deleting || !initialized) return
        outState.putString("amount", field(R.id.amount).text.toString())
        outState.putInt("day", day)
        if (type != RecordType.ENTRY) outState.putString("name", field(R.id.name).text.toString())
        if (type == RecordType.BUDGET || type == RecordType.ENTRY) outState.putString("category", key(R.id.category))
        if (type == RecordType.ENTRY) {
            attached?.let { outState.putStringArray("receipt", arrayOf(it.id, it.uri, it.displayName, it.mimeType)) }
            outState.putString("kind", key(R.id.kind))
            outState.putString("source", key(R.id.source))
            outState.putString("note", field(R.id.note).text.toString())
        }
    }

    companion object {
        private const val PERSONAL = "personal"
        fun open(manager: FragmentManager, type: RecordType, id: String? = null, parent: String? = null,
                 month: Int = 0, delete: Boolean = false) {
            if (manager.findFragmentByTag("editor") != null) return
            EditorDialog().apply { arguments = Bundle().apply {
                putString("type", type.name); putString("id", id ?: UUID.randomUUID().toString())
                putString("token", UUID.randomUUID().toString()); putBoolean("edit", id != null)
                putBoolean("delete", delete); putString("parent", parent); putInt("month", month)
            } }.show(manager, "editor")
        }
    }
}
