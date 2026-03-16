package com.serv.controller;

import com.serv.database.entities.Client;
import com.serv.database.entities.VenusUser;
import com.serv.database.entities.Worker;
import jakarta.servlet.http.HttpSession;

// SessionAwareController.java
public abstract class SessionAwareController {

    protected VenusUser sessionUser(HttpSession session) {
        return (VenusUser) session.getAttribute("user");
    }

    protected Worker sessionWorker(HttpSession session) {
        VenusUser u = sessionUser(session);
        return (u instanceof Worker w) ? w : null;
    }

    protected Client sessionClient(HttpSession session) {
        VenusUser u = sessionUser(session);
        return (u instanceof Client c) ? c : null;
    }
}