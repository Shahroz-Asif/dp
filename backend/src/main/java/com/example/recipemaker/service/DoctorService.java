package com.example.recipemaker.service;

import com.example.recipemaker.dto.PatientConditionResponse;
import com.example.recipemaker.dto.PatientProfileResponse;
import com.example.recipemaker.model.AppUser;
import com.example.recipemaker.model.PatientCondition;
import com.example.recipemaker.model.PatientProfile;
import com.example.recipemaker.repository.AppUserRepository;
import com.example.recipemaker.repository.PatientConditionRepository;
import com.example.recipemaker.repository.PatientProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DoctorService {

    private final PatientProfileRepository profileRepository;
    private final PatientConditionRepository conditionRepository;
    private final AppUserRepository userRepository;

    private AppUser getCurrentDoctor() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }

    public List<PatientProfileResponse> getDoctorPatients() {
        AppUser doctor = getCurrentDoctor();
        return profileRepository.findByAssignedDoctor(doctor)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public PatientProfileResponse assignCondition(Long patientProfileId, Long conditionId) {
        AppUser doctor = getCurrentDoctor();
        PatientProfile profile = profileRepository.findById(patientProfileId)
                .orElseThrow(() -> new NoSuchElementException("Patient profile not found"));

        if (profile.getAssignedDoctor() != null && !profile.getAssignedDoctor().getId().equals(doctor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This patient is assigned to a different doctor.");
        }

        PatientCondition condition = conditionRepository.findById(conditionId)
                .orElseThrow(() -> new NoSuchElementException("Condition not found"));

        if (profile.getAssignedDoctor() == null) {
            profile.setAssignedDoctor(doctor);
        }
        profile.getConditions().add(condition);
        return toResponse(profileRepository.save(profile));
    }

    public PatientProfileResponse removeCondition(Long patientProfileId, Long conditionId) {
        AppUser doctor = getCurrentDoctor();
        PatientProfile profile = profileRepository.findById(patientProfileId)
                .orElseThrow(() -> new NoSuchElementException("Patient profile not found"));

        if (profile.getAssignedDoctor() != null && !profile.getAssignedDoctor().getId().equals(doctor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This patient is assigned to a different doctor.");
        }

        profile.getConditions().removeIf(c -> c.getId().equals(conditionId));
        return toResponse(profileRepository.save(profile));
    }

    public PatientProfileResponse toResponse(PatientProfile profile) {
        return PatientProfileResponse.builder()
                .id(profile.getId())
                .name(profile.getName())
                .age(profile.getAge())
                .notes(profile.getNotes())
                .assignedDoctorUsername(profile.getAssignedDoctor() != null
                        ? profile.getAssignedDoctor().getUsername() : null)
                .conditions(profile.getConditions().stream()
                        .map(this::toConditionResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    public PatientConditionResponse toConditionResponse(PatientCondition condition) {
        return PatientConditionResponse.builder()
                .id(condition.getId())
                .name(condition.getName())
                .description(condition.getDescription())
                .createdByDoctorName(condition.getCreatedByDoctor() != null
                        ? condition.getCreatedByDoctor().getUsername() : null)
                .build();
    }
}
