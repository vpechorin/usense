package net.pechorina.usense;

import java.time.LocalDateTime;

public class ServiceInstance {
    private long id;
    private final LocalDateTime created;
    private LocalDateTime lastSeen;
    private final String name;
    private final String host;
    private int port;

    public ServiceInstance(long id, String name, String host, int port) {
        this.id = id;
        this.name = name;
        this.host = host;
        this.port = port;
        this.created = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
    }

    public ServiceInstance(long id, String name, String host) {
        this.id = id;
        this.name = name;
        this.host = host;
        this.created = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
    }

    public ServiceInstance(String name, String host) {
        this.name = name;
        this.host = host;
        this.created = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
    }

    public String toString() {
        return String.valueOf(id) +
                ":" +
                name +
                ":" +
                host +
                ":" +
                port;
    }

    public static ServiceInstance fromString(String str) throws IllegalArgumentException {
        if (str != null && !str.isEmpty()) {
            String[] parts = str.split(":");
            if (parts.length != 4)
                throw new IllegalArgumentException("Can't split string to the parts: " + str);

            long id = Long.parseLong(parts[0]);
            String name = parts[1];
            String host = parts[2];
            int port = Integer.parseInt(parts[3]);

            return new ServiceInstance(id, name, host, port);
        }
        return null;
    }

    public long getId() {
        return id;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceInstance that = (ServiceInstance) o;

        if (id != that.id) return false;
        return name != null ? name.equals(that.name) : that.name == null;

    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
