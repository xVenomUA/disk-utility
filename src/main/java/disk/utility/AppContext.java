// src/main/java/disk/utility/AppContext.java
package disk.utility;

import java.util.UUID;
import java.util.concurrent.*;

public final class AppContext {
    private AppContext() {
    }

    public static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread t = new Thread(r, "DU-" + UUID.randomUUID());
                t.setDaemon(true);
                return t;
            });
}
