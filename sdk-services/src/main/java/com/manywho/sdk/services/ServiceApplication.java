package com.manywho.sdk.services;

import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import org.jboss.resteasy.plugins.guice.ext.RequestScopeModule;
import org.reflections.Reflections;
import ru.vyarus.guice.validator.ImplicitValidationModule;

import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ServiceApplication extends Application {
    protected Injector injector;

    protected Module module;

    public ServiceApplication() {

    }

    public ServiceApplication(Injector injector) {
        this.injector = injector;
    }

    public void setModule(Module module) {
        this.module = module;
    }

    public void initialize() {
        final List<Module> modules = Lists.newArrayList();

        modules.add(new ImplicitValidationModule());
        modules.add(new RequestScopeModule());
        modules.add(new ServiceApplicationModule(this.getClass().getPackage().getName()));

        if (module == null) {
            injector = Guice.createInjector(modules);
        } else {
            injector = Guice.createInjector(Modules.override(modules).with(module));
        }
    }

    @Override
    public Set<Object> getSingletons() {
        final Set<Object> objects = new HashSet<>();
        Reflections reflections = getReflections(this.getClass().getPackage().getName());
        Reflections reflections2 = getReflections("com.manywho.sdk.services");

        addToObjects(objects, reflections.getTypesAnnotatedWith(Path.class));
        addToObjects(objects, reflections.getTypesAnnotatedWith(Provider.class));
        addToObjects(objects, reflections2.getTypesAnnotatedWith(Path.class));
        addToObjects(objects, reflections2.getTypesAnnotatedWith(Provider.class));

        return objects;
    }

    protected void addToObjects(Set<Object> objects, Set<Class<?>> classes) {
        objects.addAll(classes.stream().map(c -> injector.getInstance(c)).collect(Collectors.toSet()));
    }

    protected Reflections getReflections(String packageName) {
        ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration(packageName);
        ReflectionsProvider reflectionsProvider = new ReflectionsProvider(applicationConfiguration);
        return reflectionsProvider.get();
    }

}
