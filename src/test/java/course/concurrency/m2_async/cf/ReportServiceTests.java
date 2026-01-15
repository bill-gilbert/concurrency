package course.concurrency.m2_async.cf;

import course.concurrency.m2_async.loadTest.ReportServiceExecutors;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ReportServiceTests {

    private ReportServiceExecutors reportService = new ReportServiceExecutors();
//    private ReportServiceCF reportService = new ReportServiceCF();
//    private ReportServiceVirtual reportService = new ReportServiceVirtual();

    @Test
    public void testMultipleTasks() throws InterruptedException {
        int poolSize = Runtime.getRuntime().availableProcessors()*3;
        int iterations = 5;

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        for (int i = 0; i < poolSize; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException ignored) {}
                for (int it = 0; it < iterations; it++) {
                    reportService.getReport();
                }
            });
        }

        long start = System.currentTimeMillis();
        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        long end = System.currentTimeMillis();

        System.out.println("Execution time: " + (end - start));
    }
}
/**

 тестирование на методе sleep

 Executors.newSingleThreadPool();
  Execution time: 300002

 Executors.newFixedThreadPool(4);
 Execution time: 4578

 Executors.newFixedThreadPool(8);
 Execution time: 204133

  Executors.newFixedThreadPool(16);
 Execution time: 103549

  Executors.newFixedThreadPool(24);
 Execution time: 69028

  Executors.newFixedThreadPool(32);
 Execution time: 52521

  Executors.newFixedThreadPool(64);
 Execution time: 27014


  Executors.newCachedThreadPool();
 Execution time: 300002

 тестирование на методе compute

 Executors.newSingleThreadPool();
 Execution time: 16984

 Executors.newFixedThreadPool(4);
 Execution time: 4661

 Executors.newFixedThreadPool(8);
 Execution time: 3183

 Executors.newFixedThreadPool(16);
 Execution time: 3036

 Executors.newFixedThreadPool(24);
 Execution time: 2956

 Executors.newFixedThreadPool(32);
 Execution time: 2951

 Executors.newFixedThreadPool(64);
 Execution time: 2936

 Executors.newCachedThreadPool();
 Execution time: 3017


 */