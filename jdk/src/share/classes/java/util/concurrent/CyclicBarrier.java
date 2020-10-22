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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 一种同步辅助工具，允许一组线程彼此等待到达一个共同的障碍点。
 * cyclicbarrier在包含固定大小的线程的程序中非常有用，这些线程有时必须彼此等待。
 * 这个屏障被称为循环屏障，因为它可以在等待的线程被释放后重新使用。
 *
 * <p>CyclicBarrier支持一个可选的Runnable命令，该命令在每一个屏障点上运行一次，
 * 在最后一个线程到达之后，但是在释放任何线程之前。这个屏障动作对于在任何一方继续之前更新共享状态非常有用。
 *
 * <p>下面是一个在并行分解设计中使用屏障的例子:
 *
 *  <pre> {@code
 * class Solver {
 *   final int N;
 *   final float[][] data;
 *   final CyclicBarrier barrier;
 *
 *   class Worker implements Runnable {
 *     int myRow;
 *     Worker(int row) { myRow = row; }
 *     public void run() {
 *       while (!done()) {
 *         processRow(myRow);
 *
 *         try {
 *           barrier.await();
 *         } catch (InterruptedException ex) {
 *           return;
 *         } catch (BrokenBarrierException ex) {
 *           return;
 *         }
 *       }
 *     }
 *   }
 *
 *   public Solver(float[][] matrix) {
 *     data = matrix;
 *     N = matrix.length;
 *     Runnable barrierAction =
 *       new Runnable() { public void run() { mergeRows(...); }};
 *     barrier = new CyclicBarrier(N, barrierAction);
 *
 *     List<Thread> threads = new ArrayList<Thread>(N);
 *     for (int i = 0; i < N; i++) {
 *       Thread thread = new Thread(new Worker(i));
 *       threads.add(thread);
 *       thread.start();
 *     }
 *
 *     // wait until done
 *     for (Thread thread : threads)
 *       thread.join();
 *   }
 * }}</pre>
 *
 * 在这里，每个工作线程处理矩阵的一行，然后在屏障处等待，直到处理完所有行。
 * 在处理所有行时，执行所提供的Runnable barrier操作并合并行。如果合并确定找到了解决方案，
 * 那么done()将返回true，每个worker将终止。
 *
 * <p>如果barrier操作在执行时不依赖于被挂起的参与方，
 * 那么参与方中的任何线程都可以在释放该操作时执行该操作。
 * 为了促进这一点，每次调用await()都会返回该线程在屏障处的到达索引。
 * 然后你可以选择哪个线程应该执行屏障行动，例如:
 *  <pre> {@code
 * if (barrier.await() == 0) {
 *   // log the completion of this iteration
 * }}</pre>
 *
 * <p>CyclicBarrier使用一个动静极限的破碎模型同步尝试失败:如果一个线程离开障碍点过早中断,失败,或超时,
 * 所有其他线程等待障碍点也将离开异常通过BrokenBarrierException
 * (或InterruptedException如果他们也被打断了大约在同一时间)。
 *
 * <p>内存一致性效应:在调用await()之前的线程中的操作，这些操作是barrier操作的一部分，而barrier操作又是在其他线程中相应的await()成功返回之后的before操作。
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions that are part of the barrier action, which in turn
 * <i>happen-before</i> actions following a successful return from the
 * corresponding {@code await()} in other threads.
 *
 * @since 1.5
 * @see CountDownLatch
 *
 * @author Doug Lea
 */
public class CyclicBarrier {
    /**
     * 屏障的每次使用都表示为一个生成实例。无论何时触发屏障或重置屏障，都会更改生成。可以有许多代与线程相关的使用障碍,由于不确定的方式锁可能被分配到等待线程,但一次只能激活一个(一个{@code数}适用)和所有其他的破碎或绊倒。如果已经中断但没有后续重置，则不需要活动生成。
     */
    private static class Generation {
        boolean broken = false;
    }

    /** 可重入锁 */
    private final ReentrantLock lock = new ReentrantLock();
    /** 条件锁等待 */
    private final Condition trip = lock.newCondition();
    /** The number of parties */
    private final int parties;
    /* The command to run when tripped */
    private final Runnable barrierCommand;
    /** The current generation */
    private Generation generation = new Generation();

    /**
     * 许多人仍在等待。每代从聚会数到0。它被重置到每一个新一代的政党或当打破。
     */
    private int count;

    /**
     * 更新状态并唤醒所有线程
     */
    private void nextGeneration() {
        // signal completion of last generation
        trip.signalAll();
        // set up next generation
        count = parties;
        generation = new Generation();
    }

    /**
     * 将当前的屏障生成设置为被打破，并唤醒所有人。只有在持有锁时才调用。
     */
    private void breakBarrier() {
        generation.broken = true;
        count = parties;
        trip.signalAll();
    }

    /**
     * 主要屏障代码，涵盖各种政策。
     */
    private int dowait(boolean timed, long nanos)
        throws InterruptedException, BrokenBarrierException,
               TimeoutException {
        // 拿到可重入锁
        final ReentrantLock lock = this.lock;
        // 锁上
        lock.lock();
        try {
            final Generation g = generation;

            if (g.broken) // 若阻塞了，抛异常
                throw new BrokenBarrierException();

            if (Thread.interrupted()) { // 若中断
                breakBarrier();
                throw new InterruptedException();
            }

            int index = --count; // 数量-1
            if (index == 0) {  // tripped
                boolean ranAction = false;
                try {
                    final Runnable command = barrierCommand; // run函数
                    if (command != null)
                        command.run(); // 不为null，先运行函数
                    ranAction = true;
                    nextGeneration(); // 唤醒线程
                    return 0;
                } finally {
                    if (!ranAction)
                        breakBarrier();
                }
            }

            // 循环 until tripped, broken, 打断, or 超时
            for (;;) {
                try {
                    if (!timed) // 非时间模式
                        trip.await(); // 条件锁
                    else if (nanos > 0L) // 有超时时间的阻塞
                        nanos = trip.awaitNanos(nanos);
                } catch (InterruptedException ie) {
                    if (g == generation && ! g.broken) { // 中断的话
                        breakBarrier();
                        throw ie;
                    } else {
                        // 即使没有被中断，我们也将完成等待，因此这个中断被认为“属于”后续执行。
                        Thread.currentThread().interrupt();
                    }
                }

                if (g.broken)
                    throw new BrokenBarrierException();

                if (g != generation)
                    return index;

                if (timed && nanos <= 0L) {
                    breakBarrier();
                    throw new TimeoutException();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 创建一个新的CyclicBarrier，当给定数量的参与方(线程)正在等待它时，它将跳闸，当屏障跳闸时，
     * 它将执行给定的barrier动作，由最后一个进入屏障的线程执行。
     *
     * @param parties the number of threads that must invoke {@link #await}
     *        before the barrier is tripped
     * @param barrierAction the command to execute when the barrier is
     *        tripped, or {@code null} if there is no action
     * @throws IllegalArgumentException if {@code parties} is less than 1
     */
    public CyclicBarrier(int parties, Runnable barrierAction) {
        if (parties <= 0) throw new IllegalArgumentException();
        this.parties = parties;// 在触发屏障之前必须调用await()的线程的数量
        this.count = parties;
        this.barrierCommand = barrierAction; // 执行函数
    }

    /**
     * 创建一个新的CyclicBarrier，当给定数量的参与方(线程)正在等待它时，
     * 它将会跳闸，并且在跳闸时不会执行预定义的操作。
     *
     * @param parties the number of threads that must invoke {@link #await}
     *        before the barrier is tripped
     * @throws IllegalArgumentException if {@code parties} is less than 1
     */
    public CyclicBarrier(int parties) {
        this(parties, null);
    }

    /**
     * 返回需要跨越此屏障的参与方的数量。
     *
     * @return the number of parties required to trip this barrier
     */
    public int getParties() {
        return parties;
    }

    /**
     * 等待，直到所有参与方都已调用此屏障上的等待。
     *
     * <p>如果当前线程不是最后到达的，那么出于线程调度的目的，它将被禁用，并处于休眠状态，直到发生以下情况之一:
     * <ul>
     * <li>最后一个线程到达;
     * <li>其他一些线程中断当前线程;
     * <li>其他一些线程会中断一个正在等待的线程;
     * <li>其他一些线程在等待barrier时超时;
     * <li>其他一些线程在这个屏障上调用reset()。
     * </ul>
     *
     * <p>如果当前线程:
     * <ul>
     * <li>在进入此方法时已设置其中断状态;
     * <li>在等待时中断,然后抛出InterruptedException，并清除当前线程的中断状态。
     * </ul>
     * 如果在任何线程等待时壁垒被重置()，或者在调用await时壁垒被打破，或者在任何线程等待时壁垒被打破，
     * 那么抛出broken barrierexception。
     *
     * <p>如果任何线程在等待时被中断，那么所有其他等待线程将抛出broken barrierexception，
     * 该barrier将处于broken状态。
     *
     * <p>如果当前线程是最后一个到达的线程，并且构造函数中提供了一个非空屏障操作，
     * 那么当前线程在允许其他线程继续之前运行该操作。
     * 如果barrier操作期间发生异常，那么该异常将在当前线程中传播，barrier将处于中断状态。
     *
     * @return the arrival index of the current thread, where index
     *         {@code getParties() - 1} indicates the first
     *         to arrive and zero indicates the last to arrive
     * @throws InterruptedException if the current thread was interrupted
     *         while waiting
     * @throws BrokenBarrierException if <em>another</em> thread was
     *         interrupted or timed out while the current thread was
     *         waiting, or the barrier was reset, or the barrier was
     *         broken when {@code await} was called, or the barrier
     *         action (if present) failed due to an exception
     */
    public int await() throws InterruptedException, BrokenBarrierException {
        try {
            return dowait(false, 0L);
        } catch (TimeoutException toe) {
            throw new Error(toe); // cannot happen
        }
    }

    /**
     * Waits until all {@linkplain #getParties parties} have invoked
     * {@code await} on this barrier, or the specified waiting time elapses.
     *
     * <p>If the current thread is not the last to arrive then it is
     * disabled for thread scheduling purposes and lies dormant until
     * one of the following things happens:
     * <ul>
     * <li>The last thread arrives; or
     * <li>The specified timeout elapses; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * one of the other waiting threads; or
     * <li>Some other thread times out while waiting for barrier; or
     * <li>Some other thread invokes {@link #reset} on this barrier.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the specified waiting time elapses then {@link TimeoutException}
     * is thrown. If the time is less than or equal to zero, the
     * method will not wait at all.
     *
     * <p>If the barrier is {@link #reset} while any thread is waiting,
     * or if the barrier {@linkplain #isBroken is broken} when
     * {@code await} is invoked, or while any thread is waiting, then
     * {@link BrokenBarrierException} is thrown.
     *
     * <p>If any thread is {@linkplain Thread#interrupt interrupted} while
     * waiting, then all other waiting threads will throw {@link
     * BrokenBarrierException} and the barrier is placed in the broken
     * state.
     *
     * <p>If the current thread is the last thread to arrive, and a
     * non-null barrier action was supplied in the constructor, then the
     * current thread runs the action before allowing the other threads to
     * continue.
     * If an exception occurs during the barrier action then that exception
     * will be propagated in the current thread and the barrier is placed in
     * the broken state.
     *
     * @param timeout the time to wait for the barrier
     * @param unit the time unit of the timeout parameter
     * @return the arrival index of the current thread, where index
     *         {@code getParties() - 1} indicates the first
     *         to arrive and zero indicates the last to arrive
     * @throws InterruptedException if the current thread was interrupted
     *         while waiting
     * @throws TimeoutException if the specified timeout elapses.
     *         In this case the barrier will be broken.
     * @throws BrokenBarrierException if <em>another</em> thread was
     *         interrupted or timed out while the current thread was
     *         waiting, or the barrier was reset, or the barrier was broken
     *         when {@code await} was called, or the barrier action (if
     *         present) failed due to an exception
     */
    public int await(long timeout, TimeUnit unit)
        throws InterruptedException,
               BrokenBarrierException,
               TimeoutException {
        return dowait(true, unit.toNanos(timeout));
    }

    /**
     * 查询此屏障是否处于中断状态。
     *
     * @return {@code true} if one or more parties broke out of this
     *         barrier due to interruption or timeout since
     *         construction or the last reset, or a barrier action
     *         failed due to an exception; {@code false} otherwise.
     */
    public boolean isBroken() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return generation.broken;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 将势垒重置为其初始状态。如果任何一方当前正在屏障处等待，他们将返回一个破损的屏障异常。
     * 请注意，在因其他原因造成的破损发生后进行复位可能比较复杂;
     * 线程需要以其他方式重新同步，并选择一种方式执行重置。最好是为后续使用创建一个新的屏障。
     */
    public void reset() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            breakBarrier();   // break the current generation
            nextGeneration(); // start a new generation
        } finally {
            lock.unlock();
        }
    }

    /**
     * 返回当前在屏障处等待的参与方的数量。此方法主要用于调试和断言。
     *
     * @return the number of parties currently blocked in {@link #await}
     */
    public int getNumberWaiting() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return parties - count;
        } finally {
            lock.unlock();
        }
    }
}
