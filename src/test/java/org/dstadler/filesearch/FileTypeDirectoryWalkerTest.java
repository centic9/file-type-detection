package org.dstadler.filesearch;

import org.dstadler.commons.collections.MappedCounter;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FileTypeDirectoryWalkerTest {
    @Test
    public void testLocalFiles() throws IOException, InterruptedException {
        FileTypeDirectoryWalker walker = new FileTypeDirectoryWalker();
        MappedCounter<String> stats = walker.execute(new File("."));

        assertTrue(stats.sortedMap().keySet().size() > 10);
        assertTrue(stats.sortedMap().values().stream().mapToInt(value -> value).sum() > 100);
        assertTrue(stats.get("text/x-java-source") >= 3);

        assertEquals("The invalid symbolic link should lead to an error",
                1, walker.getErrorCount());
    }
}
