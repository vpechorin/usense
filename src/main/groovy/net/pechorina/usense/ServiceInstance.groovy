package net.pechorina.usense
/**
 * Created by victor on 21/05/16.
 */
class ServiceInstance {
    String id = UUID.randomUUID().toString()
    String name
    String host
    int port

    String toString() {
        return "${id}:${name}:${host}:${port}";
    }

    static ServiceInstance fromString(String str) {
        if (str) {
            List<String> parts = str.tokenize(":")
            String id = parts[0]
            String name = parts[1]
            String host = parts[2]
            int port = parts[3].toInteger()

            return new ServiceInstance(id: id, name: name, host: host, port: port)
        }
        return null;
    }
}
