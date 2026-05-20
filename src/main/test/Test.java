import org.junit.jupiter.api.Test;
import org.pcMonitor.platform.FrequencyPolicy;
import org.pcMonitor.platform.Platform;

import static org.junit.jupiter.api.Assertions.*;

class JUnitTests {

    @Test
    void addShouldReturnSum() {
//        assertEquals(7, 3+ 4);


        FrequencyPolicy frequencyPolicy = Platform.SINGLETON.getFrequencyPolicy();
       System.out.println(frequencyPolicy);


    }
}