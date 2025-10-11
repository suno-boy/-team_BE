package edu.kangwon.university.taxicarpool.party.partyException;

public class DuplicatedPartyNameException extends RuntimeException {
    public DuplicatedPartyNameException(String message) {
        super(message);
    }
}
