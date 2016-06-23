package org.dstadler.filesearch;

import com.google.common.base.Preconditions;
import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.filefilter.*;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.dstadler.commons.collections.ConcurrentMappedCounter;
import org.dstadler.commons.collections.MappedCounter;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class FileTypeDirectoryWalker extends DirectoryWalker<Void> {
    private final Tika tika = new Tika();
    private final MappedCounter<String> stats = new ConcurrentMappedCounter<>();
    private final AtomicLong count = new AtomicLong(0);
    private final AtomicLong submitCount = new AtomicLong(0);
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public FileTypeDirectoryWalker() {
        // filter out .svn and .git directories
        super(new NotFileFilter(
                new AndFileFilter(
                        DirectoryFileFilter.INSTANCE,
                        new OrFileFilter(
                            new NameFileFilter(".svn"),
                            new NameFileFilter(".git")
                ))), -1);
        //super(new NameFileFilter("test.xsb"), -1);
    }

    public MappedCounter<String> getStats() {
        return stats;
    }

    @Override
    protected void handleFile(File file, int depth, Collection<Void> results) throws IOException {
        submitCount.incrementAndGet();
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    try (TikaInputStream str = TikaInputStream.get(file.toPath())) {
                        final String mediaType = tika.detect(str, file.getName());

                        // ensure that we do not mix output from different threads
                        synchronized (this) {
                            System.out.println("{ \"fileName\":\"" + file.getAbsolutePath() + "\", \"mediaType\":\"" + mediaType + "\"}");
                        }

                        long curr = count.incrementAndGet();
                        if(curr % 1000 == 0) {
                            System.err.print(".");
                            if(curr % 100000 == 0) {
                                System.err.println();
                            }
                        }

                        stats.addInt(mediaType, 1);
                    }

                } catch (IOException e) {
                    System.err.println("Had exeception while handling file " + file + ": " + e);
                    e.printStackTrace(System.err);
                }
            }
        });
    }

    public long execute(File startDir) throws IOException, InterruptedException {
        Preconditions.checkNotNull(startDir, "Directory needs to be specified");
        Preconditions.checkState(startDir.exists(), "Directory %s needs to exist", startDir);
        Preconditions.checkState(startDir.isDirectory(), "Need to specify a directory, not a file, had %s", startDir);

        walk(startDir, Collections.emptyList());

        // wait for all started tasks to finish
        executor.shutdown();
        System.err.println();
        System.err.println("Waiting for " + submitCount.get() + " files to be processed, " + count.get() + " already finished.");
        while(!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            //throw new IllegalStateException("Could not wait for all threads to finish processing");
            System.err.println("Still waiting for " + (submitCount.get() - count.get()) + " files to be processed.");
        }

        // add a newline as we print out dots to show some progress
        System.err.println();

        return count.get();
    }
}
