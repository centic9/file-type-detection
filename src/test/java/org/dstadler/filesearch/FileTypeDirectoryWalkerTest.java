package org.dstadler.filesearch;

import org.dstadler.commons.collections.MappedCounter;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class FileTypeDirectoryWalkerTest {
    @Test
    public void testLocalFiles() throws IOException, InterruptedException {
        FileTypeDirectoryWalker walker = new FileTypeDirectoryWalker();
        File startDir = new File(".");
        MappedCounter<String> stats = walker.execute(startDir);

        assertTrue("Had: " + startDir.getAbsolutePath() + " and " + stats.sortedMap().keySet() + " and " + stats.sortedMap(),
                stats.sortedMap().keySet().size() >= 10);
        assertTrue("Had: " + startDir.getAbsolutePath() + " and " + stats.sortedMap().keySet() + " and " + stats.sortedMap(),
                stats.sortedMap().values().stream().mapToLong(value -> value).sum() > 40);
        assertTrue("Had: " + startDir.getAbsolutePath() + " and " + stats.sortedMap().keySet() + " and " + stats.sortedMap(),
                stats.get("text/x-java-source") >= 3);

        assertEquals("The invalid symbolic link should lead to an error",
                1, walker.getErrorCount());
    }

    @Test
    public void testInvalidDirectory() {
        FileTypeDirectoryWalker walker = new FileTypeDirectoryWalker();
        File startDir = new File("./notexisting");
        assertThrows(IllegalStateException.class, () -> walker.execute(startDir));
    }
}
