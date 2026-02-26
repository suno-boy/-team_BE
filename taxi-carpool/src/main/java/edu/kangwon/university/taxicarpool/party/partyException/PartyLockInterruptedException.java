package edu.kangwon.university.taxicarpool.party.partyException;

public class PartyLockInterruptedException extends RuntimeException {
    public PartyLockInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }
}