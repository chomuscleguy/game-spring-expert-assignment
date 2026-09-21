package com.gameexpert.ws.dto;

import java.util.List;
import lombok.Getter;

@Getter
public class OnlineUsersResponse {
    private final String type = "onlineUsers";
    private final List<String> users;
    private final int count;

    public OnlineUsersResponse(List<String> users, int count) {
        this.users = users;
        this.count = count;
    }
}
