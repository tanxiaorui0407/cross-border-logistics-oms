package com.logistics.oms.module.auth.dto;

import lombok.Data;

@Data
public class LoginResp {

    private String token;

    private UserInfo user;

    @Data
    public static class UserInfo {
        private Long userId;
        private String username;
        private String role;
        private Long customerId;

        public UserInfo() {
        }

        public UserInfo(Long userId, String username, String role, Long customerId) {
            this.userId = userId;
            this.username = username;
            this.role = role;
            this.customerId = customerId;
        }
    }
}
