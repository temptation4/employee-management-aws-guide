package com.employeemgmt.employeeservice.service.impl;

import com.employeemgmt.employeeservice.dto.EmployeeRequest;
import com.employeemgmt.employeeservice.dto.EmployeeResponse;
import com.employeemgmt.employeeservice.entity.Employee;
import com.employeemgmt.employeeservice.exception.EmployeeNotFoundException;
import com.employeemgmt.employeeservice.exception.ProfilePictureNotFoundException;
import com.employeemgmt.employeeservice.repository.EmployeeRepository;
import com.employeemgmt.employeeservice.service.EmployeeService;
import com.employeemgmt.employeeservice.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final S3Service s3Service;

    @Override
    public EmployeeResponse create(EmployeeRequest request) {
        Employee employee = toEntity(request);
        return toResponse(employeeRepository.save(employee));
    }

    @Override
    public List<EmployeeResponse> getAll() {
        return employeeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public EmployeeResponse getById(Long id) {
        return toResponse(findEmployeeOrThrow(id));
    }

    @Override
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee existing = findEmployeeOrThrow(id);
        existing.setName(request.getName());
        existing.setEmail(request.getEmail());
        existing.setDepartment(request.getDepartment());
        existing.setDesignation(request.getDesignation());
        return toResponse(employeeRepository.save(existing));
    }

    @Override
    public void delete(Long id) {
        employeeRepository.delete(findEmployeeOrThrow(id));
    }

    @Override
    public void uploadProfilePicture(Long id, MultipartFile file) {
        Employee employee = findEmployeeOrThrow(id);

        String objectKey = "employees/" + id + "/" + file.getOriginalFilename();
        s3Service.uploadFile(file, objectKey);

        employee.setProfilePictureKey(objectKey);
        employeeRepository.save(employee);
    }

    @Override
    public String getProfilePictureUrl(Long id) {
        Employee employee = findEmployeeOrThrow(id);

        if (employee.getProfilePictureKey() == null) {
            throw new ProfilePictureNotFoundException(id);
        }

        return s3Service.generatePreSignedUrl(employee.getProfilePictureKey());
    }

    @Override
    public void deleteProfilePicture(Long id) {
        Employee employee = findEmployeeOrThrow(id);

        if (employee.getProfilePictureKey() != null) {
            s3Service.deleteFile(employee.getProfilePictureKey());
            employee.setProfilePictureKey(null);
            employeeRepository.save(employee);
        }
    }

    private Employee findEmployeeOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    private Employee toEntity(EmployeeRequest request) {
        Employee employee = new Employee();
        employee.setName(request.getName());
        employee.setEmail(request.getEmail());
        employee.setDepartment(request.getDepartment());
        employee.setDesignation(request.getDesignation());
        return employee;
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                employee.getDepartment(),
                employee.getDesignation(),
                employee.getProfilePictureKey() != null
        );
    }
}
