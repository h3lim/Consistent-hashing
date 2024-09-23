package com.example.consistent_hashing.service;

import com.example.consistent_hashing.config.ShardRoutingDataSource;
import com.example.consistent_hashing.entity.User;
import com.example.consistent_hashing.exception.ShardException;
import com.example.consistent_hashing.repository.UserRepository;
import com.example.consistent_hashing.util.CustomIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final ConsistentHashingService consistentHashingService;
    private final CustomIdGenerator idGenerator;
    private final SimpMessagingTemplate messagingTemplate;
    private final ShardService shardService;

    public UserService(UserRepository userRepository,
                       ConsistentHashingService consistentHashingService,
                       CustomIdGenerator idGenerator,
                       SimpMessagingTemplate messagingTemplate,
                       ShardService shardService) {
        this.userRepository = userRepository;
        this.consistentHashingService = consistentHashingService;
        this.idGenerator = idGenerator;
        this.messagingTemplate = messagingTemplate;
        this.shardService = shardService;
    }

    @Transactional
    public User saveUser(User user) {
        if (user.getId() == null) {
            user.setId(idGenerator.generateId());
        }
        int shard = consistentHashingService.getShardForUser(user.getId());
        logger.info("Saving user {} to shard {}", user.getId(), shard);

        try {
            User savedUser = ShardRoutingDataSource.executeInShardContext(shard, () -> {
                User result = userRepository.save(user);
                return result;
            });

            // Update shard distribution and broadcast the change
            shardService.incrementUserCount(shard);
            Map<Integer, Integer> updatedDistribution = shardService.getShardDistribution();
            messagingTemplate.convertAndSend("/topic/shard-distribution", updatedDistribution);

            // Send user creation event with updated count
            messagingTemplate.convertAndSend("/topic/user-creation",
                    Map.of("userId", savedUser.getId(),
                            "toShard", shard,
                            "newCount", updatedDistribution.get(shard)));

            return savedUser;
        } catch (Exception ex) {
            logger.error("Error saving user {}: {}", user.getId(), ex.getMessage());
            throw new ShardException("Failed to save user to shard " + shard, ex);
        }
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        int shard = consistentHashingService.getShardForUser(id);
        logger.info("Retrieving user {} from shard {}", id, shard);

        try {
            return ShardRoutingDataSource.executeInShardContext(shard, () -> {
                Optional<User> user = userRepository.findById(id);
                return user.orElse(null);
            });
        } catch (Exception ex) {
            logger.error("Error retrieving user {} from shard {}: {}", id, shard, ex.getMessage());
            throw new ShardException("Failed to retrieve user " + id + " from shard " + shard, ex);
        }
    }

    @Transactional
    public void deleteUser(Long id) {
        int shard = consistentHashingService.getShardForUser(id);
        logger.info("Deleting user {} from shard {}", id, shard);

        try {
            ShardRoutingDataSource.executeInShardContext(shard, () -> {
                userRepository.deleteById(id);
                return null;
            });
        } catch (Exception ex) {
            logger.error("Error deleting user {} from shard {}: {}", id, shard, ex.getMessage());
            throw new ShardException("Failed to delete user " + id + " from shard " + shard, ex);
        }
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        logger.info("Retrieving all users from all shards");

        try {
            return userRepository.findAll();
        } catch (Exception ex) {
            logger.error("Error retrieving all users: {}", ex.getMessage());
            throw new ShardException("Failed to retrieve all users", ex);
        }
    }

    @Transactional
    public User updateUser(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("User ID must not be null for update operation");
        }

        int shard = consistentHashingService.getShardForUser(user.getId());
        logger.info("Updating user {} in shard {}", user.getId(), shard);

        try {
            return ShardRoutingDataSource.executeInShardContext(shard, () -> {
                Optional<User> existingUser = userRepository.findById(user.getId());
                if (existingUser.isPresent()) {
                    User updatedUser = userRepository.save(user);
                    return updatedUser;
                } else {
                    throw new IllegalArgumentException("User not found with id: " + user.getId());
                }
            });
        } catch (Exception ex) {
            logger.error("Error updating user {} in shard {}: {}", user.getId(), shard, ex.getMessage());
            throw new ShardException("Failed to update user " + user.getId() + " in shard " + shard, ex);
        }
    }

    public int getUserShard(Long userId) {
        return consistentHashingService.getShardForUser(userId);
    }

    @Transactional(readOnly = true)
    public long getUserCount() {
        logger.info("Counting all users across all shards");

        try {
            return userRepository.count();
        } catch (Exception ex) {
            logger.error("Error counting users: {}", ex.getMessage());
            throw new ShardException("Failed to count users", ex);
        }
    }

    @Transactional(readOnly = true)
    public List<User> getUsersByShardId(int shardId) {
        logger.info("Retrieving all users from shard {}", shardId);

        try {
            return ShardRoutingDataSource.executeInShardContext(shardId, () -> userRepository.findAll());
        } catch (Exception ex) {
            logger.error("Error retrieving users from shard {}: {}", shardId, ex.getMessage());
            throw new ShardException("Failed to retrieve users from shard " + shardId, ex);
        }
    }
}