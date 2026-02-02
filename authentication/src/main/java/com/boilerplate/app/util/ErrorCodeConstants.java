package com.boilerplate.app.util;

public class ErrorCodeConstants {

    private ErrorCodeConstants() {}

    public static class Success {
        public static final int HTTP_STATUS = 200;
        public static final String CATEGORY = "Success";
        public static final String SUBCODE_00 = "00";
        
        public static final String CODE_200_00 = "20000";
        public static final String MESSAGE_200_00 = "Successful";
        
        // B2B Access Token specific codes (service code 73)
        public static final String CODE_200_73_00 = "2007300";
        public static final String MESSAGE_200_73_00 = "Successful";
        
        public static final int HTTP_STATUS_202 = 202;
        public static final String CODE_202_00 = "20200";
        public static final String MESSAGE_202_00 = "Request In Progress";
        public static final String DESCRIPTION_202_00 = "Transaction still in process";
    }

    public static class System {
        public static final String CATEGORY = "System";
        
        // 400 Bad Request
        public static final int HTTP_STATUS_400 = 400;
        public static final String SUBCODE_00 = "00";
        public static final String CODE_400_00 = "40000";
        public static final String MESSAGE_400_00 = "Bad Request";
        public static final String DESCRIPTION_400_00 = "General request failed error, including message parsing failure";
        
        // 401 Unauthorized
        public static final int HTTP_STATUS_401 = 401;
        public static final String CODE_401_00 = "40100";
        public static final String MESSAGE_401_00 = "Unauthorized";
        public static final String DESCRIPTION_401_00 = "General unauthorized error (No Interface Def, API is Invalid, OAuth Failed, Verify Client Secret Fail, Client Forbidden Access API, Unknown Client, Key not Found)";
        
        public static final String SUBCODE_01 = "01";
        public static final String CODE_401_01 = "40101";
        public static final String MESSAGE_401_01 = "Invalid Token (B2B)";
        public static final String DESCRIPTION_401_01 = "Token found in request is invalid (Access Token Not Exist, Access Token Expired)";
        
        // B2B Access Token specific codes (service code 73)
        public static final String CODE_401_73_00 = "4017300";
        public static final String MESSAGE_401_73_00 = "Unauthorized. [Signature]";
        
        // 405 Method Not Allowed
        public static final int HTTP_STATUS_405 = 405;
        public static final String CODE_405_00 = "40500";
        public static final String MESSAGE_405_00 = "Requested Function Is Not Supported";
        
        // 409 Conflict
        public static final int HTTP_STATUS_409 = 409;
        public static final String CODE_409_01 = "40901";
        public static final String MESSAGE_409_01 = "Duplicate partnerReferenceNo";
        public static final String DESCRIPTION_409_01 = "Transaction has previously been processed, indicating the same partnerReferenceNo already succeeded";
        
        // 500 Internal Server Error
        public static final int HTTP_STATUS_500 = 500;
        public static final String CODE_500_00 = "50000";
        public static final String MESSAGE_500_00 = "General Error";
    }

    public static class Message {
        public static final String CATEGORY = "Message";
        public static final int HTTP_STATUS = 400;
        
        public static final String CODE_400_01 = "40001";
        public static final String MESSAGE_400_01 = "Invalid Field Format";
        public static final String DESCRIPTION_400_01 = "Invalid format";
        
        public static final String CODE_400_02 = "40002";
        public static final String MESSAGE_400_02 = "Invalid Mandatory Field";
        public static final String DESCRIPTION_400_02 = "Missing or invalid format on mandatory field";
    }

    public static class Business {
        public static final String CATEGORY = "Business";
        
        // 404 Not Found
        public static final int HTTP_STATUS_404 = 404;
        public static final String CODE_404_01 = "40401";
        public static final String MESSAGE_404_01 = "Transaction Not Found";
        public static final String DESCRIPTION_404_01 = "Transaction not found";
        
        public static final String CODE_404_08 = "40408";
        public static final String MESSAGE_404_08 = "Invalid Merchant";
        public static final String DESCRIPTION_404_08 = "Merchant does not exist or status abnormal";
        
        public static final String CODE_404_17 = "40417";
        public static final String MESSAGE_404_17 = "Invalid Terminal";
        public static final String DESCRIPTION_404_17 = "Terminal does not exist in the system";
        
        // 405 Method Not Allowed (Business)
        public static final int HTTP_STATUS_405 = 405;
        public static final String CODE_405_01 = "40501";
        public static final String MESSAGE_405_01 = "Requested Operation Is Not Allowed";
        public static final String DESCRIPTION_405_01 = "Requested operation to cancel/refund transaction is not allowed at this time";
    }

    public static String formatErrorCode(int httpStatus, String subCode) {
        return String.format("%d%s", httpStatus, subCode);
    }

    public static String getFullErrorCode(String category, int httpStatus, String subCode) {
        return String.format("%s %d any %s", category, httpStatus, subCode);
    }
}

