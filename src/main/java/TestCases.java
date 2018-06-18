import org.stellar.sdk.*;
import org.stellar.sdk.responses.AccountResponse;

/**
 * Created by Stas on 2018-06-13.
 */
class TestCases {

    //transaction: "https://horizon-testnet.stellar.org/transactions/ca4277c0b39bfab7b5b9797227f7fefcfdb8be7d5dd2d51fa84d6f5e40de71c3"
    private static final char ISSUING_SEED[] = "SA7K65VOPZMPKCQDNGRY6NYDAFOEDK7MFCNUFP4NZMUQKCIFB6PYFVLD".toCharArray();
    private static final String ISSUING_ACCOUNT_ID = "GAJFV74BU3YKS4EEFG3Y57KZNILCVH7K3VZGLY7W556DSHEL2UHIYAFI";

    private static final char BASE_SEED[] = "SBBHGVJ6SPT3QRCNDJB4T7WEYWJVMJCP55FE2SVUW77LRPTIYMZMA5UI".toCharArray();
    private static final String BASE_ACCOUNT_ID = "GAENOEM6FBUVFDUSZFQS3AHF35L6N7KGZC53ZTF2BUKHOEEE6FCHQFG6";

    //transaction: "https://horizon-testnet.stellar.org/transactions/ca4277c0b39bfab7b5b9797227f7fefcfdb8be7d5dd2d51fa84d6f5e40de71c3"
    private static final char SEED_2[] = "SDLTP2DTC7GIBKLF73QI6Q4UQR5FSXKLSO326OTHFKWB77IKLBLPMZQ4".toCharArray();
    private static final String ACCOUNT_ID_2 = "GA4ZSA3YCV25ARCLWK6N2WX2YT4GLNCMHVJSY5LJG3DF7R2BOEHKMUUS";

    private static final String ASSET_ASTRODOLLAR = "AstroDollar";

    static void printAccountDetails() {
        Accounts.printAccountDetails(ISSUING_ACCOUNT_ID);
        Accounts.printAccountDetails(BASE_ACCOUNT_ID);
        Accounts.printAccountDetails(ACCOUNT_ID_2);
        Accounts.printAccountDetails("GALMCZ76QKBJNLPTTP34KNSFEN7BVFGXSRHCCQXUDIJY65MSZB65LWVQ");
    }

    static void doTransactions() {
        doTransactionFromAccount(ISSUING_ACCOUNT_ID, ISSUING_SEED, ACCOUNT_ID_2);
        doTransactionFromAccount(ACCOUNT_ID_2, SEED_2, ISSUING_ACCOUNT_ID);
    }
    private static void doTransactionFromAccount(String accountId1, char seed1[], String accountId2) {
        AccountResponse account = Accounts.getAccount(accountId1);
        Accounts.printAccountDetails(account);

        AccountResponse account2 = Accounts.getAccount(accountId2);
        Accounts.printAccountDetails(account2);

        KeyPair source = KeyPair.fromSecretSeed(seed1); //ISSUING_SEED required. Without it there will be an error "KeyPair does not contain secret key. Use KeyPair.fromSecretSeed method to create a new KeyPair with a secret key"
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
        Payments.fetchPayments(ISSUING_ACCOUNT_ID);
        Payments.fetchPayments(ACCOUNT_ID_2);
    }

    static void readOffers() {
        Payments.fetchOffers(ISSUING_ACCOUNT_ID);
        Payments.fetchOffers(ACCOUNT_ID_2);
    }

    private static Asset getAstroDollar() {
        return getAstroDollar(KeyPair.fromSecretSeed(ISSUING_SEED));
    }
    private static Asset getAstroDollar(KeyPair pair) {
        return Asset.createNonNativeAsset(ASSET_ASTRODOLLAR, pair);
    }
    static void nonNativeAssetTest() {
        Accounts.printAccountDetails(ISSUING_ACCOUNT_ID);
        Accounts.printAccountDetails(ACCOUNT_ID_2);

        Server server = Connections.getServer();

        // Keys for accounts to issue and receive the new asset
        KeyPair issuingKeys = KeyPair.fromSecretSeed(ISSUING_SEED);
        KeyPair receivingKeys = KeyPair.fromSecretSeed(SEED_2);

        // Create an object to represent the new asset
        Asset astroDollar = getAstroDollar();

        // First, the receiving account must trust the asset
        AccountResponse receiving = Accounts.getAccount(receivingKeys);
        // The `ChangeTrust` operation creates (or alters) a trustline
        // The second parameter limits the amount the account can hold
        Payments.doTrasnaction(receivingKeys, issuingKeys, new ChangeTrustOperation.Builder(astroDollar, "1000").build(), null);

        // Second, the issuing account actually sends a payment using the asset
        Payments.doTrasnaction(issuingKeys, receivingKeys, new PaymentOperation.Builder(receivingKeys, astroDollar, "980").build(), null);

        Accounts.printAccountDetails(ISSUING_ACCOUNT_ID);
        Accounts.printAccountDetails(ACCOUNT_ID_2);
    }

    static void setDomainTest() {
        // Keys for issuing account
        KeyPair issuingKeys = KeyPair.fromSecretSeed(ISSUING_SEED);

        Payments.doTrasnaction(
                issuingKeys,
                new SetOptionsOperation.Builder()
                        .setHomeDomain("test-home-domain.com")
                        .setSetFlags(AccountFlag.AUTH_REQUIRED_FLAG.getValue() | AccountFlag.AUTH_REVOCABLE_FLAG.getValue())
                        .build(),
                Memo.text("setHomeDomain\nsetSetFlags: AUTH_REQUIRED_FLAG, AUTH_REVOCABLE_FLAG"));

    }

    static void checkTrustBeforePaying() {
        KeyPair pair_1 = KeyPair.fromAccountId(ISSUING_ACCOUNT_ID);
        KeyPair pair_2 = KeyPair.fromAccountId(ACCOUNT_ID_2);
        boolean trust1 = Payments.checkTrust(getAstroDollar(), pair_2);
        boolean trust2 = Payments.checkTrust(getAstroDollar(pair_2), pair_2);
        boolean trust3 = Payments.checkTrust(getAstroDollar(), pair_1);
        boolean trust4 = Payments.checkTrust(getAstroDollar(pair_2), pair_1);
    }

    static void changeOffer() {
        TestCases.readOffers();

        long offerId = 433276;
        KeyPair baseKeys = KeyPair.fromSecretSeed(SEED_2);

        ManageOfferOperation.Builder operationBuilder = new ManageOfferOperation.Builder(getAstroDollar(), new AssetTypeNative(), "500", "30");
        if (offerId != 0) {
            operationBuilder.setOfferId(offerId);
        }
        Payments.doTrasnaction(
                baseKeys,
                operationBuilder.build(),
                Memo.text("changeOffer id=" + String.valueOf(offerId)));

        TestCases.readOffers();
    }

    static void createNewAccount() {
        Accounts.createAccount(KeyPair.fromSecretSeed(ISSUING_SEED), "200");
    }

    static void mergeAccounts() {
//        SD4BSDEN5CXG6KDQYMGRFBAVDPEZAGUPNXT7777LJFSLEQK5L2TWY5A3
//        accounId: GALMCZ76QKBJNLPTTP34KNSFEN7BVFGXSRHCCQXUDIJY65MSZB65LWVQ
        Accounts.mergeAccounts("SD4BSDEN5CXG6KDQYMGRFBAVDPEZAGUPNXT7777LJFSLEQK5L2TWY5A3".toCharArray(), BASE_SEED);
    }
}
