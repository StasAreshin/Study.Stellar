import org.stellar.sdk.*;
import org.stellar.sdk.responses.AccountResponse;

import java.nio.charset.Charset;
import java.security.Key;

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

    private static final char SEED_3[] = "SCTNGBDFWL5KTKC7FENEKPC4IUMIZZSDR6JUCLL5NHZS7G2RRIYUX4OI".toCharArray();
    private static final String ACCOUNT_ID_3 = "GAAICMIBHZZXMMEGB6IT7AR5NK2AF2OEHBOIRHRBBJSHXGEOB3N4YB45";
    private static final char SEED_4[] = "SDQFX7MDJVJLEKQMOLBPWYIA7DSPA7IG5ZJOESHIULW6DGVRFY2UABOT".toCharArray();
    private static final String ACCOUNT_ID_4 = "GCHGUIR6UUEUXMRVXLKCFFGEEAI563R7Q3IPOEXLMS3VVE4IYJLXZOR6";
    private static final char SEED_5[] = "SATE74EXVH6USDRXN7C2L6MHQGSU5H2BLQG37SJIDMLXHA6D7Z27ZOIF".toCharArray();
    private static final String ACCOUNT_ID_5 = "GCP2DVPXGTEYCPI7MJWXGGX6PCPUZFWE2OUFUMWAUM63WYDQ6AB3S45K";

    private static final String ASSET_ASTRODOLLAR = "AstroDollar";

    static void printAccountDetails() {
        Accounts.printAccountDetails(ISSUING_ACCOUNT_ID);
        Accounts.printAccountDetails(BASE_ACCOUNT_ID);
        Accounts.printAccountDetails(ACCOUNT_ID_2);
        Accounts.printAccountDetails(ACCOUNT_ID_3);
        Accounts.printAccountDetails(ACCOUNT_ID_4);
        Accounts.printAccountDetails(ACCOUNT_ID_5);
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
                        .setHomeDomain("Issuing.com")
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

    static void clearOffers(char[] seed) {
        if (seed == null) {
            Payments.clearOffers(KeyPair.fromSecretSeed(ISSUING_SEED));
            Payments.clearOffers(KeyPair.fromSecretSeed(BASE_SEED));
            Payments.clearOffers(KeyPair.fromSecretSeed(SEED_2));
            Payments.clearOffers(KeyPair.fromSecretSeed(SEED_3));
            Payments.clearOffers(KeyPair.fromSecretSeed(SEED_4));
            Payments.clearOffers(KeyPair.fromSecretSeed(SEED_5));
        } else {
            Payments.clearOffers(KeyPair.fromSecretSeed(seed));
        }
    }
    static void clearOffers() {
        clearOffers(null);
    }

    static void createNewAccount() {
        Accounts.createAccount(KeyPair.fromSecretSeed(ISSUING_SEED), "200");
    }

    static void mergeAccounts() {
//        SD4BSDEN5CXG6KDQYMGRFBAVDPEZAGUPNXT7777LJFSLEQK5L2TWY5A3
//        accounId: GALMCZ76QKBJNLPTTP34KNSFEN7BVFGXSRHCCQXUDIJY65MSZB65LWVQ
        Accounts.mergeAccounts("SD4BSDEN5CXG6KDQYMGRFBAVDPEZAGUPNXT7777LJFSLEQK5L2TWY5A3".toCharArray(), BASE_SEED);
    }

    static void manageData() {
        Accounts.manageData(KeyPair.fromSecretSeed(ISSUING_SEED), "name", "Issuing".getBytes(Charset.defaultCharset()));
        Accounts.manageData(KeyPair.fromSecretSeed(BASE_SEED), "name", "Base".getBytes(Charset.defaultCharset()));
        Accounts.manageData(KeyPair.fromSecretSeed(SEED_2), "name", "Account 002".getBytes(Charset.defaultCharset()));
        Accounts.manageData(KeyPair.fromSecretSeed(SEED_3), "name", "Account 003".getBytes(Charset.defaultCharset()));
        Accounts.manageData(KeyPair.fromSecretSeed(SEED_4), "name", "Account 004".getBytes(Charset.defaultCharset()));
        Accounts.manageData(KeyPair.fromSecretSeed(SEED_5), "name", "Account 005".getBytes(Charset.defaultCharset()));
    }

    static void offersTest() {
        KeyPair issuing = KeyPair.fromSecretSeed(ISSUING_SEED);
        KeyPair base = KeyPair.fromSecretSeed(BASE_SEED);
        KeyPair acc2 = KeyPair.fromSecretSeed(SEED_2);
        KeyPair acc3 = KeyPair.fromSecretSeed(SEED_3);
        KeyPair acc4 = KeyPair.fromSecretSeed(SEED_4);
        KeyPair acc5 = KeyPair.fromSecretSeed(SEED_5);

        Asset USD = Asset.createNonNativeAsset("USD", issuing);
        Asset EUR = Asset.createNonNativeAsset("EUR", issuing);
        Asset UAH = Asset.createNonNativeAsset("UAH", issuing);
        Asset RUB = Asset.createNonNativeAsset("RUB", issuing);

//        Operation[] setTrustOperations = {
//                new ChangeTrustOperation.Builder(USD, "1000").build(),
//                new ChangeTrustOperation.Builder(EUR, "1000").build(),
//                new ChangeTrustOperation.Builder(UAH, "1000").build(),
//                new ChangeTrustOperation.Builder(RUB, "1000").build()};
//        Payments.doTrasnaction(base, null, setTrustOperations, null);
//        Payments.doTrasnaction(acc2, null, setTrustOperations, null);
//        Payments.doTrasnaction(acc3, null, setTrustOperations, null);
//        Payments.doTrasnaction(acc4, null, setTrustOperations, null);
//        Payments.doTrasnaction(acc5, null, setTrustOperations, null);
//
//        Payments.doTrasnaction(issuing, null,
//                new Operation[]{
//                        new PaymentOperation.Builder(base, USD, "500").build(),
//                        new PaymentOperation.Builder(base, EUR, "500").build(),
//                        new PaymentOperation.Builder(base, UAH, "500").build(),
//                        new PaymentOperation.Builder(base, RUB, "500").build(),
//                        new PaymentOperation.Builder(acc2, USD, "500").build(),
//                        new PaymentOperation.Builder(acc2, EUR, "500").build(),
//                        new PaymentOperation.Builder(acc2, UAH, "500").build(),
//                        new PaymentOperation.Builder(acc2, RUB, "500").build(),
//                        new PaymentOperation.Builder(acc3, USD, "500").build(),
//                        new PaymentOperation.Builder(acc3, EUR, "500").build(),
//                        new PaymentOperation.Builder(acc3, UAH, "500").build(),
//                        new PaymentOperation.Builder(acc3, RUB, "500").build(),
//                        new PaymentOperation.Builder(acc4, USD, "500").build(),
//                        new PaymentOperation.Builder(acc4, EUR, "500").build(),
//                        new PaymentOperation.Builder(acc4, UAH, "500").build(),
//                        new PaymentOperation.Builder(acc4, RUB, "500").build()
//                }, null);

/*      Example:
            base: sell 100 UAH - buy USD
        Steps:
            acc2: 200 RUB = 100 UAH (RUB, UAH, "200", "0.5")
                Yusd = Xuah = Xuah * 200/100 RUB
            acc3: 30 EUR = 240 RUB (EUR, RUB, "200", "0.125")
                Yusd = Xuah * 200/100 RUB = Xuah * 200/100 * 30/240 EUR
            acc4: 80 USD = 40 EUR (USD, EUR, "200", "0.5")
                Yusd = Xuah * 200/100RUB * 30/240 EUR = Xuah * 200/100RUB * 30/240 EUR * 80/40 USD = 0.5 Xuah USD
         Total:
                100 USD = 200 UAH
*/

//        clearOffers();
//        Payments.doTrasnaction(acc2, null,
//                new ManageOfferOperation.Builder(RUB, UAH, "200", "0.5").build(), null);
//        Payments.doTrasnaction(acc3, null,
//                new ManageOfferOperation.Builder(EUR, RUB, "200", "0.125").build(), null);
//        Payments.doTrasnaction(acc4, null,
//                new ManageOfferOperation.Builder(USD, EUR, "200", "0.5").build(), null);

/*
        //Sub example:  base: sell 1 UAH - buy 2 RUB
        Payments.doTrasnaction(base, null,
                new ManageOfferOperation.Builder(UAH, RUB, "1", "2.0")
                        .setOfferId(433990)
                        .build(), null);
        Payments.doTrasnaction(base, null,
                new PaymentOperation.Builder(acc2, RUB, "2").build(), null);
        Payments.doTrasnaction(acc2, null,
                new PaymentOperation.Builder(base, UAH, "1").build(), null);
*/



        // Manage offer test
//        clearOffers(BASE_SEED);
//        Payments.doTrasnaction(base, null,
//                new ManageOfferOperation.Builder(UAH, USD, "100", "0.5")
//                        .setOfferId(433988)
//                        .build(), null);

//        //// Manage offer test
//        // Set new passive offer
//        Payments.doTrasnaction(base, null,
//                new CreatePassiveOfferOperation.Builder(UAH, USD, "100", "0.5")
//                        .build(), null);
//        //Make exchange and close previous passive offer
//        Payments.doTrasnaction(acc2, null,
//                new ManageOfferOperation.Builder(USD, UAH, "50", "2")
//                        .build(), null);


//        // Path payment test
//        Payments.doTrasnaction(base, null,
//                new PathPaymentOperation.Builder(UAH, "200", acc5, USD, "100")
//                        .setPath(new Asset[]{RUB, EUR})
//                        .build(), null);


    }

    static void multiSignationTest() {

        KeyPair issuing = KeyPair.fromSecretSeed(ISSUING_SEED);
        KeyPair base = KeyPair.fromSecretSeed(BASE_SEED);
        KeyPair acc2 = KeyPair.fromSecretSeed(SEED_2);
        KeyPair acc3 = KeyPair.fromSecretSeed(SEED_3);
        KeyPair acc4 = KeyPair.fromSecretSeed(SEED_4);
        KeyPair acc5 = KeyPair.fromSecretSeed(SEED_5);

        Asset USD = Asset.createNonNativeAsset("USD", issuing);
        Asset EUR = Asset.createNonNativeAsset("EUR", issuing);
        Asset UAH = Asset.createNonNativeAsset("UAH", issuing);
        Asset RUB = Asset.createNonNativeAsset("RUB", issuing);

        Operation[] setTrustOperations = {
                new ChangeTrustOperation.Builder(USD, "1000").setSourceAccount(base).build(),
                new ChangeTrustOperation.Builder(EUR, "1000").setSourceAccount(base).build(),
                new ChangeTrustOperation.Builder(UAH, "1000").setSourceAccount(base).build(),
                new ChangeTrustOperation.Builder(RUB, "1000").setSourceAccount(base).build(),

                new ChangeTrustOperation.Builder(USD, "1000").setSourceAccount(acc2).build(),
                new ChangeTrustOperation.Builder(EUR, "1000").setSourceAccount(acc2).build(),
                new ChangeTrustOperation.Builder(UAH, "1000").setSourceAccount(acc2).build(),
                new ChangeTrustOperation.Builder(RUB, "1000").setSourceAccount(acc2).build(),

                new ChangeTrustOperation.Builder(USD, "1000").setSourceAccount(acc3).build(),
                new ChangeTrustOperation.Builder(EUR, "1000").setSourceAccount(acc3).build(),
                new ChangeTrustOperation.Builder(UAH, "1000").setSourceAccount(acc3).build(),
                new ChangeTrustOperation.Builder(RUB, "1000").setSourceAccount(acc3).build(),

                new ChangeTrustOperation.Builder(USD, "1000").setSourceAccount(acc4).build(),
                new ChangeTrustOperation.Builder(EUR, "1000").setSourceAccount(acc4).build(),
                new ChangeTrustOperation.Builder(UAH, "1000").setSourceAccount(acc4).build(),
                new ChangeTrustOperation.Builder(RUB, "1000").setSourceAccount(acc4).build(),

                new ChangeTrustOperation.Builder(USD, "1000").setSourceAccount(acc5).build(),
                new ChangeTrustOperation.Builder(EUR, "1000").setSourceAccount(acc5).build(),
                new ChangeTrustOperation.Builder(UAH, "1000").setSourceAccount(acc5).build(),
                new ChangeTrustOperation.Builder(RUB, "1000").setSourceAccount(acc5).build()
        };

        // Need to add ALL of KeyPairs
        // issuing account will only pay XLM tax for all operations. All operations are belongs to other accounts. But it also must be in signers array
        KeyPair signers[] = {issuing, base, acc2, acc3, acc4, acc5};
        Payments.doTrasnaction(issuing, null, setTrustOperations, null, signers);
    }

}
