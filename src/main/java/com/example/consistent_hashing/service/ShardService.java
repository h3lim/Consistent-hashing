package com.example.consistent_hashing.service;

import com.example.consistent_hashing.config.ShardDataSourceProperties;
import com.example.consistent_hashing.config.ShardRoutingDataSource;
import com.example.consistent_hashing.dto.MigrationStatistics;
import com.example.consistent_hashing.dto.ShardDetail;
import com.example.consistent_hashing.entity.User;
import com.example.consistent_hashing.exception.ShardException;
import com.example.consistent_hashing.exception.UserMigrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ShardService {

    private static final Logger logger = LoggerFactory.getLogger(ShardService.class);
    private final ConsistentHashingService consistentHashingService;
    private final EntityManagerFactory entityManagerFactory;
    private final ShardDataSourceProperties shardDataSourceProperties;
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<Integer, AtomicInteger> shardDistribution;
    private boolean isNewShardAdded = false;
    private final List<MigrationStatistics> migrationStats = new ArrayList<>();
    private final Map<Integer, Integer> migratingData = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> newAssignedData = new ConcurrentHashMap<>();


    public ShardService(ConsistentHashingService consistentHashingService,
                        EntityManagerFactory entityManagerFactory,
                        ShardDataSourceProperties shardDataSourceProperties,
                        SimpMessagingTemplate messagingTemplate) {
        this.consistentHashingService = consistentHashingService;
        this.entityManagerFactory = entityManagerFactory;
        this.shardDataSourceProperties = shardDataSourceProperties;
        this.messagingTemplate = messagingTemplate;
        this.shardDistribution = new HashMap<>();
        for (int i = 0; i < shardDataSourceProperties.getShards().size(); i++) {
            shardDistribution.put(i, new AtomicInteger(0));
        }
    }

    // Utility method to create an EntityManager
    private EntityManager createEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    // Utility method to close an EntityManager
    private void closeEntityManager(EntityManager em) {
        if (em != null && em.isOpen()) {
            em.close();
        }
    }

    // Utility method to handle transactions
    private void executeInTransaction(EntityManager em, Runnable operation) {
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            operation.run();
            transaction.commit();
        } catch (Exception ex) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw ex;
        }
    }

    @Transactional
    public void migrateUsersFromShard(int removedShardId) {
        logger.info("Starting migration from shard {}", removedShardId);
        migrationStats.clear();
        List<User> usersToMigrate = getUsersFromShard(removedShardId);
        logger.info("Found {} users to migrate from shard {}", usersToMigrate.size(), removedShardId);

        try {
            consistentHashingService.getConsistentHashing().removeNode(removedShardId);
            logger.info("Removed shard {} from consistent hash ring", removedShardId);

            for (User user : usersToMigrate) {
                migrateUser(user, removedShardId);
            }
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    migratingData.remove(removedShardId);
                    logger.info("Migrating data reset for shard {}", removedShardId);

                    // Broadcast shard distribution update after data reset
                    broadcastShardDistribution();
                }
            }, 20000);
        } catch (Exception ex) {
            logger.error("Error during migration from shard {}: {}", removedShardId, ex.getMessage());
            throw new ShardException("Failed to remove shard " + removedShardId + " from consistent hash ring", ex);
        }

        logger.info("Migration from shard {} completed successfully", removedShardId);
        verifyMigration(usersToMigrate);
        getUserCountsInAllShards();
    }

    private List<User> getUsersFromShard(int shardId) {
        logger.debug("Fetching users from shard {}", shardId);

        return ShardRoutingDataSource.executeInShardContext(shardId, () -> {
            EntityManager em = createEntityManager();
            try {
                return em.createQuery("SELECT u FROM User u", User.class).getResultList();
            } catch (Exception ex) {
                logger.error("Error retrieving users from shard {}: {}", shardId, ex.getMessage());
                throw new ShardException("Failed to retrieve users from shard " + shardId, ex);
            } finally {
                closeEntityManager(em);
            }
        });
    }

    private void migrateUser(User user, int oldShardId) {
        int newShardId = consistentHashingService.getShardForUser(user.getId());

        if (newShardId != oldShardId) {
            logger.info("Attempting to migrate user {} from shard {} to shard {}", user.getId(), oldShardId, newShardId);


            ShardRoutingDataSource.setCurrentShard(newShardId);
            EntityManager em = null;
            EntityTransaction transaction = null;
            updateMigratingData(oldShardId, newShardId, 1);
            updateNewAssignedData(newShardId, 1);

            migrationStats.add(new MigrationStatistics(oldShardId, newShardId, 1));


            messagingTemplate.convertAndSend("/topic/migration",
                    Map.of("userId", user.getId(), "fromShard", oldShardId, "toShard", newShardId));
            try {
                em = entityManagerFactory.createEntityManager();
                transaction = em.getTransaction();
                transaction.begin();

                em.persist(user);
                em.flush();
                transaction.commit();
                logger.info("Successfully migrated user {} to shard {}", user.getId(), newShardId);

                deleteUserFromShard(user, oldShardId);

            } catch (Exception ex) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                logger.error("Error occurred while migrating user {}: {}", user.getId(), ex.getMessage());
            } finally {
                if (em != null) {
                    em.close();
                }
                ShardRoutingDataSource.clearCurrentShard();
            }

        } else {
            logger.info("User {} remains in shard {}", user.getId(), oldShardId);
        }
    }

    private void deleteUserFromShard(User user, int shardId) {
        logger.info("Deleting user {} from shard {}", user.getId(), shardId);

        try {
            ShardRoutingDataSource.executeInShardContext(shardId, () -> {
                EntityManager em = createEntityManager();
                try {
                    executeInTransaction(em, () -> {
                        User userToDelete = em.find(User.class, user.getId());
                        if (userToDelete != null) {
                            em.remove(userToDelete);
                        }
                    });
                    logger.info("Successfully deleted user {} from shard {}", user.getId(), shardId);
                } catch (Exception ex) {
                    logger.error("Error occurred while deleting user {}: {}", user.getId(), ex.getMessage());
                    throw new ShardException("Failed to delete user " + user.getId() + " from shard " + shardId, ex);
                } finally {
                    closeEntityManager(em);
                }
                return null;
            });
        } catch (ShardException ex) {
            logger.error("Failed to delete user {} from shard {}: {}", user.getId(), shardId, ex.getMessage());
            throw ex;
        }
    }

    private void verifyMigration(List<User> migratedUsers) {
        logger.info("Verifying migration for users");

        for (User user : migratedUsers) {
            int shardId = consistentHashingService.getShardForUser(user.getId());

            try {
                ShardRoutingDataSource.executeInShardContext(shardId, () -> {
                    EntityManager em = createEntityManager();
                    try {
                        User foundUser = em.find(User.class, user.getId());
                        if (foundUser == null) {
                            logger.error("User {} not found in shard {} after migration", user.getId(), shardId);
                        } else {
                            logger.info("User {} successfully verified in shard {}", user.getId(), shardId);
                        }
                    } finally {
                        closeEntityManager(em);
                    }
                    return null;
                });
            } catch (Exception ex) {
                logger.error("Error verifying migration for user {} in shard {}: {}", user.getId(), shardId, ex.getMessage());
                throw new UserMigrationException("Failed to verify migration for user " + user.getId() + " in shard " + shardId, ex);
            }
        }
        logger.info("Migration verification completed successfully");
    }

    public void getUserCountsInAllShards() {
        logger.info("Fetching user counts from all shards");

        for (int shardId = 0; shardId < shardDataSourceProperties.getShards().size(); shardId++) {
            int userCount = getUserCountFromShard(shardId);
            shardDistribution.get(shardId).set(userCount);  // Update the shardDistribution map with actual user counts
            logger.debug("Shard {} has {} users", shardId, userCount);
        }

        logger.info("Total data distribution across shards: {}", shardDistribution);
        // Broadcast shard distribution update
        broadcastShardDistribution();  // Ensure the UI gets real-time updates
    }

    private int getUserCountFromShard(int shardId) {
        logger.debug("Getting user count from shard {}", shardId);

        return ShardRoutingDataSource.executeInShardContext(shardId, () -> {
            EntityManager em = createEntityManager();
            try {
                Query query = em.createQuery("SELECT COUNT(u) FROM User u");
                return ((Long) query.getSingleResult()).intValue();
            } finally {
                closeEntityManager(em);
            }
        });
    }

    @Transactional
    public void addShard(int shardId) {
        consistentHashingService.addShard(shardId);
        isNewShardAdded = true;
        getUserCountsInAllShards();
    }

    @Scheduled(fixedDelay = 60000)
    public void executeGradualMigration() {
        if (isNewShardAdded) {
            logger.info("Scheduled migration task started.");
            for (int shardId = 0; shardId < shardDataSourceProperties.getShards().size() - 1; shardId++) {
                gradualMigration(shardId);
            }
            isNewShardAdded = false;
        } else {
            logger.info("No new shard added, skipping migration task.");
        }
        getUserCountsInAllShards();
    }

    @Transactional
    public void gradualMigration(int oldShardId) {
        logger.info("Starting gradual migration from shard {}", oldShardId);

        List<User> usersToMigrate = getUsersFromShard(oldShardId);
        logger.info("Found {} users to migrate from shard {}", usersToMigrate.size(), oldShardId);

        for (User user : usersToMigrate) {
            int newShardId = consistentHashingService.getShardForUser(user.getId());
            if (newShardId != oldShardId) {
                migrateUser(user, oldShardId);
            } else {
                logger.info("User {} remains in shard {}", user.getId(), oldShardId);
            }
        }
        getUserCountsInAllShards();
    }

    public Map<Integer, Integer> getShardDistribution() {
        return shardDistribution.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));
    }

    public List<ShardDetail> getShardDetails() {
        // Returning shard details with shard IDs and user counts
        return shardDistribution.entrySet()
                .stream()
                .map(entry -> new ShardDetail(entry.getKey(), entry.getValue().get()))
                .collect(Collectors.toList());
    }

    public List<MigrationStatistics> getMigrationStatistics() {
        return migrationStats;
    }

    // Broadcast the updated shard distribution to WebSocket clients
    private void broadcastShardDistribution() {
        messagingTemplate.convertAndSend("/topic/shard-distribution", getShardDistribution());
    }
    public void incrementUserCount(int shard) {
        shardDistribution.get(shard).incrementAndGet();
    }

    public Map<Integer, Integer> getMigratingData() {
        return new ConcurrentHashMap<>(migratingData);
    }

    public Map<Integer, Integer> getNewAssignedData() {
        return new ConcurrentHashMap<>(newAssignedData);
    }

    public void updateMigratingData(int fromShard, int toShard, int amount) {
        migratingData.merge(fromShard, -amount, Integer::sum);
    }

    public void updateNewAssignedData(int shard, int amount) {
        newAssignedData.merge(shard, amount, Integer::sum);
    }

}
