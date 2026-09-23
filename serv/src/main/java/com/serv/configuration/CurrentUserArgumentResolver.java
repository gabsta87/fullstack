package com.serv.configuration;

import com.serv.database.entities.VenusUser;
import com.serv.database.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserRepository userRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return VenusUser.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String email = jwt.getSubject();

            // 1. On récupère le VenusUser (qui peut être un Client, Worker ou Admin)
            VenusUser user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable."));

            // 2. On regarde quel type de paramètre le contrôleur attend (ex: Client, Admin, VenusUser)
            Class<?> requiredType = parameter.getParameterType();

            // 3. Si le type attendu n'est pas compatible avec l'utilisateur connecté
            if (!requiredType.isInstance(user)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Accès refusé : Le type d'utilisateur (" + user.getClass().getSimpleName() +
                                ") ne correspond pas à la ressource demandée (" + requiredType.getSimpleName() + ").");
            }

            return user;
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié.");
    }
}