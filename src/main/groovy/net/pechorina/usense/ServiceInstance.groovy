package net.pechorina.usense

import groovy.transform.EqualsAndHashCode

import java.time.LocalDateTime

/**
 * Created by victor on 21/05/16.
 */
@EqualsAndHashCode(includes = ['id', 'name'])
class ServiceInstance {
    long id
    LocalDateTime created
    LocalDateTime lastSeen
    String name
    String host
    int port

    String toString() {
        return "${id}:${name}:${host}:${port}";
    }

    static ServiceInstance fromString(String str) throws IllegalArgumentException {
        if (str) {
            List<String> parts = str.tokenize(":")
            if (parts.size() != 4)
                throw new IllegalArgumentException("Can't split string to the parts: " + str)
            long id = parts[0].toLong()
            String name = parts[1]
            String host = parts[2]
            int port = parts[3].toInteger()

            return new ServiceInstance(id: id, name: name, host: host, port: port, created: LocalDateTime.now(), lastSeen: LocalDateTime.now())
        }
        return null;
    }

}
