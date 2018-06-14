/**
 * Created by Stas on 2018-06-13.
 */
public class main {

    private static final String PARAM_RUNINITIALIZATION = "run_initialization";

    //    public static final String HOST_HORIZON = "localhost:8000";
    public static final String HOST_HORIZON = "https://horizon-testnet.stellar.org";
    public static final String HOST_FRIEND_BOT = "https://friendbot.stellar.org";

    private static void runInitialization() {
        Accounts.createTestAccount();
    }

    private static void runOperations() {
        TestCases.doErrorTransaction();
    }

    public static void main(String arg[]) {
        Boolean runInitialization = false;
        for (int i=0; i<arg.length; i++) {
            if (PARAM_RUNINITIALIZATION.equalsIgnoreCase(arg[i])) {
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
