package edu.virginia.cs.abetvitae.extraction.controller;

import edu.virginia.cs.abetvitae.extraction.service.CvUploadRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice(assignableTypes = CvUploadController.class)
public class CvUploadExceptionHandler {

    @ExceptionHandler(CvUploadRejectedException.class)
    ProblemDetail handleRejectedUpload(CvUploadRejectedException exception) {
        return ProblemDetail.forStatusAndDetail(
                exception.status(),
                exception.getMessage()
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail handleMaximumUploadSize() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONTENT_TOO_LARGE,
                "The uploaded CV exceeds the configured size limit."
        );
    }
}
