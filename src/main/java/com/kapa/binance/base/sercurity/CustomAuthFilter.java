package com.kapa.binance.base.sercurity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapa.binance.base.constants.CodeConstant;
import com.kapa.binance.base.constants.MessageConstant;
import com.kapa.binance.base.constants.MgsConstant;
import com.kapa.binance.base.dto.CurrentUser;
import com.kapa.binance.base.exception.Ex400;
import com.kapa.binance.base.exception.Ex401;
import com.kapa.binance.base.response.BaseResponse;
import com.kapa.binance.base.utils.AesEncrypt;
import com.kapa.binance.config.EnvConfig;
import com.kapa.binance.entity.UserEntity;
import com.kapa.binance.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CustomAuthFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final EnvConfig envConfig;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        if (isWhitelisted(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            logger.info("User pre-authenticated...");
            filterChain.doFilter(request, response);
            return;
        }

        logger.info("New user authentication...");
        try {
            Optional<Authentication> authentication = extractCredentials(request.getHeader("Authorization"))
                    .flatMap(credentials -> authenticateUser(credentials, request));

            if (authentication.isPresent()) {
                SecurityContextHolder.getContext().setAuthentication(authentication.get());
                filterChain.doFilter(request, response);
                return;
            }

            writeErrorResponse(response, CodeConstant.CODE_401, MessageConstant.UNAUTHORIZED);
        } catch (Ex400 | Ex401 ex) {
            writeErrorResponse(response, ex.getCode(), ex.getMessage());
        }
    }

    private void writeErrorResponse(HttpServletResponse response, String code, String message) throws IOException {
        BaseResponse<?> errorResponse = BaseResponse.success(code, message);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
        response.getWriter().flush();
    }


    private boolean isWhitelisted(HttpServletRequest request) {
        return envConfig.getWhitelist().stream()
                .anyMatch(pattern -> new AntPathRequestMatcher(pattern).matches(request));
    }

    private boolean isMe(HttpServletRequest request) {
        return new AntPathRequestMatcher("/account/me").matches(request);
    }

    /**
     * Giải mã và tách credentials từ Header Authorization (Basic Auth).
     */
    private Optional<Credentials> extractCredentials(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            throw new Ex400("Missing Authorization header");
        }
        try {
            String base64Credentials = authHeader.substring(6);
            String decodedCredentials = new String(Base64.getDecoder().decode(base64Credentials));
            String[] values = decodedCredentials.split(":", 2);

            if (values.length != 2 || values[0].isEmpty() || values[1].isEmpty()) {
                throw new Ex400("Invalid Authorization header format");
            }

            return Optional.of(new Credentials(values[0], values[1]));
        } catch (IllegalArgumentException e) {
            throw new Ex400("Malformed Authorization header");
        }
    }

    /**
     * Xác thực user dựa trên UUID và password.
     */
    private Optional<Authentication> authenticateUser(Credentials credentials, HttpServletRequest request) {
        Optional<UserEntity> user = userRepository.findByUuid(credentials.uuid());

        if (user.isPresent()) {
            UserEntity entity = user.get();
            // Check if the password is valid
            if (isValidPassword(entity, credentials.password())) {
                // Check if the user is active
                if (entity.getIsActive() || isMe(request)) {
                    return Optional.of(new UsernamePasswordAuthenticationToken(
                            mapToCurrentUser(entity),
                            null,
                            null
                    ));
                } else {
                    throw new Ex401(MgsConstant.MGS_15);
                }
            } else {
                throw new Ex401(MgsConstant.MGS_09);
            }
        } else {
            throw new Ex401(MgsConstant.MGS_14);
        }
    }


    /**
     * Kiểm tra password có hợp lệ không.
     */
    private boolean isValidPassword(UserEntity user, String password) {
        String decryptedPass = AesEncrypt.decrypt(user.getPassPhrase(), envConfig.getSecretKey());
        return password.equals(decryptedPass);
    }

    /**
     * Chuyển đổi `UserEntity` thành `CurrentUser`.
     */
    private CurrentUser mapToCurrentUser(UserEntity user) {
        String passEncrypt = envConfig.getSecretKey();

        CurrentUser currentUser = new CurrentUser();
        currentUser.setId(user.getId());
        currentUser.setUuid(user.getUuid());
        currentUser.setApiKey(user.getApiKey());
        currentUser.setSecretKey(AesEncrypt.decrypt(user.getSecretKey(), passEncrypt));
        currentUser.setPassPhrase(AesEncrypt.decrypt(user.getPassPhrase(), passEncrypt));
        currentUser.setActive(user.getIsActive());
        currentUser.setFullName(user.getFullName());
        currentUser.setCreatedAt(user.getCreatedAt());
        currentUser.setUpdatedAt(user.getUpdatedAt());
        currentUser.setRole(user.getRole());
        return currentUser;
    }
}
