package com.maylee.privatelog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.maylee.privatelog.entity.Users;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long>{
    
}
