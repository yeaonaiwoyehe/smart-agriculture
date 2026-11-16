package cn.agrinav.measure.utils;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author 18506
 */
public class ThreadPoolUtils {
    //创建线程池
    public static ThreadPoolExecutor threadPoolExecutor  ;
    public static void initThreadPool() {
        if (threadPoolExecutor== null ||threadPoolExecutor.isShutdown()) {
            threadPoolExecutor = new ThreadPoolExecutor(
                    20,
                    30,
                    1440L,
                    TimeUnit.SECONDS,
                    new LinkedBlockingDeque<>(30),
                    new ThreadPoolExecutor.CallerRunsPolicy()
            ); // 使用调用者运行策略而不是终止策略;
        }
    }
    // 关闭线程池
    public static void shutdownThreadPool() {
        if (threadPoolExecutor!= null &&!threadPoolExecutor.isShutdown()) {
            threadPoolExecutor.shutdown();
        }
    }

}
