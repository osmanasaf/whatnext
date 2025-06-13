package com.asaf.whatnext.repository;

import com.asaf.whatnext.models.Director;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectorRepository extends JpaRepository<Director, Long> {
    Director findByName(String name);
} 