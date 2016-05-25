package net.pechorina.usense

import groovy.util.logging.Slf4j
import io.nats.client.Connection
import io.nats.client.ConnectionFactory
import io.nats.client.Message

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Slf4j
class Usense {

    ServiceInstance serviceInstance
    Connection nats

    static Usense newClient(String name, String host, int port, String natsUrl) {
        return newClient(name, host, port, getNatsConnection(natsUrl));
    }

    static Usense newClient(String name, String host, int port, Connection natsConnection) {

        ServiceInstance instance = new ServiceInstance(name: name, host: host, port: port)

        Usense usense = new Usense(
                serviceInstance: instance,
                nats: natsConnection,
        )
        usense.init();

        return usense;
    }

    public static Connection getNatsConnection(String url) {
        ConnectionFactory cf = new ConnectionFactory(url);
        Connection nc = null;
        try {
            nc = cf.createConnection();
        } catch (IOException e) {
            log.error("Can't create NATS connection", e);
        } catch (TimeoutException e) {
            log.error("Timeout connecting to NATS", e);
        }
        return nc;
    }

    private void init() {
        nats.subscribe("discovery.resolve", { m ->
            String messageText = new String(m.data)
            log.debug("Received discovery resolve reply ${messageText}")
            if (messageText == this.serviceInstance.name) nats.publish(m.replyTo, this.serviceInstance.toString().bytes);
        })

        nats.subscribe("discovery.browse", { m ->
            nats.publish("discovery.browse.reply", this.serviceInstance.toString().bytes);
        })

        nats.subscribe("discovery.browse.reply", { m ->
            String messageText = new String(m.data)
            log.debug("Received discovery browse reply ${messageText}")
            ServiceInstance serviceInstance = ServiceInstance.fromString(messageText)
        })
    }

    public ServiceInstance resolve(String name) {
        log.debug("New discover attempt: $name")

        long start = System.nanoTime();
        String messageText = null;
        try {
            Message msg = nats.request("discovery.resolve", name.bytes, 2000, TimeUnit.MILLISECONDS);
            if (msg)
                messageText = new String(msg.data)
        }
        catch (TimeoutException e) {
            log.debug("Timeout on discovery $name");
        }
        catch (IOException e) {
            log.warn("Error on discover $name", e)
        }

        String elapsed = String.format("%d us", TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start))
        log.debug("Received reply [${messageText}] in ${elapsed}")
        if (messageText)
            return ServiceInstance.fromString(messageText);

        return null;
    }

    public void browse() {
        log.debug("Broadcast discover attempt")

        long start = System.nanoTime();
        String messageText = null;
        try {
            nats.publish("discovery.browse", "*".bytes);
        }
        catch (IOException e) {
            log.warn("Error on broadcast discover", e)
        }
        String elapsed = String.format("%d us", TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start))
        log.debug("Broadcast discover request sent in ${elapsed}")
    }
}
