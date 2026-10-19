package br.com.papaya.dto.response;

import br.com.papaya.enums.ContributorTrustStatus;
import br.com.papaya.enums.UserRole;

public record UserResponse(Long id, String email, String displayName, UserRole role,
                           ContributorTrustStatus contributorTrustStatus) {
}
