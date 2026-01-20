package course.concurrency.m2_async.minPrice;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PriceAggregator {

    private PriceRetriever priceRetriever = new PriceRetriever();

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {
        // Executor для экспериментов
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(shopIds.size(), 100));

        // Создаем futures для всех магазинов
        List<CompletableFuture<Double>> futures = shopIds.stream()
                .map(shopId -> CompletableFuture.supplyAsync(() ->
                                priceRetriever.getPrice(itemId, shopId)
                        , executor))
                .toList();

        // Объединяем все futures
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        // Создаем future с таймаутом
        CompletableFuture<List<Double>> resultsFuture = allFutures
                .handle((result, ex) -> {
                    if (ex != null) {
                        System.out.println("Ошибка: " + ex.getMessage());
                        return 0;
                    } else {
                        return result;
                    }
                })
                .thenApply(v -> futures.stream()
                        .map(f -> {
                            try {
                                return f.getNow(null); // Берем результат без ожидания
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull) // Фильтруем null
                        .toList()
                );

        try {
            // Ждем максимум 2.5 секунды. Т.к. Пункт 2

            List<Double> results = resultsFuture.get(2500, TimeUnit.MILLISECONDS);

            return results.isEmpty() ? Double.NaN :
                    results.stream().min(Double::compareTo).get();

        } catch (TimeoutException e) {
            // Время вышло - собираем то, что успели
            List<Double> results = futures.stream()
                    .filter(CompletableFuture::isDone)
                    .filter(f -> !f.isCompletedExceptionally())
                    .map(f -> {
                        try {
                            return f.getNow(null);
                        } catch (Exception ex) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

            return results.isEmpty() ? Double.NaN :
                    results.stream().min(Double::compareTo).get();

        } catch (Exception e) {
            return Double.NaN;
        } finally {
            // Отменяем все задачи и shutdown executor
            futures.forEach(f -> f.cancel(true));
            executor.shutdownNow();
        }
    }
}
