package it.unibs.ingsw24_25.repository;

import it.unibs.ingsw24_25.model.Beneficiary;

import java.util.List;
import java.util.Optional;

public interface BeneficiaryRepository {
    Optional<Beneficiary> findByUsername(String username);

    List<Beneficiary> findAll();

    void save(Beneficiary beneficiary);

    void deleteByUsername(String username);
}
