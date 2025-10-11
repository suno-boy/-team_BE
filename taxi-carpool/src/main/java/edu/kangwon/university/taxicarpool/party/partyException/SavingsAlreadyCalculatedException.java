package edu.kangwon.university.taxicarpool.party.partyException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class SavingsAlreadyCalculatedException extends RuntimeException {
    public SavingsAlreadyCalculatedException(String message) {
        super(message);
    }
}
