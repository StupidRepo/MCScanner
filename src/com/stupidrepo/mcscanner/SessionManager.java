package com.stupidrepo.mcscanner;

public class SessionManager {
    public int foundThisSession = 0;

    public void serverFound() {
        ++foundThisSession;
    }
}
