package edu.kangwon.university.taxicarpool.party.partyException;

public class PartyServiceUnavailableException extends RuntimeException {
    public PartyServiceUnavailableException(String message) {
        super(message);
    }
}