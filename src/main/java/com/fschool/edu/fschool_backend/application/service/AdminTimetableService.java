package com.fschool.edu.fschool_backend.application.service;

import com.fschool.edu.fschool_backend.infrastructure.config.StudentRequestUploadProperties;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClassEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SchoolYearEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SemesterEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SubjectEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.TeacherProfileEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.TimetableEntryEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.UploadedFileEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ClassJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SchoolYearJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SemesterJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SubjectJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.TeacherProfileJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.TimetableEntryJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.UploadedFileJpaRepository;
import com.fschool.edu.fschool_backend.presentation.dto.response.AcademicYearFilterResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.AdminTimetableResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.ClassFilterResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.GradeLevelFilterResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.SemesterFilterResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.TimetableImportResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminTimetableService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String TIMETABLE_PURPOSE = "TIMETABLE";
    private static final String XLSX_EXTENSION = "xlsx";
    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final long MAX_IMPORT_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final String[] IMPORT_HEADERS = {
            "dayOfWeek",
            "period",
            "startTime",
            "endTime",
            "subjectCode",
            "teacherCode",
            "room"
    };
    private static final String UNKNOWN_SUBJECT = "Chưa rõ môn";

    private final TimetableEntryJpaRepository timetableRepository;
    private final SchoolYearJpaRepository schoolYearRepository;
    private final SemesterJpaRepository semesterRepository;
    private final ClassJpaRepository classRepository;
    private final SubjectJpaRepository subjectRepository;
    private final TeacherProfileJpaRepository teacherProfileRepository;
    private final UploadedFileJpaRepository uploadedFileRepository;
    private final StudentRequestUploadProperties uploadProperties;
    private final TeacherGradeAccessService teacherGradeAccessService;

    public AdminTimetableService(
            TimetableEntryJpaRepository timetableRepository,
            SchoolYearJpaRepository schoolYearRepository,
            SemesterJpaRepository semesterRepository,
            ClassJpaRepository classRepository,
            SubjectJpaRepository subjectRepository,
            TeacherProfileJpaRepository teacherProfileRepository,
            UploadedFileJpaRepository uploadedFileRepository,
            StudentRequestUploadProperties uploadProperties,
            TeacherGradeAccessService teacherGradeAccessService) {
        this.timetableRepository = timetableRepository;
        this.schoolYearRepository = schoolYearRepository;
        this.semesterRepository = semesterRepository;
        this.classRepository = classRepository;
        this.subjectRepository = subjectRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.uploadedFileRepository = uploadedFileRepository;
        this.uploadProperties = uploadProperties;
        this.teacherGradeAccessService = teacherGradeAccessService;
    }

    @Transactional(readOnly = true)
    public AdminTimetableResponse getTimetable(
            UUID academicYearId,
            UUID semesterId,
            Integer gradeLevel,
            UUID classId) {
        return getTimetable(academicYearId, semesterId, gradeLevel, classId, null, true);
    }

    @Transactional(readOnly = true)
    public AdminTimetableResponse getTimetable(
            UUID academicYearId,
            UUID semesterId,
            Integer gradeLevel,
            UUID classId,
            UUID currentUserId,
            boolean admin) {
        if (!admin) {
            return getTeacherTimetable(academicYearId, semesterId, currentUserId);
        }

        if (classId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "classId must not be null");
        }

        Optional<ClassEntity> classOptional = classRepository.findById(classId);
        if (classOptional.isEmpty()) {
            return AdminTimetableResponse.empty();
        }
        ClassEntity schoolClass = classOptional.get();
        if (gradeLevel != null && schoolClass.getGradeNumber().intValue() != gradeLevel) {
            return AdminTimetableResponse.empty();
        }

        UUID resolvedAcademicYearId = academicYearId == null ? schoolClass.getSchoolYearId() : academicYearId;
        if (!schoolClass.getSchoolYearId().equals(resolvedAcademicYearId)) {
            return AdminTimetableResponse.empty();
        }

        Optional<SchoolYearEntity> academicYear = schoolYearRepository.findById(resolvedAcademicYearId);
        Optional<SemesterEntity> semester = resolveSemester(resolvedAcademicYearId, semesterId);
        if (academicYear.isEmpty() || semester.isEmpty()) {
            return toResponse(academicYear.orElse(null), semester.orElse(null), schoolClass, List.of());
        }
        if (!semester.get().getSchoolYearId().equals(resolvedAcademicYearId)) {
            return AdminTimetableResponse.empty();
        }

        List<TimetableEntryEntity> entries = timetableRepository
                .findByClassIdAndSemesterIdOrderByDayOfWeekAscPeriodNoAsc(classId, semester.get().getId());
        return toResponse(academicYear.get(), semester.get(), schoolClass, entries);
    }

    private AdminTimetableResponse getTeacherTimetable(
            UUID academicYearId,
            UUID semesterId,
            UUID currentUserId) {
        Optional<SemesterEntity> requestedSemester = semesterId == null
                ? Optional.empty()
                : semesterRepository.findById(semesterId);
        Optional<SchoolYearEntity> academicYear = resolveAcademicYear(academicYearId, requestedSemester);
        if (academicYear.isEmpty()) {
            return AdminTimetableResponse.empty();
        }

        Optional<SemesterEntity> semester = semesterId == null
                ? resolveSemester(academicYear.get().getId(), null)
                : requestedSemester;
        if (semester.isEmpty()) {
            return toResponse(academicYear.get(), null, null, List.of());
        }
        if (!semester.get().getSchoolYearId().equals(academicYear.get().getId())) {
            return AdminTimetableResponse.empty();
        }

        List<TimetableEntryEntity> entries = teacherGradeAccessService
                .teachingEntriesForSemester(currentUserId, semester.get().getId());

        return toResponse(academicYear.get(), semester.get(), null, entries);
    }

    @Transactional(readOnly = true)
    public List<AcademicYearFilterResponse> getAcademicYears() {
        return schoolYearRepository.findAll(Sort.by(
                        Sort.Order.desc("startDate"),
                        Sort.Order.desc("name")))
                .stream()
                .map(year -> new AcademicYearFilterResponse(
                        year.getId(),
                        year.getName(),
                        Boolean.TRUE.equals(year.getCurrent())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SemesterFilterResponse> getSemesters(UUID academicYearId) {
        List<SemesterEntity> semesters = academicYearId == null
                ? semesterRepository.findAll()
                : semesterRepository.findBySchoolYearId(academicYearId);
        return semesters.stream()
                .sorted(Comparator
                        .comparing(SemesterEntity::getStartDate)
                        .thenComparing(SemesterEntity::getSemesterNo))
                .map(semester -> new SemesterFilterResponse(
                        semester.getId(),
                        semester.getName(),
                        semester.getSchoolYearId(),
                        semester.getStartDate(),
                        semester.getEndDate()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GradeLevelFilterResponse> getGradeLevels() {
        return classRepository.findDistinctGradeNumbers().stream()
                .map(Short::intValue)
                .map(grade -> new GradeLevelFilterResponse(grade, "Khối " + grade))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClassFilterResponse> getClasses(Integer gradeLevel) {
        List<ClassEntity> classes = gradeLevel == null
                ? classRepository.findAll(Sort.by(Sort.Order.asc("gradeNumber"), Sort.Order.asc("name")))
                : classRepository.findByGradeNumberOrderByNameAsc(gradeLevel.shortValue());
        return classes.stream()
                .map(schoolClass -> new ClassFilterResponse(
                        schoolClass.getId(),
                        schoolClass.getName(),
                        schoolClass.getGradeNumber()))
                .toList();
    }

    @Transactional
    public TimetableImportResponse importTimetable(
            MultipartFile file,
            UUID academicYearId,
            UUID semesterId,
            UUID classId,
            UUID uploadedBy) {
        validateImportRequest(academicYearId, semesterId, classId);
        validateImportFile(file);
        byte[] fileContent = readFile(file);

        SchoolYearEntity academicYear = schoolYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Academic year was not found"));
        SemesterEntity semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Semester was not found"));
        ClassEntity schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Class was not found"));
        if (!semester.getSchoolYearId().equals(academicYear.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "semesterId does not belong to academicYearId");
        }
        if (!schoolClass.getSchoolYearId().equals(academicYear.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "classId does not belong to academicYearId");
        }

        ImportParseResult parseResult = parseImportRows(fileContent);
        if (parseResult.totalRows() == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Timetable file does not contain data");
        }

        UploadedFileEntity uploadedFile = storeTimetableFile(uploadedBy, file, fileContent);
        upsertTimetableRows(parseResult.validRows(), classId, semesterId);

        int failedRows = (int) parseResult.errors().stream()
                .map(TimetableImportResponse.ImportError::rowNumber)
                .distinct()
                .count();
        return new TimetableImportResponse(
                uploadedFile.getId(),
                uploadedFile.getFileName(),
                parseResult.totalRows(),
                parseResult.validRows().size(),
                failedRows,
                importStatus(parseResult.validRows().size(), failedRows),
                parseResult.errors());
    }

    public byte[] exportImportTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Timetable");
            Row header = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            for (int i = 0; i < IMPORT_HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(IMPORT_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            Object[][] sampleRows = {
                    {1, 1, "07:00", "07:45", "MATH", "GVDEMO", "A101"},
                    {1, 2, "07:50", "08:35", "ENGLISH", "GVDEMO", "A101"},
                    {2, 1, "07:00", "07:45", "LITERATURE", "GVDEMO", "A101"}
            };
            for (int rowIndex = 0; rowIndex < sampleRows.length; rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                for (int columnIndex = 0; columnIndex < sampleRows[rowIndex].length; columnIndex++) {
                    Object value = sampleRows[rowIndex][columnIndex];
                    Cell cell = row.createCell(columnIndex);
                    if (value instanceof Number number) {
                        cell.setCellValue(number.doubleValue());
                    } else {
                        cell.setCellValue(String.valueOf(value));
                    }
                }
            }
            sheet.createFreezePane(0, 1);
            for (int i = 0; i < IMPORT_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot export timetable template");
        }
    }

    private void validateImportRequest(UUID academicYearId, UUID semesterId, UUID classId) {
        if (academicYearId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "academicYearId must not be null");
        }
        if (semesterId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "semesterId must not be null");
        }
        if (classId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "classId must not be null");
        }
    }

    private void validateImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Timetable file is required");
        }
        if (file.getSize() > MAX_IMPORT_SIZE_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Timetable file exceeds 10MB");
        }
        if (!XLSX_EXTENSION.equals(fileExtension(file.getOriginalFilename()).orElse(null))) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only .xlsx files are supported");
        }
    }

    private byte[] readFile(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Timetable file is invalid");
        }
    }

    private ImportParseResult parseImportRows(byte[] fileContent) {
        List<SubjectEntity> subjects = subjectRepository.findAll();
        Map<String, SubjectEntity> subjectsByCode = subjects.stream()
                .collect(Collectors.toMap(
                        subject -> normalizeLookup(subject.getCode()),
                        Function.identity(),
                        (current, duplicate) -> current));
        Map<String, TeacherProfileEntity> teachersByCode = teacherProfileRepository.findAll().stream()
                .collect(Collectors.toMap(
                        teacher -> normalizeLookup(teacher.getEmployeeCode()),
                        Function.identity(),
                        (current, duplicate) -> current));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(fileContent))) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Timetable file does not contain data");
            }
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            List<ParsedTimetableRow> validRows = new ArrayList<>();
            List<TimetableImportResponse.ImportError> errors = new ArrayList<>();
            Set<TimetableSlot> slots = new HashSet<>();
            int totalRows = 0;

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isBlankRow(row, formatter)) {
                    continue;
                }
                totalRows++;
                int rowNumber = rowIndex + 1;
                int errorStart = errors.size();

                Short dayOfWeek = parseDayOfWeek(row, rowNumber, formatter, errors);
                Short period = parsePeriod(row, rowNumber, formatter, errors);
                LocalTime startTime = parseTime(row, rowNumber, 2, "startTime", formatter, errors);
                LocalTime endTime = parseTime(row, rowNumber, 3, "endTime", formatter, errors);
                String subjectCode = cellText(row, 4, formatter);
                SubjectEntity subject = findSubject(subjectCode, subjectsByCode);
                if (!hasText(subjectCode)) {
                    errors.add(new TimetableImportResponse.ImportError(
                            rowNumber,
                            "subjectCode",
                            "subjectCode must not be blank"));
                } else if (subject == null) {
                    errors.add(new TimetableImportResponse.ImportError(
                            rowNumber,
                            "subjectCode",
                            "Môn học không tồn tại"));
                }
                String teacherCode = cellText(row, 5, formatter);
                TeacherProfileEntity teacher = findTeacher(teacherCode, teachersByCode);
                if (!hasText(teacherCode)) {
                    errors.add(new TimetableImportResponse.ImportError(
                            rowNumber,
                            "teacherCode",
                            "teacherCode must not be blank"));
                } else if (teacher == null) {
                    errors.add(new TimetableImportResponse.ImportError(
                            rowNumber,
                            "teacherCode",
                            "Giáo viên không tồn tại"));
                }
                String room = cellText(row, 6, formatter);

                if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
                    errors.add(new TimetableImportResponse.ImportError(
                            rowNumber,
                            "endTime",
                            "endTime must be after startTime"));
                }
                if (dayOfWeek != null && period != null && !slots.add(new TimetableSlot(dayOfWeek, period))) {
                    errors.add(new TimetableImportResponse.ImportError(
                            rowNumber,
                            "period",
                            "Duplicate dayOfWeek and period"));
                }

                if (errors.size() == errorStart) {
                    validRows.add(new ParsedTimetableRow(
                            dayOfWeek,
                            period,
                            startTime,
                            endTime,
                            subject,
                            teacher == null ? null : teacher.getUserId(),
                            teacher == null ? null : teacher.getFullName(),
                            room));
                }
            }

            return new ImportParseResult(totalRows, validRows, errors);
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Timetable file is invalid");
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Timetable file is invalid");
        }
    }

    private void upsertTimetableRows(List<ParsedTimetableRow> rows, UUID classId, UUID semesterId) {
        if (rows.isEmpty()) {
            return;
        }
        List<TimetableEntryEntity> entities = rows.stream()
                .map(row -> {
                    TimetableEntryEntity entity = timetableRepository
                            .findByClassIdAndSemesterIdAndDayOfWeekAndPeriodNo(
                                    classId,
                                    semesterId,
                                    row.dayOfWeek(),
                                    row.period())
                            .orElseGet(TimetableEntryEntity::new);
                    entity.setClassId(classId);
                    entity.setSemesterId(semesterId);
                    entity.setDayOfWeek(row.dayOfWeek());
                    entity.setPeriodNo(row.period());
                    entity.setStartTime(row.startTime());
                    entity.setEndTime(row.endTime());
                    entity.setSubjectId(row.subject().getId());
                    entity.setTeacherId(row.teacherId());
                    entity.setTeacherName(row.teacherName());
                    entity.setRoomName(row.room());
                    return entity;
                })
                .toList();
        timetableRepository.saveAll(entities);
    }

    private UploadedFileEntity storeTimetableFile(UUID uploadedBy, MultipartFile file, byte[] fileContent) {
        String fileCode = generateFileCode();
        String storedFileName = fileCode + "." + XLSX_EXTENSION;
        Path timetableUploadDir = uploadProperties.timetableDirPath();
        Path target = timetableUploadDir.resolve(storedFileName).normalize();
        if (!target.startsWith(timetableUploadDir)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Timetable file name is invalid");
        }
        try {
            Files.createDirectories(timetableUploadDir);
            Files.write(target, fileContent);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot store timetable file");
        }

        UploadedFileEntity uploadedFile = new UploadedFileEntity();
        uploadedFile.setFileCode(fileCode);
        uploadedFile.setFileName(safeOriginalFileName(file.getOriginalFilename()));
        uploadedFile.setUrl(timetableFileUrl(storedFileName));
        uploadedFile.setMimeType(XLSX_CONTENT_TYPE);
        uploadedFile.setSize(file.getSize());
        uploadedFile.setPurpose(TIMETABLE_PURPOSE);
        uploadedFile.setUploadedBy(uploadedBy);
        return uploadedFileRepository.save(uploadedFile);
    }

    private SubjectEntity findSubject(
            String subjectCode,
            Map<String, SubjectEntity> subjectsByCode) {
        if (hasText(subjectCode)) {
            return subjectsByCode.get(normalizeLookup(subjectCode));
        }
        return null;
    }

    private TeacherProfileEntity findTeacher(
            String teacherCode,
            Map<String, TeacherProfileEntity> teachersByCode) {
        if (!hasText(teacherCode)) {
            return null;
        }
        return teachersByCode.get(normalizeLookup(teacherCode));
    }

    private Short parseDayOfWeek(
            Row row,
            int rowNumber,
            DataFormatter formatter,
            List<TimetableImportResponse.ImportError> errors) {
        Cell cell = row.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            errors.add(new TimetableImportResponse.ImportError(rowNumber, "dayOfWeek", "dayOfWeek must not be blank"));
            return null;
        }
        Integer numericValue = numericInteger(cell);
        if (numericValue != null) {
            return validateShortRange(numericValue, rowNumber, "dayOfWeek", 1, 7, errors);
        }

        String text = cellText(row, 0, formatter);
        String normalized = normalizeLookup(text);
        if ("chu nhat".equals(normalized) || "sunday".equals(normalized) || "cn".equals(normalized)) {
            return 7;
        }
        for (int day = 2; day <= 7; day++) {
            if (normalized.equals("thu " + day) || normalized.equals("thu" + day)) {
                return (short) (day - 1);
            }
        }
        try {
            return validateShortRange(Integer.parseInt(text), rowNumber, "dayOfWeek", 1, 7, errors);
        } catch (NumberFormatException exception) {
            errors.add(new TimetableImportResponse.ImportError(
                    rowNumber,
                    "dayOfWeek",
                    "dayOfWeek must be between 1 and 7"));
            return null;
        }
    }

    private Short parsePeriod(
            Row row,
            int rowNumber,
            DataFormatter formatter,
            List<TimetableImportResponse.ImportError> errors) {
        Cell cell = row.getCell(1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            errors.add(new TimetableImportResponse.ImportError(rowNumber, "period", "period must not be blank"));
            return null;
        }
        Integer numericValue = numericInteger(cell);
        if (numericValue != null) {
            return validateShortRange(numericValue, rowNumber, "period", 1, 15, errors);
        }
        String text = cellText(row, 1, formatter);
        try {
            return validateShortRange(Integer.parseInt(text), rowNumber, "period", 1, 15, errors);
        } catch (NumberFormatException exception) {
            errors.add(new TimetableImportResponse.ImportError(
                    rowNumber,
                    "period",
                    "period must be between 1 and 15"));
            return null;
        }
    }

    private Short validateShortRange(
            int value,
            int rowNumber,
            String column,
            int min,
            int max,
            List<TimetableImportResponse.ImportError> errors) {
        if (value < min || value > max) {
            errors.add(new TimetableImportResponse.ImportError(
                    rowNumber,
                    column,
                    column + " must be between " + min + " and " + max));
            return null;
        }
        return (short) value;
    }

    private LocalTime parseTime(
            Row row,
            int rowNumber,
            int columnIndex,
            String column,
            DataFormatter formatter,
            List<TimetableImportResponse.ImportError> errors) {
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            errors.add(new TimetableImportResponse.ImportError(rowNumber, column, column + " must not be blank"));
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return numericTime(cell, rowNumber, column, errors);
        }
        String text = cellText(row, columnIndex, formatter);
        for (DateTimeFormatter timeFormatter : List.of(
                DateTimeFormatter.ofPattern("H:mm"),
                DateTimeFormatter.ofPattern("HH:mm"),
                DateTimeFormatter.ofPattern("H:mm:ss"),
                DateTimeFormatter.ofPattern("HH:mm:ss"))) {
            try {
                return LocalTime.parse(text, timeFormatter).withSecond(0).withNano(0);
            } catch (DateTimeParseException ignored) {
            }
        }
        errors.add(new TimetableImportResponse.ImportError(
                rowNumber,
                column,
                column + " must use HH:mm format"));
        return null;
    }

    private LocalTime numericTime(
            Cell cell,
            int rowNumber,
            String column,
            List<TimetableImportResponse.ImportError> errors) {
        double value = cell.getNumericCellValue();
        if (DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalTime().withSecond(0).withNano(0);
        }
        if (value >= 0 && value < 1) {
            int seconds = (int) Math.round(value * 24 * 60 * 60);
            if (seconds == 24 * 60 * 60) {
                seconds = 0;
            }
            return LocalTime.ofSecondOfDay(seconds).withSecond(0).withNano(0);
        }
        errors.add(new TimetableImportResponse.ImportError(
                rowNumber,
                column,
                column + " must use HH:mm format"));
        return null;
    }

    private Integer numericInteger(Cell cell) {
        if (cell.getCellType() != CellType.NUMERIC || DateUtil.isCellDateFormatted(cell)) {
            return null;
        }
        double value = cell.getNumericCellValue();
        int integerValue = (int) value;
        return value == integerValue ? integerValue : null;
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        if (row == null) {
            return true;
        }
        for (int i = 0; i < IMPORT_HEADERS.length; i++) {
            if (hasText(cellText(row, i, formatter))) {
                return false;
            }
        }
        return true;
    }

    private String cellText(Row row, int index, DataFormatter formatter) {
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? null : formatter.formatCellValue(cell).trim();
    }

    private String importStatus(int successRows, int failedRows) {
        if (failedRows == 0) {
            return "SUCCESS";
        }
        return successRows == 0 ? "FAILED" : "PARTIAL_SUCCESS";
    }

    private Optional<String> fileExtension(String filename) {
        if (!hasText(filename)) {
            return Optional.empty();
        }
        String fileName = Paths.get(filename).getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return Optional.empty();
        }
        return Optional.of(fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT));
    }

    private String safeOriginalFileName(String originalFilename) {
        if (!hasText(originalFilename)) {
            return "timetable." + XLSX_EXTENSION;
        }
        String fileName = Paths.get(originalFilename).getFileName().toString()
                .replace('\\', '_')
                .replace('/', '_')
                .replace('\r', '_')
                .replace('\n', '_');
        return fileName.isBlank() ? "timetable." + XLSX_EXTENSION : fileName;
    }

    private String timetableFileUrl(String storedFileName) {
        String publicPath = trimTrailingSlash(uploadProperties.getTimetablePublicPath());
        if (publicPath.isBlank()) {
            return storedFileName;
        }
        return publicPath + "/" + storedFileName;
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String generateFileCode() {
        return "TIMETABLE-" + UUID.randomUUID();
    }

    private Optional<SemesterEntity> resolveSemester(UUID academicYearId, UUID semesterId) {
        if (semesterId != null) {
            return semesterRepository.findById(semesterId);
        }
        return semesterRepository.findBySchoolYearIdAndCurrentTrue(academicYearId)
                .or(() -> semesterRepository.findBySchoolYearId(academicYearId).stream()
                        .min(Comparator.comparing(SemesterEntity::getStartDate)));
    }

    private Optional<SchoolYearEntity> resolveAcademicYear(
            UUID academicYearId,
            Optional<SemesterEntity> requestedSemester) {
        if (academicYearId != null) {
            return schoolYearRepository.findById(academicYearId);
        }
        if (requestedSemester.isPresent()) {
            return schoolYearRepository.findById(requestedSemester.get().getSchoolYearId());
        }
        return schoolYearRepository.findByCurrentTrue();
    }

    private AdminTimetableResponse toResponse(
            SchoolYearEntity academicYear,
            SemesterEntity semester,
            ClassEntity schoolClass,
            List<TimetableEntryEntity> entries) {
        Map<UUID, SubjectEntity> subjectsById = subjectMap(entries);
        Map<UUID, ClassEntity> classesById = classMap(entries);
        Map<String, UUID> teacherIdsByName = teacherIdsByName(entries);
        Map<Short, List<TimetableEntryEntity>> entriesByDay = entries.stream()
                .collect(Collectors.groupingBy(
                        TimetableEntryEntity::getDayOfWeek,
                        TreeMap::new,
                        Collectors.toList()));

        List<AdminTimetableResponse.Day> days = entriesByDay.entrySet().stream()
                .map(entry -> new AdminTimetableResponse.Day(
                        entry.getKey(),
                        dayName(entry.getKey()),
                        entry.getValue().stream()
                                .sorted(Comparator.comparing(TimetableEntryEntity::getPeriodNo))
                                .map(lesson -> toLesson(lesson, subjectsById, classesById, teacherIdsByName))
                                .toList()))
                .toList();

        return new AdminTimetableResponse(
                academicYear == null ? null : new AdminTimetableResponse.AcademicYear(
                        academicYear.getId(),
                        academicYear.getName()),
                semester == null ? null : new AdminTimetableResponse.Semester(
                        semester.getId(),
                        semester.getName()),
                schoolClass == null ? null : new AdminTimetableResponse.ClassInfo(
                        schoolClass.getId(),
                        schoolClass.getName(),
                        schoolClass.getGradeNumber()),
                days);
    }

    private AdminTimetableResponse.Lesson toLesson(
            TimetableEntryEntity entry,
            Map<UUID, SubjectEntity> subjectsById,
            Map<UUID, ClassEntity> classesById,
            Map<String, UUID> teacherIdsByName) {
        SubjectEntity subject = subjectsById.get(entry.getSubjectId());
        ClassEntity schoolClass = classesById.get(entry.getClassId());
        return new AdminTimetableResponse.Lesson(
                entry.getPeriodNo(),
                time(entry.getStartTime()),
                time(entry.getEndTime()),
                entry.getSubjectId(),
                subject == null ? UNKNOWN_SUBJECT : subject.getName(),
                entry.getTeacherId() == null
                        ? teacherIdsByName.get(normalizeName(entry.getTeacherName()))
                        : entry.getTeacherId(),
                entry.getTeacherName(),
                entry.getRoomName(),
                entry.getClassId(),
                schoolClass == null ? null : schoolClass.getName(),
                schoolClass == null ? null : schoolClass.getGradeNumber().intValue());
    }

    private Map<UUID, SubjectEntity> subjectMap(List<TimetableEntryEntity> entries) {
        return subjectRepository.findAllById(entries.stream()
                        .map(TimetableEntryEntity::getSubjectId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(SubjectEntity::getId, Function.identity()));
    }

    private Map<UUID, ClassEntity> classMap(List<TimetableEntryEntity> entries) {
        Set<UUID> classIds = entries.stream()
                .map(TimetableEntryEntity::getClassId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (classIds.isEmpty()) {
            return Map.of();
        }
        return classRepository.findAllById(classIds).stream()
                .collect(Collectors.toMap(ClassEntity::getId, Function.identity()));
    }

    private Map<String, UUID> teacherIdsByName(List<TimetableEntryEntity> entries) {
        List<String> teacherNames = entries.stream()
                .map(TimetableEntryEntity::getTeacherName)
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .distinct()
                .toList();
        if (teacherNames.isEmpty()) {
            return Map.of();
        }

        Map<String, List<TeacherProfileEntity>> teachersByName = teacherProfileRepository.findByFullNameIn(teacherNames)
                .stream()
                .collect(Collectors.groupingBy(teacher -> normalizeName(teacher.getFullName())));
        Map<String, UUID> uniqueTeacherIds = new LinkedHashMap<>();
        teachersByName.forEach((name, teachers) -> {
            if (teachers.size() == 1) {
                uniqueTeacherIds.put(name, teachers.getFirst().getUserId());
            }
        });
        return uniqueTeacherIds;
    }

    private String dayName(Short dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "Thứ 2";
            case 2 -> "Thứ 3";
            case 3 -> "Thứ 4";
            case 4 -> "Thứ 5";
            case 5 -> "Thứ 6";
            case 6 -> "Thứ 7";
            case 7 -> "Chủ nhật";
            default -> "Thứ " + dayOfWeek;
        };
    }

    private String time(LocalTime time) {
        return time == null ? null : TIME_FORMATTER.format(time);
    }

    private String normalizeName(String value) {
        return normalizeLookup(value);
    }

    private String normalizeLookup(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.replaceAll("\\s+", " ");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ImportParseResult(
            int totalRows,
            List<ParsedTimetableRow> validRows,
            List<TimetableImportResponse.ImportError> errors) {
    }

    private record ParsedTimetableRow(
            Short dayOfWeek,
            Short period,
            LocalTime startTime,
            LocalTime endTime,
            SubjectEntity subject,
            UUID teacherId,
            String teacherName,
            String room) {
    }

    private record TimetableSlot(Short dayOfWeek, Short period) {
    }
}
