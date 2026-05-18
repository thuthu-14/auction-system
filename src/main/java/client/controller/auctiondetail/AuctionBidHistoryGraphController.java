package client.controller.auctiondetail;

import client.service.BidHistoryCache;
import javafx.application.Platform;
import javafx.scene.chart.LineChart;
import javafx.scene.control.ScrollPane;
import server.model.Bid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

public class AuctionBidHistoryGraphController {
    private final LineChart<String, Number> chart;
    private final ScrollPane scrollPane;
    private final Supplier<String> auctionIdSupplier;
    private final DoubleSupplier firstValueSupplier;
    private final DoubleSupplier minimumBidIncrementSupplier;
    private final AuctionBidHistoryGraphRenderer renderer;
    private final Consumer<List<Bid>> priceSyncer;
    private final BidHistoryCache cache = new BidHistoryCache();

    private boolean initialPositioned;
    private boolean fallbackRendered;
    private String lastSignature;

    public AuctionBidHistoryGraphController(LineChart<String, Number> chart,
                                            ScrollPane scrollPane,
                                            Supplier<String> auctionIdSupplier,
                                            DoubleSupplier firstValueSupplier,
                                            DoubleSupplier minimumBidIncrementSupplier,
                                            AuctionBidHistoryGraphRenderer renderer,
                                            Consumer<List<Bid>> priceSyncer) {
        this.chart = chart;
        this.scrollPane = scrollPane;
        this.auctionIdSupplier = auctionIdSupplier;
        this.firstValueSupplier = firstValueSupplier;
        this.minimumBidIncrementSupplier = minimumBidIncrementSupplier;
        this.renderer = renderer;
        this.priceSyncer = priceSyncer;
    }

    public void reset() {
        initialPositioned = false;
        fallbackRendered = false;
        lastSignature = null;
    }

    public void clearSignature() {
        lastSignature = null;
    }

    public void updateGraph(List<Bid> bids) {
        updateGraph(bids, false);
    }

    public void updateGraph(List<Bid> bids, boolean scrollToLatest) {
        if (chart == null) {
            return;
        }

        List<Bid> sortedBids = new ArrayList<>(bids != null ? bids : List.of());
        sortedBids.sort(Comparator.comparingLong(Bid::getBidTime));
        double firstValue = firstValueSupplier.getAsDouble();
        String newSignature = cache.buildSignature(sortedBids, firstValue);
        if (newSignature.equals(lastSignature)) {
            scrollToLatest(scrollToLatest);
            return;
        }
        if (sortedBids.isEmpty() && fallbackRendered) {
            return;
        }
        if (sortedBids.isEmpty() && shouldKeepExistingGraph()) {
            return;
        }

        renderer.render(sortedBids, firstValue, minimumBidIncrementSupplier.getAsDouble());
        fallbackRendered = sortedBids.isEmpty();
        lastSignature = newSignature;

        if (!sortedBids.isEmpty()) {
            priceSyncer.accept(sortedBids);
        }

        String auctionId = auctionIdSupplier.get();
        if (auctionId != null && !auctionId.isBlank()) {
            cache.put(auctionId, sortedBids);
        }
        scrollToLatest(scrollToLatest);
    }

    public boolean shouldKeepExistingGraph() {
        String auctionId = auctionIdSupplier.get();
        if (auctionId != null) {
            List<Bid> cachedBids = cache.get(auctionId);
            if (cachedBids != null && !cachedBids.isEmpty()) {
                return true;
            }
        }
        return chart != null
                && !chart.getData().isEmpty()
                && !chart.getData().get(0).getData().isEmpty()
                && chart.getData().get(0).getData().size() > 1;
    }

    public void showFallbackCurrentPriceGraph() {
        if (chart == null || fallbackRendered) {
            return;
        }

        double firstValue = firstValueSupplier.getAsDouble();
        renderer.render(List.of(), firstValue, minimumBidIncrementSupplier.getAsDouble());
        fallbackRendered = true;
        lastSignature = cache.buildSignature(List.of(), firstValue);
    }

    public void keepExistingOrShowFallback() {
        if (!shouldKeepExistingGraph()) {
            showFallbackCurrentPriceGraph();
        }
    }

    public boolean shouldAutoScrollInitialHistory() {
        if (initialPositioned) {
            return false;
        }
        initialPositioned = true;
        return true;
    }

    public void mergeBidIntoHistory(Bid bid) {
        mergeBidIntoHistory(bid, false);
    }

    public void mergeBidIntoHistory(Bid bid, boolean scrollToLatest) {
        String auctionId = auctionIdSupplier.get();
        if (bid == null || auctionId == null || auctionId.isBlank()) {
            return;
        }

        List<Bid> cachedBids = cache.merge(auctionId, bid);
        fallbackRendered = false;
        updateGraph(cachedBids, scrollToLatest);
    }

    public List<Bid> mergeWithCachedHistory(String auctionId, List<Bid> serverBids) {
        return cache.merge(auctionId, serverBids);
    }

    public List<Bid> getCachedBids(String auctionId) {
        return cache.get(auctionId);
    }

    public double getHighestPrice(String auctionId) {
        return cache.getHighestPrice(auctionId);
    }

    private void scrollToLatest(boolean scrollToLatest) {
        if (scrollPane != null && scrollToLatest) {
            Platform.runLater(() -> scrollPane.setHvalue(1.0));
        }
    }
}
