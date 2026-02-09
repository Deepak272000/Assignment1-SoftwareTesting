package coen448.computablefuture.test;


import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class AsyncProcessorTest {
	@RepeatedTest(5)
    public void testProcessAsyncSuccess() throws ExecutionException, InterruptedException, TimeoutException {
        
		Microservice service1 = new Microservice("Hello") {
            @Override
            public CompletableFuture<String> retrieveAsync(String input) {
                return CompletableFuture.completedFuture("Hello");
            }
        };
        Microservice service2 = new Microservice("World") {
            @Override
            public CompletableFuture<String> retrieveAsync(String input) {
                return CompletableFuture.completedFuture("World");
            }
        };

        AsyncProcessor processor = new AsyncProcessor();
        CompletableFuture<String> resultFuture = processor.processAsync(List.of(service1, service2), "msg");
        
		String result = resultFuture.get(1, TimeUnit.SECONDS);
        assertEquals("Hello World", result);
        
//        CompletableFuture<List<String>> resultFuture =
//        	    processor.processAsyncWithCompletionOrder(
//        	        List.of(mockService1, mockService2));

//        	List<String> order = resultFuture.get();
//        	System.out.println(order);

        
    }
	
	
	@ParameterizedTest
    @CsvSource({
        "hi, Hello:HI World:HI",
        "cloud, Hello:CLOUD World:CLOUD",
        "async, Hello:ASYNC World:ASYNC"
    })
    public void testProcessAsync_withDifferentMessages(
            String message,
            String expectedResult)
            throws ExecutionException, InterruptedException, TimeoutException {

        Microservice service1 = new Microservice("Hello") {
            @Override
            public CompletableFuture<String> retrieveAsync(String input) {
                return CompletableFuture.completedFuture("Hello:" + input.toUpperCase());
            }
        };
        Microservice service2 = new Microservice("World") {
            @Override
            public CompletableFuture<String> retrieveAsync(String input) {
                return CompletableFuture.completedFuture("World:" + input.toUpperCase());
            }
        };

        AsyncProcessor processor = new AsyncProcessor();

        CompletableFuture<String> resultFuture =
            processor.processAsync(List.of(service1, service2), message);

        String result = resultFuture.get(1, TimeUnit.SECONDS);

        assertEquals(expectedResult, result);
        
    }
	
	
	@RepeatedTest(20)
    void showNondeterminism_completionOrderVaries() throws Exception {

        Microservice s1 = new Microservice("A") {
            @Override
            public CompletableFuture<String> retrieveAsync(String input) {
                return CompletableFuture.completedFuture(input)
                    .thenApplyAsync(value -> {
                        int delayMs = ThreadLocalRandom.current().nextInt(0, 31);
                        try {
                            TimeUnit.MILLISECONDS.sleep(delayMs);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        }
                        return "A:" + value.toUpperCase();
                    });
            }
        };
        Microservice s2 = new Microservice("B") {
            @Override
            public CompletableFuture<String> retrieveAsync(String input) {
                return CompletableFuture.completedFuture(input)
                    .thenApplyAsync(value -> {
                        int delayMs = ThreadLocalRandom.current().nextInt(0, 31);
                        try {
                            TimeUnit.MILLISECONDS.sleep(delayMs);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        }
                        return "B:" + value.toUpperCase();
                    });
            }
        };
        Microservice s3 = new Microservice("C") {
            @Override
            public CompletableFuture<String> retrieveAsync(String input) {
                return CompletableFuture.completedFuture(input)
                    .thenApplyAsync(value -> {
                        int delayMs = ThreadLocalRandom.current().nextInt(0, 31);
                        try {
                            TimeUnit.MILLISECONDS.sleep(delayMs);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        }
                        return "C:" + value.toUpperCase();
                    });
            }
        };

        AsyncProcessor processor = new AsyncProcessor();

        List<String> order = processor
            .processAsyncCompletionOrder(List.of(s1, s2, s3), "msg")
            .get(1, TimeUnit.SECONDS);

        // Not asserting a fixed order (because it is intentionally nondeterministic)
        System.out.println(order);

        // A minimal sanity check: all three must be present
        assertEquals(3, order.size());
   
        assertTrue(order.stream().anyMatch(x -> x.startsWith("A:")));
        assertTrue(order.stream().anyMatch(x -> x.startsWith("B:")));
        assertTrue(order.stream().anyMatch(x -> x.startsWith("C:")));
    }


    @Test
    void failFast_processAsyncFailFast_throwsOnAnyFailure() throws Exception {
        Microservice okService = new Microservice("OK") {
            @Override
            public CompletableFuture<String> retrieveAsync(String input) {
                return CompletableFuture.completedFuture("OK:" + input.toUpperCase());
            }
        };
        Microservice failingService = new Microservice("FAIL") {
            @Override
            public CompletableFuture<String> retrieveAsync(String input) {
                return CompletableFuture.failedFuture(new RuntimeException("boom"));
            }
        };

        AsyncProcessor processor = new AsyncProcessor();

        CompletableFuture<String> resultFuture = processor.processAsyncFailFast(
            List.of(okService, failingService),
            List.of("msg", "msg"));

        assertThrows(ExecutionException.class, () -> resultFuture.get(1, TimeUnit.SECONDS));
    }


    @Test
    void testProcessAsyncFailPartial_returnsOnlySuccess_noExceptionEscapes() throws Exception {
        Microservice okService1 = new Microservice("S1") {
            @Override
            public CompletableFuture<String> retrieveAsync(String input) {
                return CompletableFuture.completedFuture("S1:" + input);
            }
        };
        Microservice failingService = new Microservice("FAIL") {
            @Override
            public CompletableFuture<String> retrieveAsync(String input) {
                return CompletableFuture.failedFuture(new RuntimeException("boom"));
            }
        };
        Microservice okService2 = new Microservice("S3") {
            @Override
            public CompletableFuture<String> retrieveAsync(String input) {
                return CompletableFuture.completedFuture("S3:" + input);
            }
        };

        AsyncProcessor processor = new AsyncProcessor();

        CompletableFuture<List<String>> resultFuture = processor.processAsyncFailPartial(
            List.of(okService1, failingService, okService2),
            List.of("m1", "m2", "m3"));

        List<String> results = assertDoesNotThrow(() -> resultFuture.get(1, TimeUnit.SECONDS));

        assertEquals(2, results.size());
        assertTrue(results.contains("S1:m1"));
        assertTrue(results.contains("S3:m3"));
    }
}
	