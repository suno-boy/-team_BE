package edu.kangwon.university.taxicarpool.party.partyException;

public class KakaoApiException extends RuntimeException {
    public KakaoApiException(String message) {
        super(message);
    }
    public KakaoApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
