package vn.studentfinance.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;
import vn.studentfinance.data.local.FinanceDatabase.*;

@Dao
public interface FinanceDao {
    @Query("SELECT * FROM report_headers WHERE loanId=:loan") ReportHeader reportHeader(String loan);
    @androidx.room.Upsert void saveHeader(ReportHeader header);
    @Query("SELECT * FROM disbursements WHERE loanId=:loan AND day<=:end ORDER BY day,id")
    List<Disbursement> reportDraws(String loan, int end);
    String REPORT_JOIN = " FROM transactions t JOIN disbursements d ON t.disbursementId=d.id " +
            "JOIN categories c ON c.id=t.categoryId WHERE d.loanId=:loan AND t.source='LOAN' AND c.income=0 ";
    @Query("SELECT t.*" + REPORT_JOIN + "AND t.day<=:end ORDER BY t.day,t.id")
    List<Transaction> reportExpenses(String loan, int end);
    @Query("SELECT * FROM receipts WHERE transactionId IN (SELECT t.id" + REPORT_JOIN +
            "AND t.day BETWEEN :start AND :end) ORDER BY id")
    List<Receipt> reportReceipts(String loan, int start, int end);

    @Query("SELECT * FROM categories WHERE id=:id") Category category(String id);
    @Query("SELECT * FROM loans WHERE id=:id") Loan loan(String id);
    @Query("SELECT * FROM disbursements WHERE id=:id") Disbursement draw(String id);
    @Query("SELECT COALESCE(SUM(amount),0) FROM disbursements WHERE loanId=:loan AND id!=:except")
    long drawn(String loan, String except);
    @Query("SELECT COALESCE(SUM(amount),0) FROM transactions WHERE disbursementId=:draw AND id!=:except")
    long used(String draw, String except);
    @Query("SELECT MIN(day) FROM transactions WHERE disbursementId=:draw") Integer earliestUse(String draw);
    @Query("SELECT EXISTS(SELECT 1 FROM budgets WHERE month=:month AND categoryId=:category AND id!=:except)")
    boolean hasBudget(int month, String category, String except);

    @Query("SELECT * FROM loans ORDER BY id")
    List<Loan> loans();
    @Insert void insert(Loan value);
    @Update int update(Loan value);
    @Query("DELETE FROM loans WHERE id = :id") int deleteLoan(String id);

    @Query("SELECT * FROM disbursements ORDER BY day DESC, id")
    List<Disbursement> disbursements();
    @Insert void insert(Disbursement value);
    @Update int update(Disbursement value);
    @Query("DELETE FROM disbursements WHERE id = :id") int deleteDisbursement(String id);

    @Query("SELECT * FROM categories ORDER BY id")
    List<Category> categories();
    @Insert void insert(Category value);

    @Query("SELECT * FROM transactions ORDER BY day DESC, id")
    List<Transaction> transactions();
    @Insert void insert(Transaction value);
    @Update int update(Transaction value);
    @Query("DELETE FROM transactions WHERE id = :id") int deleteTransaction(String id);

    @Query("SELECT * FROM budgets ORDER BY id")
    List<Budget> budgets();
    @Insert void insert(Budget value);
    @Update int update(Budget value);
    @Query("DELETE FROM budgets WHERE id = :id") int deleteBudget(String id);

    @Query("SELECT * FROM receipts ORDER BY id")
    List<Receipt> receipts();
    @Insert void insert(Receipt value);
}
