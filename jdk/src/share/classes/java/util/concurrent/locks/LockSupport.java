/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.locks;
import sun.misc.Unsafe;

/**
 * 创建锁和其他同步类的基本线程阻塞原语。
 *
 * 这个类与使用它的每个线程关联一个许可证(在信号量类的意义上)。
 * 如果获得许可证，调用park将立即返回，并在此过程中消耗掉它;否则它可能阻塞。
 * 如果尚未获得许可证，则调用unpark将使许可证可用。(与信号量不同，许可证不会累积。最多只有一个。)
 *
 * 方法park和unpark提供了有效的阻塞和非阻塞线程的方法，这些方法不会遇到导致废弃方法线程的问题
 * s Thread.suspend and Thread.resume:一个线程调用park和另一个线程unpark将保持线程活性，
 * 由于许可证。另外，如果调用者的线程被中断，park将返回，并且支持超时版本。
 * park方法也可以在任何其他时间返回，因为“没有原因”，所以通常必须在返回时在循环中重新检查条件。
 * 在这个意义上，park作为一个“繁忙的等待”的优化，不浪费太多的时间旋转，但必须配合unpark是有效的。
 *
 * park的三种形式也都支持blocker对象参数。这个对象在线程被阻塞时进行记录，
 * 以允许监视和诊断工具识别线程被阻塞的原因。(这些工具可能会使用getBlocker(Thread)方法访问阻塞程序。)
 * 强烈建议使用这些表单而不是没有此参数的原始表单。在锁实现中作为阻塞器提供的正常参数是这样的。
 *
 *  <pre> {@code
 * while (!canProceed()) { ... LockSupport.park(this); }}</pre>
 *
 * 在调用park之前，既不能继续操作，也不能执行任何其他操作，从而导致锁定或阻塞。因为每个线程只有一个许可证.
 *
 * <p><b>Sample Usage.</b> Here is a sketch of a first-in-first-out
 * non-reentrant lock class:
 *  <pre> {@code
 * class FIFOMutex {
 *   private final AtomicBoolean locked = new AtomicBoolean(false);
 *   private final Queue<Thread> waiters
 *     = new ConcurrentLinkedQueue<Thread>();
 *
 *   public void lock() {
 *     boolean wasInterrupted = false;
 *     Thread current = Thread.currentThread();
 *     waiters.add(current);
 *
 *     // Block while not first in queue or cannot acquire lock
 *     while (waiters.peek() != current ||
 *            !locked.compareAndSet(false, true)) {
 *       LockSupport.park(this);
 *       if (Thread.interrupted()) // ignore interrupts while waiting
 *         wasInterrupted = true;
 *     }
 *
 *     waiters.remove();
 *     if (wasInterrupted)          // reassert interrupt status on exit
 *       current.interrupt();
 *   }
 *
 *   public void unlock() {
 *     locked.set(false);
 *     LockSupport.unpark(waiters.peek());
 *   }
 * }}</pre>
 */
public class LockSupport {
    private LockSupport() {} // Cannot be instantiated.

    // 用反射设置Thread的预留字段，阻断的对象
    private static void setBlocker(Thread t, Object arg) {
        // Even though volatile, hotspot doesn't need a write barrier here.
        UNSAFE.putObject(t, parkBlockerOffset, arg);
    }

    /**
     * 提供给定线程的许可证(如果它还没有可用)。如果线程在park上被阻塞，那么它将解锁。
     * 否则，它的下一个调用park是保证不阻塞。如果没有启动给定的线程，则不能保证此操作有任何效果。
     *
     * @param thread 要unpark的线程，或null，在这种情况下，此操作无效
     */
    public static void unpark(Thread thread) {
        if (thread != null)
            UNSAFE.unpark(thread);
    }

    /**
     * 为线程调度的目的禁用当前线程，除非许可证可用。
     *
     * <p>如果许可证可用，则使用许可证并立即返回;否则，当前线程将出于线程调度的目的而被禁用，并处于休眠状态，直到发生以下三种情况之一:
     *
     * <ul>
     * <li>其他一些线程以当前线程为目标调用unpark;
     *
     * <li>其他一些线程中断当前线程;
     *
     * <li>虚假的调用(也就是说，没有任何理由)返回。
     * </ul>
     *
     * <p>此方法不报告是哪些原因导致该方法返回。呼叫者应该重新检查导致线程首先停止的条件。
     * 例如，调用者还可以确定线程返回时的中断状态。
     *
     * @param blocker 负责此线程停止的同步对象
     * @since 1.6
     */
    public static void park(Object blocker) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        UNSAFE.park(false, 0L);
        setBlocker(t, null);
    }

    /**
     * 除非有许可证，否则在指定的等待时间内，禁止当前线程用于线程调度。
     *
     * <p>如果许可证可用，则使用许可证并立即返回;否则，当前线程将出于线程调度的目的而被禁用，
     * 并处于休眠状态，直到发生以下四种情况之一:
     *
     * <ul>
     * <li>其他一些线程以当前线程为目标调用unpark;
     *
     * <li>其他一些线程中断当前线程;
     *
     * <li>指定的等候时间已过;
     *
     * <li>虚假的调用(也就是说，没有任何理由)返回。
     * </ul>
     *
     * <p>此方法不报告是哪些原因导致该方法返回。呼叫者应该重新检查导致线程首先停止的条件。
     * 例如，调用者还可以确定线程的中断状态或返回时的运行时间。
     *
     * @param blocker 负责此线程停止的同步对象
     * @param nanos 等待的最大纳秒数
     * @since 1.6
     */
    public static void parkNanos(Object blocker, long nanos) {
        if (nanos > 0) {
            Thread t = Thread.currentThread();
            setBlocker(t, blocker);
            UNSAFE.park(false, nanos);
            setBlocker(t, null);
        }
    }

    /**
     * 为了线程调度的目的，在指定的截止日期之前禁用当前线程，除非许可证可用。
     *
     * <p>如果许可证可用，则使用许可证并立即返回;
     * 否则，当前线程将出于线程调度的目的而被禁用，并处于休眠状态，直到发生以下四种情况之一:
     *
     * <ul>
     * <li>其他一些线程以当前线程为目标调用unpark
     *
     * <li>其他一些线程中断当前线程;
     *
     * <li>超过指定期限;
     *
     * <li>虚假的调用(也就是说，没有任何理由)返回。
     * </ul>
     *
     * <p>此方法不报告是哪些原因导致该方法返回。呼叫者应该重新检查导致线程首先停止的条件。
     * 例如，调用者还可以确定线程的中断状态，或返回时的当前时间。
     *
     * @param blocker 负责此线程停止的同步对象
     * @param deadline 等待时间的绝对时间(以毫秒为单位)
     * @since 1.6
     */
    public static void parkUntil(Object blocker, long deadline) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        UNSAFE.park(true, deadline);
        setBlocker(t, null);
    }

    /**
     * 返回提供给park方法的最近一次调用的blocker对象，该方法尚未被解除阻塞，如果未被阻塞，则返回null。
     * 返回的值只是一个瞬时快照——线程可能在不同的blocker对象上解除了阻塞或阻塞。
     *
     * @param t the thread
     * @return the blocker
     * @throws NullPointerException if argument is null
     * @since 1.6
     */
    public static Object getBlocker(Thread t) {
        if (t == null)
            throw new NullPointerException();
        return UNSAFE.getObjectVolatile(t, parkBlockerOffset);
    }

    /**
     * 为线程调度的目的禁用当前线程，除非许可证可用。
     *
     * <p>如果许可证可用，则使用许可证并立即返回;否则，当前线程将出于线程调度的目的而被禁用，
     * 并处于休眠状态，直到发生以下三种情况之一:
     *
     * <ul>
     *
     * <li>其他一些线程以当前线程为目标调用unpark;
     *
     * <li>其他一些线程中断当前线程;
     *
     * <li>虚假的调用(也就是说，没有任何理由)返回。
     * </ul>
     *
     * <p>此方法不报告是哪些原因导致该方法返回。呼叫者应该重新检查导致线程首先停止的条件。
     * 例如，调用者还可以确定线程返回时的中断状态。
     */
    public static void park() {
        UNSAFE.park(false, 0L);
    }

    /**
     * Disables the current thread for thread scheduling purposes, for up to
     * the specified waiting time, unless the permit is available.
     *
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of four
     * things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The specified waiting time elapses; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the elapsed time
     * upon return.
     *
     * @param nanos the maximum number of nanoseconds to wait
     */
    public static void parkNanos(long nanos) {
        if (nanos > 0)
            UNSAFE.park(false, nanos);
    }

    /**
     * 为了线程调度的目的，在指定的截止日期之前禁用当前线程，除非许可证可用。
     *
     * <p>如果许可证可用，则使用许可证并立即返回;否则，当前线程将出于线程调度的目的而被禁用，
     * 并处于休眠状态，直到发生以下四种情况之一:
     *
     * <ul>
     * <li>其他一些线程以当前线程为目标调用unpark;
     *
     * <li>其他一些线程中断当前线程;
     *
     * <li>超过指定期限;
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * <p>此方法不报告是哪些原因导致该方法返回。呼叫者应该重新检查导致线程首先停止的条件。
     * 例如，调用者还可以确定线程的中断状态，或返回时的当前时间。
     *
     * @param deadline the absolute time, in milliseconds from the Epoch,
     *        to wait until
     */
    public static void parkUntil(long deadline) {
        UNSAFE.park(true, deadline);
    }

    /**
     * 返回伪随机初始化或更新的辅助种子。由于包访问限制，从ThreadLocalRandom复制。
     */
    static final int nextSecondarySeed() {
        int r;
        Thread t = Thread.currentThread();
        if ((r = UNSAFE.getInt(t, SECONDARY)) != 0) {
            r ^= r << 13;   // xorshift
            r ^= r >>> 17;
            r ^= r << 5;
        }
        else if ((r = java.util.concurrent.ThreadLocalRandom.current().nextInt()) == 0)
            r = 1; // avoid zero
        UNSAFE.putInt(t, SECONDARY, r);
        return r;
    }

    // Hotspot implementation via intrinsics API
    private static final sun.misc.Unsafe UNSAFE;
    private static final long parkBlockerOffset;
    private static final long SEED;
    private static final long PROBE;
    private static final long SECONDARY;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            parkBlockerOffset = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("parkBlocker"));
            SEED = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("threadLocalRandomSeed"));
            PROBE = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("threadLocalRandomProbe"));
            SECONDARY = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("threadLocalRandomSecondarySeed"));
        } catch (Exception ex) { throw new Error(ex); }
    }

}
