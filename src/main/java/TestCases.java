import org.stellar.sdk.*;
import org.stellar.sdk.responses.AccountResponse;

/**
 * Created by Stas on 2018-06-13.
 */
class TestCases {

    //transaction: "https://horizon-testnet.stellar.org/transactions/ca4277c0b39bfab7b5b9797227f7fefcfdb8be7d5dd2d51fa84d6f5e40de71c3"
    private static final char SEED_1[] = "SA7K65VOPZMPKCQDNGRY6NYDAFOEDK7MFCNUFP4NZMUQKCIFB6PYFVLD".toCharArray();
    private static final String ACCOUNT_ID_1 = "GAJFV74BU3YKS4EEFG3Y57KZNILCVH7K3VZGLY7W556DSHEL2UHIYAFI";

    //transaction: "https://horizon-testnet.stellar.org/transactions/ca4277c0b39bfab7b5b9797227f7fefcfdb8be7d5dd2d51fa84d6f5e40de71c3"
    private static final char SEED_2[] = "SDLTP2DTC7GIBKLF73QI6Q4UQR5FSXKLSO326OTHFKWB77IKLBLPMZQ4".toCharArray();
    private static final String ACCOUNT_ID_2 = "GA4ZSA3YCV25ARCLWK6N2WX2YT4GLNCMHVJSY5LJG3DF7R2BOEHKMUUS";

    private static final String ASSET_ASTRODOLLAR = "AstroDollar";

    static void doTransactions() {
        doTransactionFromAccount(ACCOUNT_ID_1, SEED_1, ACCOUNT_ID_2);
        doTransactionFromAccount(ACCOUNT_ID_2, SEED_2, ACCOUNT_ID_1);
    }
    private static void doTransactionFromAccount(String accountId1, char seed1[], String accountId2) {
        AccountResponse account = Accounts.getAccount(accountId1);
        Accounts.printAccountDetails(account);

        AccountResponse account2 = Accounts.getAccount(accountId2);
        Accounts.printAccountDetails(account2);

        KeyPair source = KeyPair.fromSecretSeed(seed1); //SEED_1 required. Without it there will be an error "KeyPair does not contain secret key. Use KeyPair.fromSecretSeed method to create a new KeyPair with a secret key"
        KeyPair destination = account2.getKeypair();

        PaymentOperation operations[] = {
                Payments.getOperation(destination, new AssetTypeNative(), "10"),
                Payments.getOperation(destination, new AssetTypeNative(), "100000000"), // This will cause an error
                Payments.getOperation(destination, new AssetTypeNative(), "15")
        };

        Payments.doTrasnaction(source, destination, operations, Memo.text("Test Transaction"));

        Accounts.printAccountDetails(accountId1);

        Accounts.printAccountDetails(accountId2);
    }

    static void readPayments() {
        Payments.fetchPayments(ACCOUNT_ID_1);
        Payments.fetchPayments(ACCOUNT_ID_2);
    }

    static void nonNativeAssetTest() {
        Accounts.printAccountDetails(ACCOUNT_ID_1);
        Accounts.printAccountDetails(ACCOUNT_ID_2);

        Server server = Connections.getServer();

        // Keys for accounts to issue and receive the new asset
        KeyPair issuingKeys = KeyPair.fromSecretSeed(SEED_1);
        KeyPair receivingKeys = KeyPair.fromSecretSeed(SEED_2);

        // Create an object to represent the new asset
        Asset astroDollar = Asset.createNonNativeAsset(ASSET_ASTRODOLLAR, issuingKeys);

        // First, the receiving account must trust the asset
        AccountResponse receiving = Accounts.getAccount(receivingKeys);
        // The `ChangeTrust` operation creates (or alters) a trustline
        // The second parameter limits the amount the account can hold
        Payments.doTrasnaction(receivingKeys, issuingKeys, new ChangeTrustOperation.Builder(astroDollar, "1000").build(), null);

        // Second, the issuing account actually sends a payment using the asset
        Payments.doTrasnaction(issuingKeys, receivingKeys, new PaymentOperation.Builder(receivingKeys, astroDollar, "10").build(), null);

        Accounts.printAccountDetails(ACCOUNT_ID_1);
        Accounts.printAccountDetails(ACCOUNT_ID_2);
    }

    static void setDomainTest() {
        // Keys for issuing account
        KeyPair issuingKeys = KeyPair.fromSecretSeed(SEED_1);

        Payments.doTrasnaction(
                issuingKeys,
                new SetOptionsOperation.Builder()
                        .setHomeDomain("test-home-domain.com")
                        .build(),
                null);

    }
}
