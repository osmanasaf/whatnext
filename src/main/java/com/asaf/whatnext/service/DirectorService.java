package com.asaf.whatnext.service;

import com.asaf.whatnext.models.Director;
import com.asaf.whatnext.repository.DirectorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DirectorService {

    private final DirectorRepository directorRepository;

    @Autowired
    public DirectorService(DirectorRepository directorRepository) {
        this.directorRepository = directorRepository;
    }

    public Optional<Director> findById(Long id) {
        return directorRepository.findById(id);
    }

    public Director findByName(String name) {
        return directorRepository.findByName(name);
    }

    public Director findOrCreateDirector(String name) {
        Director director = findByName(name);
        if (director == null) {
            director = new Director();
            director.setName(name);
            director = save(director);
        }
        return director;
    }

    public Director save(Director director) {
        return directorRepository.save(director);
    }

    public void deleteById(Long id) {
        directorRepository.deleteById(id);
    }
} 