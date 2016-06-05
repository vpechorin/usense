package net.pechorina.usense;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ServiceRegistry {
    private static final Logger log = LoggerFactory.getLogger(ServiceRegistry.class);

    private long EXPIRE_TIME_SEC = 300L;  // 5min
    private long EXPIRE_TIMER_INITIAL_DELAY = 300L;  // 5min

    private List<ServiceInstance> registry = new CopyOnWriteArrayList<>();
    private long registryId = Math.round(Math.random() * 100000D);

    private final ScheduledExecutorService expireScheduler = Executors.newScheduledThreadPool(1);


    void init() {
        makeExpireTimer(EXPIRE_TIME_SEC, EXPIRE_TIMER_INITIAL_DELAY);
    }

    public boolean checkIfExists(ServiceInstance instance) {
        log.info("[r{}] check |{}|", registryId, instance);
        return registry.contains(instance);
    }

    public boolean add(ServiceInstance instance) {
        log.debug("[r{}] add |i{}|", registryId, instance.getId());
        if (!registry.contains(instance))
            return registry.add(instance);
        return false;
    }

    public boolean addOrUpdate(ServiceInstance instance) {
        log.debug("[r{}] addOrUpdate |i{}|", registryId, instance.getId());
        if (registry.contains(instance)) {
            ServiceInstance existingInstance = registry.stream().filter(it -> it.getId() == instance.getId()).findFirst().orElse(null);
            existingInstance.setLastSeen(LocalDateTime.now());
        } else {
            return registry.add(instance);
        }

        return false;
    }

    public boolean remove(ServiceInstance instance) {
        log.info("[r{}] Remove instance from registry: |{}|", registryId, instance);
        return registry.remove(instance);
    }

    public ServiceInstance findByName(String name) {
        log.debug("[r{}] find by name |{}|", registryId, name);
        return registry.stream()
                .filter(it -> it.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public List<ServiceInstance> findAll(String name) {
        log.debug("[r{}] find all by name |{}|", registryId, name);
        return registry.stream()
                .filter(it -> it.getName().equalsIgnoreCase(name))
                .collect(Collectors.toList());
    }

    public List<ServiceInstance> findAll() {
        log.debug("[r{}] find all", registryId);
        return registry.stream().collect(Collectors.toList());
    }

    private void makeExpireTimer(long expireTimeSec, long initialDelay) {
        log.debug("Define expire scheduler, expire instances after: {} sec", expireTimeSec);
        expireScheduler.scheduleAtFixedRate(() -> checkAndExpireInstances(expireTimeSec), initialDelay, expireTimeSec * 1000L, TimeUnit.MILLISECONDS);
    }

    private void checkAndExpireInstances(long expireTimeSec) {
        LocalDateTime cutOffTime = LocalDateTime.now().minusSeconds(expireTimeSec);
        log.debug("checkAndExpireInstances, cut off time: {}", cutOffTime);
        this.findAll()
                .stream()
                .filter(it -> it.getLastSeen().isBefore(cutOffTime))
                .forEach(it -> {
                    log.debug("Found expired |i{}| service instance", it.getId());
                    remove(it);
                });
    }
}
