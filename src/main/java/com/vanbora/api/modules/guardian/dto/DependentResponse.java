package com.vanbora.api.modules.guardian.dto;

import com.vanbora.api.modules.guardian.domain.Dependent;

/** Dependente do responsável. */
public record DependentResponse(Long id, String name, String school, String photoUrl) {

    public static DependentResponse from(Dependent dependent) {
        return new DependentResponse(
                dependent.getId(), dependent.getName(), dependent.getSchool(), dependent.getPhotoUrl());
    }
}
