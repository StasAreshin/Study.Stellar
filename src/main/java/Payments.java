import org.stellar.sdk.*;
import org.stellar.sdk.requests.EventListener;
import org.stellar.sdk.requests.OffersRequestBuilder;
import org.stellar.sdk.requests.PaymentsRequestBuilder;
import org.stellar.sdk.requests.RequestBuilder;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.OfferResponse;
import org.stellar.sdk.responses.Page;
import org.stellar.sdk.responses.SubmitTransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Stas on 2018-06-13.
 */
class Payments {

    private final static Boolean USE_LISTENER = false;

    static PaymentOperation getOperation(KeyPair destination, Asset asset, String amount) {
        return new PaymentOperation.Builder(destination, asset, amount).build();
    }

    static Transaction doTrasnaction(KeyPair source, Operation operation, Memo memo) {
        return doTrasnaction(source, null, operation, memo);
    }

    static Transaction doTrasnaction(KeyPair source, KeyPair destination, Operation operation, Memo memo) {
        Operation ops[] = {operation};
        return doTrasnaction(source, destination, ops, memo);
    }

    static Transaction doTrasnaction(KeyPair source, KeyPair destination, Operation operations[], Memo memo) {

        if (operations == null || operations.length == 0) {
            return null;
        }

        // Not all operation needs the destination
        if (destination != null) {
            // First, check to make sure that the destination account exists.
            // You could skip this, but if the account does not exist, you will be charged
            // the transaction fee when the transaction fails.
            // It will throw HttpResponseException if account does not exist or there was another error.
            if (Accounts.getAccount(destination) == null) {
                return null;
            }
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
        for (Operation op : operations) {
            trBuilder.addOperation(op);
            if (op instanceof PaymentOperation) {
                Config.log(String.format(
                        "%s -> %s: %s = %s",
                        op.getClass().getName(),
                        ((PaymentOperation) op).getDestination().getAccountId(),
                        formatAssetName(((PaymentOperation) op).getAsset()),
                        ((PaymentOperation) op).getAmount()));
            } else if (op instanceof ChangeTrustOperation) {
                Config.log(String.format(
                        "%s -> %s = %s",
                        op.getClass().getName(),
                        formatAssetName(((ChangeTrustOperation) op).getAsset()),
                        ((ChangeTrustOperation) op).getLimit()));
            } else if (op instanceof SetOptionsOperation) {
                Config.log(String.format(
                        "%s -> ClearFlags = %s\n\tSetFlags = %s\n\tHomeDomain = %s\n\tInflationDestination = %s\n\tLowThreshold = %s\n\tMediumThreshold = %s\n\tHighThreshold = %s\n\tMasterKeyWeight = %s\n\tSigner = %s\n\tSignerWeight = %s",
                        op.getClass().getSimpleName(),
                        ((SetOptionsOperation) op).getClearFlags(),
                        ((SetOptionsOperation) op).getSetFlags(),
                        ((SetOptionsOperation) op).getHomeDomain(),
                        ((SetOptionsOperation) op).getInflationDestination(),
                        ((SetOptionsOperation) op).getLowThreshold(),
                        ((SetOptionsOperation) op).getMediumThreshold(),
                        ((SetOptionsOperation) op).getHighThreshold(),
                        ((SetOptionsOperation) op).getMasterKeyWeight(),
                        ((SetOptionsOperation) op).getSigner(),
                        ((SetOptionsOperation) op).getSignerWeight()));
            } else if (op instanceof ManageOfferOperation) {
                Config.log(String.format(
                        "%s -> id = %s\n\tAmount = %s\n\tSelling = %s\n\tPrice = %s\n\tBuying = %s",
                        op.getClass().getSimpleName(),
                        ((ManageOfferOperation) op).getOfferId(),
                        ((ManageOfferOperation) op).getAmount(),
                        ((ManageOfferOperation) op).getSelling(),
                        ((ManageOfferOperation) op).getPrice(),
                        ((ManageOfferOperation) op).getBuying()
                ));
            } else if (op instanceof CreateAccountOperation) {
                Config.log(String.format(
                        "%s -> id = %s\n\tseed = %s\n\tStartingBalance = %s\n\tSourceAccountId = %s",
                        op.getClass().getSimpleName(),
                        ((CreateAccountOperation) op).getDestination().getAccountId(),
                        new String(((CreateAccountOperation) op).getDestination().getSecretSeed()),
                        ((CreateAccountOperation) op).getStartingBalance(),
                        op.getSourceAccount() == null ? null : op.getSourceAccount().getAccountId()
                ));
            } else if (op instanceof AccountMergeOperation) {
                Config.log(String.format(
                        "%s -> from = %s\n\tto = %s",
                        op.getClass().getSimpleName(),
                        op.getSourceAccount() == null ? null : op.getSourceAccount().getAccountId(),
                        ((AccountMergeOperation) op).getDestination().getAccountId()
                ));
            } else if (op instanceof ManageDataOperation) {
                Config.log(String.format(
                        "%s -> acc = %s:\n\t%s = %s",
                        op.getClass().getSimpleName(),
                        op.getSourceAccount() == null ? null : op.getSourceAccount().getAccountId(),
                        ((ManageDataOperation) op).getName(),
                        new String(((ManageDataOperation) op).getValue())
                ));
            } else {
                Config.log(String.format("%s -> ...", op.getClass().getSimpleName()));
            }
        }
        Transaction transaction = trBuilder.build();

        // Sign the transaction to prove you are actually the person sending it.
        transaction.sign(source);

        Server server = Connections.getServer();
        //And finally, send it off to Stellar!
        try {
            SubmitTransactionResponse response = server.submitTransaction(transaction);
            Config.log(formatTransactionResponse(response));
        } catch (Exception e) {
            Config.log("Exception:\n" + e.getMessage());
            // If the result is unknown (no response body, timeout etc.) we simply resubmit
            // already built transaction:
            // SubmitTransactionResponse response = server.submitTransaction(transaction);
        }

        return transaction;
    }

    private static String formatTransactionResponse(SubmitTransactionResponse response) {
        String result = "";
        if (response.isSuccess()) {
            result = "Success!";
        } else {
            ArrayList<String> operationResultCodes = response.getExtras().getResultCodes().getOperationsResultCodes();
            String transactionResultCode = response.getExtras().getResultCodes().getTransactionResultCode();
            result = "Something went wrong:\n" +
                    String.format(
                            "\n\tEnvelopeXdr = %s\n\tResultXdr = %s\n\toperationResultCodes = %s\n\ttransactionResultCode = %s",
                            response.getEnvelopeXdr(),
                            response.getResultXdr(),
                            String.join(", ", operationResultCodes),
                            transactionResultCode
                    );
        }

        return result;
    }

    static boolean checkTrust(Asset asset, KeyPair keysToCheck) {
        Config.log("\nChecking trust for " + formatAssetName(asset) + " in account " + keysToCheck.getAccountId());

        // Load the account you want to check
        AccountResponse accountToCheck = Accounts.getAccount(keysToCheck);

        if (accountToCheck != null) {
            // See if any balances are for the asset code and issuer we're looking for
            for (AccountResponse.Balance balance : accountToCheck.getBalances()) {
                if (balance.getAsset().equals(asset)) {
                    Config.log("\tIt's ok");
                    return true;
                }
            }
        }

        Config.log("\tDo not trust");
        return false;
    }

    static String formatAssetName(Asset asset) {
        if (asset.equals(new AssetTypeNative())) {
            return "lumens";
        } else {
            return ((AssetTypeCreditAlphaNum) asset).getCode() +
                    ":" +
                    ((AssetTypeCreditAlphaNum) asset).getIssuer().getAccountId();
        }
    }

    private static void printOffer(OfferResponse offerResponse, KeyPair pair) {
//        // Record the paging token so we can start from here next time.
//        Config.saveLastPagingToken(pair.getAccountId(), offerResponse.getPagingToken());

        String output = "" +
                String.valueOf(offerResponse.getId()) +
                ": " +
                offerResponse.getAmount() +
                " " +
                formatAssetName(offerResponse.getSelling()) +
                ", price " +
                offerResponse.getPrice() +
                " " +
                formatAssetName(offerResponse.getBuying());
        System.out.println(output);

    }

    static void fetchOffers(final String accountId) {

        Server server = Connections.getServer();
        final KeyPair pair = KeyPair.fromAccountId(accountId);

        OffersRequestBuilder request = server.offers().forAccount(pair);

//        // If some payments have already been handled, start the results from the
//        // last seen payment. (See below in `handlePayment` where it gets saved.)
//        String lastToken = Config.loadLastPagingToken(accountId);
//        if (lastToken != null) {
//            request.cursor(lastToken);
//        }

        Page<OfferResponse> page = null;
        try {
            page = request.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (page != null) {
            ArrayList<OfferResponse> pages = page.getRecords();
            if (pages.size() > 0) {
                Config.log("Offers:");
                for (OfferResponse response : pages) {
                    printOffer(response, pair);
                }
            }
        }
    }

    static void clearOffers(KeyPair pair) {

        Server server = Connections.getServer();

        OffersRequestBuilder request = server.offers().forAccount(pair);

        Page<OfferResponse> page = null;
        try {
            page = request.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (page != null) {
            Config.log(Config.DELIMITER + "Clear offers for " + pair.getAccountId());
            ArrayList<OfferResponse> pages = page.getRecords();
            ManageOfferOperation operations[] = new ManageOfferOperation[pages.size()];
            int i = 0;
            for (OfferResponse response : pages) {
                operations[i] = new ManageOfferOperation.Builder(response.getSelling(), response.getBuying(), "0", response.getPrice())
                        .setOfferId(response.getId())
                        .build();
                i++;
            }
            if (operations.length > 0) {
                Payments.doTrasnaction(pair, null, operations, null);
            }
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

            String output = directionPrefix +
                    " " +
                    payment.getAmount() +
                    " " +
                    formatAssetName(payment.getAsset()) +
                    " from " +
                    payment.getFrom().getAccountId();
            System.out.println(output);
        }

    }

    static void fetchPayments(final String accountId) {
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
