package com.example.consistent_hashing.repository;

import com.example.consistent_hashing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<User, Long> {

}