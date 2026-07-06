package com.fschool.edu.fschool_backend.application.service;

import com.fschool.edu.fschool_backend.domain.enums.StudentRequestStatus;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClassEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.RequestAttachmentEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.RequestHistoryEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.RequestTypeFieldValue;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.RequestTypeEntity;
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
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentRequestService {

    private static final ZoneId REQUEST_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter REQUEST_NUMBER_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final RequestTypeJpaRepository requestTypeRepository;
    private final StudentRequestJpaRepository requestRepository;
    private final RequestAttachmentJpaRepository attachmentRepository;
    private final RequestHistoryJpaRepository historyRepository;
    private final UploadedFileJpaRepository uploadedFileRepository;
    private final UserJpaRepository userRepository;
    private final ClassJpaRepository classRepository;

    public StudentRequestService(
            RequestTypeJpaRepository requestTypeRepository,
            StudentRequestJpaRepository requestRepository,
            RequestAttachmentJpaRepository attachmentRepository,
            RequestHistoryJpaRepository historyRepository,
            UploadedFileJpaRepository uploadedFileRepository,
            UserJpaRepository userRepository,
            ClassJpaRepository classRepository) {
        this.requestTypeRepository = requestTypeRepository;
        this.requestRepository = requestRepository;
        this.attachmentRepository = attachmentRepository;
        this.historyRepository = historyRepository;
        this.uploadedFileRepository = uploadedFileRepository;
        this.userRepository = userRepository;
        this.classRepository = classRepository;
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
        Optional<RequestTypeEntity> typeFilter = resolveTypeFilter(typeCode);
        if (isPresent(typeCode) && typeFilter.isEmpty()) {
            return emptyListResponse(page, limit);
        }

        Instant fromInstant = fromDate == null ? null : fromDate.atStartOfDay(REQUEST_ZONE).toInstant();
        Instant toInstant = toDate == null ? null : toDate.plusDays(1).atStartOfDay(REQUEST_ZONE).toInstant();
        Specification<StudentRequestEntity> spec = studentRequestSpec(
                studentId,
                statusFilter,
                typeFilter.map(RequestTypeEntity::getId).orElse(null),
                fromInstant,
                toInstant);
        Page<StudentRequestEntity> result = requestRepository.findAll(
                spec,
                PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt")));
        Map<UUID, RequestTypeEntity> requestTypes = requestTypeMap(result.getContent());
        List<StudentRequestListResponse.Item> items = result.getContent().stream()
                .map(request -> toListItem(request, requestTypes.get(request.getTypeId())))
                .toList();
        return new StudentRequestListResponse(
                items,
                new StudentRequestListResponse.Pagination(
                        page,
                        limit,
                        result.getTotalElements(),
                        result.getTotalPages()));
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
                requestType.getCode(),
                requestType.getName(),
                request.getTitle(),
                request.getStatus().name(),
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
        RequestTypeEntity requestType = validateCreateRequest(request);
        Map<String, Object> formData = request.formData() == null ? Map.of() : request.formData();
        List<UploadedFileEntity> attachments = resolveAttachments(request.attachmentIds());

        StudentRequestEntity requestEntity = new StudentRequestEntity();
        requestEntity.setRequestNumber(generateRequestNumber());
        requestEntity.setStudentId(studentId);
        requestEntity.setTypeId(requestType.getId());
        requestEntity.setTitle(isPresent(request.title()) ? request.title().trim() : requestType.getName());
        requestEntity.setStatus(StudentRequestStatus.SUBMITTED);
        requestEntity.setFormData(new LinkedHashMap<>(formData));
        StudentRequestEntity savedRequest = requestRepository.save(requestEntity);

        attachments.stream()
                .map(file -> toAttachment(savedRequest.getId(), file.getId()))
                .forEach(attachmentRepository::save);
        historyRepository.save(toHistory(
                savedRequest.getId(),
                StudentRequestStatus.SUBMITTED,
                "Học sinh gửi đơn",
                studentId));

        return new CreateStudentRequestResponse(
                savedRequest.getRequestNumber(),
                requestType.getCode(),
                requestType.getName(),
                savedRequest.getTitle(),
                savedRequest.getStatus().name(),
                statusLabel(savedRequest.getStatus()),
                toOffsetDateTime(savedRequest.getCreatedAt()));
    }

    private RequestTypeEntity validateCreateRequest(CreateStudentRequestRequest request) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        if (request == null) {
            addError(errors, "typeCode", "Vui lòng chọn loại đơn");
            throw new RequestValidationException(errors);
        }
        RequestTypeEntity requestType = null;
        if (!isPresent(request.typeCode())) {
            addError(errors, "typeCode", "Vui lòng chọn loại đơn");
        } else {
            requestType = requestTypeRepository
                    .findByCode(normalizeCode(request.typeCode()))
                    .filter(RequestTypeEntity::isActive)
                    .orElse(null);
            if (requestType == null) {
                addError(errors, "typeCode", "Loại đơn không hợp lệ");
            }
        }
        if (requestType != null) {
            validateFormData(requestType, request.formData(), errors);
            validateAttachmentRequirement(requestType, request.attachmentIds(), errors);
            validateAttachmentsExist(request.attachmentIds(), errors);
        }
        if (!errors.isEmpty()) {
            throw new RequestValidationException(errors);
        }
        return requestType;
    }

    private void validateFormData(
            RequestTypeEntity requestType,
            Map<String, Object> formData,
            Map<String, List<String>> errors) {
        Map<String, Object> resolvedFormData = formData == null ? Map.of() : formData;
        for (RequestTypeResponse.Field field : toRequestTypeFields(requestType.getFields())) {
            if (field.required() && isBlankValue(resolvedFormData.get(field.key()))) {
                addError(errors, field.key(), requiredMessage(field));
            }
        }
        if (requestType.isRequiresDateRange()) {
            validateDateRange(resolvedFormData, errors);
        }
    }

    private void validateDateRange(Map<String, Object> formData, Map<String, List<String>> errors) {
        Optional<LocalDate> fromDate = parseLocalDate(formData.get("fromDate"), "fromDate", errors);
        Optional<LocalDate> toDate = parseLocalDate(formData.get("toDate"), "toDate", errors);
        if (fromDate.isPresent() && toDate.isPresent() && toDate.get().isBefore(fromDate.get())) {
            addError(errors, "toDate", "Đến ngày phải sau hoặc bằng từ ngày");
        }
    }

    private Optional<LocalDate> parseLocalDate(
            Object value,
            String field,
            Map<String, List<String>> errors) {
        if (isBlankValue(value)) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(value.toString()));
        } catch (DateTimeParseException exception) {
            addError(errors, field, "Ngày không hợp lệ");
            return Optional.empty();
        }
    }

    private void validateAttachmentRequirement(
            RequestTypeEntity requestType,
            List<String> attachmentIds,
            Map<String, List<String>> errors) {
        if (requestType.isRequiresAttachment() && (attachmentIds == null || attachmentIds.isEmpty())) {
            addError(errors, "attachmentIds", "Vui lòng đính kèm tệp");
        }
    }

    private void validateAttachmentsExist(List<String> attachmentIds, Map<String, List<String>> errors) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }
        for (String attachmentId : attachmentIds) {
            if (!isPresent(attachmentId) || findUploadedFile(attachmentId).isEmpty()) {
                addError(errors, "attachmentIds", "Tệp đính kèm không hợp lệ");
                return;
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
            throw notFound("Không tìm thấy đơn");
        }
        try {
            UUID id = UUID.fromString(requestId);
            return requestRepository.findByIdAndStudentId(id, studentId)
                    .orElseThrow(() -> notFound("Không tìm thấy đơn"));
        } catch (IllegalArgumentException exception) {
            return requestRepository.findByRequestNumberAndStudentId(requestId, studentId)
                    .orElseThrow(() -> notFound("Không tìm thấy đơn"));
        }
    }

    private RequestTypeEntity requireRequestType(UUID typeId) {
        return requestTypeRepository.findById(typeId)
                .orElseThrow(() -> notFound("Không tìm thấy loại đơn"));
    }

    private UserEntity requireUser(UUID studentId) {
        return userRepository.findById(studentId)
                .orElseThrow(() -> notFound("Không tìm thấy học sinh"));
    }

    private Optional<RequestTypeEntity> resolveTypeFilter(String typeCode) {
        if (!isPresent(typeCode)) {
            return Optional.empty();
        }
        return requestTypeRepository.findByCode(normalizeCode(typeCode));
    }

    private StudentRequestListResponse emptyListResponse(int page, int limit) {
        Page<StudentRequestEntity> empty = new PageImpl<>(List.of(), PageRequest.of(page - 1, limit), 0);
        return new StudentRequestListResponse(
                List.of(),
                new StudentRequestListResponse.Pagination(
                        page,
                        limit,
                        empty.getTotalElements(),
                        empty.getTotalPages()));
    }

    private Specification<StudentRequestEntity> studentRequestSpec(
            UUID studentId,
            StudentRequestStatus status,
            UUID typeId,
            Instant fromInstant,
            Instant toInstant) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("studentId"), studentId));
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (typeId != null) {
                predicates.add(criteriaBuilder.equal(root.get("typeId"), typeId));
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
                requestType == null ? null : requestType.getCode(),
                requestType == null ? null : requestType.getName(),
                request.getTitle(),
                request.getStatus().name(),
                statusLabel(request.getStatus()),
                toOffsetDateTime(request.getCreatedAt()),
                toOffsetDateTime(request.getUpdatedAt()));
    }

    private RequestTypeResponse toRequestTypeResponse(RequestTypeEntity requestType) {
        return new RequestTypeResponse(
                requestType.getCode(),
                requestType.getName(),
                requestType.getDescription(),
                requestType.getIcon(),
                requestType.isRequiresDateRange(),
                requestType.isRequiresAttachment(),
                toRequestTypeFields(requestType.getFields()));
    }

    private List<RequestTypeResponse.Field> toRequestTypeFields(List<RequestTypeFieldValue> fields) {
        if (fields == null || fields.isEmpty()) {
            return List.of();
        }
        return fields.stream()
                .map(field -> new RequestTypeResponse.Field(
                        field.getKey(),
                        field.getLabel(),
                        field.getType(),
                        field.isRequired()))
                .toList();
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
                history.getStatus().name(),
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
            addError(errors, "status", "Trạng thái không hợp lệ");
            throw new RequestValidationException(errors);
        }
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private void validatePagination(int page, int limit) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        if (page < 1) {
            addError(errors, "page", "Trang không hợp lệ");
        }
        if (limit < 1 || limit > 100) {
            addError(errors, "limit", "Số lượng mỗi trang không hợp lệ");
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
            case "reason" -> "Vui lòng nhập lý do";
            case "purpose" -> "Vui lòng nhập mục đích xác nhận";
            case "fromDate" -> "Vui lòng chọn từ ngày";
            case "toDate" -> "Vui lòng chọn đến ngày";
            default -> "Vui lòng nhập " + field.label().toLowerCase(Locale.ROOT);
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

    private String statusLabel(StudentRequestStatus status) {
        return switch (status) {
            case SUBMITTED -> "Đã gửi";
            case PROCESSING -> "Đang xử lý";
            case APPROVED -> "Đã duyệt";
            case REJECTED -> "Từ chối";
            case CANCELLED -> "Đã hủy";
        };
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }
}
