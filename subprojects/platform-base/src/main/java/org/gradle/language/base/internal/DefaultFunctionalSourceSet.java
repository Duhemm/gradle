/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.language.base.internal;

import com.google.common.base.Joiner;
import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.internal.NoFactoryRegisteredForTypeException;
import org.gradle.api.internal.rules.AddOnlyRuleAwarePolymorphicDomainObjectContainer;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.language.base.FunctionalSourceSet;
import org.gradle.language.base.LanguageSourceSet;
import org.gradle.language.base.ProjectSourceSet;
import org.gradle.language.base.internal.registry.LanguageRegistration;
import org.gradle.language.base.internal.registry.LanguageRegistry;

import java.io.File;

import static com.google.common.base.Strings.emptyToNull;

public class DefaultFunctionalSourceSet extends AddOnlyRuleAwarePolymorphicDomainObjectContainer<LanguageSourceSet> implements FunctionalSourceSet {
    private final String name;
    private final LanguageRegistry languageRegistry;
    private final File baseDir;

    public DefaultFunctionalSourceSet(String name, Instantiator instantiator, final ProjectSourceSet projectSourceSet, LanguageRegistry languageRegistry, File baseDir) {
        super(LanguageSourceSet.class, instantiator);
        this.name = name;
        this.languageRegistry = languageRegistry;
        this.baseDir = baseDir;
        whenObjectAdded(new Action<LanguageSourceSet>() {
            public void execute(LanguageSourceSet languageSourceSet) {
                projectSourceSet.add(languageSourceSet);
            }
        });
    }

    @Override
    protected <U extends LanguageSourceSet> U doCreate(String name, Class<U> type) {
        U languageSourceSet = createLanguageSourceSet(name, type);
        return languageSourceSet;
    }

    @Override
    public void maybeAddDefaultSrcDirs(LanguageSourceSet languageSourceSet) {
        if (languageSourceSet.getSource().getSrcDirs().isEmpty()) {
            String defaultSourceDir = calculateDefaultPath(languageSourceSet);
            languageSourceSet.getSource().srcDir(defaultSourceDir);
        }
    }

    private <U extends LanguageSourceSet> U createLanguageSourceSet(String name, Class<U> type) {
        NamedDomainObjectFactory<? extends LanguageSourceSet> sourceSetFactory = findSourceSetFactory(type);
        if (sourceSetFactory == null) {
            throw new InvalidUserDataException(
                String.format("Cannot create a %s because this type is not known to %s. Known types are: %s", type.getSimpleName(), getDisplayName(), languageRegistry.getSupportedTypeNames()),
                new NoFactoryRegisteredForTypeException());
        }
        return type.cast(sourceSetFactory.create(name));
    }

    private String calculateDefaultPath(LanguageSourceSet languageSourceSet) {
        return Joiner.on(File.separator).skipNulls().join(baseDir.getPath(), emptyToNull(languageSourceSet.getSourceDirConvention()), emptyToNull(languageSourceSet.getParentName()), emptyToNull(languageSourceSet.getName()));
    }

    private <U extends LanguageSourceSet> NamedDomainObjectFactory<? extends LanguageSourceSet> findSourceSetFactory(Class<U> type) {
        for (LanguageRegistration<?> languageRegistration : languageRegistry) {
            Class<? extends LanguageSourceSet> sourceSetType = languageRegistration.getSourceSetType();
            if (type.equals(sourceSetType)) {
                return languageRegistration.getSourceSetFactory(getName());
            }
        }
        return null;
    }


    @Override
    public String getDisplayName() {
        return FunctionalSourceSet.class.getSimpleName();
    }

    @Override
    public String toString() {
        return String.format("source set '%s'", name);
    }

    public String getName() {
        return name;
    }
}
