package de.eleon.watchcopy;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static de.eleon.watchcopy.Log.ERROR;
import static de.eleon.watchcopy.Log.LOG;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Opens a {link WatchService} and watches a directory with its subdirectories for changes. Detected changes will be
 * processed by {link WatchEventProcessor}. New directories will also be registered within the watchservice.
 */
public class WatchCopy {

    private final WatchService watchService;
    private final WatchEventProcessor watchEventProcessor;
    private ScheduledExecutorService executor;

    private final List<Config> configs;

    private final Map<WatchKey, Config> watchKeyConfigMap = Maps.newHashMap();

    /**
     * Constructor
     *
     * @param configs List of watch configs
     * @throws IOException
     */
    public WatchCopy(List<Config> configs) throws IOException {
        LOG("START: configs %s", Joiner.on(", ").join(configs).toString());
        this.configs = configs;
        this.watchService = FileSystems.getDefault().newWatchService();
        this.watchEventProcessor = new WatchEventProcessor();

        for (Config config : configs) {
            registerAll(config);
        }

    }

    /**
     * Run watch process in an endless thread.
     * @param daemon boolean if new thread should be a daemon
     */
    public void run(final boolean daemon) {
        executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(daemon);
                return t;
            }
        });
        executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                watch();
            }
        }, 0, 1, TimeUnit.MILLISECONDS);
    }

    /**
     * Watch baseFrom for the next change event
     */
    public void watch()  {
        try {
            WatchKey watchKey = watchService.poll(60, TimeUnit.HOURS);
            List<WatchEvent<?>> events = watchKey.pollEvents();
            for (WatchEvent event : events) {
                Config config = watchKeyConfigMap.get(watchKey);
                this.watchEventProcessor.process(watchKey, event, config);

                processNewDirectories(watchKey, event, config);
            }
            if (!watchKey.reset()) {
                LOG("watch key no longer valid");
            }
        } catch (InterruptedException e) {
            ERROR(e, "interrupted");
        }
    }

    private void registerAll(final Config config) throws IOException {
        Files.walkFileTree(config.getFrom(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
            {
                register(dir, config);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void register(Path path, Config config) throws IOException {
        WatchKey watchKey = path.register(
                watchService,
                ENTRY_CREATE,
                ENTRY_DELETE,
                ENTRY_MODIFY
        );
        watchKeyConfigMap.put(watchKey, config);
    }

    private void processNewDirectories(WatchKey watchKey, WatchEvent event, Config config) {
        if (event.kind().equals(ENTRY_CREATE)) {
            Path createdEntry = Paths.get(((Path) watchKey.watchable()).toString() + "/" + ((Path) event.context()).toString());
            if (Files.isDirectory(createdEntry)) {
                try {
                    LOG("register new directory %s", createdEntry);
                    register(createdEntry, config);
                } catch (IOException e) {
                    ERROR("ERROR, cant register new directory %s, %s", createdEntry, e.toString());
                }
            }
        }
    }

    public boolean active() {
        return executor != null && !executor.isTerminated();
    }

    public void stop() {
        if (executor == null) return;
        executor.shutdown();
    }
}