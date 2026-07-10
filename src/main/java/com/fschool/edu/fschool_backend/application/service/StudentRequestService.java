package com.fschool.edu.fschool_backend.application.service;

import com.fschool.edu.fschool_backend.domain.enums.StudentRequestStatus;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClassEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.RequestAttachmentEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.RequestHistoryEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.RequestTypeEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.RequestTypeFieldValue;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.StudentRequestEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.UploadedFileEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.UserEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ClassJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.RequestAttachmentJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.RequestHistoryJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.RequestTypeJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.StudentRequestJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.UploadedFileJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.UserJpaRepository;
import com.fschool.edu.fschool_backend.presentation.dto.request.CreateStudentRequestRequest;
import com.fschool.edu.fschool_backend.presentation.dto.response.CreateStudentRequestResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.RequestTypeResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentRequestDetailResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentRequestListResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import com.fschool.edu.fschool_backend.presentation.exception.RequestValidationException;
import jakarta.persistence.criteria.Predicate;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StudentRequestService {

    private static final ZoneId REQUEST_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter REQUEST_NUMBER_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final long MAX_ATTACHMENT_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_ATTACHMENT_EXTENSIONS = Set.of(
            "pdf",
            "jpg",
            "jpeg",
            "png",
            "doc",
            "docx");

    private final RequestTypeJpaRepository requestTypeRepository;
    private final StudentRequestJpaRepository requestRepository;
    private final RequestAttachmentJpaRepository attachmentRepository;
    private final RequestHistoryJpaRepository historyRepository;
    private final UploadedFileJpaRepository uploadedFileRepository;
    private final UserJpaRepository userRepository;
    private final ClassJpaRepository classRepository;
    private final Path studentRequestUploadDir;
    private final String studentRequestPublicPath;

    public StudentRequestService(
            RequestTypeJpaRepository requestTypeRepository,
            StudentRequestJpaRepository requestRepository,
            RequestAttachmentJpaRepository attachmentRepository,
            RequestHistoryJpaRepository historyRepository,
            UploadedFileJpaRepository uploadedFileRepository,
            UserJpaRepository userRepository,
            ClassJpaRepository classRepository,
            @Value("${app.upload.student-request-dir:uploads/student-requests}") String studentRequestUploadDir,
            @Value("${app.upload.student-request-public-path:/uploads/student-requests}")
                    String studentRequestPublicPath) {
        this.requestTypeRepository = requestTypeRepository;
        this.requestRepository = requestRepository;
        this.attachmentRepository = attachmentRepository;
        this.historyRepository = historyRepository;
        this.uploadedFileRepository = uploadedFileRepository;
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.studentRequestUploadDir = Paths.get(studentRequestUploadDir).toAbsolutePath().normalize();
        this.studentRequestPublicPath = trimTrailingSlash(studentRequestPublicPath);
    }

    @Transactional(readOnly = true)
    public List<RequestTypeResponse> getRequestTypes() {
        return requestTypeRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(this::toRequestTypeResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentRequestListResponse getStudentRequests(
            UUID studentId,
            int page,
            int limit,
            String status,
            String typeCode,
            LocalDate fromDate,
            LocalDate toDate) {
        validatePagination(page, limit);
        StudentRequestStatus statusFilter = parseStatus(status);
        Set<UUID> typeFilterIds = resolveTypeFilterIds(typeCode);
        if (isPresent(typeCode) && typeFilterIds.isEmpty()) {
            return emptyListResponse(page, limit);
        }

        Instant fromInstant = fromDate == null ? null : fromDate.atStartOfDay(REQUEST_ZONE).toInstant();
        Instant toInstant = toDate == null ? null : toDate.plusDays(1).atStartOfDay(REQUEST_ZONE).toInstant();
        Specification<StudentRequestEntity> spec = studentRequestSpec(
                studentId,
                statusFilter,
                typeFilterIds,
                fromInstant,
                toInstant);
        Page<StudentRequestEntity> result = requestRepository.findAll(
                spec,
                PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt")));
        Map<UUID, RequestTypeEntity> requestTypes = requestTypeMap(result.getContent());
        List<StudentRequestListResponse.Item> items = result.getContent().stream()
                .map(request -> toListItem(request, requestTypes.get(request.getTypeId())))
                .toList();
        StudentRequestListResponse.Pagination pagination = new StudentRequestListResponse.Pagination(
                page,
                limit,
                result.getTotalElements(),
                result.getTotalPages());
        return new StudentRequestListResponse(
                items,
                page,
                limit,
                result.getTotalElements(),
                result.getTotalPages(),
                pagination);
    }

    @Transactional(readOnly = true)
    public StudentRequestDetailResponse getStudentRequest(UUID studentId, String requestId) {
        StudentRequestEntity request = findStudentRequest(studentId, requestId);
        RequestTypeEntity requestType = requireRequestType(request.getTypeId());
        UserEntity student = requireUser(studentId);
        List<StudentRequestDetailResponse.Attachment> attachments = getAttachments(request.getId());
        List<StudentRequestDetailResponse.History> history = historyRepository
                .findByRequestIdOrderByCreatedAtAsc(request.getId())
                .stream()
                .map(this::toHistoryResponse)
                .toList();

        return new StudentRequestDetailResponse(
                request.getRequestNumber(),
                canonicalRequestTypeCode(requestType),
                canonicalRequestTypeName(requestType),
                request.getTitle(),
                statusCode(request.getStatus()),
                statusLabel(request.getStatus()),
                new StudentRequestDetailResponse.Student(
                        student.getStudentCode(),
                        student.getFullName(),
                        className(student)),
                request.getFormData() == null ? Map.of() : request.getFormData(),
                attachments,
                history,
                toOffsetDateTime(request.getCreatedAt()),
                toOffsetDateTime(request.getUpdatedAt()));
    }

    @Transactional
    public CreateStudentRequestResponse createStudentRequest(UUID studentId, CreateStudentRequestRequest request) {
        return createStudentRequest(studentId, request, List.of());
    }

    @Transactional
    public CreateStudentRequestResponse createStudentRequest(
            UUID studentId,
            CreateStudentRequestRequest request,
            List<MultipartFile> attachmentFiles) {
        RequestTypeEntity requestType = validateCreateRequest(request, attachmentFiles);
        Map<String, Object> formData = buildFormData(request);
        List<UploadedFileEntity> attachments = new ArrayList<>(resolveAttachments(request.attachmentIds()));
        attachments.addAll(storeAttachments(studentId, attachmentFiles));

        StudentRequestEntity requestEntity = new StudentRequestEntity();
        requestEntity.setRequestNumber(generateRequestNumber());
        requestEntity.setStudentId(studentId);
        requestEntity.setTypeId(requestType.getId());
        requestEntity.setTitle(isPresent(request.title()) ? request.title().trim() : canonicalRequestTypeName(requestType));
        requestEntity.setStatus(StudentRequestStatus.SUBMITTED);
        requestEntity.setFormData(new LinkedHashMap<>(formData));
        StudentRequestEntity savedRequest = requestRepository.save(requestEntity);

        attachments.stream()
                .map(file -> toAttachment(savedRequest.getId(), file.getId()))
                .forEach(attachmentRepository::save);
        historyRepository.save(toHistory(
                savedRequest.getId(),
                StudentRequestStatus.SUBMITTED,
                "Student submitted request",
                studentId));

        return new CreateStudentRequestResponse(
                savedRequest.getRequestNumber(),
                canonicalRequestTypeCode(requestType),
                canonicalRequestTypeName(requestType),
                savedRequest.getTitle(),
                statusCode(savedRequest.getStatus()),
                statusLabel(savedRequest.getStatus()),
                savedRequest.getCreatedAt(),
                savedRequest.getUpdatedAt());
    }

    private RequestTypeEntity validateCreateRequest(
            CreateStudentRequestRequest request,
            List<MultipartFile> attachmentFiles) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        if (request == null) {
            addError(errors, "requestTypeCode", "Request type is required");
            throw new RequestValidationException(errors);
        }
        RequestTypeEntity requestType = null;
        if (!isPresent(request.typeCode())) {
            addError(errors, "requestTypeCode", "Request type is required");
        } else {
            requestType = findRequestTypeByClientCode(request.typeCode(), true).orElse(null);
            if (requestType == null) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Invalid request type");
            }
        }
        if (requestType != null) {
            validateFormData(requestType, buildFormData(request), errors);
            validateAttachmentRequirement(requestType, request.attachmentIds(), attachmentFiles, errors);
            validateAttachmentsExist(request.attachmentIds(), errors);
            validateUploadedAttachmentFiles(attachmentFiles);
        }
        if (!errors.isEmpty()) {
            throw new RequestValidationException(errors);
        }
        return requestType;
    }

    private Map<String, Object> buildFormData(CreateStudentRequestRequest request) {
        Map<String, Object> formData = new LinkedHashMap<>();
        if (request.formData() != null) {
            formData.putAll(request.formData());
        }
        putIfPresent(formData, "content", request.content());
        putIfPresent(formData, "startDate", request.startDate());
        putIfPresent(formData, "endDate", request.endDate());
        return formData;
    }

    private void putIfPresent(Map<String, Object> formData, String key, String value) {
        if (isPresent(value)) {
            formData.put(key, value.trim());
        }
    }

    private void validateFormData(
            RequestTypeEntity requestType,
            Map<String, Object> formData,
            Map<String, List<String>> errors) {
        Map<String, Object> resolvedFormData = formData == null ? Map.of() : formData;
        for (RequestTypeResponse.Field field : toRequestTypeFields(requestType)) {
            if (field.required() && isBlankValue(resolvedFormData.get(field.key()))) {
                addError(errors, field.key(), requiredMessage(field));
            }
        }
        if (requestType.isRequiresDateRange()) {
            validateDateRange(resolvedFormData, errors);
        }
    }

    private void validateDateRange(Map<String, Object> formData, Map<String, List<String>> errors) {
        Object startValue = firstPresent(formData.get("startDate"), formData.get("fromDate"));
        Object endValue = firstPresent(formData.get("endDate"), formData.get("toDate"));
        Optional<LocalDate> startDate = Optional.empty();
        Optional<LocalDate> endDate = Optional.empty();
        if (isBlankValue(startValue)) {
            addError(errors, "startDate", "Start date is required");
        } else {
            startDate = parseLocalDate(startValue, "startDate", errors);
        }
        if (isBlankValue(endValue)) {
            addError(errors, "endDate", "End date is required");
        } else {
            endDate = parseLocalDate(endValue, "endDate", errors);
        }
        if (startDate.isPresent() && endDate.isPresent() && endDate.get().isBefore(startDate.get())) {
            addError(errors, "endDate", "End date must be after or equal to start date");
        }
    }

    private Object firstPresent(Object first, Object second) {
        return isBlankValue(first) ? second : first;
    }

    private Optional<LocalDate> parseLocalDate(
            Object value,
            String field,
            Map<String, List<String>> errors) {
        if (isBlankValue(value)) {
            return Optional.empty();
        }
        String text = value.toString().trim();
        try {
            return Optional.of(LocalDate.parse(text));
        } catch (DateTimeParseException ignored) {
            // Try date-time formats below.
        }
        try {
            return Optional.of(LocalDateTime.parse(text).toLocalDate());
        } catch (DateTimeParseException ignored) {
            // Try the leading ISO date segment below.
        }
        if (text.length() >= 10) {
            try {
                return Optional.of(LocalDate.parse(text.substring(0, 10)));
            } catch (DateTimeParseException ignored) {
                // Fall through to validation error.
            }
        }
        addError(errors, field, "Date is invalid");
        return Optional.empty();
    }

    private void validateAttachmentRequirement(
            RequestTypeEntity requestType,
            List<String> attachmentIds,
            List<MultipartFile> attachmentFiles,
            Map<String, List<String>> errors) {
        if (requestType.isRequiresAttachment()
                && (attachmentIds == null || attachmentIds.isEmpty())
                && !hasUploadedFiles(attachmentFiles)) {
            addError(errors, "attachments", "Attachment is required");
        }
    }

    private boolean hasUploadedFiles(List<MultipartFile> attachmentFiles) {
        return attachmentFiles != null && attachmentFiles.stream().anyMatch(file -> file != null && !file.isEmpty());
    }

    private void validateAttachmentsExist(List<String> attachmentIds, Map<String, List<String>> errors) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }
        for (String attachmentId : attachmentIds) {
            if (!isPresent(attachmentId) || findUploadedFile(attachmentId).isEmpty()) {
                addError(errors, "attachmentIds", "Attachment is invalid");
                return;
            }
        }
    }

    private void validateUploadedAttachmentFiles(List<MultipartFile> attachmentFiles) {
        for (MultipartFile file : normalizedAttachmentFiles(attachmentFiles)) {
            if (file.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Attachment file is empty");
            }
            if (file.getSize() > MAX_ATTACHMENT_SIZE_BYTES) {
                throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Attachment file exceeds 10MB");
            }
            String extension = fileExtension(file.getOriginalFilename()).orElse("");
            if (!ALLOWED_ATTACHMENT_EXTENSIONS.contains(extension)) {
                throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Attachment file type is not supported");
            }
        }
    }

    private List<UploadedFileEntity> resolveAttachments(List<String> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, UploadedFileEntity> files = new LinkedHashMap<>();
        for (String attachmentId : attachmentIds) {
            findUploadedFile(attachmentId).ifPresent(file -> files.put(file.getId(), file));
        }
        return new ArrayList<>(files.values());
    }

    private Optional<UploadedFileEntity> findUploadedFile(String attachmentId) {
        String resolvedAttachmentId = attachmentId.trim();
        Optional<UploadedFileEntity> byCode = uploadedFileRepository.findByFileCode(resolvedAttachmentId);
        if (byCode.isPresent()) {
            return byCode;
        }
        try {
            return uploadedFileRepository.findById(UUID.fromString(resolvedAttachmentId));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private List<UploadedFileEntity> storeAttachments(UUID studentId, List<MultipartFile> attachmentFiles) {
        List<MultipartFile> files = normalizedAttachmentFiles(attachmentFiles);
        if (files.isEmpty()) {
            return List.of();
        }
        try {
            Files.createDirectories(studentRequestUploadDir);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot prepare attachment storage");
        }
        List<UploadedFileEntity> storedFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            storedFiles.add(storeAttachment(studentId, file));
        }
        return storedFiles;
    }

    private UploadedFileEntity storeAttachment(UUID studentId, MultipartFile file) {
        String extension = fileExtension(file.getOriginalFilename())
                .orElseThrow(() -> new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Attachment file type is not supported"));
        String fileCode = generateFileCode();
        String storedFileName = fileCode + "." + extension;
        Path target = studentRequestUploadDir.resolve(storedFileName).normalize();
        if (!target.startsWith(studentRequestUploadDir)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attachment file name is invalid");
        }
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot store attachment");
        }

        UploadedFileEntity uploadedFile = new UploadedFileEntity();
        uploadedFile.setFileCode(fileCode);
        uploadedFile.setFileName(safeOriginalFileName(file.getOriginalFilename(), extension));
        uploadedFile.setUrl(fileUrl(storedFileName));
        uploadedFile.setMimeType(contentType(file, extension));
        uploadedFile.setSize(file.getSize());
        uploadedFile.setPurpose("STUDENT_REQUEST");
        uploadedFile.setUploadedBy(studentId);
        return uploadedFileRepository.save(uploadedFile);
    }

    private List<MultipartFile> normalizedAttachmentFiles(List<MultipartFile> attachmentFiles) {
        if (attachmentFiles == null || attachmentFiles.isEmpty()) {
            return List.of();
        }
        return attachmentFiles.stream()
                .filter(file -> file != null)
                .toList();
    }

    private Optional<String> fileExtension(String filename) {
        if (!isPresent(filename)) {
            return Optional.empty();
        }
        String fileName = Paths.get(filename).getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return Optional.empty();
        }
        return Optional.of(fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT));
    }

    private String safeOriginalFileName(String originalFilename, String extension) {
        if (!isPresent(originalFilename)) {
            return "attachment." + extension;
        }
        String fileName = Paths.get(originalFilename).getFileName().toString()
                .replace('\\', '_')
                .replace('/', '_')
                .replace('\r', '_')
                .replace('\n', '_');
        return fileName.isBlank() ? "attachment." + extension : fileName;
    }

    private String fileUrl(String storedFileName) {
        if (studentRequestPublicPath.isBlank()) {
            return storedFileName;
        }
        return studentRequestPublicPath + "/" + storedFileName;
    }

    private String contentType(MultipartFile file, String extension) {
        if (isPresent(file.getContentType())) {
            return file.getContentType();
        }
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }

    private String generateFileCode() {
        String datePart = LocalDate.now(REQUEST_ZONE).format(REQUEST_NUMBER_DATE_FORMATTER);
        String fileCode;
        do {
            fileCode = "FILE-" + datePart + "-" + UUID.randomUUID()
                    .toString()
                    .substring(0, 8)
                    .toUpperCase(Locale.ROOT);
        } while (uploadedFileRepository.findByFileCode(fileCode).isPresent());
        return fileCode;
    }

    private RequestAttachmentEntity toAttachment(UUID requestId, UUID fileId) {
        RequestAttachmentEntity attachment = new RequestAttachmentEntity();
        attachment.setRequestId(requestId);
        attachment.setFileId(fileId);
        return attachment;
    }

    private RequestHistoryEntity toHistory(
            UUID requestId,
            StudentRequestStatus status,
            String note,
            UUID createdBy) {
        RequestHistoryEntity history = new RequestHistoryEntity();
        history.setRequestId(requestId);
        history.setStatus(status);
        history.setNote(note);
        history.setCreatedBy(createdBy);
        return history;
    }

    private String generateRequestNumber() {
        LocalDate today = LocalDate.now(REQUEST_ZONE);
        Instant start = today.atStartOfDay(REQUEST_ZONE).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(REQUEST_ZONE).toInstant();
        long count = requestRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end);
        String datePart = today.format(REQUEST_NUMBER_DATE_FORMATTER);
        long sequence = count + 1;
        String requestNumber = formatRequestNumber(datePart, sequence);
        while (requestRepository.existsByRequestNumber(requestNumber)) {
            sequence++;
            requestNumber = formatRequestNumber(datePart, sequence);
        }
        return requestNumber;
    }

    private String formatRequestNumber(String datePart, long sequence) {
        return "REQ-" + datePart + "-" + String.format(Locale.ROOT, "%03d", sequence);
    }

    private StudentRequestEntity findStudentRequest(UUID studentId, String requestId) {
        if (!isPresent(requestId)) {
            throw notFound("Request was not found");
        }
        try {
            UUID id = UUID.fromString(requestId);
            return requestRepository.findByIdAndStudentId(id, studentId)
                    .orElseThrow(() -> notFound("Request was not found"));
        } catch (IllegalArgumentException exception) {
            return requestRepository.findByRequestNumberAndStudentId(requestId, studentId)
                    .orElseThrow(() -> notFound("Request was not found"));
        }
    }

    private RequestTypeEntity requireRequestType(UUID typeId) {
        return requestTypeRepository.findById(typeId)
                .orElseThrow(() -> notFound("Request type was not found"));
    }

    private UserEntity requireUser(UUID studentId) {
        return userRepository.findById(studentId)
                .orElseThrow(() -> notFound("Student was not found"));
    }

    private Set<UUID> resolveTypeFilterIds(String typeCode) {
        if (!isPresent(typeCode)) {
            return Set.of();
        }
        return requestTypeCodeCandidates(typeCode).stream()
                .map(requestTypeRepository::findByCode)
                .flatMap(Optional::stream)
                .map(RequestTypeEntity::getId)
                .collect(Collectors.toSet());
    }

    private Optional<RequestTypeEntity> findRequestTypeByClientCode(String typeCode, boolean activeOnly) {
        return requestTypeCodeCandidates(typeCode).stream()
                .map(requestTypeRepository::findByCode)
                .flatMap(Optional::stream)
                .filter(requestType -> !activeOnly || requestType.isActive())
                .findFirst();
    }

    private List<String> requestTypeCodeCandidates(String typeCode) {
        if (!isPresent(typeCode)) {
            return List.of();
        }
        String trimmed = typeCode.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        String upper = trimmed.toUpperCase(Locale.ROOT);
        List<String> candidates = new ArrayList<>();
        candidates.add(trimmed);
        candidates.add(lower);
        candidates.add(upper);
        switch (lower) {
            case "absence" -> candidates.add("ABSENCE");
            case "confirmation", "student_confirmation" -> {
                candidates.add("confirmation");
                candidates.add("STUDENT_CONFIRMATION");
            }
            default -> {
            }
        }
        return candidates.stream().distinct().toList();
    }

    private StudentRequestListResponse emptyListResponse(int page, int limit) {
        Page<StudentRequestEntity> empty = new PageImpl<>(List.of(), PageRequest.of(page - 1, limit), 0);
        StudentRequestListResponse.Pagination pagination = new StudentRequestListResponse.Pagination(
                page,
                limit,
                empty.getTotalElements(),
                empty.getTotalPages());
        return new StudentRequestListResponse(
                List.of(),
                page,
                limit,
                empty.getTotalElements(),
                empty.getTotalPages(),
                pagination);
    }

    private Specification<StudentRequestEntity> studentRequestSpec(
            UUID studentId,
            StudentRequestStatus status,
            Set<UUID> typeIds,
            Instant fromInstant,
            Instant toInstant) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("studentId"), studentId));
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (typeIds != null && !typeIds.isEmpty()) {
                predicates.add(root.get("typeId").in(typeIds));
            }
            if (fromInstant != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), fromInstant));
            }
            if (toInstant != null) {
                predicates.add(criteriaBuilder.lessThan(root.get("createdAt"), toInstant));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Map<UUID, RequestTypeEntity> requestTypeMap(List<StudentRequestEntity> requests) {
        Set<UUID> typeIds = requests.stream()
                .map(StudentRequestEntity::getTypeId)
                .collect(Collectors.toSet());
        if (typeIds.isEmpty()) {
            return Map.of();
        }
        return requestTypeRepository.findAllById(typeIds).stream()
                .collect(Collectors.toMap(RequestTypeEntity::getId, Function.identity()));
    }

    private StudentRequestListResponse.Item toListItem(
            StudentRequestEntity request,
            RequestTypeEntity requestType) {
        return new StudentRequestListResponse.Item(
                request.getRequestNumber(),
                requestType == null ? null : canonicalRequestTypeCode(requestType),
                requestType == null ? null : canonicalRequestTypeName(requestType),
                request.getTitle(),
                statusCode(request.getStatus()),
                statusLabel(request.getStatus()),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }

    private RequestTypeResponse toRequestTypeResponse(RequestTypeEntity requestType) {
        return new RequestTypeResponse(
                canonicalRequestTypeCode(requestType),
                canonicalRequestTypeName(requestType),
                canonicalRequestTypeDescription(requestType),
                canonicalRequestTypeIconName(requestType),
                requestType.isRequiresDateRange(),
                requestType.isRequiresAttachment(),
                toRequestTypeFields(requestType));
    }

    private List<RequestTypeResponse.Field> toRequestTypeFields(RequestTypeEntity requestType) {
        String code = canonicalRequestTypeCode(requestType);
        if ("absence".equals(code)) {
            return List.of(new RequestTypeResponse.Field(
                    "reason",
                    "L\u00fd do ngh\u1ec9",
                    "textarea",
                    true));
        }
        if ("confirmation".equals(code)) {
            return List.of(new RequestTypeResponse.Field(
                    "purpose",
                    "M\u1ee5c \u0111\u00edch x\u00e1c nh\u1eadn",
                    "text",
                    true));
        }
        List<RequestTypeFieldValue> fields = requestType.getFields();
        if (fields == null || fields.isEmpty()) {
            return List.of();
        }
        return fields.stream()
                .filter(field -> !isDateRangeField(field.getKey()))
                .map(field -> new RequestTypeResponse.Field(
                        field.getKey(),
                        field.getLabel(),
                        field.getType(),
                        field.isRequired()))
                .toList();
    }

    private boolean isDateRangeField(String fieldKey) {
        if (fieldKey == null) {
            return false;
        }
        return switch (fieldKey) {
            case "fromDate", "toDate", "startDate", "endDate" -> true;
            default -> false;
        };
    }

    private String canonicalRequestTypeCode(RequestTypeEntity requestType) {
        if (requestType == null || requestType.getCode() == null) {
            return null;
        }
        String code = requestType.getCode().trim().toLowerCase(Locale.ROOT);
        return switch (code) {
            case "absence" -> "absence";
            case "student_confirmation", "confirmation" -> "confirmation";
            default -> code;
        };
    }

    private String canonicalRequestTypeName(RequestTypeEntity requestType) {
        return switch (canonicalRequestTypeCode(requestType)) {
            case "absence" -> "\u0110\u01a1n xin ngh\u1ec9 h\u1ecdc";
            case "confirmation" -> "\u0110\u01a1n x\u00e1c nh\u1eadn h\u1ecdc sinh";
            default -> requestType.getName();
        };
    }

    private String canonicalRequestTypeDescription(RequestTypeEntity requestType) {
        return switch (canonicalRequestTypeCode(requestType)) {
            case "absence" -> "G\u1eedi \u0111\u01a1n ngh\u1ec9 h\u1ecdc c\u00f3 ph\u00e9p cho gi\u00e1o vi\u00ean ch\u1ee7 nhi\u1ec7m.";
            case "confirmation" -> "Y\u00eau c\u1ea7u x\u00e1c nh\u1eadn th\u00f4ng tin h\u1ecdc sinh \u0111ang theo h\u1ecdc.";
            default -> requestType.getDescription();
        };
    }

    private String canonicalRequestTypeIconName(RequestTypeEntity requestType) {
        return switch (canonicalRequestTypeCode(requestType)) {
            case "absence" -> "absence";
            case "confirmation" -> "verified";
            default -> requestType.getIcon();
        };
    }

    private List<StudentRequestDetailResponse.Attachment> getAttachments(UUID requestId) {
        List<RequestAttachmentEntity> attachments = attachmentRepository.findByRequestId(requestId);
        if (attachments.isEmpty()) {
            return List.of();
        }
        Map<UUID, UploadedFileEntity> files = uploadedFileRepository
                .findAllById(attachments.stream().map(RequestAttachmentEntity::getFileId).toList())
                .stream()
                .collect(Collectors.toMap(UploadedFileEntity::getId, Function.identity()));
        return attachments.stream()
                .map(RequestAttachmentEntity::getFileId)
                .map(files::get)
                .filter(file -> file != null)
                .map(this::toAttachmentResponse)
                .toList();
    }

    private StudentRequestDetailResponse.Attachment toAttachmentResponse(UploadedFileEntity file) {
        return new StudentRequestDetailResponse.Attachment(
                file.getFileCode(),
                file.getFileName(),
                file.getUrl(),
                file.getMimeType(),
                file.getSize());
    }

    private StudentRequestDetailResponse.History toHistoryResponse(RequestHistoryEntity history) {
        return new StudentRequestDetailResponse.History(
                statusCode(history.getStatus()),
                statusLabel(history.getStatus()),
                history.getNote(),
                toOffsetDateTime(history.getCreatedAt()));
    }

    private String className(UserEntity student) {
        if (student.getClassId() == null) {
            return null;
        }
        return classRepository.findById(student.getClassId())
                .map(ClassEntity::getName)
                .orElse(null);
    }

    private StudentRequestStatus parseStatus(String status) {
        if (!isPresent(status)) {
            return null;
        }
        try {
            return StudentRequestStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            Map<String, List<String>> errors = new LinkedHashMap<>();
            addError(errors, "status", "Status is invalid");
            throw new RequestValidationException(errors);
        }
    }

    private void validatePagination(int page, int limit) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        if (page < 1) {
            addError(errors, "page", "Page is invalid");
        }
        if (limit < 1 || limit > 100) {
            addError(errors, "limit", "Limit is invalid");
        }
        if (!errors.isEmpty()) {
            throw new RequestValidationException(errors);
        }
    }

    private void addError(Map<String, List<String>> errors, String field, String message) {
        errors.computeIfAbsent(field, ignored -> new ArrayList<>()).add(message);
    }

    private String requiredMessage(RequestTypeResponse.Field field) {
        return switch (field.key()) {
            case "reason" -> "Reason is required";
            case "purpose" -> "Purpose is required";
            default -> field.label() + " is required";
        };
    }

    private boolean isBlankValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String text) {
            return text.isBlank();
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        return false;
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private java.time.OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atZone(REQUEST_ZONE).toOffsetDateTime();
    }

    private String statusCode(StudentRequestStatus status) {
        return status.name().toLowerCase(Locale.ROOT);
    }

    private String statusLabel(StudentRequestStatus status) {
        return switch (status) {
            case SUBMITTED -> "\u0110\u00e3 g\u1eedi";
            case PROCESSING -> "\u0110ang x\u1eed l\u00fd";
            case APPROVED -> "\u0110\u00e3 duy\u1ec7t";
            case REJECTED -> "T\u1eeb ch\u1ed1i";
            case CANCELLED -> "\u0110\u00e3 h\u1ee7y";
        };
    }

    private String trimTrailingSlash(String value) {
        if (!isPresent(value)) {
            return "/uploads/student-requests";
        }
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }
}
