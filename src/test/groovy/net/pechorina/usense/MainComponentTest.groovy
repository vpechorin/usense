package net.pechorina.usense

import org.junit.Test

/**
 * Created by victor on 22/05/16.
 */
class MainComponentTest {

    @Test
    public void discoverTest() {
        Usense u = Usense.newClient("service1", "localhost", 4222);
        int i = 0;
        while (i < 100) {
            Thread.sleep(1000);
            i++;
        }
    }
}
