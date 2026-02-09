package coen448.computablefuture.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AsyncProcessor {
	
    public CompletableFuture<String> processAsync(List<Microservice> microservices, String message) {
    	
        List<CompletableFuture<String>> futures = microservices.stream()
            .map(client -> client.retrieveAsync(message))
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.joining(" ")));
        
    }
    
    public CompletableFuture<List<String>> processAsyncCompletionOrder(
            List<Microservice> microservices, String message) {

        List<String> completionOrder =
            Collections.synchronizedList(new ArrayList<>());

        List<CompletableFuture<Void>> futures = microservices.stream()
            .map(ms -> ms.retrieveAsync(message)
                .thenAccept(completionOrder::add))
            .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> completionOrder);
        
    }

    public CompletableFuture<String> processAsyncFailFast(
            List<Microservice> services,
            List<String> messages) {

        List<CompletableFuture<String>> futures = IntStream.range(0, services.size())
            .mapToObj(i -> services.get(i).retrieveAsync(messages.get(i)))
            .collect(Collectors.toList());

        // Fail-fast: any exception causes allOf to complete exceptionally.
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.joining(" ")));

    }

    public CompletableFuture<List<String>> processAsyncFailPartial(
            List<Microservice> services,
            List<String> messages) {

        List<CompletableFuture<String>> futures = IntStream.range(0, services.size())
            .mapToObj(i -> services.get(i).retrieveAsync(messages.get(i))
                .handle((value, ex) -> ex == null ? value : null))
            .collect(Collectors.toList());

        // Fail-partial: collect only successful results; failures are skipped.
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .filter(value -> value != null)
                .collect(Collectors.toList()));

    }
    
}