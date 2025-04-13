package sleep.manager.java;

import org.junit.Test;
import static org.junit.Assert.*;

import com.tatayless.sleepmanager.SleepManager;

public class LibraryTest {
    @Test
    public void someLibraryMethodReturnsTrue() {
        SleepManager classUnderTest = new SleepManager();
        assertTrue("someLibraryMethod should return 'true'", classUnderTest.someLibraryMethod());
    }
}