package vn.studentfinance.data.local;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.*;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {FinanceDatabase.Loan.class, FinanceDatabase.Disbursement.class, FinanceDatabase.Category.class, FinanceDatabase.Transaction.class, FinanceDatabase.Budget.class, FinanceDatabase.Receipt.class, FinanceDatabase.ReportHeader.class}, version = 2, exportSchema = true)
public abstract class FinanceDatabase extends RoomDatabase {
    public abstract FinanceDao dao();
    private static volatile FinanceDatabase instance;

    public static FinanceDatabase open(Context context) {
        return open(context, null);
    }

    public static FinanceDatabase open(Context context, @Nullable Callback initialData) {
        synchronized (FinanceDatabase.class) {
            if (instance == null) {
                Builder<FinanceDatabase> builder = Room.databaseBuilder(context.getApplicationContext(),
                        FinanceDatabase.class, "student-finance.db").addMigrations(MIGRATION_1_2).addCallback(SEED);
                if (initialData != null) builder.addCallback(initialData);
                instance = builder.build();
            }
            return instance;
        }
    }

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE report_headers (loanId TEXT NOT NULL PRIMARY KEY, author TEXT NOT NULL, lender TEXT NOT NULL, " +
                    "FOREIGN KEY(loanId) REFERENCES loans(id) ON UPDATE NO ACTION ON DELETE CASCADE)");
        }
    };

    @Entity(tableName = "report_headers", foreignKeys = @ForeignKey(entity = Loan.class, parentColumns = "id",
            childColumns = "loanId", onDelete = ForeignKey.CASCADE))
    public static class ReportHeader {
        @NonNull @PrimaryKey public final String loanId;
        @NonNull public final String author;
        @NonNull public final String lender;
        public ReportHeader(@NonNull String loanId, @NonNull String author, @NonNull String lender) {
            this.loanId = loanId;
            this.author = author;
            this.lender = lender;
        }
    }

    public static final Callback SEED = new Callback() {
        @Override public void onCreate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("INSERT INTO categories(id, name, income) VALUES " +
                    "('study','Học tập',0),('food','Ăn uống',0),('living','Sinh hoạt',0)," +
                    "('travel','Đi lại',0),('expense-other','Khác',0)," +
                    "('allowance','Trợ cấp',1),('income-other','Khác',1)");
        }
    };

    @Entity(tableName = "loans")
    public static class Loan {
        @PrimaryKey @NonNull public final String id;
        @NonNull public final String name;
        public final long limitVnd;

        public Loan(@NonNull String id, @NonNull String name, long limitVnd) {
            this.id = id;
            this.name = name;
            this.limitVnd = limitVnd;
        }
    }

    @Entity(tableName = "disbursements", foreignKeys = @ForeignKey(entity = Loan.class, parentColumns = "id", childColumns = "loanId", onDelete = ForeignKey.RESTRICT), indices = @Index("loanId"))
    public static class Disbursement {
        @PrimaryKey @NonNull public final String id;
        @NonNull public final String loanId;
        @NonNull public final String name;
        public final int day;
        public final long amount;

        public Disbursement(@NonNull String id, @NonNull String loanId, @NonNull String name, int day, long amount) {
            this.id = id;
            this.loanId = loanId;
            this.name = name;
            this.day = day;
            this.amount = amount;
        }
    }

    @Entity(tableName = "categories", indices = @Index(value = {"income", "name"}, unique = true))
    public static class Category {
        @PrimaryKey @NonNull public final String id;
        @NonNull public final String name;
        public final boolean income;

        public Category(@NonNull String id, @NonNull String name, boolean income) {
            this.id = id;
            this.name = name;
            this.income = income;
        }
    }

    @Entity(tableName = "transactions", foreignKeys = {@ForeignKey(entity = Category.class, parentColumns = "id", childColumns = "categoryId", onDelete = ForeignKey.RESTRICT), @ForeignKey(entity = Disbursement.class, parentColumns = "id", childColumns = "disbursementId", onDelete = ForeignKey.RESTRICT)},
            indices = {@Index({"categoryId", "day"}), @Index("disbursementId"), @Index("day")})
    public static class Transaction {
        @PrimaryKey @NonNull public final String id;
        @NonNull public final String categoryId;
        @NonNull public final String source;
        @Nullable public final String disbursementId;
        public final int day;
        public final long amount;
        @NonNull public final String note;

        public Transaction(@NonNull String id, @NonNull String categoryId, @NonNull String source, @Nullable String disbursementId, int day, long amount, @NonNull String note) {
            this.id = id;
            this.categoryId = categoryId;
            this.source = source;
            this.disbursementId = disbursementId;
            this.day = day;
            this.amount = amount;
            this.note = note;
        }
    }

    @Entity(tableName = "budgets", foreignKeys = @ForeignKey(entity = Category.class, parentColumns = "id", childColumns = "categoryId", onDelete = ForeignKey.RESTRICT),
            indices = {@Index("categoryId"), @Index(value = {"month", "categoryId"}, unique = true)})
    public static class Budget {
        @PrimaryKey @NonNull public final String id;
        public final int month;
        @NonNull public final String categoryId;
        public final long limitVnd;

        public Budget(@NonNull String id, int month, @NonNull String categoryId, long limitVnd) {
            this.id = id;
            this.month = month;
            this.categoryId = categoryId;
            this.limitVnd = limitVnd;
        }
    }

    @Entity(tableName = "receipts", foreignKeys = @ForeignKey(entity = Transaction.class, parentColumns = "id", childColumns = "transactionId", onDelete = ForeignKey.CASCADE), indices = @Index("transactionId"))
    public static class Receipt {
        @PrimaryKey @NonNull public final String id;
        @NonNull public final String transactionId;
        @NonNull public final String uri;
        @NonNull public final String displayName;
        @NonNull public final String mimeType;

        public Receipt(@NonNull String id, @NonNull String transactionId, @NonNull String uri, @NonNull String displayName, @NonNull String mimeType) {
            this.id = id;
            this.transactionId = transactionId;
            this.uri = uri;
            this.displayName = displayName;
            this.mimeType = mimeType;
        }
    }
}
