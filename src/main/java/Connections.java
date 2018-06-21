import org.stellar.sdk.Network;
import org.stellar.sdk.Server;
import org.stellar.sdk.responses.LedgerResponse;
import org.stellar.sdk.responses.OfferResponse;
import org.stellar.sdk.responses.Page;
import org.stellar.sdk.responses.Response;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Stas on 2018-06-13.
 */
class Connections {

    static Server getServer() {
        return getServer(false);
    }

    private static Server getServer(Boolean isProd) {
        Server result = null;
        if (!isProd) {
            Network.useTestNetwork();
            result = new Server(app.HOST_HORIZON);
        } else {
            //TODO: Prod server not implemented yet, because we are only studying
        }
        return result;
    }


    static void getCommonInfo() {
        try {
            getCommonInfoException();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void printResponse(Response response) {
        String className = response.getClass().getSimpleName();
        if (response instanceof LedgerResponse) {
            LedgerResponse resp = (LedgerResponse) response;
            Config.log(String.format(
                    "%s --> PagingToken = %s" +
                            "\n\tBaseFeeInStroops = %s, BaseReserveInStroops = %s" +
                            "\n\tClosedAt = %s, FeePool = %s" +
                            "\n\tHash = %s" +
                            "\n\tPrevHash = %s" +
                            "\n\tMaxTxSetSize = %s" +
                            "\n\tOperationCount = %s" +
                            "\n\tSequence = %s" +
                            "\n\tTotalCoins = %s" +
                            "\n\tTransactionCount = %s",
                    className,
                    resp.getPagingToken(),
                    resp.getBaseFeeInStroops(), resp.getBaseReserveInStroops(),
                    resp.getClosedAt(),
                    resp.getFeePool(),
                    resp.getHash(),
                    resp.getPrevHash(),
                    resp.getMaxTxSetSize(),
                    resp.getOperationCount(),
                    resp.getSequence(),
                    resp.getTotalCoins(),
                    resp.getTransactionCount()
            ));
        } else {
            Config.log(String.format(
                    "%s --> ...",
                    className
            ));
        }
    }
    private static void getCommonInfoException() throws IOException {
        Server server = Connections.getServer();

        Page<LedgerResponse> page = server.ledgers().execute();
        if (page != null) {
            ArrayList<LedgerResponse> pages = page.getRecords();
            if (pages.size() > 0) {
                Config.log(Config.DELIMITER + "Ledger:");
                for (Response response : pages) {
                    printResponse(response);
                }
            }
        }

        Page<OfferResponse> pageOffers = server.offers().execute();
        if (page != null) {
            ArrayList<OfferResponse> pages = pageOffers.getRecords();
            if (pages.size() > 0) {
                Config.log(Config.DELIMITER + "Offer:");
                for (Response response : pages) {
                    printResponse(response);
                }
            }
        }
    }

}
