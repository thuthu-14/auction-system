package client.controller.auctiondetail;

import client.network.AuctionListener;
import common.Message;
import server.model.Auction;
import server.model.Bid;
import util.LoggerUtil;

public class AuctionRealtimeDispatcher implements AuctionListener {
    private final AuctionListener delegate;

    public AuctionRealtimeDispatcher(AuctionListener delegate) {
        this.delegate = delegate;
    }

    public void dispatch(Message message) {
        if (message == null || message.getType() == null) {
            return;
        }

        Object data = message.getData();
        switch (message.getType()) {
            case UPDATE_PRICE_REALTIME -> {
                if (data instanceof Bid bid) {
                    onBidPlaced(bid);
                } else if (data instanceof Auction auction) {
                    onPriceUpdated(auction);
                }
            }
            case UPDATE, SELLER_AUCTIONS_UPDATED -> {
                if (data instanceof Auction auction) {
                    onAuctionUpdated(auction);
                }
            }
            case AUCTION_FINISHED_NOTIFICATION -> {
                if (data instanceof Auction auction) {
                    onAuctionFinished(auction);
                }
            }
            case OUTBID_NOTIFICATION -> {
                if (data instanceof Auction auction) {
                    onOutbid(auction);
                }
            }
            case AUCTION_EXTENDED_NOTIFICATION -> {
                if (data instanceof Auction auction) {
                    onAuctionExtended(auction);
                }
            }
            case NOTIFICATION -> onNotification(message.getMessage());
            case ERROR -> onError(message.getMessage());
            default -> {
            }
        }
    }

    @Override
    public void onAuctionUpdated(Auction auction) {
        delegate.onAuctionUpdated(auction);
    }

    @Override
    public void onBidPlaced(Bid bid) {
        delegate.onBidPlaced(bid);
    }

    @Override
    public void onPriceUpdated(Auction auction) {
        delegate.onPriceUpdated(auction);
    }

    @Override
    public void onAuctionFinished(Auction auction) {
        delegate.onAuctionFinished(auction);
    }

    @Override
    public void onError(String errorMessage) {
        if (delegate != null) {
            delegate.onError(errorMessage);
        } else {
            LoggerUtil.warn("Auction realtime error: " + safeText(errorMessage));
        }
    }

    @Override
    public void onNotification(String message) {
        delegate.onNotification(message);
    }

    @Override
    public void onOutbid(Auction auction) {
        delegate.onOutbid(auction);
    }

    @Override
    public void onAuctionExtended(Auction auction) {
        delegate.onAuctionExtended(auction);
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "" : value;
    }
}
