import com.google.gson.*;
import com.sun.deploy.net.HttpResponse;
import org.stellar.sdk.*;
import org.stellar.sdk.requests.ErrorResponse;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.LedgerResponse;
import org.stellar.sdk.responses.Page;
import sun.net.www.http.HttpClient;
import sun.rmi.runtime.Log;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Stas on 2018-06-13.
 */
class Accounts {

    static String getAccountData(String accountId, String key) {
        String result = null;
        try {
            URL obj = new URL(app.HOST_HORIZON + "/accounts/" + accountId + "/data/" + key);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "Mozilla/5.0");

            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JsonParser parser = new JsonParser();
                JsonObject mainObject = parser.parse(response.toString()).getAsJsonObject();
                result = new String(DatatypeConverter.parseBase64Binary(mainObject.get("value").getAsString()), Charset.defaultCharset());
            }
        } catch (
                Exception e)
        {
            e.printStackTrace();
        }
        return result;
    }
    static void printAccountDetails(String accountId) {
        printAccountDetails(getAccount(accountId));
    }

    static void printAccountDetails(AccountResponse account) {
        if (account == null) {
            return;
        }
        String accountName = getAccountData(account.getKeypair().getAccountId(), "name");

        Config.log(Config.DELIMITER + "Account: " + account.getKeypair().getAccountId() + " " + (accountName != null ? accountName : ""));
        Config.log("Currencies:");
        for (AccountResponse.Balance balance : account.getBalances()) {
            Config.log(String.format(
                    "\t%s: Balance: %s",
                    Payments.formatAssetName(balance.getAsset()),
                    balance.getBalance()
            ));
        }
//        Connections.getServer().accounts().
//        account.getSigners()

        Payments.fetchOffers(account.getKeypair().getAccountId());

    }

    static AccountResponse getAccount(String accountId) {
        return getAccount(KeyPair.fromAccountId(accountId));
    }

    static AccountResponse getAccount(KeyPair pair) {
        AccountResponse result = null;

        if (pair != null) {
            Server server = Connections.getServer();
            try {
                result = server.accounts().account(pair);
            } catch (IOException e) {
                Config.log(Config.DELIMITER + "Error in fetching Account " + pair.getAccountId() + ":\n" + e.toString());
            } catch (ErrorResponse e) {
                if (e.getCode() == 404) {
                    Config.log(Config.DELIMITER + "Account " + pair.getAccountId() + " not found");
                } else {
                    Config.log(String.format(
                            Config.DELIMITER + "Error in fetching Account %s: \n\tCode = %s\n\tBody = %s\n\tMessage = %s",
                            pair.getAccountId(),
                            e.getCode(),
                            e.getBody(),
                            e.getMessage()
                    ));
                }
            }
        }

        return result;
    }

    // Use it to create new independent account in the test network only
    static void createTestAccount() {
        KeyPair pair = generateNewPair();
        String friendBotURL = String.format(app.HOST_FRIEND_BOT + "/?addr=%s", pair.getAccountId());
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

    static KeyPair createAccount(KeyPair sourceAccount, String startAmount) {
        KeyPair result = generateNewPair();
        Config.log("creating from source " + sourceAccount.getAccountId() + " ...");

        CreateAccountOperation.Builder operationBuilder = new CreateAccountOperation.Builder(result, startAmount)
                .setSourceAccount(sourceAccount);

        Payments.doTrasnaction(
                sourceAccount,
                operationBuilder.build(),
                Memo.text("Create Account Operation"));

        return result;
    }

    static void mergeAccounts(char fromSeed[], char toSeed[]) {
        Config.log("Merging account ...");

        KeyPair pairTo = KeyPair.fromSecretSeed(toSeed);
        KeyPair pairFrom = KeyPair.fromSecretSeed(fromSeed);

        AccountMergeOperation.Builder operationBuilder = new AccountMergeOperation.Builder(pairTo)
                .setSourceAccount(pairFrom);

        Payments.doTrasnaction(
                pairFrom,
                operationBuilder.build(),
                Memo.text("Merge Account Operation"));
    }

    static void manageData(KeyPair account, String key, byte[] value) {
        Map<String, byte[]> values = new HashMap<>();
        values.put(key, value);
        manageData(account, values);
    }

    static void manageData(KeyPair account, Map<String, byte[]> values) {
        Config.log("Managing data ...");

        ManageDataOperation ops[] = new ManageDataOperation[values.size()];
        int i = 0;
        for (Map.Entry<String, byte[]> value : values.entrySet()) {

            ops[i] = new ManageDataOperation.Builder(value.getKey(), value.getValue())
                    .setSourceAccount(account)
                    .build();
            i++;
        }

        Payments.doTrasnaction(
                account,
                null,
                ops,
                Memo.text("Merge Account Operation"));
    }

    private static KeyPair generateNewPair() {
        KeyPair pair = KeyPair.random();
        Config.log("seed: " + new String(pair.getSecretSeed()));
        Config.log("accounId: " + pair.getAccountId());
        return pair;
    }
}
