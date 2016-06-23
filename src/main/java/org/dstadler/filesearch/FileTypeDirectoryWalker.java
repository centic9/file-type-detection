package org.dstadler.filesearch;

import com.google.common.base.Preconditions;
import org.apache.commons.io.DirectoryWalker;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FileTypeDirectoryWalker extends DirectoryWalker<Void> {
    private final DefaultDetector detector = new DefaultDetector();
    private long count = 0;

    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public FileTypeDirectoryWalker() {
        super();
    }

    @Override
    protected void handleFile(File file, int depth, Collection<Void> results) throws IOException {
        count++;
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    try (TikaInputStream str = TikaInputStream.get(file.toPath())) {
                        final MediaType mediaType = detector.detect(str, new Metadata());

                        // ensure that we do not mix output from different threads
                        synchronized (this) {
                            System.out.println("{ \"fileName\":\"" + file.getAbsolutePath() + "\", \"mediaType\":\"" + mediaType.toString() + "\"}");
                        }
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

        executor.shutdown();
        if(!executor.awaitTermination(1, TimeUnit.MINUTES)) {
            throw new IllegalStateException("Could not wait for all threads to finish processing");
        }

        return count;
    }
}
