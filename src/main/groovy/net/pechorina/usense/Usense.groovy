package net.pechorina.usense

import groovy.util.logging.Slf4j
import io.nats.client.Connection
import io.nats.client.ConnectionFactory
import io.nats.client.Message

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Slf4j
class Usense {
    private long BROWSE_INTERVAL = 30000L

    Long id = Math.round( Math.random() * 1000000D)
    ServiceInstance serviceInstance
    Connection nats
    ServiceRegistry registry

    private Timer browserTimer

    static Usense newClient(String name, String host, int port, String natsUrl) {
        return newClient(name, host, port, getNatsConnection(natsUrl))
    }

    static Usense newClient(String name, String host, int port, Connection natsConnection) {

        ServiceInstance instance = new ServiceInstance(
                id: Math.round( Math.random() * 1000000000000D),
                created: LocalDateTime.now(),
                lastSeen: LocalDateTime.now(),
                name: name,
                host: host,
                port: port)

        ServiceRegistry registry = new ServiceRegistry()
        registry.init()

        Usense usense = new Usense(
                serviceInstance: instance,
                nats: natsConnection,
                registry: registry
        )
        usense.init()

        return usense;
    }

    public static Connection getNatsConnection(String url) {
        ConnectionFactory cf = new ConnectionFactory(url)
        Connection nc = null
        try {
            nc = cf.createConnection();
        } catch (IOException e) {
            log.error("Can't create a NATS connection", e)
        } catch (TimeoutException e) {
            log.error("Timeout connecting to NATS", e)
        }
        return nc
    }

    private void init() {
        log.debug("[u${id}] Init: Subscribe to all known topics")

        nats.subscribe("discovery.resolve", { m ->
            String messageText = new String(m.data)
            log.debug("[u${id}] Received the discovery resolve reply |${messageText}|")
            if (this.serviceInstance &&
                    messageText == this.serviceInstance.name)
                nats.publish(m.replyTo, this.serviceInstance.toString().bytes);
        })

        nats.subscribe("discovery.browse", { m ->
            if (this.serviceInstance)
                nats.publish("discovery.browse.reply", this.serviceInstance.toString().bytes);
        })

        nats.subscribe("discovery.browse.reply", { m ->
            String messageText = new String(m.data)
            log.debug("[u${id}] Received the discovery browse reply |${messageText}|")
            try {
                ServiceInstance serviceInstance = ServiceInstance.fromString(messageText)
                if (serviceInstance)
                    registry.addOrUpdate(serviceInstance)
            }
            catch (IllegalArgumentException ex) {
                log.warn("[u${id}] Received an incorrect message |${messageText}|", ex)
            }
        })

        nats.subscribe("discovery.unregister", { m ->
            String messageText = new String(m.data)
            log.debug("[u${id}] Received the unregister notice for |${messageText}|")
            try {
                ServiceInstance serviceInstance = ServiceInstance.fromString(messageText)
                if (serviceInstance)
                    registry.remove(serviceInstance)

                if (serviceInstance == this.serviceInstance) {
                    this.serviceInstance = null
                }
            }
            catch (IllegalArgumentException e) {
                log.warn("[u${id}] Received an incorrect message |${messageText}|", e)
            }
        })

        scheduleBrowse()

        browse()

        log.debug("[u${id}] Init: completed")
    }

    ServiceInstance resolve(String name) {
        log.debug("[u${id}] New discover attempt: $name")

        long start = System.nanoTime()
        String messageText = null;
        try {
            Message msg = nats.request("discovery.resolve", name.bytes, 2000, TimeUnit.MILLISECONDS);
            if (msg)
                messageText = new String(msg.data)
        }
        catch (TimeoutException e) {
            log.debug("[u${id}] Timeout on discovery $name");
        }
        catch (IOException e) {
            log.warn("[u${id}] Error on discover $name", e)
        }

        String elapsed = String.format("%d us", TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start))
        log.debug("[u${id}] Received reply |${messageText}| in ${elapsed}")
        if (messageText)
            return ServiceInstance.fromString(messageText);

        return null;
    }

    void browse() {
        log.debug("[u${id}] Broadcast discover attempt")

        long start = System.nanoTime()
        try {
            nats.publish("discovery.browse", "*".bytes)
        }
        catch (IOException e) {
            log.warn("[u${id}] Error on broadcast discover", e)
        }
        String elapsed = String.format("%d us", TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start))
        log.debug("[u${id}] Broadcast discover request sent in ${elapsed}")
    }

    ServiceInstance findService(String name) {
        ServiceInstance instance = this.registry.findByName(name)
        if (instance == null) {
            log.debug("[u${id}] ${name} was not found in local registry, try to send direct request")
            instance = this.resolve(name)
            if (instance != null)
                this.registry.add(instance)
        }

        return instance
    }

    List<ServiceInstance> findAllServiceInstances(String name) {
        List<ServiceInstance> result = this.registry.findAll(name)
        if (result == null || result.isEmpty()) {
            log.warn("Nothing found in registry, trying to browse all")
            this.browse()
            Thread.sleep(200)
            result = this.registry.findAll(name)
        }

        return result
    }

    ServiceInstance getOwnInstance() {
        return this.serviceInstance;
    }

    void unregister(ServiceInstance serviceInstance) {
        log.debug("[u${id}] Unregister instance ${serviceInstance.toString()}")
        nats.publish("discovery.unregister", serviceInstance.toString().bytes)
    }

    void unregister() {
        unregister(this.serviceInstance)
    }

    void scheduleBrowse() {
        long initialDelay = Math.round( Math.random() * 30000D)
        log.debug("Define browse scheduler, initial delay: ${initialDelay} ms, interval: ${BROWSE_INTERVAL} ms")
        this.browserTimer = new Timer().schedule({
            browse()
        } as TimerTask, initialDelay, BROWSE_INTERVAL) //magic numbers are initial-delay & repeat-interval
    }


}
