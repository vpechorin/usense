package net.pechorina.usense

import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class RegistryIntegrationTest {

    @Test
    public void resolveTest() {
        Usense u = Usense.newClient("service1", "srv1", 8080, "nats://localhost:4222")

        ServiceInstance instance = u.resolve("service1")

        assertThat(instance.id).isNotNull()
        assertThat(instance.name).isEqualTo("service1")
        assertThat(instance.host).isEqualTo("srv1")
        assertThat(instance.port).isEqualTo(8080)
    }

    @Test
    public void resolveUnknownTest() {
        Usense u = Usense.newClient("service1", "srv1", 8080, "nats://localhost:4222")

        ServiceInstance instance = u.resolve("service2")

        assertThat(instance).isNull()
    }

    @Test
    public void browseTest() {
        Usense u1 = Usense.newClient("service1", "srv1", 8080, "nats://localhost:4222")

        Usense.newClient("service2", "srv2", 8081, "nats://localhost:4222")
        Usense.newClient("service3", "srv3", 8082, "nats://localhost:4222")

        u1.browse()

        Thread.sleep(200)

        ServiceInstance i1 = u1.findService("service1")
        ServiceInstance i2 = u1.findService("service2")

        assertThat(i1.id).isNotNull()
        assertThat(i1.name).isEqualTo("service1")
        assertThat(i1.host).isEqualTo("srv1")
        assertThat(i1.port).isEqualTo(8080)

        assertThat(i2.id).isNotNull()
        assertThat(i2.name).isEqualTo("service2")
        assertThat(i2.host).isEqualTo("srv2")
        assertThat(i2.port).isEqualTo(8081)
    }

    @Test
    public void findDirectTest() {
        Usense u1 = Usense.newClient("service1", "srv1", 8080, "nats://localhost:4222")

        ServiceInstance i1 = u1.findService("service1")

        assertThat(i1.id).isNotNull()
        assertThat(i1.name).isEqualTo("service1")
        assertThat(i1.host).isEqualTo("srv1")
        assertThat(i1.port).isEqualTo(8080)
    }

    @Test
    public void findAllTest() {
        Usense u1 = Usense.newClient("service1", "srv1", 8080, "nats://localhost:4222")

        List<ServiceInstance> result = u1.findAllServiceInstances("service1")

        assertThat(result).extracting("host").containsOnly("srv1")
    }

    @Test
    public void getOwnInstanceTest() {
        Usense u1 = Usense.newClient("service1", "srv1", 8080, "nats://localhost:4222")

        assertThat(u1.getOwnInstance().name).isEqualTo("service1")
    }
}
