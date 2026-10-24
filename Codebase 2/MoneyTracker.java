
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

    static final class Validation {

        static BigDecimal money(String text) {
            if (!text.matches("[0-9]+(\\.[0-9]{1,2})?")) {
                throw new IllegalArgumentException(
                        "Enter a non-negative amount with at most 2 decimals.");
            }
            return new BigDecimal(text).setScale(2);
        }

        static void amount(BigDecimal value, boolean allowZero) {
            if (value == null || value.scale() > 2
                    || value.signum() < 0
                    || (!allowZero && value.signum() == 0)) {
                throw new IllegalArgumentException(
                        allowZero ? "Opening balance must be non-negative."
                                : "Amount must be above zero.");
            }
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

        static String accountName(String name, int excludedId,
                Collection<Account> accounts) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Name is required.");
            }
            String trimmed = name.trim();
            for (Account a : accounts) {
                if (a.getId() != excludedId
                        && a.getName().equalsIgnoreCase(trimmed)) {
                    throw new IllegalArgumentException(
                            "Account name already exists.");
                }
            }
            return trimmed;
        }
    }

    record BudgetKey(YearMonth month, int categoryId) {

    }

    record Filter(LocalDate start, LocalDate end, Integer accountId,
            Integer categoryId, TransactionType type, String keyword) {

    }

    record BudgetStatus(BigDecimal limit, BigDecimal spent) {

        BigDecimal remaining() {
            return limit.subtract(spent);
        }
    }

    record Summary(BigDecimal income, BigDecimal expenses,
            Map<String, BigDecimal> byCategory) {

        BigDecimal netCashFlow() {
            return income.subtract(expenses);
        }
    }

    static final class LedgerStore {

        private final Map<Integer, Account> accounts = new LinkedHashMap<>();
        private final Map<Integer, Category> categories = new LinkedHashMap<>();
        private final Map<Integer, Transaction> transactions
                = new LinkedHashMap<>();
        private final Map<BudgetKey, MonthlyBudget> budgets = new HashMap<>();
        private int nextAccountId = 4;
        private int nextTransactionId = 1;

        LedgerStore() {
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

        List<Account> accounts() {
            return List.copyOf(accounts.values());
        }

        List<Category> categories() {
            return List.copyOf(categories.values());
        }

        List<Transaction> transactions() {
            return List.copyOf(transactions.values());
        }

        Account account(int id) {
            return require(accounts.get(id), "Unknown account.");
        }

        Category category(int id) {
            return require(categories.get(id), "Unknown category.");
        }

        Transaction transaction(int id) {
            return require(transactions.get(id), "Unknown transaction.");
        }

        private static <T> T require(T value, String message) {
            if (value == null) {
                throw new IllegalArgumentException(message);
            }
            return value;
        }

        Account addAccount(String name, BigDecimal opening) {
            Account a = new Account(nextAccountId++, name, opening);
            accounts.put(a.getId(), a);
            return a;
        }

        void replaceAccount(Account a) {
            accounts.put(a.getId(), a);
        }

        int nextTransactionId() {
            return nextTransactionId;
        }

        void save(Transaction t, boolean isNew) {
            transactions.put(t.getId(), t);
            if (isNew) {
                nextTransactionId++;
            }
        }

        void delete(int id) {
            transactions.remove(id);
        }

        MonthlyBudget budget(YearMonth month, int categoryId) {
            return budgets.get(new BudgetKey(month, categoryId));
        }

        void saveBudget(MonthlyBudget b) {
            budgets.put(new BudgetKey(b.getMonth(), b.getCategoryId()), b);
        }
    }

    static final class TrackerService {

        private final LedgerStore store;

        TrackerService(LedgerStore store) {
            this.store = store;
        }

        List<Account> accounts() {
            return store.accounts();
        }

        List<Category> categories() {
            return store.categories();
        }

        Account account(int id) {
            return store.account(id);
        }

        Category category(int id) {
            return store.category(id);
        }

        Transaction transaction(int id) {
            return store.transaction(id);
        }

        Account addAccount(String name, BigDecimal opening) {
            String clean = Validation.accountName(name, 0, accounts());
            Validation.amount(opening, true);
            return store.addAccount(clean, opening);
        }

        void renameAccount(int id, String name) {
            Account old = account(id);
            String clean = Validation.accountName(name, id, accounts());
            store.replaceAccount(new Account(id, clean, old.getOpeningBalance()));
        }

        Transaction save(Transaction candidate, boolean isNew) {
            validateTransaction(candidate);
            int id = isNew ? store.nextTransactionId() : candidate.getId();
            if (!isNew && transaction(id).getType() != candidate.getType()) {
                throw new IllegalArgumentException(
                        "Transaction type cannot change.");
            }
            Transaction saved = new Transaction(id, candidate.getType(),
                    candidate.getDate(), candidate.getAmount(),
                    candidate.getAccountId(), candidate.getTargetAccountId(),
                    candidate.getCategoryId(), candidate.getNote());
            store.save(saved, isNew);
            return saved;
        }

        private void validateTransaction(Transaction t) {
            Validation.amount(t.getAmount(), false);
            if (t.getDate() == null || t.getDate().isAfter(LocalDate.now())) {
                throw new IllegalArgumentException(
                        "Use a date no later than today.");
            }
            if (t.getType() == null || t.getNote() == null) {
                throw new IllegalArgumentException(
                        "Type and note value are required.");
            }
            account(t.getAccountId());
            if (t.getType() == TransactionType.TRANSFER) {
                if (t.getTargetAccountId() == null || t.getCategoryId() != null) {
                    throw new IllegalArgumentException(
                            "Transfer fields are invalid.");
                }
                account(t.getTargetAccountId());
                if (t.getAccountId() == t.getTargetAccountId()) {
                    throw new IllegalArgumentException(
                            "Choose two different accounts.");
                }
            } else {
                if (t.getCategoryId() == null || t.getTargetAccountId() != null) {
                    throw new IllegalArgumentException(
                            "Income/expense fields are invalid.");
                }
                if (category(t.getCategoryId()).getType() != t.getType()) {
                    throw new IllegalArgumentException(
                            "Category does not match the type.");
                }
            }
        }

        void delete(int id) {
            transaction(id);
            store.delete(id);
        }

        BigDecimal balance(int id) {
            BigDecimal value = account(id).getOpeningBalance();
            for (Transaction t : store.transactions()) {
                value = value.add(effect(t, id));
            }
            return value;
        }

        private BigDecimal effect(Transaction t, int accountId) {
            if (t.getAccountId() == accountId) {
                return t.getType() == TransactionType.INCOME
                        ? t.getAmount() : t.getAmount().negate();
            }
            return Objects.equals(t.getTargetAccountId(), accountId)
                    ? t.getAmount() : BigDecimal.ZERO;
        }

        private BigDecimal total(YearMonth m, TransactionType type,
                Integer categoryId) {
            BigDecimal sum = BigDecimal.ZERO;
            for (Transaction t : store.transactions()) {
                if (t.getType() == type && YearMonth.from(t.getDate()).equals(m)
                        && (categoryId == null
                        || categoryId.equals(t.getCategoryId()))) {
                    sum = sum.add(t.getAmount());
                }
            }
            return sum;
        }

        void setBudget(YearMonth m, int categoryId, BigDecimal limit) {
            if (m == null) {
                throw new IllegalArgumentException(
                        "Month is required.");
            }
            if (category(categoryId).getType() != TransactionType.EXPENSE) {
                throw new IllegalArgumentException("Choose an expense category.");
            }
            Validation.amount(limit, false);
            store.saveBudget(new MonthlyBudget(m, categoryId, limit));
        }

        BudgetStatus budgetStatus(YearMonth m, int categoryId) {
            MonthlyBudget b = store.budget(m, categoryId);
            return b == null ? null : new BudgetStatus(b.getLimit(),
                    total(m, TransactionType.EXPENSE, categoryId));
        }

        Summary summary(YearMonth m) {
            Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
            for (Category c : categories()) {
                if (c.getType() == TransactionType.EXPENSE) {
                    byCategory.put(c.getName(),
                            total(m, TransactionType.EXPENSE, c.getId()));
                }
            }
            return new Summary(total(m, TransactionType.INCOME, null),
                    total(m, TransactionType.EXPENSE, null),
                    Collections.unmodifiableMap(byCategory));
        }

        List<Transaction> search(Filter f) {
            if (f.start() != null && f.end() != null
                    && f.start().isAfter(f.end())) {
                throw new IllegalArgumentException(
                        "Start date must not follow end date.");
            }
            if (f.accountId() != null) {
                account(f.accountId());
            }
            if (f.categoryId() != null) {
                category(f.categoryId());
            }
            return store.transactions().stream().filter(t -> matches(t, f))
                    .sorted(Comparator.comparing(Transaction::getDate)
                            .thenComparingInt(Transaction::getId).reversed())
                    .toList();
        }

        private boolean matches(Transaction t, Filter f) {
            if (f.start() != null && t.getDate().isBefore(f.start())) {
                return false;
            }
            if (f.end() != null && t.getDate().isAfter(f.end())) {
                return false;
            }
            if (f.accountId() != null && f.accountId() != t.getAccountId()
                    && !f.accountId().equals(t.getTargetAccountId())) {
                return false;
            }
            if (f.categoryId() != null
                    && !f.categoryId().equals(t.getCategoryId())) {
                return false;
            }
            if (f.type() != null && f.type() != t.getType()) {
                return false;
            }
            return t.getNote().toLowerCase(Locale.ROOT)
                    .contains(f.keyword().toLowerCase(Locale.ROOT));
        }
    }

    public static void main(String[] args) {
        new ConsoleUI(new TrackerService(new LedgerStore())).run();
    }

    static final class ConsoleUI {

        private final TrackerService service;
        private final Scanner input = new Scanner(System.in);

        ConsoleUI(TrackerService service) {
            this.service = service;
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

        private static BigDecimal money(String text) {
            return Validation.money(text);
        }

        private static LocalDate date(String text) {
            return Validation.date(text);
        }

        private static YearMonth month(String text) {
            return Validation.month(text);
        }

        private static String rm(BigDecimal value) {
            return "RM" + value.setScale(2).toPlainString();
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
                } catch (IllegalArgumentException
                        | java.time.DateTimeException ex) {
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
            Account old = choice == 2
                    ? service.account(number("Account ID")) : null;
            String name = ask("Account name");
            if (old == null) {
                BigDecimal opening = money(ask("Opening balance"));
                Account saved = service.addAccount(name, opening);
                System.out.println("Account saved: " + saved.getId());
            } else {
                service.renameAccount(old.getId(), name);
                System.out.println("Account saved: " + old.getId());
            }
        }

        private void listAccounts() {
            BigDecimal total = BigDecimal.ZERO;
            for (Account a : service.accounts()) {
                BigDecimal balance = service.balance(a.getId());
                total = total.add(balance);
                System.out.println(a.getId() + " " + a.getName()
                        + " | opening " + rm(a.getOpeningBalance())
                        + " | balance " + rm(balance));
            }
            System.out.println("Total balance: " + rm(total));
        }

        private void listCategories(TransactionType type) {
            for (Category c : service.categories()) {
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

            Transaction draft = new Transaction(old == null ? 0 : old.getId(),
                    type, day, amount, source, target, categoryId, note);
            Transaction saved = service.save(draft, old == null);
            System.out.println("Transaction saved: " + saved.getId());
            if (type == TransactionType.EXPENSE) {
                showBudget(YearMonth.from(day), service.category(categoryId));
            }
        }

        private void editOrDelete() {
            printTransactions(service.search(
                    new Filter(null, null, null, null, null, "")));
            Transaction old = service.transaction(number("Transaction ID"));
            int choice = number("1 Edit | 2 Delete | 0 Back");
            if (choice == 1) {
                enterTransaction(old.getType(), old);
            } else if (choice == 2) {
                if (ask("Delete? yes/no").equalsIgnoreCase("yes")) {
                    service.delete(old.getId());
                    System.out.println("Transaction deleted.");
                } else {
                    System.out.println("Deletion cancelled.");
                }
            } else if (choice != 0) {
                throw new IllegalArgumentException("Choose 0, 1 or 2.");
            }
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
                int id = number("Expense category ID");
                BigDecimal limit = money(ask("Budget limit"));
                service.setBudget(m, id, limit);
                System.out.println("Budget saved.");
            }
            for (Category c : service.categories()) {
                if (c.getType() == TransactionType.EXPENSE) {
                    showBudget(m, c);
                }
            }
        }

        private void showBudget(YearMonth m, Category c) {
            BudgetStatus b = service.budgetStatus(m, c.getId());
            if (b == null) {
                System.out.println(c.getName() + ": No budget set");
                return;
            }
            BigDecimal left = b.remaining();
            String status = left.signum() > 0 ? "Remaining " + rm(left)
                    : left.signum() == 0 ? "Budget reached"
                    : "Overspent " + rm(left.abs());
            System.out.println(c.getName() + " | limit " + rm(b.limit())
                    + " | spent " + rm(b.spent()) + " | " + status);
        }

        private void summary(YearMonth m) {
            Summary s = service.summary(m);
            System.out.println("Summary " + m + " | Income " + rm(s.income())
                    + " | Expenses " + rm(s.expenses())
                    + " | Net cash flow " + rm(s.netCashFlow()));
            s.byCategory().forEach((name, value)
                    -> System.out.println(name + ": " + rm(value)));
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

            printTransactions(service.search(
                    new Filter(start, end, aid, cid, type, keyword)));
        }

        private void printTransactions(List<Transaction> list) {
            if (list.isEmpty()) {
                System.out.println("No matching transactions.");
            }
            for (Transaction t : list) {
                String destination = t.getTargetAccountId() == null ? ""
                        : " -> " + service.account(t.getTargetAccountId()).getName();
                String cat = t.getCategoryId() == null ? "-"
                        : service.category(t.getCategoryId()).getName();
                System.out.println("TX " + t.getId() + " | " + t.getDate()
                        + " | " + t.getType() + " | " + rm(t.getAmount())
                        + " | " + service.account(t.getAccountId()).getName()
                        + destination
                        + " | " + cat + " | " + t.getNote());
            }
        }
    }
}
