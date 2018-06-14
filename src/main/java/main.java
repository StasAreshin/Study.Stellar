/**
 * Created by Stas on 2018-06-13.
 */
public class main {

    private static final String PARAM_RUNINITIALIZATION = "run_initialization";

    //    public static final String HOST_HORIZON = "localhost:8000";
     static final String HOST_HORIZON = "https://horizon-testnet.stellar.org";
     static final String HOST_FRIEND_BOT = "https://friendbot.stellar.org";

    private static void runInitialization() {
        Accounts.createTestAccount();
    }

    private static void runOperations() {
//        TestCases.doTransactions();
//        TestCases.readPayments();
        TestCases.nonNativeAssetTest();
    }

    public static void main(String arg[]) {
        Boolean runInitialization = false;
        for (String anArg : arg) {
            if (PARAM_RUNINITIALIZATION.equalsIgnoreCase(anArg)) {
                runInitialization = true;
            }
        }

        if (runInitialization) {
            runInitialization();
        } else {
            runOperations();
        }
    }
}
