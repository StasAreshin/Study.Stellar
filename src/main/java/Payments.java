import org.stellar.sdk.*;
import org.stellar.sdk.requests.EventListener;
import org.stellar.sdk.requests.PaymentsRequestBuilder;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.Page;
import org.stellar.sdk.responses.SubmitTransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;

import java.io.IOException;

/**
 * Created by Stas on 2018-06-13.
 */
public class Payments {

    private final static Boolean USE_LISTENER = false;

    public static PaymentOperation getOperation(KeyPair destination, Asset asset, String amount) {
        return new PaymentOperation.Builder(destination, asset, amount).build();
    }

    public static void makeTrasnaction(KeyPair source, KeyPair destination, PaymentOperation operations[], Memo memo) {

        if (operations == null || operations.length == 0) {
            return;
        }

        // First, check to make sure that the destination account exists.
        // You could skip this, but if the account does not exist, you will be charged
        // the transaction fee when the transaction fails.
        // It will throw HttpResponseException if account does not exist or there was another error.
        if (Accounts.getAccount(destination) == null) {
            return;
        }

        // If there was no error, load up-to-date information on your account.
        AccountResponse sourceAccount = Accounts.getAccount(source);

        // Start building the transaction.

        Transaction.Builder trBuilder = new Transaction.Builder(sourceAccount);
        Config.log("\nTransaction");
        if (memo != null) {
            Config.log("Memo " + memo.toString());
            trBuilder.addMemo(memo);
        }
        for (PaymentOperation op : operations) {
            trBuilder.addOperation(op);
            Config.log(String.format(
                    "Operation: -> %s: %s = %s",
                    op.getDestination().getAccountId(),
                    op.getAsset().getType(),
                    op.getAmount()));
        }
        Transaction transaction = trBuilder.build();

        // Sign the transaction to prove you are actually the person sending it.
        transaction.sign(source);

        Server server = Connections.getServer();
        //And finally, send it off to Stellar!
        try {
            SubmitTransactionResponse response = server.submitTransaction(transaction);
            if (response.isSuccess()) {
                Config.log("Success!");
            } else {
                Config.log("Something went wrong:\n" + response);
            }
        } catch (Exception e) {
            Config.log("Exception:\n" + e.getMessage());
            // If the result is unknown (no response body, timeout etc.) we simply resubmit
            // already built transaction:
            // SubmitTransactionResponse response = server.submitTransaction(transaction);
        }
    }

    private static void printOperation(OperationResponse operationResponse, KeyPair pair) {
        // Record the paging token so we can start from here next time.
        Config.saveLastPagingToken(pair.getAccountId(), operationResponse.getPagingToken());

        // The payments stream includes both sent and received payments. We only
        // want to process received payments here.
        if (operationResponse instanceof PaymentOperationResponse) {
            PaymentOperationResponse payment = (PaymentOperationResponse) operationResponse;

            String directionPrefix = (payment.getTo().getAccountId().equals(pair.getAccountId()) ? "in" : "out");

            String amount = payment.getAmount();
            Asset asset = payment.getAsset();
            String assetName;
            if (asset.equals(new AssetTypeNative())) {
                assetName = "lumens";
            } else {
                StringBuilder assetNameBuilder = new StringBuilder();
                assetNameBuilder.append(((AssetTypeCreditAlphaNum)asset).getCode());
                assetNameBuilder.append(":");
                assetNameBuilder.append(((AssetTypeCreditAlphaNum)asset).getIssuer().getAccountId());
                assetName = assetNameBuilder.toString();
            }

            StringBuilder output = new StringBuilder();
            output.append(directionPrefix);
            output.append(" ");
            output.append(amount);
            output.append(" ");
            output.append(assetName);
            output.append(" from ");
            output.append(payment.getFrom().getAccountId());
            System.out.println(output.toString());
        }

    }
    public static void fetchPayments(final String accountId) {
        Server server = Connections.getServer();
        final KeyPair pair = KeyPair.fromAccountId(accountId);

        // Create an API call to query payments involving the account.
        PaymentsRequestBuilder paymentsRequest = server.payments().forAccount(pair);

        // If some payments have already been handled, start the results from the
        // last seen payment. (See below in `handlePayment` where it gets saved.)
        String lastToken = Config.loadLastPagingToken(accountId);
        if (lastToken != null) {
            paymentsRequest.cursor(lastToken);
        }

        if (USE_LISTENER) {
            Config.log("\nPayments for " + accountId + " (listener)");
            // `stream` will send each recorded payment, one by one, then keep the
            // connection open and continue to send you new payments as they occur.
            paymentsRequest.stream(new EventListener<OperationResponse>() {
                public void onEvent(OperationResponse operationResponse) {
                    printOperation(operationResponse, pair);
                }
            });
        } else {
            Config.log("\nPayments for " + accountId + " (execute)");
            paymentsRequest = server.payments().forAccount(pair);
            Page<OperationResponse> page = null;
            try {
                page = paymentsRequest.execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (page != null) {
                for (OperationResponse operationResponse : page.getRecords()) {
                    printOperation(operationResponse, pair);
                }
            }
        }

    }
}
