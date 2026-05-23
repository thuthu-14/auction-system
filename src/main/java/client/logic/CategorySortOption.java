package client.logic;

import server.model.Auction;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

public enum CategorySortOption {
    ENDING_SOON("Sắp kết thúc") {
        @Override
        public Comparator<Auction> comparator(ToIntFunction<Auction> bidCounter) {
            return Comparator.comparingLong(Auction::getEndTime);
        }
    },
    NEWEST("Mới nhất") {
        @Override
        public Comparator<Auction> comparator(ToIntFunction<Auction> bidCounter) {
            return Comparator.comparingLong(Auction::getCreatedAt).reversed();
        }
    },
    HIGHEST_PRICE("Giá cao nhất") {
        @Override
        public Comparator<Auction> comparator(ToIntFunction<Auction> bidCounter) {
            return Comparator.comparingDouble(Auction::getCurrentPrice).reversed();
        }
    },
    LOWEST_PRICE("Giá thấp nhất") {
        @Override
        public Comparator<Auction> comparator(ToIntFunction<Auction> bidCounter) {
            return Comparator.comparingDouble(Auction::getCurrentPrice);
        }
    },
    MOST_BIDS("Nhiều lượt đặt giá") {
        @Override
        public Comparator<Auction> comparator(ToIntFunction<Auction> bidCounter) {
            return Comparator.comparingInt(bidCounter)
                    .reversed()
                    .thenComparing(Comparator.comparingDouble(Auction::getCurrentPrice).reversed());
        }
    };

    private final String label;

    CategorySortOption(String label) {
        this.label = label;
    }

    public abstract Comparator<Auction> comparator(ToIntFunction<Auction> bidCounter);

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static List<String> labels() {
        return Arrays.stream(values())
                .map(CategorySortOption::label)
                .toList();
    }

    public static CategorySortOption fromLabel(String label) {
        return Arrays.stream(values())
                .filter(option -> option.label.equals(label))
                .findFirst()
                .orElse(ENDING_SOON);
    }
}
