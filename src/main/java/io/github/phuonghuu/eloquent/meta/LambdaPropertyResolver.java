package io.github.phuonghuu.eloquent.meta;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

final class LambdaPropertyResolver {

    private LambdaPropertyResolver() {
    }

    static String propertyName(SFunction<?, ?> function) {
        try {
            Method method = function.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(true);
            SerializedLambda lambda = (SerializedLambda) method.invoke(function);
            String methodName = lambda.getImplMethodName();
            if (methodName.startsWith("get") && methodName.length() > 3) {
                return decapitalize(methodName.substring(3));
            }
            if (methodName.startsWith("is") && methodName.length() > 2) {
                return decapitalize(methodName.substring(2));
            }
            return methodName;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException("Unable to resolve lambda property name", ex);
        }
    }

    private static String decapitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }
}

