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
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * 一种同步帮助，允许一个或多个线程等待，直到在其他线程中执行的一组操作完成。
 *
 * <p>CountDownLatch是用给定的count初始化的。由于调用了countDown()方法，await方法阻塞，
 * 直到当前计数为零，之后释放所有等待线程，并立即返回任何后续的await调用。
 * 这是一种一次性现象——计数无法重置。如果需要重置计数的版本，可以考虑使用CyclicBarrier。
 *
 * <p>CountDownLatch是一种通用的同步工具，可以用于多种用途。
 * count为1时初始化的CountDownLatch用作简单的on/off latch或gate:所有调用wait的线程都在gate处等待，
 * 直到调用countDown()的线程打开它。一个初始化为N的CountDownLatch可以用来让一个线程等待，
 * 直到N个线程完成某个动作，或者某个动作已经完成N次。
 *
 * <p>ACountDownLatch的一个有用的特性是，它不需要调用倒计时的线程等待计数达到零才继续，
 * 它只是防止任何线程继续等待，直到所有线程都通过。
 *
 * <p><b>下面是两个类，其中一组工作线程使用两个倒计时锁存器:
 * <ul>
 * <li>第一个是启动信号，它阻止任何worker继续工作，直到驱动程序准备好让它们继续工作;
 * <li>第二个是完成信号，允许司机等待，直到所有工人完成。
 * </ul>
 *
 *  <pre> {@code
 * class Driver { // ...
 *   void main() throws InterruptedException {
 *     CountDownLatch startSignal = new CountDownLatch(1);
 *     CountDownLatch doneSignal = new CountDownLatch(N);
 *
 *     for (int i = 0; i < N; ++i) // create and start threads
 *       new Thread(new Worker(startSignal, doneSignal)).start();
 *
 *     doSomethingElse();            // don't let run yet
 *     startSignal.countDown();      // let all threads proceed
 *     doSomethingElse();
 *     doneSignal.await();           // wait for all to finish
 *   }
 * }
 *
 * class Worker implements Runnable {
 *   private final CountDownLatch startSignal;
 *   private final CountDownLatch doneSignal;
 *   Worker(CountDownLatch startSignal, CountDownLatch doneSignal) {
 *     this.startSignal = startSignal;
 *     this.doneSignal = doneSignal;
 *   }
 *   public void run() {
 *     try {
 *       startSignal.await();
 *       doWork();
 *       doneSignal.countDown();
 *     } catch (InterruptedException ex) {} // return;
 *   }
 *
 *   void doWork() { ... }
 * }}</pre>
 *
 * <p>另一种典型的用法是将一个问题分成N个部分，用一个Runnable来描述每个部分，
 * 该Runnable执行该部分并在锁存器上计数，然后将所有Runnables排队给一个执行器。
 * 当所有子部件完成后，协调线程将能够通过wait。(当线程必须以这种方式重复计数时，使用CyclicBarrier。)
 *
 *  <pre> {@code
 * class Driver2 { // ...
 *   void main() throws InterruptedException {
 *     CountDownLatch doneSignal = new CountDownLatch(N);
 *     Executor e = ...
 *
 *     for (int i = 0; i < N; ++i) // create and start threads
 *       e.execute(new WorkerRunnable(doneSignal, i));
 *
 *     doneSignal.await();           // wait for all to finish
 *   }
 * }
 *
 * class WorkerRunnable implements Runnable {
 *   private final CountDownLatch doneSignal;
 *   private final int i;
 *   WorkerRunnable(CountDownLatch doneSignal, int i) {
 *     this.doneSignal = doneSignal;
 *     this.i = i;
 *   }
 *   public void run() {
 *     try {
 *       doWork(i);
 *       doneSignal.countDown();
 *     } catch (InterruptedException ex) {} // return;
 *   }
 *
 *   void doWork() { ... }
 * }}</pre>
 *
 * <p>内存一致性效应:在计数为0之前，在调用countDown()之前的线程中的操作发生—在从另一个
 * 线程中相应的await()成功返回之前的操作发生。
 *
 * @since 1.5
 * @author Doug Lea
 */
public class CountDownLatch {
    /**
     * 倒计时锁存器的同步控制。使用AQS状态表示计数。
     */
    private static final class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 4982264981922014374L;

        Sync(int count) { // 设置状态锁
            setState(count);
        }

        int getCount() { // 得到锁计数
            return getState();
        }

        protected int tryAcquireShared(int acquires) { // 获得共享锁 == await
            return (getState() == 0) ? 1 : -1;
        }

        protected boolean tryReleaseShared(int releases) { // 释放共享锁
            // Decrement count; signal when transition to zero
            for (;;) {
                int c = getState();
                if (c == 0)
                    return false;
                int nextc = c-1;
                if (compareAndSetState(c, nextc))
                    return nextc == 0;
            }
        }
    }

    private final Sync sync;

    /**
     * 构造一个 {@code CountDownLatch} 初始化给定数量。
     *
     * @param count the number of times {@link #countDown} must be invoked
     *        before threads can pass through {@link #await}
     * @throws IllegalArgumentException if {@code count} is negative
     */
    public CountDownLatch(int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        this.sync = new Sync(count);
    }

    /**
     * 导致当前线程等待，直到锁存器计数到零，除非线程被中断。
     *
     * <p>如果当前计数为零，则此方法立即返回。
     *
     * <p>如果当前线程数大于0，则当前线程将出于线程调度的目的而禁用，并处于休眠状态，直到发生以下两种情况之一:
     * <ul>
     * <li>由于调用了countDown()方法，计数为零;
     * <li>其他一些线程中断当前线程。
     * </ul>
     *
     * <p>如果当前线程:
     * <ul>
     * <li>在进入此方法时已设置其中断状态;
     * <li>在等待时中断，然后抛出InterruptedException，并清除当前线程的中断状态。
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(1); // 获取共享中断锁
    }

    /**
     * 导致当前线程等待，直到锁存器计数到零，除非线程被中断，或者指定的等待时间已过。
     *
     * <p>如果当前计数为零，则此方法立即返回值true。
     *
     * <p>如果当前线程数大于0，则当前线程将出于线程调度的目的而禁用，并处于休眠状态，直到发生以下三种情况之一:
     * <ul>
     * <li>由于调用了countDown()方法，计数为零;
     * <li>其他一些线程中断当前线程;
     * </ul>
     *
     * <p>如果计数为零，则该方法返回值true。
     *
     * <p>如果当前线程:
     * <ul>
     * <li>在进入此方法时已设置其中断状态;
     * <li>在等待时中断，然后抛出InterruptedException，并清除当前线程的中断状态。
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>如果指定的等待时间过期，则返回false值。如果时间小于或等于0，则该方法根本不会等待。
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument
     * @return {@code true} if the count reached zero and {@code false}
     *         if the waiting time elapsed before the count reached zero
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
    public boolean await(long timeout, TimeUnit unit)
        throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    /**
     * 递减锁存器的计数，如果计数为零，则释放所有等待的线程。
     *
     * <p>如果当前计数大于零，则递减。如果新计数为零，那么所有等待的线程都将重新启用，以便进行线程调度。
     *
     * <p>如果当前计数等于0，则什么也不会发生。
     */
    public void countDown() {
        sync.releaseShared(1);
    }

    /**
     * 返回当前计数。
     *
     * <p>此方法通常用于调试和测试目的。
     *
     * @return the current count
     */
    public long getCount() {
        return sync.getCount();
    }

    /**
     * 返回标识此锁存器的字符串及其状态。方括号中的状态包括字符串“Count =”和当前计数。
     *
     * @return a string identifying this latch, as well as its state
     */
    public String toString() {
        return super.toString() + "[Count = " + sync.getCount() + "]";
    }
}
