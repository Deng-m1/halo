package com.book.infrastructure.queue;

import com.book.common.event.UploadProgressEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class BookProcessingQueue {
    private final Sinks.Many<ProcessingTask> taskSink = Sinks.many().multicast().onBackpressureBuffer();
    private final ConcurrentHashMap<String, ProcessingTask> taskTracker = new ConcurrentHashMap<>();
    private final Sinks.Many<UploadProgressEvent> progressSink = Sinks.many().multicast().onBackpressureBuffer();

    @PostConstruct
    public void init() {
        taskSink.asFlux()
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .flatMap(this::processTask)
                .subscribe();
    }

    public void submitTask(ProcessingTask task) {
        taskTracker.put(task.getIdentifier(), task);
        taskSink.tryEmitNext(task);
    }

    public Flux<UploadProgressEvent> subscribeToProgress(String identifier) {
        return progressSink.asFlux()
                .filter(event -> event.getIdentifier().equals(identifier));
    }

    private Mono<Void> processTask(ProcessingTask task) {
        return Mono.defer(() -> {
            updateProgress(task, 0, "开始处理");
            
            return task.execute()
                    .doOnNext(progress -> updateProgress(task, progress, "处理中"))
                    .doOnComplete(() -> {
                        updateProgress(task, 100, "处理完成");
                        taskTracker.remove(task.getIdentifier());
                    })
                    .doOnError(error -> {
                        updateProgress(task, -1, "处理失败: " + error.getMessage());
                        if (task.shouldRetry()) {
                            resubmitTask(task);
                        } else {
                            taskTracker.remove(task.getIdentifier());
                        }
                    })
                    .then();
        });
    }

    private void updateProgress(ProcessingTask task, double progress, String message) {
        UploadProgressEvent event = new UploadProgressEvent(task.getIdentifier(), task.getFilename());
        event.setProgress(progress);
        event.setMessage(message);
        event.setStatus(progress >= 100 ? UploadStatus.COMPLETED : 
                       progress < 0 ? UploadStatus.FAILED : 
                       UploadStatus.PROCESSING);
        
        progressSink.tryEmitNext(event);
    }

    private void resubmitTask(ProcessingTask task) {
        if (task.incrementRetryCount()) {
            taskSink.tryEmitNext(task);
        }
    }
} 