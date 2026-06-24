package com.nutricoach.library.service;

import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.library.dto.AssignProgramRequest;
import com.nutricoach.library.dto.ProgramAssignmentResponse;
import com.nutricoach.library.entity.ClientProgramAssignment;
import com.nutricoach.library.repository.ClientProgramAssignmentRepository;
import com.nutricoach.library.repository.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgramAssignmentService {

    private final ClientProgramAssignmentRepository assignmentRepository;
    private final ProgramRepository programRepository;
    private final ClientRepository clientRepository;

    @Transactional
    public List<ProgramAssignmentResponse> assign(UUID programId, UUID coachId, AssignProgramRequest req) {
        requireProgram(programId, coachId);
        List<ProgramAssignmentResponse> result = new ArrayList<>();
        for (UUID clientId : req.clientIds()) {
            requireClient(clientId, coachId);
            ClientProgramAssignment existing = assignmentRepository
                    .findByClientIdAndProgramIdAndDeletedAtIsNull(clientId, programId).orElse(null);
            if (existing != null) {
                result.add(toResponse(existing));
                continue;
            }
            ClientProgramAssignment saved = assignmentRepository.save(ClientProgramAssignment.builder()
                    .coachId(coachId)
                    .clientId(clientId)
                    .programId(programId)
                    .assignedAt(Instant.now())
                    .startDate(req.startDate())
                    .notes(req.notes())
                    .build());
            result.add(toResponse(saved));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ProgramAssignmentResponse> listByProgram(UUID programId, UUID coachId) {
        requireProgram(programId, coachId);
        return assignmentRepository.findByProgramIdAndDeletedAtIsNull(programId).stream()
                .filter(a -> a.getCoachId().equals(coachId))
                .map(this::toResponse).toList();
    }

    @Transactional
    public void unassign(UUID programId, UUID assignmentId, UUID coachId) {
        requireProgram(programId, coachId);
        ClientProgramAssignment a = assignmentRepository.findByIdAndCoachIdAndDeletedAtIsNull(assignmentId, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Assignment not found"));
        if (!a.getProgramId().equals(programId)) {
            throw NutriCoachException.notFound("Assignment not found");
        }
        a.setDeletedAt(Instant.now());
        assignmentRepository.save(a);
    }

    private void requireProgram(UUID programId, UUID coachId) {
        programRepository.findByIdAndCoachIdAndDeletedAtIsNull(programId, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Program not found"));
    }

    private void requireClient(UUID clientId, UUID coachId) {
        clientRepository.findByIdAndCoachIdAndDeletedAtIsNull(clientId, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Client not found"));
    }

    private ProgramAssignmentResponse toResponse(ClientProgramAssignment a) {
        return new ProgramAssignmentResponse(
                a.getId(), a.getClientId(), a.getProgramId(),
                a.getAssignedAt(), a.getStartDate(), a.getNotes());
    }
}
