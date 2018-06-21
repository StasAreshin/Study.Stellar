/**
 * Created by Stas on 2018-06-13.
 */
public class app {

    private static final String PARAM_RUNINITIALIZATION = "run_initialization";

    static final String HOST_HORIZON_LOCAL = "http://localhost:8000";
    static final String HOST_HORIZON_REMOTE = "https://horizon-testnet.stellar.org";
    public static final String HOST_HORIZON = HOST_HORIZON_LOCAL;

     static final String HOST_FRIEND_BOT = "https://friendbot.stellar.org";

    private static void runInitialization() {
        Accounts.createTestAccount();
    }

    private static void runOperations() {
//        TestCases.printAccountDetails();
        Accounts.createTestAccount();
//        TestCases.doTransactions();
//        TestCases.readPayments();
//        TestCases.nonNativeAssetTest();
//        TestCases.setDomainTest();
//        TestCases.checkTrustBeforePaying();
//        TestCases.changeOffer();
//        TestCases.createNewAccount();
//        TestCases.mergeAccounts();
//        TestCases.manageData();
//        TestCases.clearOffers();
//        Connections.getCommonInfo();

//        TestCases.offersTest();
//        TestCases.printAccountDetails();
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
