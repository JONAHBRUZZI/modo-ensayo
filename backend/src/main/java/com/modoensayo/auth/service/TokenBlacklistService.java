package com.modoensayo.auth.service;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();

    public void invalidate(String token) {
        blacklist.add(token);
    }

    public boolean isInvalidated(String token) {
        return blacklist.contains(token);
    }

    public void cleanup() {
        blacklist.clear();
    }
}
