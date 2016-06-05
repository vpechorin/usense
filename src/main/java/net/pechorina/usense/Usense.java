package net.pechorina.usense;

import io.nats.client.Connection;
import io.nats.client.ConnectionFactory;
import io.nats.client.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Usense {
    private static final Logger log = LoggerFactory.getLogger(Usense.class);

    private long BROWSE_INTERVAL = 30000L;

    private Long id = Math.round(Math.random() * 1000000D);
    private ServiceInstance serviceInstance;
    private Connection nats;
    private ServiceRegistry registry;

    private final ScheduledExecutorService browseScheduler = Executors.newScheduledThreadPool(1);

    public Usense(ServiceInstance serviceInstance, Connection nats, ServiceRegistry registry) {
        this.serviceInstance = serviceInstance;
        this.nats = nats;
        this.registry = registry;
    }

    public static Usense newClient(String name, String host, int port, String natsUrl) {
        return newClient(name, host, port, getNatsConnection(natsUrl));
    }

    public static Usense newClient(String name, String host, int port, Connection natsConnection) {

        ServiceInstance instance = new ServiceInstance(
                Math.round(Math.random() * 1000000000000D),
                name,
                host,
                port);

        ServiceRegistry registry = new ServiceRegistry();
        registry.init();

        Usense usense = new Usense(
                instance,
                natsConnection,
                registry
        );
        usense.init();

        return usense;
    }

    public static Connection getNatsConnection(String url) {
        ConnectionFactory cf = new ConnectionFactory(url);
        Connection nc = null;
        try {
            nc = cf.createConnection();
        } catch (IOException e) {
            log.error("Can't create a NATS connection", e);
        } catch (TimeoutException e) {
            log.error("Timeout connecting to NATS", e);
        }
        return nc;
    }

    private void init() {
        log.debug("[u{} Init: Subscribe to all known topics", id);

        nats.subscribe("discovery.resolve", m -> {
            String messageText = new String(m.getData());
            log.debug("[u{}] Received the discovery resolve reply |{}|", id, messageText);
            if (this.serviceInstance != null &&
                    messageText.equalsIgnoreCase(this.serviceInstance.getName()))
                try {
                    nats.publish(m.getReplyTo(), this.serviceInstance.toString().getBytes());
                } catch (IOException e) {
                    log.warn("NATS publish error on usense init", e);
                }
        });

        nats.subscribe("discovery.browse", m -> {
            if (this.serviceInstance != null)
                try {
                    nats.publish("discovery.browse.reply", this.serviceInstance.toString().getBytes());
                } catch (IOException e) {
                    log.warn("NATS publish error on usense init", e);
                }
        });

        nats.subscribe("discovery.browse.reply", m -> {
            String messageText = new String(m.getData());
            log.debug("[u{}] Received the discovery browse reply |{}|", id, messageText);
            try {
                ServiceInstance serviceInstance = ServiceInstance.fromString(messageText);
                if (serviceInstance != null)
                    registry.addOrUpdate(serviceInstance);
            } catch (IllegalArgumentException ex) {
                log.warn("[u" + id + "] Received an incorrect message |" + messageText + "|", ex);
            }
        });

        nats.subscribe("discovery.unregister", m -> {
            String messageText = new String(m.getData());
            log.debug("[u{}] Received the unregister notice for |{}|", id, messageText);
            try {
                ServiceInstance serviceInstance = ServiceInstance.fromString(messageText);
                if (serviceInstance != null)
                    registry.remove(serviceInstance);

                if (serviceInstance != null && serviceInstance.equals(this.serviceInstance)) {
                    this.serviceInstance = null;
                }
            } catch (IllegalArgumentException ex) {
                log.warn("[u" + id + "] Received an incorrect message |" + messageText + "|", ex);
            }
        });

        scheduleBrowse();

        browse();

        log.debug("[u{}] Init: completed", id);
    }

    public ServiceInstance resolve(String name) {
        log.debug("[u{}] New discover attempt: {}", id, name);

        long start = System.nanoTime();
        String messageText = null;
        try {
            Message msg = nats.request("discovery.resolve", name.getBytes(), 2000, TimeUnit.MILLISECONDS);
            if (msg != null)
                messageText = new String(msg.getData());
        } catch (TimeoutException e) {
            log.debug("[u{}] Timeout on discovery {}", id, name);
        } catch (IOException e) {
            log.warn("[u" + id + "] Error on discover " + name, e);
        }

        String elapsed = String.format("%d us", TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start));
        log.debug("[u{}] Received reply |{}| in {}", id, messageText, elapsed);
        if (messageText != null)
            return ServiceInstance.fromString(messageText);

        return null;
    }

    public void browse() {
        log.debug("[u{}] Broadcast discover attempt", id);

        long start = System.nanoTime();
        try {
            nats.publish("discovery.browse", "*".getBytes());
        } catch (IOException e) {
            log.warn("[u" + id + "] Error on broadcast discover", e);
        }
        String elapsed = String.format("%d us", TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start));
        log.debug("[u{}] Broadcast discover request sent in {}", id, elapsed);
    }

    public ServiceInstance findService(String name) {
        ServiceInstance instance = this.registry.findByName(name);
        if (instance == null) {
            log.debug("[u{}] {} was not found in local registry, try to send direct request", id, name);
            instance = this.resolve(name);
            if (instance != null)
                this.registry.add(instance);
        }

        return instance;
    }

    public List<ServiceInstance> findAllServiceInstances(String name) {
        List<ServiceInstance> result = this.registry.findAll(name);
        if (result == null || result.isEmpty()) {
            log.warn("Nothing found in registry, trying to browse all");
            this.browse();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                log.warn("Sleep error", e);
            }
            result = this.registry.findAll(name);
        }

        return result;
    }

    public ServiceInstance getOwnInstance() {
        return this.serviceInstance;
    }

    public void unregister(ServiceInstance serviceInstance) {
        log.debug("[u{}] Unregister instance {}", id, serviceInstance);
        try {
            nats.publish("discovery.unregister", serviceInstance.toString().getBytes());
        } catch (IOException e) {
            log.warn("NATS publish error on unregister", e);
        }
    }

    public void unregister() {
        unregister(this.serviceInstance);
    }

    private void scheduleBrowse() {
        long initialDelay = Math.round(Math.random() * 30000D);
        log.debug("Start browse scheduler, initial delay: {}ms, interval: {}ms", initialDelay, BROWSE_INTERVAL);

        browseScheduler.scheduleAtFixedRate(() -> browse(), initialDelay, BROWSE_INTERVAL, TimeUnit.MILLISECONDS);
    }
}
