// shared/logging/LoggingAspect.java
package com.agroruta.shared.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect {

    /**
     * Intercepta todos los métodos en los paquetes de aplicación y dominio.
     * Ajusta los paquetes según tu estructura real.
     */
    @Around("""
        execution(* com.agroruta..application..*(..)) ||
        execution(* com.agroruta..domain..*(..))
    """)
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Logger log = LoggerFactory.getLogger(joinPoint.getTarget().getClass());

        String methodName = signature.getName();
        Object[] args     = joinPoint.getArgs();

        log.debug("▶ {}() args={}", methodName, Arrays.toString(args));

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsed  = System.currentTimeMillis() - start;

            log.debug("◀ {}() completado en {}ms", methodName, elapsed);
            return result;

        } catch (Exception ex) {
            log.error("✗ {}() lanzó {}: {}", methodName, ex.getClass().getSimpleName(), ex.getMessage());
            throw ex; // re-lanzar para que el GlobalExceptionHandler lo maneje
        }
    }
}