package consumer;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Assert the actual packaged descriptors, including transitive API readability. */
public final class ModuleMetadata {
    public static void main(String[] args) {
        String[] modules = {"owasp.encoder", "owasp.encoder.jsp", "owasp.encoder.jakarta", "owasp.encoder.esapi"};
        String[] packages = {"org.owasp.encoder", "org.owasp.encoder.tag", "org.owasp.encoder.tag", "org.owasp.encoder.esapi"};
        String[] apis = {null, "javax.servlet.jsp.api", "jakarta.servlet.jsp", "esapi"};
        for (int i = 0; i < modules.length; ++i) {
            ModuleDescriptor module = ModuleFinder.of(Paths.get(args[i])).find(modules[i]).orElseThrow(AssertionError::new).descriptor();
            if (module.isAutomatic() || module.isOpen()) throw new AssertionError(module);
            Set<String> requires = new HashSet<>(Collections.singleton("java.base"));
            if (apis[i] != null) {
                requires.add("owasp.encoder");
                requires.add(apis[i]);
            }
            if (!requires.equals(module.requires().stream().map(ModuleDescriptor.Requires::name).collect(Collectors.toSet()))) {
                throw new AssertionError(module);
            }
            for (ModuleDescriptor.Requires requirement : module.requires()) {
                Set<ModuleDescriptor.Requires.Modifier> expected = Collections.emptySet();
                if (requirement.name().equals("java.base")) expected = Collections.singleton(ModuleDescriptor.Requires.Modifier.MANDATED);
                if (requirement.name().equals(apis[i])) expected = Collections.singleton(ModuleDescriptor.Requires.Modifier.TRANSITIVE);
                if (!expected.equals(requirement.modifiers())) throw new AssertionError(requirement);
            }
            if (module.exports().size() != 1 || !module.exports().iterator().next().source().equals(packages[i])
                    || module.exports().iterator().next().isQualified() || !module.opens().isEmpty()
                    || !module.uses().isEmpty() || !module.provides().isEmpty()) throw new AssertionError(module);
            System.out.println("Descriptor passed: " + module.name());
        }
    }
}
