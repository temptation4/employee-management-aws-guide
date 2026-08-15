package com.employeemgmt.employeeservice.service;

import com.employeemgmt.employeeservice.dto.EmployeeRequest;
import com.employeemgmt.employeeservice.dto.EmployeeResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EmployeeService {

    EmployeeResponse create(EmployeeRequest request);

    List<EmployeeResponse> getAll();

    EmployeeResponse getById(Long id);

    EmployeeResponse update(Long id, EmployeeRequest request);

    void delete(Long id);

    void uploadProfilePicture(Long id, MultipartFile file);

    String getProfilePictureUrl(Long id);

    void deleteProfilePicture(Long id);
}
