package net.pechorina.usense

import groovy.util.logging.Slf4j

import java.time.LocalDateTime
import java.util.concurrent.CopyOnWriteArrayList

@Slf4j
class ServiceRegistry {

    private long EXPIRE_TIME_SEC = 300L  // 5min
    private long EXPIRE_TIMER_INITIAL_DELAY = 300L  // 5min

    private List<ServiceInstance> registry = new CopyOnWriteArrayList<>();
    private long registryId = Math.round( Math.random() * 100000D)
    private Timer expireTimer

    void init() {
        makeExpireTimer(EXPIRE_TIME_SEC, EXPIRE_TIMER_INITIAL_DELAY)
    }

    boolean checkIfExists(ServiceInstance instance) {
        log.info("[r${registryId}] check |${instance.toString()}|")
        return registry.contains(instance)
    }

    boolean add(ServiceInstance instance) {
        log.debug("[r${registryId}] add |i${instance.id}|")
        if (!registry.contains(instance))
            return registry.add(instance)
        return false
    }

    boolean addOrUpdate(ServiceInstance instance) {
        log.debug("[r${registryId}] addOrUpdate |i${instance.id}|")
        if (registry.contains(instance)) {
            ServiceInstance existingInstance = registry.find { it.id == instance.id}
            existingInstance.lastSeen = LocalDateTime.now()
        }
        else {
            return registry.add(instance)
        }

        return false
    }

    boolean remove(ServiceInstance instance) {
        log.info("[r${registryId}] Remove instance from registry: |${instance.toString()}|")
        return registry.remove(instance)
    }

    ServiceInstance findByName(String name) {
        log.debug("[r${registryId}] find by name |${name}|")
        registry.find { it.name == name }
    }

    List<ServiceInstance> findAll(String name) {
        log.debug("[r${registryId}] find all by name |${name}|")
        registry.findAll { it.name == name }
    }

    List<ServiceInstance> findAll() {
        log.debug("[r${registryId}] find all")
        registry.findAll()
    }

    void makeExpireTimer(long expireTimeSec, long initialDelay) {
        log.debug("Define expire scheduler, expire instances after: ${expireTimeSec} sec")
        this.expireTimer = new Timer().schedule({
            checkAndExpireInstances(expireTimeSec)
        } as TimerTask, initialDelay, expireTimeSec * 1000L) //magic numbers are initial-delay & repeat-interval
    }

    void checkAndExpireInstances(long expireTimeSec) {
        LocalDateTime cutOffTime = LocalDateTime.now().minusSeconds( expireTimeSec )
        log.debug("checkAndExpireInstances, cut off time: ${cutOffTime.toString()}")
        registry.findAll()
                .findAll { it.lastSeen.isBefore(cutOffTime) }
                .each {
            log.debug("Found expired |i${it.id}| service instance")
            remove(it)
        }
    }
}
