/**
 * Created by Stas on 2018-06-13.
 */
public class Config {
    public static final Boolean printLog = true;

    public static void log(String text) {
        if (printLog) {
            System.out.println(text);
        }
    }

    public static String loadLastPagingToken(String accountId) {
        //TODO: Here can implemented reading the token of the last page viewed for each account
        return null;
    }

    public static void saveLastPagingToken(String accountId, String pagingToken) {
        //TODO: Here can implemented saving the token of the last page viewed for each account
    }
}
