package edu.kangwon.university.taxicarpool.party.partyException;

public class MemberAlreadyInPartyException extends RuntimeException{
    public MemberAlreadyInPartyException(String message) {
        super(message);
    }

}
