package com.asaf.whatnext.service;

import com.asaf.whatnext.repository.BaseRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public abstract class BaseService<T, ID> {

    protected final BaseRepository<T, ID> repository;

    public BaseService(BaseRepository<T, ID> repository) {
        this.repository = repository;
    }

    public List<T> findAll() {
        return repository.findAllByDeletedFalse();
    }

    public Optional<T> findById(ID id) {
        return repository.findById(id);
    }

    @Transactional
    public T save(T entity) {
        return repository.save(entity);
    }

    @Transactional
    public T update(ID id, T entity) {
        if (repository.existsById(id)) {
            return repository.save(entity);
        } else {
            throw new RuntimeException("Entity not found");
        }
    }

    @Transactional
    public void delete(ID id) {
        repository.softDelete(id);
    }

    public Page<T> findAllPaged(Pageable pageable) {
        return repository.findAll(pageable);
    }
}
