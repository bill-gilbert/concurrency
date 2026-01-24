package course.concurrency.m2_async.minPrice;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PriceAggregator {
    // to handle all blocking requests
    private ExecutorService executor = Executors.newCachedThreadPool();

    private PriceRetriever priceRetriever = new PriceRetriever();

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {
        List<CompletableFuture<Double>> completableFutureList =
                shopIds.stream().map(shopId ->
                                CompletableFuture.supplyAsync(() -> priceRetriever.getPrice(itemId, shopId), executor)
                                        .completeOnTimeout(Double.POSITIVE_INFINITY, 2900, TimeUnit.MILLISECONDS)
                                        .exceptionally(ex -> Double.POSITIVE_INFINITY))
                        .toList();

        CompletableFuture
                .allOf(completableFutureList.toArray(CompletableFuture[]::new))
                .join();

        return completableFutureList
                .stream()
                .mapToDouble(CompletableFuture::join)
                .filter(Double::isFinite)
                .min()
                .orElse(Double.NaN);
    }

    public double getMinPriceV2(long itemId) {
        return shopIds.stream()
                .map(shopId -> CompletableFuture
                        .supplyAsync(() -> priceRetriever.getPrice(itemId, shopId), executor)
                        .exceptionally(ex -> Double.POSITIVE_INFINITY)
                        .completeOnTimeout(Double.POSITIVE_INFINITY, 2900, TimeUnit.MILLISECONDS)
                )
                .reduce((future1, future2) -> future1.thenCombine(future2, Double::min))
                .map(CompletableFuture::join)
                .filter(Double::isFinite)
                .orElse(Double.NaN);
    }
}
