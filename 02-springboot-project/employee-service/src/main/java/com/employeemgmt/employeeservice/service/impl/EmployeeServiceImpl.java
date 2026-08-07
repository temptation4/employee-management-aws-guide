package com.employeemgmt.employeeservice.service.impl;

import com.employeemgmt.employeeservice.dto.EmployeeRequest;
import com.employeemgmt.employeeservice.dto.EmployeeResponse;
import com.employeemgmt.employeeservice.entity.Employee;
import com.employeemgmt.employeeservice.exception.EmployeeNotFoundException;
import com.employeemgmt.employeeservice.repository.EmployeeRepository;
import com.employeemgmt.employeeservice.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;

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
                employee.getDesignation()
        );
    }
}
