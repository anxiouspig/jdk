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

package java.util.concurrent;
import java.util.concurrent.locks.LockSupport;

/**
 * 可取消的异步计算。这个类提供了一个Future的基本实现，使用一些方法来启动和取消计算、
 * 查询以查看计算是否完成以及检索计算结果。只有在计算完成后，才可检索结果;如果计算尚未完成，get方法将阻塞。
 * 一旦计算完成，就不能重新启动或取消计算(除非使用runAndReset()调用计算)。
 *
 * <p>FutureTask可以用来包装一个可调用或可运行的对象。因为FutureTask实现了Runnable，
 * 所以可以将FutureTask提交给执行程序执行
 *
 * <p>除了作为一个独立的类之外，这个类还提供了在创建自定义任务类时可能有用的受保护功能。
 *
 * @since 1.5
 * @author Doug Lea
 * @param <V> The result type returned by this FutureTask's {@code get} methods
 */
public class FutureTask<V> implements RunnableFuture<V> {
    /*
     * 修订说明:这与这个类以前的版本不同，以前的版本依赖于AbstractQueuedSynchronizer，主要是为了避免在取消竞争期间保持中断状态给用户带来意外。当前设计中的同步控制依赖于通过CAS更新的“state”字段来跟踪完成情况，以及一个简单的Treiber堆栈来保存等待的线程。
     *
     * Style note: 与往常一样，我们绕过了使用AtomicXFieldUpdaters的开销，而是直接使用不安全的内部特性。
     */

    /**
     * 此任务的运行状态，最初是新的。运行状态仅在set、setException和cancel方法中转换为终端状态。在完成期间，状态可以采用暂态值来完成(在设置结果时)或中断(仅在中断运行程序以满足取消(true)时)。从这些中间状态到最终状态的转换使用更便宜的有序/延迟写操作，因为值是惟一的，不能进一步修改。
     *
     * 可能的状态转换:
     * NEW -> COMPLETING -> NORMAL
     * NEW -> COMPLETING -> EXCEPTIONAL
     * NEW -> CANCELLED
     * NEW -> INTERRUPTING -> INTERRUPTED
     */
    private volatile int state;
    private static final int NEW          = 0;
    private static final int COMPLETING   = 1;
    private static final int NORMAL       = 2;
    private static final int EXCEPTIONAL  = 3;
    private static final int CANCELLED    = 4;
    private static final int INTERRUPTING = 5;
    private static final int INTERRUPTED  = 6;

    /** 底层调用; 运行后为空 */
    private Callable<V> callable;
    /** get()返回的结构或异常 */
    private Object outcome; // non-volatile, protected by state reads/writes
    /** 线程运行的结果; 运行时cas操作 */
    private volatile Thread runner;
    /** Treiber stack of waiting threads */
    private volatile WaitNode waiters;

    /**
     * 返回结果或抛出完成任务异常。
     *
     * @param s 完成状态值
     */
    @SuppressWarnings("unchecked")
    private V report(int s) throws ExecutionException {
        Object x = outcome; // 结果
        if (s == NORMAL) // 若状态为正常，则返回结果
            return (V)x;
        if (s >= CANCELLED) // 若为取消或中断，则抛出异常
            throw new CancellationException();
        throw new ExecutionException((Throwable)x); // 其他状态抛出异常
    }

    /**
     * 创建一个 {@code FutureTask} that will, 在运行, 执行给定的 {@code Callable}.
     *
     * @param  callable 可调用的任务
     * @throws NullPointerException if the callable is null
     */
    public FutureTask(Callable<V> callable) {
        if (callable == null) // 若任务为null，则抛出空指针异常
            throw new NullPointerException();
        this.callable = callable; // 赋值任务
        this.state = NEW;       // 确保调用的可见性
    }

    /**
     * 创建一个 {@code FutureTask} that will, 在运行时, 执行
     * 给定的 {@code Runnable}, 安排 that {@code get} 去返回
     * 成功完成给定的结果。
     *
     * @param runnable 运行任务
     * @param result 成功完成返回的结果
     * 如果不需要特定的结果, 考虑如下构造函数
     * {@code Future<?> f = new FutureTask<Void>(runnable, null)}
     * @throws NullPointerException if the runnable is null
     */
    public FutureTask(Runnable runnable, V result) {
        this.callable = Executors.callable(runnable, result); //
        this.state = NEW;       // ensure visibility of callable
    }
    // 如果此任务在正常完成之前被取消，则返回true。
    public boolean isCancelled() {
        return state >= CANCELLED;
    }
    // 如果此任务已完成，则返回true。完成可能是由于正常的终止、异常或取消——在所有这些情况下，此方法都将返回true。
    public boolean isDone() {
        return state != NEW;
    }

    // 试图取消此任务的执行。如果任务已经完成，已经取消，或者由于其他原因无法取消，则此尝试将失败。如果成功，并且在调用cancel时此任务尚未启动，则此任务不应运行。如果任务已经启动，那么mayInterruptIfRunning参数确定执行此任务的线程是否应该中断，以试图停止该任务。
    // 在此方法返回后，对Future.isDone()的后续调用将始终返回true。如果该方法返回true，那么对future.iscancel()的后续调用将始终返回true。
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!(state == NEW && // 如果线程为刚创建的
              UNSAFE.compareAndSwapInt(this, stateOffset, NEW,
                  mayInterruptIfRunning ? INTERRUPTING : CANCELLED))) // 设置线程状态为中断或取消
            return false; // 设置不成功，返回false
        try {    // 以防调用打断抛异常
            if (mayInterruptIfRunning) { // 为true，中断正在运行的线程
                try {
                    Thread t = runner; // 正在运行的线程
                    if (t != null) // 若不为null，则中断
                        t.interrupt();
                } finally { // 状态设置为中断
                    UNSAFE.putOrderedInt(this, stateOffset, INTERRUPTED);
                }
            }
        } finally {
            finishCompletion();
        }
        return true;
    }

    /**
     * 如果需要，则等待计算完成，然后检索其结果。
     * @throws CancellationException {@inheritDoc}
     */
    public V get() throws InterruptedException, ExecutionException {
        int s = state; // 状态
        if (s <= COMPLETING)
            s = awaitDone(false, 0L); // 自旋等待任务完成
        return report(s);
    }

    /**
     * 如果需要，将等待最多给定的时间以完成计算，然后检索其结果(如果可用)。
     * @throws CancellationException {@inheritDoc}
     */
    public V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        if (unit == null)
            throw new NullPointerException();
        int s = state;
        if (s <= COMPLETING &&
            (s = awaitDone(true, unit.toNanos(timeout))) <= COMPLETING) //  自旋等待任务完成
            throw new TimeoutException();
        return report(s);
    }

    /**
     * 在此任务转换到状态isDone时调用受保护的方法(无论是正常情况下还是通过取消)。默认实现什么也不做。子类可以覆盖此方法来调用完成回调或执行簿记。
     * 请注意，您可以查询此方法实现中的状态，以确定此任务是否已被取消。
     */
    protected void done() { }

    /**
     * 将此future的结果设置为给定值，除非此future已被设置或已被取消。
     *
     * <p>此方法在成功完成计算后由run()方法在内部调用。
     *
     * @param v the value
     */
    protected void set(V v) { // 设置结果
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) { // 状态设为正在完成
            outcome = v; // 赋值结果
            UNSAFE.putOrderedInt(this, stateOffset, NORMAL); // final state 状态设为正常完成
            finishCompletion();
        }
    }

    /**
     * Causes this future to report an {@link ExecutionException}
     * with the given throwable as its cause, unless this future has
     * already been set or has been cancelled.
     *
     * <p>This method is invoked internally by the {@link #run} method
     * upon failure of the computation.
     *
     * @param t the cause of failure
     */
    protected void setException(Throwable t) { // 设置异常信息
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) { // 状态设为正在完成
            outcome = t; // 结果为异常信息
            UNSAFE.putOrderedInt(this, stateOffset, EXCEPTIONAL); // final state 状态设为异常状态
            finishCompletion();
        }
    }

    public void run() {
        if (state != NEW || // 此方法要求状态必须是new
            !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                         null, Thread.currentThread())) // 把运行线程换为当前线程
            return; // 不满足运行条件，返回；
        try {
            Callable<V> c = callable; // 回调函数
            if (c != null && state == NEW) { // 判断函数不为null并且状态为new
                V result; // 结果
                boolean ran; // 是否成功
                try {
                    result = c.call();  // 返回的结果，这一步耗时看任务执行时间
                    ran = true; // 任务执行成功
                } catch (Throwable ex) {
                    result = null; // 结果为null
                    ran = false; // 运行失败
                    setException(ex); // 出现异常，设置异常信息
                }
                if (ran)
                    set(result); // 设置结果
            }
        } finally {
            // runner 必须非null，until state is settled to
            // prevent concurrent calls to run()
            runner = null; // 执行结束，执行线程设为null
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            int s = state;
            if (s >= INTERRUPTING) // 任务如果中断
                handlePossibleCancellationInterrupt(s);
        }
    }

    /**
     * Executes the computation without setting its result, and then
     * resets this future to initial state, failing to do so if the
     * computation encounters an exception or is cancelled.  This is
     * designed for use with tasks that intrinsically execute more
     * than once.
     *
     * @return {@code true} if successfully run and reset
     */
    protected boolean runAndReset() {
        if (state != NEW || // 状态必须为new
            !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                         null, Thread.currentThread())) // 设置运行线程为当前线程
            return false;
        boolean ran = false;
        int s = state;
        try {
            Callable<V> c = callable; // 回调函数
            if (c != null && s == NEW) { // 函数不为null，状态为new
                try {
                    c.call(); // 不设置结果
                    ran = true;
                } catch (Throwable ex) {
                    setException(ex); // 设置异常信息
                }
            }
        } finally {
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            runner = null;
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            s = state;
            if (s >= INTERRUPTING) // 若为中断状态
                handlePossibleCancellationInterrupt(s);
        }
        return ran && s == NEW;
    }

    /**
     * Ensures that any interrupt from a possible cancel(true) is only
     * delivered to a task while in run or runAndReset.
     */
    private void handlePossibleCancellationInterrupt(int s) {
        // 在有机会打断我们之前，我们的中断器可能会停止。让我们耐心地循环等待。
        if (s == INTERRUPTING) // 若为中断状态
            while (state == INTERRUPTING) // 循环，只要中断状态就让出cpu
                Thread.yield(); // 等待中断

        // 断言 state == INTERRUPTED;

        // 我们想要确认我们收到的任何中断cancel(true).
        //   然而, 允许使用中断
        // 作为一个独立的机制 对于沟通调用者的任务
        // and 没办法只清除取消中断
        //
        // Thread.interrupted();

    }

    /**
     * Simple linked list nodes to record waiting threads in a Treiber
     * stack.  See other classes such as Phaser and SynchronousQueue
     * for more detailed explanation.
     */
    static final class WaitNode {
        volatile Thread thread; // 等待线程
        volatile WaitNode next; // 下一个等待线程
        WaitNode() { thread = Thread.currentThread(); } // 当前线程作为等待线程
    }

    /**
     * Removes and signals all waiting threads, invokes done(), and
     * nulls out callable.
     */
    private void finishCompletion() {
        // assert state > COMPLETING; // 状态已完成
        for (WaitNode q; (q = waiters) != null;) { // 等待线程不为null
            if (UNSAFE.compareAndSwapObject(this, waitersOffset, q, null)) { // 等待线程设为null
                for (;;) { // 自旋释放等待线程
                    Thread t = q.thread; // 等待线程
                    if (t != null) { // 线程不为null
                        q.thread = null; // GC
                        LockSupport.unpark(t); // 释放等待线程
                    }
                    WaitNode next = q.next; // 下一个等待线程
                    if (next == null)
                        break;
                    q.next = null; // unlink to help gc
                    q = next;
                }
                break;
            }
        }

        done();

        callable = null;        // to reduce footprint
    }

    /**
     * Awaits completion or aborts on interrupt or timeout.
     *
     * @param timed true if use timed waits
     * @param nanos time to wait, if timed
     * @return state upon completion
     */
    private int awaitDone(boolean timed, long nanos)
            throws InterruptedException {
        final long deadline = timed ? System.nanoTime() + nanos : 0L;
        WaitNode q = null;
        boolean queued = false;
        for (;;) { // 自旋等待任务完成
            if (Thread.interrupted()) { // 若线程中断
                removeWaiter(q); // 移除等待线程
                throw new InterruptedException();
            }

            int s = state; // 状态
            if (s > COMPLETING) { // 已完成
                if (q != null)
                    q.thread = null;
                return s; // 返回状态
            }
            else if (s == COMPLETING) // cannot time out yet
                Thread.yield(); // 未完成则让出cpu
            else if (q == null)
                q = new WaitNode();
            else if (!queued)
                queued = UNSAFE.compareAndSwapObject(this, waitersOffset,
                        q.next = waiters, q); // 替换等待线程
            else if (timed) { // 超时
                nanos = deadline - System.nanoTime();
                if (nanos <= 0L) {
                    removeWaiter(q); // 移除等待线程
                    return state;
                }
                LockSupport.parkNanos(this, nanos);
            }
            else
                LockSupport.park(this); // 阻塞当前线程
        }
    }

    /**
     * Tries to unlink a timed-out or interrupted wait node to avoid
     * accumulating garbage.  Internal nodes are simply unspliced
     * without CAS since it is harmless if they are traversed anyway
     * by releasers.  To avoid effects of unsplicing from already
     * removed nodes, the list is retraversed in case of an apparent
     * race.  This is slow when there are a lot of nodes, but we don't
     * expect lists to be long enough to outweigh higher-overhead
     * schemes.
     */
    private void removeWaiter(WaitNode node) {
        if (node != null) {
            node.thread = null;
            retry:
            for (;;) {          // 自旋找到该节点，移除
                for (WaitNode pred = null, q = waiters, s; q != null; q = s) {
                    s = q.next;
                    if (q.thread != null)
                        pred = q;
                    else if (pred != null) {
                        pred.next = s;
                        if (pred.thread == null) // check for race
                            continue retry;
                    }
                    else if (!UNSAFE.compareAndSwapObject(this, waitersOffset,
                            q, s))
                        continue retry;
                }
                break;
            }
        }
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long stateOffset;
    private static final long runnerOffset;
    private static final long waitersOffset;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = FutureTask.class;
            stateOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("state"));
            runnerOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("runner"));
            waitersOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("waiters"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}
