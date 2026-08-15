package com.employeemgmt.employeeservice.exception;

public class ProfilePictureNotFoundException extends RuntimeException {

    public ProfilePictureNotFoundException(Long employeeId) {
        super("No profile picture uploaded for employee id: " + employeeId);
    }
}
