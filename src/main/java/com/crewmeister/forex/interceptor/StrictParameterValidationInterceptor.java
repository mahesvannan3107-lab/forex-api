package com.crewmeister.forex.interceptor;

import com.crewmeister.forex.exception.InvalidParameterException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class StrictParameterValidationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;

            Set<String> allowedParams = new HashSet<>();
            Parameter[] parameters = handlerMethod.getMethod().getParameters();

            for (Parameter parameter : parameters) {
                RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
                if (requestParam != null) {
                    String paramName = requestParam.value().isEmpty() ?
                            requestParam.name().isEmpty() ? parameter.getName() : requestParam.name()
                            : requestParam.value();
                    allowedParams.add(paramName);
                }
            }

            Map<String, String[]> actualParams = request.getParameterMap();

            for (String actualParam : actualParams.keySet()) {
                if (!allowedParams.contains(actualParam)) {
                    throw new InvalidParameterException(
                            String.format("Unknown parameter '%s'. Allowed parameters: %s",
                                    actualParam, allowedParams)
                    );
                }
            }
        }

        return true;
    }
}

