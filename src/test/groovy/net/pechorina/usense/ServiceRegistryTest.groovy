package net.pechorina.usense

import org.junit.Test

import java.time.LocalDateTime

import static org.assertj.core.api.Assertions.assertThat

class ServiceRegistryTest {

    String s1 = "123:test1:localhost:11111"
    String s11 = "124:test1:localhost:11110"
    String s2 = "2:test2:localhost:11112"
    String s3 = "3:test3:localhost:11113"

    @Test
    void registryAddTest() {
        ServiceRegistry registry = new ServiceRegistry();
        ServiceInstance i1 = ServiceInstance.fromString(s1)
        registry.add(i1);

        ServiceInstance instance = registry.findByName("test1");
        assertThat(instance.id).isEqualTo(i1.id)
        assertThat(instance.name).isEqualTo(i1.name)
        assertThat(instance.host).isEqualTo(i1.host)
        assertThat(instance.port).isEqualTo(i1.port)
    }

    @Test
    void registryAdd2Test() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.add(ServiceInstance.fromString(s1));

        assertThat(registry.findAll()).containsOnly(ServiceInstance.fromString(s1))
    }

    @Test
    void multipleAddTest() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.add(ServiceInstance.fromString(s1));
        registry.add(ServiceInstance.fromString(s2));
        registry.add(ServiceInstance.fromString(s3));

        assertThat(registry.findAll()).extracting("id").containsOnly(123L, 2L, 3L)
    }

    @Test
    void removeTest() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.add(ServiceInstance.fromString(s1));
        registry.add(ServiceInstance.fromString(s2));
        registry.add(ServiceInstance.fromString(s3));

        registry.remove(ServiceInstance.fromString(s2));

        assertThat(registry.findAll()).extracting("id").containsOnly(123L, 3L)
    }

    @Test
    void removeTest2() {
        String iStr = "908065607147:service1:srv1:8080"
        ServiceRegistry registry = new ServiceRegistry();
        registry.add(ServiceInstance.fromString(iStr));

        registry.remove(ServiceInstance.fromString(iStr));

        assertThat(registry.findAll()).hasSize(0)
    }

    @Test
    void findAllByNameTest() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.add(ServiceInstance.fromString(s1));
        registry.add(ServiceInstance.fromString(s11));
        registry.add(ServiceInstance.fromString(s2));
        registry.add(ServiceInstance.fromString(s3));

        assertThat(registry.findAll("test1")).extracting("id").containsOnly(123L, 124L)
    }

    @Test
    void addOrUpdateTest() {

        ServiceInstance i1 = ServiceInstance.fromString(s1)
        ServiceInstance i2 = ServiceInstance.fromString(s1)
        i2.lastSeen = LocalDateTime.from(i1.lastSeen)

        ServiceRegistry registry = new ServiceRegistry()
        registry.addOrUpdate(i2)

        assertThat(registry.findByName(i1.name).lastSeen).isEqualTo(i2.lastSeen)

        registry.addOrUpdate(i1)
        ServiceInstance i = registry.findByName(i1.name)
        assertThat(i.lastSeen).isAfter(i1.lastSeen)
    }

    @Test
    void checkIfExistsTest() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.add(ServiceInstance.fromString(s1));
        registry.add(ServiceInstance.fromString(s11));
        registry.add(ServiceInstance.fromString(s2));
        registry.add(ServiceInstance.fromString(s3));

        assertThat(registry.checkIfExists(ServiceInstance.fromString(s11))).isTrue()
    }

    @Test
    void checkIfExistsTestExpectingFalse() {
        ServiceRegistry registry = new ServiceRegistry();

        assertThat(registry.checkIfExists(ServiceInstance.fromString(s11))).isFalse()
    }

    @Test
    void expireTest() {
        String iStr = "908065607158:serviceasdca:srv1:8089"
        ServiceRegistry registry = new ServiceRegistry();
        registry.add(ServiceInstance.fromString(iStr));

        registry.makeExpireTimer(1, 0)

        Thread.sleep(2200)

        assertThat(registry.checkIfExists(ServiceInstance.fromString(iStr))).isFalse()
    }
}
