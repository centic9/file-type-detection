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

public class FileTypeDirectoryWalker extends DirectoryWalker<Void> {
    private final DefaultDetector detector = new DefaultDetector();

    public FileTypeDirectoryWalker() {
        super();
    }

    @Override
    protected void handleFile(File file, int depth, Collection<Void> results) throws IOException {
        try (TikaInputStream str = TikaInputStream.get(file.toPath())) {
            final MediaType mediaType = detector.detect(str, new Metadata());
            System.out.println("{ \"fileName\":\"" + file.getAbsolutePath() + "\", \"mediaType\":\"" + mediaType.toString() + "\"}");
        }
    }

    public void execute(File startDir) throws IOException {
        Preconditions.checkNotNull(startDir, "Directory needs to be specified");
        Preconditions.checkState(startDir.exists(), "Directory %s needs to exist", startDir);
        Preconditions.checkState(startDir.isDirectory(), "Need to specify a directory, not a file, had %s", startDir);

        walk(startDir, Collections.emptyList());
    }
}
