package vn.studentfinance

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import vn.studentfinance.data.local.FinanceDatabase as Db
import vn.studentfinance.data.repository.FinanceRepository
import vn.studentfinance.report.*
import java.io.File

class ReportDeviceTest {
    @Test fun R13_nativePdfAndCsvFromRoom() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(ctx, Db::class.java).addCallback(Db.SEED).build()
        try {
            val repo = FinanceRepository(db)
            repo.saveLoan(Db.Loan("qa","Khoản vay học tập",10000000),false)
            repo.saveDraw(Db.Disbursement("d","qa","Đợt tháng mười",20261001,5000000),false)
            repo.saveReportHeader("qa","Nguyễn Thị Ánh","Quỹ hỗ trợ sinh viên")
            for (i in 1..18) {
                val id = "g$i"
                repo.saveEntry(Db.Transaction(id,"study","LOAN","d",20261100+i,10000L * i,
                    "Học phí, \"kỳ một\"\nPhiếu số $i"), false,
                    listOf(Db.Receipt("r$i",id,"content://qa/$i","Hóa đơn $i.pdf","application/pdf")))
            }
            val report = repo.report("qa",202611)
            assertEquals(3290000.toBigInteger(),report.figures.closing)
            for (format in ReportFormat.values()) {
                File(ctx.cacheDir,"qa-monthly." + format.extension).outputStream().use { ReportWriter.write(report,format,it) }
            }
            val fd = ParcelFileDescriptor.open(File(ctx.cacheDir,"qa-monthly.pdf"),ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            try {
                assertTrue(renderer.pageCount > 1)
                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    try {
                        val image = Bitmap.createBitmap(page.width,page.height,Bitmap.Config.ARGB_8888)
                        page.render(image,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        image.recycle()
                    } finally { page.close() }
                }
            } finally { renderer.close() }
        } finally { db.close() }
    }
}
