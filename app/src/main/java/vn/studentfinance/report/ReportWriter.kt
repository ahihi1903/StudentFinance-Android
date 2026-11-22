package vn.studentfinance.report

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.OutputStream

enum class ReportFormat(val extension: String) { PDF("pdf"), CSV("csv") }

object ReportWriter {
    fun write(report: MonthlyReport, format: ReportFormat, out: OutputStream) {
        report.requireHeader()
        when (format) {
            ReportFormat.CSV -> csv(report, out)
            ReportFormat.PDF -> pdf(report, out)
        }
    }

    private fun csv(r: MonthlyReport, out: OutputStream) {
        val writer = out.bufferedWriter(Charsets.UTF_8)
        writer.write("\uFEFF")
        fun row(cells: List<String>) {
            writer.write(cells.joinToString(",") { "\"" + it.replace("\"", "\"\"") + "\"" })
            writer.write("\r\n")
        }
        fun text(s: String) = if (s.trimStart().firstOrNull() in listOf('=', '+', '-', '@')) "'$s" else s
        row(listOf("Loại dòng","Tháng","Người lập","Khoản vay","Đơn vị cho vay","Đầu kỳ (VND)",
            "Giải ngân (VND)","Chi vốn vay (VND)","Cuối kỳ (VND)","Ngày","Đợt giải ngân","Danh mục",
            "Nội dung","Số tiền (VND)","Chứng từ","Khai báo"))
        val meta = listOf(r.period, text(r.author), text(r.loan), text(r.lender))
        row(listOf("Tổng hợp") + meta + r.totals.map { it.second.toString() } + List(6) { "" } + DECLARATION)
        r.lines.forEach {
            row(listOf(it.kind) + meta + List(4) { "" } + listOf(it.date, text(it.draw), text(it.category),
                text(it.note), it.amount.toString(), text(it.receipts.joinToString("\n")), DECLARATION))
        }
        writer.flush()
    }

    private fun pdf(r: MonthlyReport, out: OutputStream) {
        val doc = PdfDocument()
        try {
            val ink = Color.rgb(26, 47, 48)
            val teal = Color.rgb(0, 95, 87)
            var page: PdfDocument.Page? = null
            var number = 0
            var y = 0f
            fun nextPage() {
                page?.let(doc::finishPage)
                page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, ++number).create())
                val c = page!!.canvas
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = teal; textSize = 12f; typeface = Typeface.DEFAULT_BOLD }
                c.drawText("StudentFinance", 40f, 42f, paint)
                paint.typeface = Typeface.DEFAULT
                paint.textSize = 10f
                c.drawText("Giải trình vốn vay · " + r.period, 365f, 42f, paint)
                c.drawLine(40f, 53f, 555f, 53f, paint)
                c.drawText("Dữ liệu do người dùng tự khai", 40f, 815f, paint)
                c.drawText("Trang " + number, 485f, 815f, paint)
                y = 80f
            }
            fun layoutFor(value: String, size: Float, bold: Boolean): StaticLayout {
                val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (bold) teal else ink
                    typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    textSize = size
                }
                return StaticLayout.Builder.obtain(value, 0, value.length, p, 515)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).build()
            }
            fun paragraph(value: String, size: Float = 11f, bold: Boolean = false, gap: Float = 3f) {
                val layout = layoutFor(value, size, bold)
                for (i in 0 until layout.lineCount) {
                    if (y + size > 788) nextPage()
                    page!!.canvas.drawText(value.substring(layout.getLineStart(i), layout.getLineEnd(i)).trimEnd('\n','\r'),
                        40f, y, layout.paint)
                    y += size * 1.45f
                }
                y += gap
            }
            nextPage()
            try {
                paragraph("Báo cáo giải trình vốn vay", 21f, true)
                paragraph("Tháng " + r.period, 13f)
                paragraph("Người lập: " + r.author)
                paragraph("Khoản vay: " + r.loan)
                paragraph("Đơn vị cho vay: " + r.lender)
                paragraph(DECLARATION, 10f, gap = 10f)
                r.totals.forEach { (label, value) -> paragraph(label + ": " + vnd(value), 12f, true) }
                y += 7f
                paragraph("Chi tiết phát sinh", 15f, true)
                if (r.lines.isEmpty()) paragraph("Không có phát sinh trong tháng.")
                r.lines.forEach { line ->
                    val title = line.date + "  ·  " + line.kind + "  ·  " + vnd(line.amount)
                    val details = buildList {
                        add("Đợt: " + line.draw)
                        if (line.expense) {
                            add("Danh mục: " + line.category)
                            if (line.note.isNotEmpty()) add("Nội dung: " + line.note)
                            add("Chứng từ: " + line.receipts.ifEmpty { listOf("Chưa đính kèm") }.joinToString(", "))
                        }
                    }
                    val height = layoutFor(title, 12f, true).lineCount * 17.4f + 3f +
                        details.sumOf { layoutFor(it, 11f, false).lineCount * 16 + 3 }
                    if (y + height > 788 && (height < 708 || y > 728)) nextPage()
                    paragraph(title, 12f, true)
                    details.forEach { paragraph(it) }
                    y += 6f
                }
            } finally { page?.let(doc::finishPage) }
            doc.writeTo(out)
        } finally { doc.close() }
    }
}
