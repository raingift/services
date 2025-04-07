package com.hermes.service.register;

import static java.util.Collections.emptyList;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Represents a registry of service provider
 *
 * @author hermes
 */
public class ServiceRegistry {

    private final static Map<String, List<String>> sRegistry = new HashMap<>();
    private final static Map<String, List<ImplEntry<?>>> sCache = new HashMap<>();

    /**
     * Returns the implementations of the specified type of service
     *
     * @param service The class of service
     * @param <S>     The type of service
     * @return The instances of all implementations
     */
    @SuppressWarnings("unchecked")
    public static <S> List<S> get(final Class<S> service) {
        final List<String> creators = sRegistry.getOrDefault(service.getName(), emptyList());
        if (creators.isEmpty()) {
            return emptyList();
        }

        try {
            final ArrayList<S> instances = new ArrayList<>(creators.size());
            for (final String callable : creators) {
                instances.add(createInstance(service.getName(), callable));
            }
            return Collections.unmodifiableList(instances);
        } catch (final Exception e) {
            throw new ServiceConfigurationError(service.getName(), e);
        }
    }

    /**
     * Returns a instance of the specified service
     *
     * @param service The class of service
     * @param <S>     The type of service
     * @return an instance of service
     */
    @SuppressWarnings("unchecked")
    public static <S> S single(final Class<S> service) {
        final Iterator<String> i = sRegistry.getOrDefault(service.getName(), emptyList()).iterator();
        if (i.hasNext()) {
            try {
                return (S) createInstance(service.getName(), i.next());
            } catch (final Exception e) {
                throw new ServiceConfigurationError(service.getName(), e);
            }
        }
        return null;
    }

    /**
     * Returns a instance of the specified service
     *
     * @param service    The class of service
     * @param comparator A comparator of service creator
     * @param <S>        The type of service
     * @return an instance of service
     */
    @SuppressWarnings("unchecked")
    public static <S> S single(final Class<S> service, final Comparator<String> comparator) {
        final ArrayList<String> creators = new ArrayList<>(sRegistry.getOrDefault(service.getName(), emptyList()));
        creators.sort(comparator);

        final Iterator<String> i = creators.iterator();
        if (i.hasNext()) {
            try {
                return (S) createInstance(service.getName(), i.next());
            } catch (final Exception e) {
                throw new ServiceConfigurationError(service.getName(), e);
            }
        }

        return null;
    }

    public static <S> S createInstance(String interfaceName, String creator) {
        // hit cache to provider impl instances
        List<ImplEntry<?>> entryList = sCache.getOrDefault(interfaceName, new ArrayList<>());
        if (!entryList.isEmpty()) {
            Map<String, ?> map = entryList.parallelStream().
                    collect(Collectors.toMap(implEntry -> implEntry.implName, implEntry -> implEntry.implClazz));
            Object implClazz = map.getOrDefault(creator, null);
            if (implClazz != null) {
                return (S) implClazz;
            }
        }

        Class<?> cls = null;
        S instance = null;
        try {
            cls = Class.forName(creator);
            Constructor<?> constructor = cls.getConstructor();
            instance = (S) (((Callable<S>) constructor.newInstance()).call());
            entryList.add(new ImplEntry<>(creator, instance));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return instance;
    }


    /**
     * Register a creator for the specified service
     *
     * @param service The class of service.name
     * @param impl The service impl.name
     * @param <S>     The type of service
     */
    public static <S> void register(final String service, final String impl) {
        sRegistry.computeIfAbsent(service, new Function<String, List<String>>() {
            @Override
            public List<String> apply(final String clazz) {
                return new ArrayList<>();
            }
        }).add(impl);
        sCache.computeIfAbsent(service, new Function<String, List<ImplEntry<?>>>() {
            @Override
            public List<ImplEntry<?>> apply(String s) {
                return new ArrayList<>();
            }
        });
    }


    private ServiceRegistry() {
    }
}
