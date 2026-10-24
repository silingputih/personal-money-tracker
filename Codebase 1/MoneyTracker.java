import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class MoneyTracker {

    enum TransactionType {
        INCOME, EXPENSE, TRANSFER
    }

    static final class Account {

        private final int id;
        private final String name;
        private final BigDecimal openingBalance;

        Account(int id, String name, BigDecimal openingBalance) {
            this.id = id;
            this.name = name;
            this.openingBalance = openingBalance;
        }

        int getId() {
            return id;
        }

        String getName() {
            return name;
        }

        BigDecimal getOpeningBalance() {
            return openingBalance;
        }
    }

    static final class Transaction {

        private final int id;
        private final TransactionType type;
        private final LocalDate date;
        private final BigDecimal amount;
        private final int accountId;
        private final Integer targetAccountId;
        private final Integer categoryId;
        private final String note;

        Transaction(int id, TransactionType type, LocalDate date,
                BigDecimal amount, int accountId,
                Integer targetAccountId, Integer categoryId,
                String note) {
            this.id = id;
            this.type = type;
            this.date = date;
            this.amount = amount;
            this.accountId = accountId;
            this.targetAccountId = targetAccountId;
            this.categoryId = categoryId;
            this.note = note;
        }

        int getId() {
            return id;
        }

        TransactionType getType() {
            return type;
        }

        LocalDate getDate() {
            return date;
        }

        BigDecimal getAmount() {
            return amount;
        }

        int getAccountId() {
            return accountId;
        }

        Integer getTargetAccountId() {
            return targetAccountId;
        }

        Integer getCategoryId() {
            return categoryId;
        }

        String getNote() {
            return note;
        }
    }

    static final class Category {

        private final int id;
        private final String name;
        private final TransactionType type;

        Category(int id, String name, TransactionType type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }

        int getId() {
            return id;
        }

        String getName() {
            return name;
        }

        TransactionType getType() {
            return type;
        }
    }

    static final class MonthlyBudget {

        private final YearMonth month;
        private final int categoryId;
        private final BigDecimal limit;

        MonthlyBudget(YearMonth month, int categoryId,
                BigDecimal limit) {
            this.month = month;
            this.categoryId = categoryId;
            this.limit = limit;
        }

        YearMonth getMonth() {
            return month;
        }

        int getCategoryId() {
            return categoryId;
        }

        BigDecimal getLimit() {
            return limit;
        }
    }

    private final Map<Integer, Account> accounts = new LinkedHashMap<>();
    private final Map<Integer, Category> categories = new LinkedHashMap<>();
    private final Map<Integer, Transaction> transactions
            = new LinkedHashMap<>();
    private final Map<String, MonthlyBudget> budgets = new HashMap<>();
    private final Scanner input = new Scanner(System.in);
    private int nextAccountId = 4;
    private int nextTransactionId = 1;

    MoneyTracker() {
        accounts.put(1, new Account(1, "Bank", new BigDecimal("500.00")));
        accounts.put(2, new Account(2, "Cash", new BigDecimal("100.00")));
        accounts.put(3, new Account(3, "E-wallet", new BigDecimal("50.00")));
        String[] names = {"Salary", "Allowance", "Freelance",
            "Food", "Transport", "Shopping", "Bills"};
        for (int i = 0; i < names.length; i++) {
            TransactionType type = i < 3 ? TransactionType.INCOME
                    : TransactionType.EXPENSE;
            categories.put(i + 1, new Category(i + 1, names[i], type));
        }
    }

    public static void main(String[] args) {
        new MoneyTracker().run();
    }

    private String ask(String label) {
        System.out.print(label + ": ");
        if (!input.hasNextLine()) {
            throw new NoSuchElementException();
        }
        return input.nextLine().trim();
    }

    private int number(String label) {
        return Integer.parseInt(ask(label));
    }

    static BigDecimal money(String text) {
        if (!text.matches("[0-9]+(\\.[0-9]{1,2})?")) {
            throw new IllegalArgumentException(
                    "Enter a non-negative amount with at most 2 decimals.");
        }
        return new BigDecimal(text).setScale(2);
    }

    static LocalDate date(String text) {
        if (!text.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            throw new IllegalArgumentException("Use YYYY-MM-DD.");
        }
        return LocalDate.parse(text);
    }

    static YearMonth month(String text) {
        if (!text.matches("[0-9]{4}-[0-9]{2}")) {
            throw new IllegalArgumentException("Use YYYY-MM.");
        }
        return YearMonth.parse(text);
    }

    static String rm(BigDecimal value) {
        return "RM" + value.setScale(2).toPlainString();
    }

    private Account account(int id) {
        Account a = accounts.get(id);
        if (a == null) {
            throw new IllegalArgumentException("Unknown account.");
        }
        return a;
    }

    private Category category(int id) {
        Category c = categories.get(id);
        if (c == null) {
            throw new IllegalArgumentException("Unknown category.");
        }
        return c;
    }

    private Transaction transaction(int id) {
        Transaction t = transactions.get(id);
        if (t == null) {
            throw new IllegalArgumentException("Unknown transaction.");
        }
        return t;
    }

    private void run() {
        System.out.println("Personal Income and Expense Management System");
        while (true) {
            System.out.println("\n1 Accounts | 2 Income | 3 Expense");
            System.out.println("4 Edit/delete | 5 Transfer | 6 Budgets");
            System.out.println("7 Search | 8 Monthly summary | 0 Exit");
            try {
                switch (number("Choice")) {
                    case 0:
                        return;
                    case 1:
                        manageAccounts();
                        break;
                    case 2:
                        enterTransaction(TransactionType.INCOME, null);
                        break;
                    case 3:
                        enterTransaction(TransactionType.EXPENSE, null);
                        break;
                    case 4:
                        editOrDelete();
                        break;
                    case 5:
                        enterTransaction(TransactionType.TRANSFER, null);
                        break;
                    case 6:
                        manageBudgets();
                        break;
                    case 7:
                        search();
                        break;
                    case 8:
                        summary(month(ask("Month YYYY-MM")));
                        break;
                    default:
                        System.out.println("Choose a listed option.");
                }
            } catch (NoSuchElementException ex) {
                System.out.println("Input ended. Goodbye.");
                return;
            } catch (RuntimeException ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }

    private void manageAccounts() {
        listAccounts();
        int choice = number("1 Add | 2 Rename | 0 Back");
        if (choice == 0) {
            return;
        }
        if (choice != 1 && choice != 2) {
            throw new IllegalArgumentException("Choose 0, 1 or 2.");
        }
        Account old = choice == 2 ? account(number("Account ID")) : null;
        String name = ask("Account name");
        if (name.isBlank()) {
            throw new IllegalArgumentException(
                    "Name is required.");
        }
        for (Account a : accounts.values()) {
            if ((old == null || a.getId() != old.getId())
                    && a.getName().equalsIgnoreCase(name)) {
                throw new IllegalArgumentException(
                        "Account name already exists.");
            }
        }
        BigDecimal opening = old == null
                ? money(ask("Opening balance")) : old.getOpeningBalance();
        int id = old == null ? nextAccountId : old.getId();
        accounts.put(id, new Account(id, name, opening));
        if (old == null) {
            nextAccountId++;
        }
        System.out.println("Account saved: " + id);
    }

    private void listAccounts() {
        BigDecimal total = BigDecimal.ZERO;
        for (Account a : accounts.values()) {
            BigDecimal balance = balance(a.getId());
            total = total.add(balance);
            System.out.println(a.getId() + " " + a.getName()
                    + " | opening " + rm(a.getOpeningBalance())
                    + " | balance " + rm(balance));
        }
        System.out.println("Total balance: " + rm(total));
    }

    private BigDecimal balance(int id) {
        BigDecimal value = account(id).getOpeningBalance();
        for (Transaction t : transactions.values()) {
            if (t.getAccountId() == id) {
                value = t.getType() == TransactionType.INCOME
                        ? value.add(t.getAmount()) : value.subtract(t.getAmount());
            }
            if (Objects.equals(t.getTargetAccountId(), id)) {
                value = value.add(t.getAmount());
            }
        }
        return value;
    }

    private void listCategories(TransactionType type) {
        for (Category c : categories.values()) {
            if (type == null || c.getType() == type) {
                System.out.println(c.getId() + " " + c.getName()
                        + " (" + c.getType() + ")");
            }
        }
    }

    private void enterTransaction(TransactionType type, Transaction old) {
        listAccounts();
        LocalDate day = date(ask("Date YYYY-MM-DD"));
        BigDecimal amount = money(ask("Amount"));
        int source = number("Primary/source account ID");
        Integer target = null;
        Integer categoryId = null;
        if (type == TransactionType.TRANSFER) {
            target = number("Destination account ID");
        } else {
            listCategories(type);
            categoryId = number("Category ID");
        }
        String note = ask("Note (optional)");
        int id = old == null ? nextTransactionId : old.getId();
        Transaction t = new Transaction(id, type, day, amount,
                source, target, categoryId, note);
        if (day.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Future transactions are not allowed.");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be above zero.");
        }
        account(source);
        if (type == TransactionType.TRANSFER) {
            account(target);
            if (source == target) {
                throw new IllegalArgumentException(
                        "Choose two different accounts.");
            }
        } else if (category(categoryId).getType() != type) {
            throw new IllegalArgumentException(
                    "Category does not match the type.");
        }
// Commit only after the complete replacement has passed validation.
        transactions.put(id, t);
        if (old == null) {
            nextTransactionId++;
        }
        System.out.println("Transaction saved: " + id);
        if (type == TransactionType.EXPENSE) {
            showBudget(YearMonth.from(day), category(categoryId));
        }
    }

    private void editOrDelete() {
        printTransactions(new ArrayList<>(transactions.values()));
        Transaction old = transaction(number("Transaction ID"));
        int choice = number("1 Edit | 2 Delete | 0 Back");
        if (choice == 1) {
            enterTransaction(old.getType(), old);
        } else if (choice == 2) {
            if (ask("Delete? yes/no").equalsIgnoreCase("yes")) {
                transactions.remove(old.getId());
                System.out.println("Transaction deleted.");
            } else {
                System.out.println("Deletion cancelled.");
            }
        } else if (choice != 0) {
            throw new IllegalArgumentException("Choose 0, 1 or 2.");
        }
    }

    private BigDecimal total(YearMonth m, TransactionType type,
            Integer categoryId) {
        BigDecimal value = BigDecimal.ZERO;
        for (Transaction t : transactions.values()) {
            if (t.getType() == type && YearMonth.from(t.getDate()).equals(m)
                    && (categoryId == null
                    || categoryId.equals(t.getCategoryId()))) {
                value = value.add(t.getAmount());
            }
        }
        return value;
    }

    private void manageBudgets() {
        int choice = number("1 Set/replace | 2 View | 0 Back");
        if (choice == 0) {
            return;
        }
        if (choice != 1 && choice != 2) {
            throw new IllegalArgumentException("Choose 0, 1 or 2.");
        }
        YearMonth m = month(ask("Month YYYY-MM"));
        if (choice == 1) {
            listCategories(TransactionType.EXPENSE);
            Category c = category(number("Expense category ID"));
            if (c.getType() != TransactionType.EXPENSE) {
                throw new IllegalArgumentException("Choose an expense category.");
            }
            BigDecimal limit = money(ask("Budget limit"));
            if (limit.signum() <= 0) {
                throw new IllegalArgumentException("Budget must be above zero.");
            }
            budgets.put(m + ":" + c.getId(),
                    new MonthlyBudget(m, c.getId(), limit));
            System.out.println("Budget saved.");
        }
        for (Category c : categories.values()) {
            if (c.getType() == TransactionType.EXPENSE) {
                showBudget(m, c);
            }
        }
    }

    private void showBudget(YearMonth m, Category c) {
        MonthlyBudget b = budgets.get(m + ":" + c.getId());
        if (b == null) {
            System.out.println(c.getName() + ": No budget set");
            return;
        }
        BigDecimal spent = total(m, TransactionType.EXPENSE, c.getId());
        BigDecimal left = b.getLimit().subtract(spent);
        String status = left.signum() > 0 ? "Remaining " + rm(left)
                : left.signum() == 0 ? "Budget reached"
                : "Overspent " + rm(left.abs());
        System.out.println(c.getName() + " | limit " + rm(b.getLimit())
                + " | spent " + rm(spent) + " | " + status);
    }

    private void summary(YearMonth m) {
        BigDecimal income = total(m, TransactionType.INCOME, null);
        BigDecimal expense = total(m, TransactionType.EXPENSE, null);
        System.out.println("Summary " + m + " | Income " + rm(income)
                + " | Expenses " + rm(expense)
                + " | Net cash flow " + rm(income.subtract(expense)));
        for (Category c : categories.values()) {
            if (c.getType() == TransactionType.EXPENSE) {
                System.out.println(c.getName() + ": "
                        + rm(total(m, TransactionType.EXPENSE, c.getId())));
            }
        }
        System.out.println("Current balances (all recorded dates):");
        listAccounts();
    }

    private void search() {
        listAccounts();
        listCategories(null);
        String startText = ask("Start date (blank = any)");
        String endText = ask("End date (blank = any)");
        String accountText = ask("Account ID (blank = any)");
        String categoryText = ask("Category ID (blank = any)");
        String typeText = ask("INCOME/EXPENSE/TRANSFER (blank = any)");
        String keyword = ask("Note keyword (blank = any)");
        LocalDate start = startText.isEmpty() ? null : date(startText);
        LocalDate end = endText.isEmpty() ? null : date(endText);
        Integer aid = accountText.isEmpty() ? null
                : Integer.valueOf(accountText);
        Integer cid = categoryText.isEmpty() ? null
                : Integer.valueOf(categoryText);
        TransactionType type = typeText.isEmpty() ? null
                : TransactionType.valueOf(typeText.toUpperCase(Locale.ROOT));
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException(
                    "Start date must not follow end date.");
        }
        if (aid != null) {
            account(aid);
        }
        if (cid != null) {
            category(cid);
        }
        List<Transaction> found = new ArrayList<>();
        for (Transaction t : transactions.values()) {
            if (start != null && t.getDate().isBefore(start)) {
                continue;
            }
            if (end != null && t.getDate().isAfter(end)) {
                continue;
            }
            if (aid != null && aid != t.getAccountId()
                    && !aid.equals(t.getTargetAccountId())) {
                continue;
            }
            if (cid != null && !cid.equals(t.getCategoryId())) {
                continue;
            }
            if (type != null && type != t.getType()) {
                continue;
            }
            if (!t.getNote().toLowerCase(Locale.ROOT)
                    .contains(keyword.toLowerCase(Locale.ROOT))) {
                continue;
            }
            found.add(t);
        }
        printTransactions(found);
    }

    private void printTransactions(List<Transaction> list) {
        list.sort(Comparator.comparing(Transaction::getDate)
                .thenComparingInt(Transaction::getId).reversed());
        if (list.isEmpty()) {
            System.out.println("No matching transactions.");
        }
        for (Transaction t : list) {
            String destination = t.getTargetAccountId() == null ? ""
                    : " -> " + account(t.getTargetAccountId()).getName();
            String cat = t.getCategoryId() == null ? "-"
                    : category(t.getCategoryId()).getName();
            System.out.println("TX " + t.getId() + " | " + t.getDate()
                    + " | " + t.getType() + " | " + rm(t.getAmount())
                    + " | " + account(t.getAccountId()).getName() + destination
                    + " | " + cat + " | " + t.getNote());
        }
    }
}
