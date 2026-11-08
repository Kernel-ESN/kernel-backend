package com.kernelconsulting.plateforme.account.services;

import com.kernelconsulting.plateforme.account.api.dto.CvResponse;
import com.kernelconsulting.plateforme.account.api.dto.ProfileRequest;
import com.kernelconsulting.plateforme.account.api.dto.RegisterRequest;
import com.kernelconsulting.plateforme.account.api.dto.UserResponse;
import com.kernelconsulting.plateforme.account.api.mapper.UserMapper;
import com.kernelconsulting.plateforme.account.domain.AppUser;
import com.kernelconsulting.plateforme.account.domain.Organization;
import com.kernelconsulting.plateforme.account.domain.UserRole;
import com.kernelconsulting.plateforme.account.domain.Verification;
import com.kernelconsulting.plateforme.account.repository.AppUserRepository;
import com.kernelconsulting.plateforme.account.repository.OrganizationRepository;
import com.kernelconsulting.plateforme.common.exception.ApiException;
import com.kernelconsulting.plateforme.common.security.CurrentUser;
import com.kernelconsulting.plateforme.common.storage.FileKind;
import com.kernelconsulting.plateforme.common.storage.FileStorage;
import com.kernelconsulting.plateforme.common.storage.UploadValidator;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Inscription, profil et CV du compte. */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AppUserRepository users;
    private final OrganizationRepository organizations;
    private final PasswordEncoder passwordEncoder;
    private final UploadValidator uploads;
    private final FileStorage storage;
    private final CurrentUser currentUser;

    /**
     * Crée un compte. Un employeur fournit obligatoirement une organisation, un téléphone et un
     * justificatif ; l'organisation attend ensuite sa vérification.
     *
     * @throws ApiException 409 si l'e-mail est pris, 400 ou 422 si l'inscription est incomplète
     */
    @Transactional
    public UserResponse register(RegisterRequest request, MultipartFile document) {
        if (request.role() == UserRole.ADMIN) {
            throw ApiException.badRequest("Ce profil n'est pas disponible.", List.of());
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("Un compte existe déjà avec cet e-mail.");
        }
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setName(request.name().trim());
        user.setPhone(blankToNull(request.phone()));
        user.setRole(request.role());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        if (request.role() == UserRole.EMPLOYEUR) {
            user.setOrganization(createOrganization(request, document));
        }
        return UserMapper.toResponse(users.save(user));
    }

    private Organization createOrganization(RegisterRequest request, MultipartFile document) {
        if (request.organization() == null || request.phone() == null || request.phone().isBlank()) {
            throw ApiException.badRequest(
                    "L'inscription d'une organisation demande son nom, son type et un téléphone.", List.of());
        }
        uploads.check(document, FileKind.ORGANIZATION_DOCUMENT);
        Organization organization = new Organization();
        organization.setName(request.organization().name().trim());
        organization.setType(request.organization().type());
        organization.setVerification(Verification.EN_ATTENTE);
        organization.setContactPhone(request.phone().trim());
        organization.setDocumentName(document.getOriginalFilename());
        organization.setDocumentPath(storage.store(document, "organizations"));
        return organizations.save(organization);
    }

    /**
     * Remplace le justificatif de l'organisation et la renvoie en vérification.
     *
     * @throws ApiException 409 si l'organisation est déjà vérifiée
     */
    @Transactional
    public UserResponse replaceOrganizationDocument(MultipartFile document) {
        AppUser user = current();
        Organization organization = user.getOrganization();
        if (organization == null) {
            throw ApiException.notFound("Ce compte n'a pas d'organisation.");
        }
        if (organization.isVerified()) {
            throw ApiException.conflict("Cette organisation est déjà vérifiée.");
        }
        uploads.check(document, FileKind.ORGANIZATION_DOCUMENT);
        String previous = organization.getDocumentPath();
        organization.setDocumentPath(storage.store(document, "organizations"));
        organization.setDocumentName(document.getOriginalFilename());
        organization.setVerification(Verification.EN_ATTENTE);
        organization.setVerificationNote(null);
        storage.delete(previous);
        return UserMapper.toResponse(user);
    }

    /** @return le compte de la session */
    @Transactional(readOnly = true)
    public UserResponse me() {
        return UserMapper.toResponse(current());
    }

    /** Modifie le nom et le téléphone. L'e-mail ne change pas ici. */
    @Transactional
    public UserResponse updateProfile(ProfileRequest request) {
        AppUser user = current();
        user.setName(request.name().trim());
        user.setPhone(blankToNull(request.phone()));
        return UserMapper.toResponse(user);
    }

    /** @return le CV enregistré, s'il y en a un */
    @Transactional(readOnly = true)
    public Optional<CvResponse> cv() {
        AppUser user = current();
        return user.getCvPath() == null ? Optional.empty() : Optional.of(UserMapper.toCv(user));
    }

    /** Enregistre ou remplace le CV du compte. */
    @Transactional
    public CvResponse saveCv(MultipartFile file) {
        uploads.check(file, FileKind.CV);
        AppUser user = current();
        String previous = user.getCvPath();
        keepCv(user, file);
        storage.delete(previous);
        return UserMapper.toCv(user);
    }

    /** Conserve un fichier déjà contrôlé comme CV du compte. */
    public void keepCv(AppUser user, MultipartFile file) {
        user.setCvPath(storage.store(file, "cv"));
        user.setCvName(file.getOriginalFilename());
        user.setCvSize(file.getSize());
        user.setCvUpdatedAt(Instant.now());
    }

    /** Supprime le CV du compte. Les candidatures déjà envoyées gardent leur copie. */
    @Transactional
    public void removeCv() {
        AppUser user = current();
        storage.delete(user.getCvPath());
        user.setCvPath(null);
        user.setCvName(null);
        user.setCvSize(null);
        user.setCvUpdatedAt(null);
    }

    private AppUser current() {
        return users.findById(currentUser.id())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Connectez-vous pour continuer."));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
