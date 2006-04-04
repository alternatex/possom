package no.schibstedsok.front.searchportal.executor;

import java.util.concurrent.ThreadPoolExecutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author <a href="mailto:magnus.eklund@schibsted.no">Magnus Eklund</a>
 * @version <tt>$Revision$</tt>
 */
public class ThreadPoolInspector extends TimerTask {

    private ThreadPoolExecutor threadPool;

    public ThreadPoolInspector(ThreadPoolExecutor threadPool, int msPeriod) {
        this.threadPool = threadPool;
        Timer t = new Timer();
        LOG.info("Scheduling to run every " + msPeriod + "ms");
        t.schedule(this, 0, msPeriod);
    }

    private static final Log LOG = LogFactory.getLog(ThreadPoolInspector.class);

    public void run() {
        LOG.info("Thread pool size: " + threadPool.getPoolSize());
        LOG.info("Largest size: " + threadPool.getLargestPoolSize());
        LOG.info("Active threads: " + threadPool.getActiveCount());
        LOG.info("Approx. task count: " + threadPool.getTaskCount());
        LOG.info("Completed count: " + threadPool.getCompletedTaskCount());
    }
}
