package com.pred.pred_api.service;

import com.pred.pred_api.dto.TypeRecoursRequest;
import com.pred.pred_api.dto.TypeRecoursResponse;
import com.pred.pred_api.exception.ResourceNotFoundException;
import com.pred.pred_api.model.TypeRecours;
import com.pred.pred_api.repository.RecoursRepository;
import com.pred.pred_api.repository.TypeRecoursRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TypeRecoursService {

    private final TypeRecoursRepository typeRecoursRepository;
    private final RecoursRepository recoursRepository;
    private final AuditLogService auditLogService;

    // ============================================================
    // CRUD Operations
    // ============================================================

    public List<TypeRecoursResponse> findAll() {
        return typeRecoursRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<TypeRecoursResponse> findAllActive() {
        return typeRecoursRepository.findByActifTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<TypeRecoursResponse> findByCategorie(String categorie) {
        return typeRecoursRepository.findByCategorieAndActifTrue(categorie).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public TypeRecoursResponse findById(Long id) {
        TypeRecours typeRecours = typeRecoursRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Type de recours non trouvé avec l'ID : " + id));
        return toResponse(typeRecours);
    }

    public TypeRecours findByCode(String code) {
        return typeRecoursRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Type de recours non trouvé avec le code : " + code));
    }

    public TypeRecours findEntityById(Long id) {
        return typeRecoursRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Type de recours non trouvé avec l'ID : " + id));
    }

    // ============================================================
    // Administration
    // ============================================================

    @Transactional
    public TypeRecoursResponse create(TypeRecoursRequest request) {
        // Validation
        if (typeRecoursRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Ce code est déjà utilisé");
        }

        TypeRecours typeRecours = TypeRecours.builder()
                .code(request.getCode())
                .categorie(request.getCategorie())
                .libelleFr(request.getLibelleFr())
                .libelleAr(request.getLibelleAr())
                .descriptionFr(request.getDescriptionFr())
                .descriptionAr(request.getDescriptionAr())
                .actif(request.getActif() != null ? request.getActif() : true)
                .build();

        TypeRecours saved = typeRecoursRepository.save(typeRecours);

        auditLogService.logAction(null, "CREATE_TYPE_RECOURS",
                "Création du type de recours : " + saved.getCode());

        return toResponse(saved);
    }

    @Transactional
    public TypeRecoursResponse update(Long id, TypeRecoursRequest request) {
        TypeRecours typeRecours = findEntityById(id);

        typeRecours.setCategorie(request.getCategorie());
        typeRecours.setLibelleFr(request.getLibelleFr());
        typeRecours.setLibelleAr(request.getLibelleAr());
        typeRecours.setDescriptionFr(request.getDescriptionFr());
        typeRecours.setDescriptionAr(request.getDescriptionAr());
        typeRecours.setActif(request.getActif());

        TypeRecours updated = typeRecoursRepository.save(typeRecours);

        auditLogService.logAction(null, "UPDATE_TYPE_RECOURS",
                "Mise à jour du type de recours : " + updated.getCode());

        return toResponse(updated);
    }

    @Transactional
    public void activate(Long id) {
        TypeRecours typeRecours = findEntityById(id);
        typeRecours.setActif(true);
        typeRecoursRepository.save(typeRecours);

        auditLogService.logAction(null, "ACTIVATE_TYPE_RECOURS",
                "Activation du type de recours ID : " + id);
    }

    @Transactional
    public void deactivate(Long id) {
        TypeRecours typeRecours = findEntityById(id);
        typeRecours.setActif(false);
        typeRecoursRepository.save(typeRecours);

        auditLogService.logAction(null, "DEACTIVATE_TYPE_RECOURS",
                "Désactivation du type de recours ID : " + id);
    }

    @Transactional
    public void delete(Long id) {
        TypeRecours typeRecours = findEntityById(id);

        // Vérifier si le type est utilisé
        long count = recoursRepository.countByTypeRecours(typeRecours);
        if (count > 0) {
            throw new RuntimeException("Impossible de supprimer ce type car il est utilisé par " + count + " recours");
        }

        typeRecoursRepository.delete(typeRecours);

        auditLogService.logAction(null, "DELETE_TYPE_RECOURS",
                "Suppression du type de recours : " + typeRecours.getCode());
    }

    // ============================================================
    // Statistiques
    // ============================================================

    public java.util.Map<String, Long> getStatistiquesParCategorie() {
        return typeRecoursRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        TypeRecours::getCategorie,
                        Collectors.counting()
                ));
    }

    // ============================================================
    // Conversion DTO
    // ============================================================

    private TypeRecoursResponse toResponse(TypeRecours typeRecours) {
        long nombreRecours = recoursRepository.countByTypeRecours(typeRecours);

        return TypeRecoursResponse.builder()
                .id(typeRecours.getId())
                .code(typeRecours.getCode())
                .categorie(typeRecours.getCategorie())
                .libelleFr(typeRecours.getLibelleFr())
                .libelleAr(typeRecours.getLibelleAr())
                .descriptionFr(typeRecours.getDescriptionFr())
                .descriptionAr(typeRecours.getDescriptionAr())
                .actif(typeRecours.getActif())
                .dateCreation(typeRecours.getDateCreation())
                .nombreRecours(nombreRecours)
                .build();
    }
}