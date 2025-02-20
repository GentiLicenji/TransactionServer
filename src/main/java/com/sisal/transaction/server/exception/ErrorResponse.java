package com.sisal.transaction.server.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Size;
import java.util.Objects;

/**
 * ErrorResponse
 */
@Validated
@javax.annotation.Generated(value = "com.glic.GentiSpringCodegen", date = "2025-02-20T13:46:50.094+01:00")


public class ErrorResponse {
    @JsonProperty("httpErrorCode")
    private String httpErrorCode = null;

    @JsonProperty("errorCode")
    private String errorCode = null;

    @JsonProperty("errorMessage")
    private String errorMessage = null;

    public ErrorResponse httpErrorCode(String httpErrorCode) {
        this.httpErrorCode = httpErrorCode;
        return this;
    }

    /**
     * Get httpErrorCode
     *
     * @return httpErrorCode
     **/
    @ApiModelProperty(value = "")


    public String getHttpErrorCode() {
        return httpErrorCode;
    }

    public void setHttpErrorCode(String httpErrorCode) {
        this.httpErrorCode = httpErrorCode;
    }

    public ErrorResponse errorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    /**
     * Get errorCode
     *
     * @return errorCode
     **/
    @ApiModelProperty(value = "")

    @Size(max = 10)
    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public ErrorResponse errorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    /**
     * Get errorMessage
     *
     * @return errorMessage
     **/
    @ApiModelProperty(value = "")

    @Size(max = 1024)
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ErrorResponse errorResponse = (ErrorResponse) o;
        return Objects.equals(this.httpErrorCode, errorResponse.httpErrorCode) &&
                Objects.equals(this.errorCode, errorResponse.errorCode) &&
                Objects.equals(this.errorMessage, errorResponse.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(httpErrorCode, errorCode, errorMessage);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ErrorResponse {\n");

        sb.append("    httpErrorCode: ").append(toIndentedString(httpErrorCode)).append("\n");
        sb.append("    errorCode: ").append(toIndentedString(errorCode)).append("\n");
        sb.append("    errorMessage: ").append(toIndentedString(errorMessage)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}

