package com.nutricoach.client.service;

import com.nutricoach.client.dto.ClientNoteRequest;
import com.nutricoach.client.dto.ClientNoteResponse;
import com.nutricoach.client.entity.ClientNote;
import com.nutricoach.client.repository.ClientNoteRepository;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.common.exception.NutriCoachException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Coach-authored notes on a client. */
@Service
@RequiredArgsConstructor
public class ClientNoteService {

    private final ClientNoteRepository noteRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public List<ClientNoteResponse> list(UUID clientId, UUID coachId) {
        requireClient(clientId, coachId);
        return noteRepository.findByCoachIdAndClientIdAndDeletedAtIsNullOrderByCreatedAtDesc(coachId, clientId)
                .stream().map(ClientNoteService::toResponse).toList();
    }

    @Transactional
    public ClientNoteResponse create(UUID clientId, UUID coachId, ClientNoteRequest req) {
        requireClient(clientId, coachId);
        return toResponse(noteRepository.save(ClientNote.builder()
                .coachId(coachId).clientId(clientId).body(req.body().trim()).build()));
    }

    @Transactional
    public ClientNoteResponse update(UUID clientId, UUID noteId, UUID coachId, ClientNoteRequest req) {
        ClientNote note = requireNote(clientId, noteId, coachId);
        note.setBody(req.body().trim());
        return toResponse(noteRepository.save(note));
    }

    @Transactional
    public void delete(UUID clientId, UUID noteId, UUID coachId) {
        ClientNote note = requireNote(clientId, noteId, coachId);
        note.setDeletedAt(Instant.now());
        noteRepository.save(note);
    }

    /**
     * A note is addressed through its client, so the note must actually belong
     * to that client — otherwise one coach's note id could be read or edited
     * under a different client of theirs.
     */
    private ClientNote requireNote(UUID clientId, UUID noteId, UUID coachId) {
        requireClient(clientId, coachId);
        ClientNote note = noteRepository.findByIdAndCoachIdAndDeletedAtIsNull(noteId, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Note not found"));
        if (!note.getClientId().equals(clientId)) {
            throw NutriCoachException.notFound("Note not found");
        }
        return note;
    }

    private void requireClient(UUID clientId, UUID coachId) {
        if (clientRepository.findByIdAndCoachIdAndDeletedAtIsNull(clientId, coachId).isEmpty()) {
            throw NutriCoachException.notFound("Client not found");
        }
    }

    private static ClientNoteResponse toResponse(ClientNote n) {
        return new ClientNoteResponse(n.getId(), n.getBody(), n.getCreatedAt(), n.getUpdatedAt());
    }
}
