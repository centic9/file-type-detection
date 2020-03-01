package org.dstadler.filesearch;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.filefilter.*;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.dstadler.commons.collections.ConcurrentMappedCounter;
import org.dstadler.commons.collections.MappedCounter;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A directory walker which recursively visits all files in the given
 * directory and submits jobs via an {@link ExecutorService} to let
 * Apache Tika detect the mime-type of the file.
 */
public class FileTypeDirectoryWalker extends DirectoryWalker<Void> {
    private static final int NUMBER_OF_THREADS = 8;

    private final Tika tika = new Tika();

    private final MappedCounter<String> stats = new ConcurrentMappedCounter<>();

    private final AtomicLong count = new AtomicLong();
    private final AtomicLong submitCount = new AtomicLong();
    private final AtomicLong errorCount = new AtomicLong();

    private final ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public FileTypeDirectoryWalker() {
        // filter out .svn and .git directories
        super(new NotFileFilter(
                new AndFileFilter(
                        DirectoryFileFilter.INSTANCE,
                        new OrFileFilter(
                            new NameFileFilter(".svn"),
                            new NameFileFilter(".git")
                ))), -1);
    }

    @Override
    protected void handleFile(File file, int depth, Collection<Void> results) throws IOException {
        // count the file and possibly delay to not submit all files immediately which
        // would blow up memory-usage of the executor queue
        countSubmitted();

        // submit a job to detect the mime-type of this file
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    try (TikaInputStream str = TikaInputStream.get(file.toPath())) {
                        final String mediaType = tika.detect(str, file.getName());

                        // we found a mime-type, print out the JSON for it
                        recordMediaType(mediaType);
                    }
                } catch (FileSystemException e) {
                    // report some types of exception without stacktrace
                    System.err.println("Had exception while handling file " + file + ": " + e);
                    errorCount.incrementAndGet();
                } catch (IOException e) {
                    System.err.println("Had exception while handling file " + file + ": " + e);
                    e.printStackTrace(System.err);
                    errorCount.incrementAndGet();
                } finally {
                    long curr = count.incrementAndGet();
                    if (curr % 1000 == 0) {
                        System.err.print(".");
                        if (curr % 100000 == 0) {
                            System.err.println(submitCount.get() + "/" + curr);
                        }
                    }
                }
            }

            private void recordMediaType(String mediaType) {
                JsonObject json = new JsonObject();
                json.addProperty("fileName", file.getAbsolutePath());
                json.addProperty("mediaType", mediaType);

                // synchronize to ensure that we do not mix output from different threads
                synchronized (this) {
                    //System.out.println("{ \"fileName\":\"" + file.getAbsolutePath() + "\", \"mediaType\":\"" + mediaType + "\"}");
                    System.out.println(json);
                }

                stats.addInt(mediaType, 1);
            }
        });
    }

    private void countSubmitted() throws IOException {
        long submitted = submitCount.incrementAndGet();
        if(submitted - count.get() > 2000) {
            // if there are more than 2000, wait until we are below 1000 again to not fill up memory with all the submitted tasks
            while (submitted - count.get() > 1000) {
                System.err.println("Delaying submitting a bit to not build up too many submitted jobs, having " + submitted + " overall, " + count.get() +
                        " done, " + errorCount.get() + " errors, " + (submitted - count.get()) +
                        " currently queued, waiting for this number to be below 1000 before adding new jobs.");
                try {
                    Thread.sleep(1000*60);
                } catch (InterruptedException e) {
                    throw new IOException(e);
                }
            }
        }
    }

    /**
     * Starts walking the given directory and invokes Tika for
     * every file that is found.
     *
     * @param startDir The directory to walk recursively
     * @throws IOException If reading directories or files cauess a problem
     * @throws InterruptedException If execution is interrupted
     * @return Statistics about which mime-types was found how often
     */
    public MappedCounter<String> execute(File startDir) throws IOException, InterruptedException {
        Preconditions.checkNotNull(startDir, "Directory needs to be specified");
        Preconditions.checkState(startDir.exists(), "Directory %s needs to exist", startDir);
        Preconditions.checkState(startDir.isDirectory(), "Need to specify a directory, not a file, had %s", startDir);

        long start = System.currentTimeMillis();
        System.err.println("Processing directory " + startDir);

        walk(startDir, Collections.emptyList());

        // wait for all started tasks to finish
        executor.shutdown();
        System.err.println();
        System.err.println("Waiting for " + submitCount.get() + " files to finish processing, " +
                count.get() + " already finished, " + errorCount.get() + " failed until now.");
        while(!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            //throw new IllegalStateException("Could not wait for all threads to finish processing");
            System.err.println("Still waiting for " + (submitCount.get() - count.get()) + " files to be processed, " +
                    count.get() + " already finished, " + errorCount.get() + " failed until now.");
        }

        // add a newline as we print out dots to show some progress
        System.err.println();

        System.err.println("Found " + count + " files in directory '" + startDir + "', could not read " + errorCount.get() + " files, took " + (System.currentTimeMillis() - start) + "ms");
        System.err.println("Had stats: " + stats.sortedMap());

        return stats;
    }

    public long getErrorCount() {
        return errorCount.get();
    }
}
