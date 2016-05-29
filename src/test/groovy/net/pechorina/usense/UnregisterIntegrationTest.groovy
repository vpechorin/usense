package net.pechorina.usense

import org.junit.Ignore
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

/**
 * Created by victor on 22/05/16.
 */
@Ignore("unstable")
class UnregisterIntegrationTest {

    @Test
    public void unregisterTest() {
        Usense u1 = Usense.newClient("service1X", "srv3X", 8081, "nats://localhost:4222")
        Usense u2 = Usense.newClient("service11X", "srv11X", 8083, "nats://localhost:4222")

        Thread.sleep(200)

        assertThat(u2.registry.findAll()).hasSize(2)
        assertThat(u1.registry.findAll()).hasSize(2)

        u1.unregister(u1.getOwnInstance())

        Thread.sleep(200)

        assertThat(u2.getRegistry().findAll()).hasSize(1)
        assertThat(u1.getRegistry().findAll()).hasSize(1)

        ServiceInstance i = u1.findService("service1X")

        assertThat(i).isNull()
    }

    @Test
    public void unregisterTest2() {
        Usense u1 = Usense.newClient("service2X", "srv2X", 8082, "nats://localhost:4222")

        Thread.sleep(200)

        u1.unregister()

        Thread.sleep(200)

        ServiceInstance i = u1.findService("service2X")

        assertThat(i).isNull()
    }
}
