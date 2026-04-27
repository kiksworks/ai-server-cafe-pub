package ai_server_cafe.updater;

import ai_server_cafe.exception.TestErrorException;
import ai_server_cafe.util.interfaces.AbstractCloneable;
import ai_server_cafe.util.interfaces.IFuncParam1;
import ai_server_cafe.util.interfaces.InterfaceHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TestUpdater {
    private static final List<Class<?>> UPDATER_CLASSES_BLACKLIST =
            List.of(AbstractFilteredUpdater.class, AbstractUpdater.class, UpdaterBall.class,
                    UpdaterRobot.class, TestUpdater.class, UpdaterScoreBoard.ScoreUpdateListener.class, ExcludedTest.class);
    private static final Logger LOGGER = LogManager.getLogger("test-updater");

    public static Set<Class<?>> listClasses(String packageName, String resourcePath) {
        final String resourceName = resourcePath;
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final URL root = classLoader.getResource(resourceName);
        LOGGER.info("testing at {}", root);
        if ("file".equals(root.getProtocol())) {
            File[] files = new File(root.getFile()).listFiles((dir, name) -> name.endsWith(".class"));
            return Arrays.asList(files).stream()
                    .map(file -> file.getName())
                    .map(name -> name.replaceAll(".class$", ""))
                    .map(name -> packageName + "." + name)
                    .map(fullName -> uncheckCall(() -> Class.forName(fullName)))
                    .collect(Collectors.toSet());
        }
        if ("jar".equals(root.getProtocol())) {
            try (JarFile jarFile = ((JarURLConnection) root.openConnection()).getJarFile()) {
                return Collections.list(jarFile.entries()).stream()
                        .map(jarEntry -> jarEntry.getName())
                        .filter(name -> name.startsWith(resourceName))
                        .filter(name -> name.endsWith(".class"))
                        .map(name -> name.replace('/', '.').replaceAll(".class$", ""))
                        .map(fullName -> uncheckCall(() -> classLoader.loadClass(fullName)))
                        .collect(Collectors.toSet());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new HashSet<>();
    }

    public static <T> T uncheckCall(Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    public static List<Class<?>> getUpdatersClazz() {
        List<Class<?>> result = new ArrayList<>(listClasses("ai_server_cafe.updater", "../main/ai_server_cafe/updater"));
        result.removeIf(InterfaceHelper.getPredicate(new IFuncParam1<Boolean, Class<?>>() {
            @Override
            public Boolean function(Class<?> aClass) {
                return aClass.getCanonicalName() == null || UPDATER_CLASSES_BLACKLIST.contains(aClass);
            }
        }));
        return result;
    }

    @Test
    public void isUpdaterMethodCloneable() throws TestErrorException {
        label:
        for (Class<?> clazz : getUpdatersClazz()) {
            for (Annotation annotation : clazz.getAnnotations()) {
                if (annotation instanceof ExcludedTest) {
                    continue label;
                }
            }
            label2:
            for (Method method : clazz.getDeclaredMethods()) {
                for (Annotation annotation : method.getAnnotations()) {
                    if (annotation instanceof ExcludedTest) {
                        continue label2;
                    }
                }
                int mod = method.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isPrivate(mod) || Modifier.isProtected(mod)) continue;
                Class<?> typeReturn = method.getReturnType();
                if (typeReturn == void.class || typeReturn.isPrimitive() || typeReturn.isEnum() ||
                        typeReturn.isRecord() || AbstractCloneable.class.isAssignableFrom(typeReturn) || typeReturn == String.class) {
                    // No problem
                } else {
                    throw new TestErrorException("The method '" + method.getName() +
                            "' return type must be any of the below : \n1. void\n2. primitive\n3. enum\n4. record\n5. String\n" +
                            "6. a Class extends 'AbstractCloneable'\nor the method must be " +
                            "declared as 'private' or 'protected' or 'static' at : '" + clazz.getCanonicalName() + "'");
                }
            }
        }
    }

    @Test
    public void isUpdaterMethodSynchronized() throws TestErrorException {
        label2:
        for (Class<?> clazz : getUpdatersClazz()) {
            for (Annotation annotation : clazz.getAnnotations()) {
                if (annotation instanceof ExcludedTest) {
                    continue label2;
                }
            }
            label:
            for (Method method : clazz.getDeclaredMethods()) {
                int mod = method.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isPrivate(mod) || Modifier.isProtected(mod)) continue;
                for (Annotation annotation : method.getAnnotations()) {
                    if (annotation instanceof ExcludedTest) {
                        continue label;
                    }
                }
                if (!Modifier.isSynchronized(mod)) {
                    throw new TestErrorException(
                            "The method : '" + method.getName() + "' must be declared as 'synchronized' at : '" +
                                    clazz.getCanonicalName() + "'");
                }
            }
        }
    }

    @Test
    public void isFinalUpdater() throws TestErrorException {
        label:
        for (Class<?> clazz : getUpdatersClazz()) {
            for (Annotation annotation : clazz.getAnnotations()) {
                if (annotation instanceof ExcludedTest) {
                    continue label;
                }
            }
            if (!Modifier.isFinal(clazz.getModifiers())) {
                throw new TestErrorException(
                        clazz.getCanonicalName() + " must be declared as 'final' at : '" + clazz.getCanonicalName() +
                                "'");
            }
        }
    }

    @Test
    public void hasUpdaterMethodInstanceFactory() throws TestErrorException {
        label:
        for (Class<?> clazz : getUpdatersClazz()) {
            for (Annotation annotation : clazz.getAnnotations()) {
                if (annotation instanceof ExcludedTest) {
                    continue label;
                }
            }
            boolean flag = false;
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals("getInstance")) {
                    int mod = method.getModifiers();
                    if (Modifier.isPublic(mod) && Modifier.isStatic(mod) && Modifier.isSynchronized(mod)) {
                        LOGGER.info("Exists instance factory at : {}", clazz.getCanonicalName());
                        flag = true;
                        break;
                    }
                }
            }
            if (!flag)
                throw new TestErrorException("There is no public static synchronized getInstance() at : '" + clazz.getCanonicalName() + "'");
            flag = false;
            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                if (Modifier.isPrivate(constructor.getModifiers())) {
                    LOGGER.info("Exists private constructor at : {}", clazz.getCanonicalName());
                    flag = true;
                    break;
                }
            }
            if (!flag)
                throw new TestErrorException("There is no private constructor at : '" + clazz.getCanonicalName() + "'");
        }
    }

    @Test
    public void getUpdaterInstance() throws InvocationTargetException, IllegalAccessException {
        label:
        for (Class<?> clazz : getUpdatersClazz()) {
            for (Annotation annotation : clazz.getAnnotations()) {
                if (annotation instanceof ExcludedTest) {
                    continue label;
                }
            }
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals("getInstance")) {
                    assertNotNull(method.invoke(null),
                            "The instance factor is null : '" + clazz.getCanonicalName() + "'");
                }
            }
        }
    }
}
