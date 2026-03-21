package com.nutricoach.client.service;

import com.nutricoach.client.dto.ClientResponse;
import com.nutricoach.client.dto.CreateClientRequest;
import com.nutricoach.client.dto.UpdateClientRequest;
import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.common.exception.NutriCoachException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public List<ClientResponse> findAll(UUID coachId, Client.Status status) {
        List<Client> clients = (status != null)
                ? clientRepository.findByCoachIdAndStatusAndDeletedAtIsNull(coachId, status)
                : clientRepository.findByCoachIdAndDeletedAtIsNull(coachId);
        return clients.stream().map(ClientResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ClientResponse findById(UUID id, UUID coachId) {
        Client client = requireOwned(id, coachId);
        return ClientResponse.from(client);
    }

    @Transactional
    public ClientResponse create(CreateClientRequest req, UUID coachId) {
        if (clientRepository.existsByCoachIdAndPhoneAndDeletedAtIsNull(coachId, req.phone())) {
            throw NutriCoachException.conflict("A client with this phone number already exists");
        }

        Client client = Client.builder()
                .coachId(coachId)
                .phone(req.phone())
                .name(req.name())
                .email(req.email())
                .whatsappNumber(req.whatsappNumber())
                .dateOfBirth(req.dateOfBirth())
                .gender(req.gender())
                .heightCm(req.heightCm())
                .weightKg(req.weightKg())
                .goal(req.goal())
                .dietaryPref(req.dietaryPref())
                .activityLevel(req.activityLevel())
                .healthConditions(req.healthConditions())
                .allergies(req.allergies())
                .build();

        return ClientResponse.from(clientRepository.save(client));
    }

    @Transactional
    public ClientResponse update(UUID id, UpdateClientRequest req, UUID coachId) {
        Client client = requireOwned(id, coachId);

        if (StringUtils.hasText(req.name()))          client.setName(req.name());
        if (req.email() != null)                       client.setEmail(req.email());
        if (req.whatsappNumber() != null)              client.setWhatsappNumber(req.whatsappNumber());
        if (req.dateOfBirth() != null)                 client.setDateOfBirth(req.dateOfBirth());
        if (req.gender() != null)                      client.setGender(req.gender());
        if (req.heightCm() != null)                    client.setHeightCm(req.heightCm());
        if (req.weightKg() != null)                    client.setWeightKg(req.weightKg());
        if (req.goal() != null)                        client.setGoal(req.goal());
        if (req.dietaryPref() != null)                 client.setDietaryPref(req.dietaryPref());
        if (req.activityLevel() != null)               client.setActivityLevel(req.activityLevel());
        if (req.status() != null)                      client.setStatus(req.status());
        if (req.healthConditions() != null)            client.setHealthConditions(req.healthConditions());
        if (req.allergies() != null)                   client.setAllergies(req.allergies());

        return ClientResponse.from(clientRepository.save(client));
    }

    @Transactional
    public void delete(UUID id, UUID coachId) {
        Client client = requireOwned(id, coachId);
        client.setDeletedAt(Instant.now());
        clientRepository.save(client);
    }

    private Client requireOwned(UUID id, UUID coachId) {
        return clientRepository.findByIdAndCoachIdAndDeletedAtIsNull(id, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Client not found"));
    }
}
