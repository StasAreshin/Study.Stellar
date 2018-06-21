/**
 * Created by Stas on 2018-06-13.
 */
class Config {
    private static final Boolean PRINT_LOG = true;
    public static final String DELIMITER = "---------------------------------\n";

    static void log(String text) {
        if (PRINT_LOG) {
            System.out.println(text);
        }
    }

    static String loadLastPagingToken(String accountId) {
        //TODO: Here can implemented reading the token of the last page viewed for each account
        return null;
    }

    static void saveLastPagingToken(String accountId, String pagingToken) {
        //TODO: Here can implemented saving the token of the last page viewed for each account
    }
}
