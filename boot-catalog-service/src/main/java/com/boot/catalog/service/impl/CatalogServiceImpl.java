package com.boot.catalog.service.impl;

import com.boot.catalog.entity.CatalogEntity;
import com.boot.catalog.repository.CatalogRepository;
import com.boot.catalog.service.CatalogService;
import org.springframework.stereotype.Service;

@Service
public class CatalogServiceImpl implements CatalogService {

    private CatalogRepository catalogRepository;

    public CatalogServiceImpl(CatalogRepository catalogRepository) {
        this.catalogRepository = catalogRepository;
    }

    @Override
    public Iterable<CatalogEntity> getAllCatalogs() {
        return catalogRepository.findAll();
    }
}
