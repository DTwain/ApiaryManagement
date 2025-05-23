package org.apiary.service.impl;

import org.apiary.model.Apiary;
import org.apiary.model.Beekeeper;
import org.apiary.repository.interfaces.ApiaryRepository;
import org.apiary.service.interfaces.ApiaryService;
import org.apiary.utils.events.EntityChangeEvent;
import org.apiary.utils.observer.EventManager;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ApiaryServiceImpl extends EventManager<EntityChangeEvent<?>> implements ApiaryService {

    private static final Logger LOGGER = Logger.getLogger(ApiaryServiceImpl.class.getName());
    private final ApiaryRepository apiaryRepository;

    public ApiaryServiceImpl(ApiaryRepository apiaryRepository) {
        this.apiaryRepository = apiaryRepository;
    }

    @Override
    public Apiary createApiary(String name, String location, Beekeeper beekeeper) {
        try {
            Apiary apiary = new Apiary(name, location, beekeeper);
            Apiary savedApiary = apiaryRepository.save(apiary);

            // Notify observers
            notifyObservers(new EntityChangeEvent<>(EntityChangeEvent.Type.CREATED, savedApiary));

            LOGGER.info("Created new apiary: " + name + " for beekeeper: " + beekeeper.getUsername());
            return savedApiary;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating apiary: " + name, e);
            return null;
        }
    }


    @Override
    public Apiary updateApiary(Integer apiaryId, String name, String location, Beekeeper beekeeper) {
        try {
            Optional<Apiary> apiaryOpt = apiaryRepository.findById(apiaryId);
            if (apiaryOpt.isEmpty()) {
                LOGGER.warning("Apiary not found: " + apiaryId);
                return null;
            }

            Apiary apiary = apiaryOpt.get();
            Apiary oldApiary = new Apiary(apiary.getName(), apiary.getLocation(), apiary.getBeekeeper());
            oldApiary.setApiaryId(apiary.getApiaryId());

            // Check if the apiary belongs to the beekeeper
            if (!apiary.getBeekeeper().equals(beekeeper)) {
                LOGGER.warning("Apiary does not belong to beekeeper: " + beekeeper.getUsername());
                return null;
            }

            apiary.setName(name);
            apiary.setLocation(location);

            Apiary updatedApiary = apiaryRepository.save(apiary);

            // Notify observers
            notifyObservers(new EntityChangeEvent<>(EntityChangeEvent.Type.UPDATED, updatedApiary, oldApiary));

            LOGGER.info("Updated apiary: " + apiaryId);
            return updatedApiary;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating apiary: " + apiaryId, e);
            return null;
        }
    }

    @Override
    public boolean deleteApiary(Integer apiaryId) {
        try {
            Optional<Apiary> apiaryOpt = apiaryRepository.findById(apiaryId);
            if (apiaryOpt.isEmpty()) {
                LOGGER.warning("Apiary not found: " + apiaryId);
                return false;
            }

            Apiary apiary = apiaryOpt.get();
            apiaryRepository.deleteById(apiaryId);

            // Notify observers
            notifyObservers(new EntityChangeEvent<>(EntityChangeEvent.Type.DELETED, apiary));

            LOGGER.info("Deleted apiary: " + apiaryId);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting apiary: " + apiaryId, e);
            return false;
        }
    }

    @Override
    public Optional<Apiary> findById(Integer apiaryId) {
        try {
            return apiaryRepository.findById(apiaryId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding apiary by ID: " + apiaryId, e);
            return Optional.empty();
        }
    }

    @Override
    public List<Apiary> findByBeekeeper(Beekeeper beekeeper) {
        try {
            return apiaryRepository.findByBeekeeper(beekeeper);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding apiaries by beekeeper: " + beekeeper.getUsername(), e);
            return List.of();
        }
    }

    @Override
    public List<Apiary> findByNameContaining(String name) {
        try {
            return apiaryRepository.findByNameContaining(name);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding apiaries by name: " + name, e);
            return List.of();
        }
    }

    @Override
    public List<Apiary> findByLocationContaining(String location) {
        try {
            return apiaryRepository.findByLocationContaining(location);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding apiaries by location: " + location, e);
            return List.of();
        }
    }

    @Override
    public boolean isApiaryOwnedByBeekeeper(Beekeeper beekeeper, Integer apiaryId) {
        try {
            Optional<Apiary> apiaryOpt = apiaryRepository.findById(apiaryId);
            return apiaryOpt.isPresent() &&
                    apiaryOpt.get().getBeekeeper().getUserId().equals(beekeeper.getUserId());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking if apiary is owned by beekeeper: " +
                    apiaryId + ", " + beekeeper.getUsername(), e);
            return false;
        }
    }

    @Override
    public List<Apiary> findAll() {
        try {
            return apiaryRepository.findAll();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding all apiaries", e);
            return List.of();
        }
    }

    @Override
    public List<String> findAllLocations() {
        try {
            return apiaryRepository.findAll().stream()
                    .map(Apiary::getLocation)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding all locations", e);
            return List.of();
        }
    }
}