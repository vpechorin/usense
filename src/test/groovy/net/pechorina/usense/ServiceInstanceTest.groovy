package net.pechorina.usense

import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class ServiceInstanceTest {

    @Test
    void fromStringTest() {
        ServiceInstance i = ServiceInstance.fromString("123:t1:localhost:8080");
        assertThat(i.id).isEqualTo(123);
        assertThat(i.name).isEqualTo("t1");
        assertThat(i.host).isEqualTo("localhost");
        assertThat(i.port).isEqualTo(8080);
    }

    @Test
    void fromNullStringTest() {
        assertThat(ServiceInstance.fromString(null)).isNull()
    }

    @Test
    void fromShortStringTest() {
        try {
            ServiceInstance.fromString("123:t1:localhost")
        }
        catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalArgumentException.class)
            assertThat(e).hasMessageContaining("Can't split string to the parts")
        }
    }

    @Test
    void fromEmtptyStringTest() {
        try {
            ServiceInstance.fromString("")
        }
        catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalArgumentException.class)
            assertThat(e).hasMessageContaining("Can't split string to the parts")
        }
    }

    @Test
    void toStringTest() {
        String s = new ServiceInstance(123, "testX", "127.0.0.8", 1919).toString()
        assertThat(s).isEqualTo("123:testX:127.0.0.8:1919")
    }

    @Test
    void toStringNullTest() {
        String s = new ServiceInstance("testX", "127.0.0.8").toString()
        assertThat(s).isEqualTo("0:testX:127.0.0.8:0")
    }

    @Test
    void equalsTest() {
        ServiceInstance i1 = new ServiceInstance(1L, "testX1", "127.0.0.8", 2020)
        ServiceInstance i2 = new ServiceInstance(1L, "testX1", "127.0.0.8")

        assertThat(i1).isEqualTo(i2)
    }

    @Test
    void notEqualsTest() {
        ServiceInstance i1 = new ServiceInstance(1L, "testX1", "127.0.0.8")
        ServiceInstance i2 = new ServiceInstance(2L, "testX1", "127.0.0.8")

        assertThat(i1).isNotEqualTo(i2)
    }

}
