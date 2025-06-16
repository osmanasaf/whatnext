package com.asaf.whatnext.repository;

import com.asaf.whatnext.models.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.time.LocalDate;
import java.util.List;

@NoRepositoryBean
public interface BaseEventRepository<T extends Event> extends BaseRepository<T, Long> {
    List<T> findByStartDateBetweenAndVenue_CityIgnoreCase(LocalDate startDate, LocalDate endDate, String city);
    List<T> findByStartDateBetween(LocalDate startDate, LocalDate endDate);
    List<T> findByVenue_CityIgnoreCase(String city);
}

