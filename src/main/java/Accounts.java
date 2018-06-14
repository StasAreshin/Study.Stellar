import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Server;
import org.stellar.sdk.responses.AccountResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

/**
 * Created by Stas on 2018-06-13.
 */
public class Accounts {

    public static void printAccountDetails(String accountId) {
        printAccountDetails(getAccount(accountId));
    }
    public static void printAccountDetails(AccountResponse account) {
        if (account == null) {
            return;
        }

        Config.log("\nAccount: " + account.getKeypair().getAccountId());
        Config.log("Currencies:");
        for (AccountResponse.Balance balance : account.getBalances()) {
            Config.log(String.format(
                    "\tType: %s, Code: %s, Balance: %s",
                    balance.getAssetType(),
                    balance.getAssetCode(),
                    balance.getBalance()
            ));
        }
    }

    public static AccountResponse getAccount(String accountId) {
        AccountResponse result = null;
        KeyPair pair = KeyPair.fromAccountId(accountId);
        if (pair == null) {
            Config.log("Pair for account id is null");
        } else {
            result = getAccount(pair);
        }
        return result;
    }
    public static AccountResponse getAccount(KeyPair pair) {
        AccountResponse result = null;

        if (pair != null) {
            Server server = Connections.getServer();
            try {
                result = server.accounts().account(pair);
            } catch (IOException e) {
                Config.log("Exception on fetching account response:\n" + e.toString());
            }
        }

        return result;
    }

    public static void createTestAccount() {
        KeyPair pair = generateNewPair();
        String friendBotURL = String.format(main.HOST_FRIEND_BOT + "/?addr=%s", pair.getAccountId());
        InputStream response = null;
        try {
            response = new URL(friendBotURL).openStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (response != null) {
            String body = new Scanner(response, "UTF-8").useDelimiter("\\A").next();
            Config.log("SUCCESS! You have a new account :)\n" + body);
        }
    }

    public static KeyPair generateNewPair() {
        KeyPair pair = KeyPair.random();
        Config.log("seed: " + new String(pair.getSecretSeed()));
        Config.log("accounId: " + pair.getAccountId());
        return pair;
    }
}