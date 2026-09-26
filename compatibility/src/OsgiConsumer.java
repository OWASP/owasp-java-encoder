package consumer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.apache.felix.framework.FrameworkFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;

/** The framework host supplies API libraries; encoder code comes only from bundles. */
public final class OsgiConsumer {
    public static void main(String[] args) throws Exception {
        try {
            Class.forName("org.owasp.encoder.Encode");
            throw new AssertionError("Encoder leaked onto the framework host classpath");
        } catch (ClassNotFoundException expected) {
            // The only encoder implementation must be the installed artifact.
        }
        Map<String, String> config = new HashMap<String, String>();
        config.put(Constants.FRAMEWORK_STORAGE, args[0]);
        config.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, args[1]);
        config.put("felix.service.urlhandlers", "false");
        // "--expect-unresolved" installs an older core and the adapter, which must
        // then fail to resolve against it instead of wiring (#137).
        boolean expectUnresolved = "--expect-unresolved".equals(args[2]);
        Framework framework = new FrameworkFactory().newFramework(config);
        framework.init();
        try {
            framework.start();
            for (int i = 3; i < args.length; ++i) {
                Bundle bundle = framework.getBundleContext().installBundle(new File(args[i]).toURI().toString());
                if (expectUnresolved && i == args.length - 1) {
                    try {
                        bundle.start();
                    } catch (BundleException expected) {
                        String message = String.valueOf(expected.getMessage());
                        if (bundle.getState() == Bundle.ACTIVE || !message.contains("org.owasp.encoder")) {
                            throw new AssertionError("Unexpected failure: " + message, expected);
                        }
                        System.out.println("Rejected as expected: " + message);
                        return;
                    }
                    throw new AssertionError(bundle.getSymbolicName() + " wired to an unsupported core");
                }
                bundle.start();
                if (bundle.getState() != Bundle.ACTIVE) throw new AssertionError(bundle);
                if (i == args.length - 1) {
                    bundle.loadClass(args[2]).getMethod("main", String[].class)
                        .invoke(null, (Object) new String[0]);
                }
            }
        } finally {
            framework.stop();
            if (framework.waitForStop(5000).getType() != FrameworkEvent.STOPPED) {
                throw new AssertionError("Framework did not stop");
            }
        }
    }
}
