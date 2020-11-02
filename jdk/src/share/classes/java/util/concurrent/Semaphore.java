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
import java.util.Collection;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * 计数信号量。从概念上讲，信号量维护一组许可证。如果需要，每个acquire()都会阻塞，
 * 直到获得许可证，然后获取许可证。每个release()添加一个许可证，潜在地释放一个阻塞的获取者。
 * 但是，没有实际使用许可证对象;信号量只是保持可用数量的一个计数，并相应地进行操作。
 *
 * <p>信号量通常用于限制能够访问某些(物理或逻辑)资源的线程数量。例如，这里有一个类使用信号量来控制对项池的访问:
 *  <pre> {@code
 * class Pool {
 *   private static final int MAX_AVAILABLE = 100;
 *   private final Semaphore available = new Semaphore(MAX_AVAILABLE, true);
 *
 *   public Object getItem() throws InterruptedException {
 *     available.acquire();
 *     return getNextAvailableItem();
 *   }
 *
 *   public void putItem(Object x) {
 *     if (markAsUnused(x))
 *       available.release();
 *   }
 *
 *   // Not a particularly efficient data structure; just for demo
 *
 *   protected Object[] items = ... whatever kinds of items being managed
 *   protected boolean[] used = new boolean[MAX_AVAILABLE];
 *
 *   protected synchronized Object getNextAvailableItem() {
 *     for (int i = 0; i < MAX_AVAILABLE; ++i) {
 *       if (!used[i]) {
 *          used[i] = true;
 *          return items[i];
 *       }
 *     }
 *     return null; // not reached
 *   }
 *
 *   protected synchronized boolean markAsUnused(Object item) {
 *     for (int i = 0; i < MAX_AVAILABLE; ++i) {
 *       if (item == items[i]) {
 *          if (used[i]) {
 *            used[i] = false;
 *            return true;
 *          } else
 *            return false;
 *       }
 *     }
 *     return false;
 *   }
 * }}</pre>
 *
 * <p>在获得一个项之前，每个线程必须从信号量获得一个许可，以确保一个项是可用的。当线程处理完该项后，它将返回到池中，
 * 并向信号量返回一个许可证，从而允许另一个线程获取该项。请注意，在调用acquire()时不会持有同步锁，
 * 因为这会阻止将项返回到池中。信号量封装了限制对池的访问所需的同步，与维护池本身的一致性所需的任何同步分开。
 *
 * <p>一个初始化为一个的信号量，它的使用方式是最多只有一个可用的许可证，可以用作互斥锁。这通常被称为二进制信号量，
 * 因为它只有两种状态:一个可用许可证，或者0个可用许可证。当以这种方式使用时，
 * 二进制信号量具有这样的属性(与许多锁实现不同)，即“锁”可以由所有者以外的线程释放(因为信号量没有所有权的概念)。
 * 这在某些特定上下文中很有用，比如死锁恢复。
 *
 * <p> 该类的构造函数可选地接受公平性参数。当设置为false时，该类不能保证线程获得许可的顺序。
 * 特别是，允许倒挂，也就是说，可以在一直在等待的线程之前为调用acquire()的线程分配许可证——从逻辑上讲，
 * 新线程将自己放在等待线程队列的最前面。当公平性设置为真时，信号量保证会选择调用任何获取方法的线程，
 * 从而按照它们对这些方法的调用的处理顺序(先进先出;先进先出)。
 * 请注意，FIFO顺序必然适用于这些方法中的特定内部执行点。
 * 因此，一个线程可以在另一个线程之前调用acquire，但是在另一个线程之后到达排序点，
 * 同样地，在方法返回时到达排序点。还请注意，不定时tryAcquire方法不尊重公平设置，但将采取任何许可是可用的。
 *
 * <p>通常，用于控制资源访问的信号量应该被初始化为公平的，以确保没有线程因为访问资源而饿死。
 * 当将信号量用于其他类型的同步控制时，非公平排序的吞吐量优势常常超过了公平性考虑。
 *
 * <p>这个类还提供了一次获取和释放多个许可的便利方法。当使用这些方法而不将公平性设置为真时，要注意无限期延迟的风险。
 *
 * <p>内存一致性效应:在调用“release”方法(如release())之前的线程中的操作发生在另一个线程中的acquire()
 * 等成功的“acquire”方法之后的操作发生之前。
 *
 * @since 1.5
 * @author Doug Lea
 */
public class Semaphore implements java.io.Serializable {
    private static final long serialVersionUID = -3222578661600680210L;
    /** 通过aps实现的锁 */
    private final Sync sync;

    /**
     * 同步实现 for semaphore.  用 AQS state
     * 代表许可. 子类实现公平和非公平版本
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1192457210091910933L;
        // 设置state
        Sync(int permits) {
            setState(permits);
        }
        // 得到state
        final int getPermits() {
            return getState();
        }
        // 非公平获得共享锁
        final int nonfairTryAcquireShared(int acquires) {
            for (;;) { // 自旋
                int available = getState(); // 拿到许可
                int remaining = available - acquires; // 剩下许可
                if (remaining < 0 || // 没有许可或者设置许可成功
                    compareAndSetState(available, remaining))
                    return remaining; // 返回许可
            }
        }
        // 释放共享锁
        protected final boolean tryReleaseShared(int releases) {
            for (;;) { // 自旋
                int current = getState(); // 拿到许可
                int next = current + releases;// 锁加
                if (next < current) // overflow // 释放完小于释放前则跑异常
                    throw new Error("Maximum permit count exceeded");
                if (compareAndSetState(current, next)) // 替换状态
                    return true;
            }
        }
        // 减许可
        final void reducePermits(int reductions) {
            for (;;) { // 自旋
                int current = getState(); // 拿到许可
                int next = current - reductions; // 锁减
                if (next > current) // overflow // 释放完大于释放前则跑异常
                    throw new Error("Permit count underflow");
                if (compareAndSetState(current, next)) // 替换状态
                    return;
            }
        }
        // 许可归0
        final int drainPermits() {
            for (;;) {
                int current = getState();
                if (current == 0 || compareAndSetState(current, 0))
                    return current;
            }
        }
    }

    /**
     * 非公平版本
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -2694183684443567898L;

        NonfairSync(int permits) {
            super(permits);
        }

        protected int tryAcquireShared(int acquires) {
            return nonfairTryAcquireShared(acquires);
        }
    }

    /**
     * 公平版本
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = 2014338818796000944L;

        FairSync(int permits) {
            super(permits);
        }

        protected int tryAcquireShared(int acquires) {
            for (;;) {
                if (hasQueuedPredecessors())
                    return -1;
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 ||
                    compareAndSetState(available, remaining))
                    return remaining;
            }
        }
    }

    /**
     * 创建一个 {@code Semaphore} 用给定数量的许可和非公平设置
     *
     * @param permits 允许可获得初始许可数量
     *        这个值可以是负的, 在任何授予许可后释放
     */
    public Semaphore(int permits) {
        sync = new NonfairSync(permits);
    }

    /**
     * 创建一个 {@code Semaphore} 用给定数量的许可和公平设置
     *
     * @param permits 许可数量
     *        This value may be negative, in which case releases
     *        must occur before any acquires will be granted.
     * @param fair {@code true} if this semaphore will guarantee
     *        first-in first-out granting of permits under contention,
     *        else {@code false}
     */
    public Semaphore(int permits, boolean fair) {
        sync = fair ? new FairSync(permits) : new NonfairSync(permits);
    }

    /**
     * 从这个信号量获得一个许可，阻塞直到有一个可用，或者线程被中断。
     *
     * <p>获得许可证，如果有许可证，立即返回，将可获得的许可证数量减少一个。
     *
     * <p>如果没有可用的许可证，那么当前线程将出于线程调度的目的被禁用，并处于休眠状态，直到发生以下两种情况之一:
     * <ul>
     * <li>其他一些线程调用这个信号量的release()方法，当前线程接下来被分配一个许可证;
     * <li>其他一些线程中断当前线程。
     * </ul>
     *
     * <p>如果当前线程:
     * <ul>
     * <li>在进入此方法时已设置其中断状态;
     * <li>在等待许可证时被打断，然后抛出InterruptedException，并清除当前线程的中断状态。
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * @throws InterruptedException if the current thread is interrupted
     */
    public void acquire() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    /**
     * 从这个信号量获得许可，阻塞直到有一个可用。
     *
     * <p>获得许可证，如果有许可证，立即返回，将可获得的许可证数量减少一个。
     *
     * <p>如果没有可用的许可证，那么当前线程将出于线程调度的目的而被禁用，并处于休眠状态，
     * 直到其他线程调用此信号量的release()方法，然后为当前线程分配许可证。
     *
     * <p>如果当前线程在等待许可证的过程中被中断，那么它将继续等待，
     * 但是分配给线程许可证的时间可能与没有中断的情况下它收到许可证的时间不同。
     * 当线程从这个方法返回时，它的中断状态将被设置。
     */
    public void acquireUninterruptibly() {
        sync.acquireShared(1);
    }

    /**
     * 仅在调用时可用的情况下，从该信号量获取许可证。
     *
     * <p>如果有许可证，则获得许可证并立即返回，值为true，从而将可用许可证的数量减少1。
     *
     * <p>如果没有许可证可用，那么这个方法将立即返回值false。
     *
     * <p>即使将这个信号量设置为使用公平的排序策略，如果有一个可用的许可证，
     * 那么调用tryAcquire()将立即获得许可证，不管其他线程是否正在等待。
     * 这种“冲撞”行为在某些情况下是有用的，即使它破坏了公平。如果你想遵守公平设置，
     * 那么使用tryAcquire(0, TimeUnit.SECONDS)，这几乎是等价的(它也检测中断)。
     *
     * @return {@code true} if a permit was acquired and {@code false}
     *         otherwise
     */
    public boolean tryAcquire() {
        return sync.nonfairTryAcquireShared(1) >= 0;
    }

    /**
     * 如果在给定的等待时间内可用并且当前线程没有被中断，则从该信号量获得许可。
     *
     * <p>如果有许可证，则获得许可证并立即返回，值为true，从而将可用许可证的数量减少1。
     *
     * <p>如果没有可用的许可，那么当前线程将出于线程调度的目的被禁用，并处于休眠状态，直到发生以下三种情况之一:
     * <ul>
     * <li>其他一些线程调用这个信号量的release()方法，当前线程接下来被分配一个许可证;
     * <li>其他一些线程中断当前线程;
     * <li>指定的等待时间已经过了。
     * </ul>
     *
     * <p>如果获得许可证，则返回true值。
     *
     * <p>如果当前线程:
     * <ul>
     * <li>在进入此方法时已设置其中断状态;
     * <li>在等候许可证时被打断，然后抛出InterruptedException，并清除当前线程的中断状态。
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>如果指定的等待时间过期，则返回false值。如果时间小于或等于0，则该方法根本不会等待。
     *
     * @param timeout the maximum time to wait for a permit
     * @param unit the time unit of the {@code timeout} argument
     * @return {@code true} if a permit was acquired and {@code false}
     *         if the waiting time elapsed before a permit was acquired
     * @throws InterruptedException if the current thread is interrupted
     */
    public boolean tryAcquire(long timeout, TimeUnit unit)
        throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    /**
     * 释放许可证，将其返回到信号量。
     *
     * <p>发放许可证，将可获得的许可证数量增加一份。如果有任何线程试图获得许可证，
     * 则选中一个，并给予刚刚发布的许可证。出于线程调度的目的，该线程被(重新)启用。
     *
     * <p>没有要求释放许可证的线程必须通过调用acquire()来获得许可证。
     * 信号量的正确用法是通过应用程序中的编程约定来确定的。
     */
    public void release() {
        sync.releaseShared(1);
    }

    /**
     * 从这个信号量获取给定数量的许可，阻塞直到所有的都可用，或者线程被中断。
     *
     * <p>Acquires the given number of permits, if they are available,
     * and returns immediately, reducing the number of available permits
     * by the given amount.
     *
     * <p>If insufficient permits are available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * one of two things happens:
     * <ul>
     * <li>Some other thread invokes one of the {@link #release() release}
     * methods for this semaphore, the current thread is next to be assigned
     * permits and the number of available permits satisfies this request; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * for a permit,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * Any permits that were to be assigned to this thread are instead
     * assigned to other threads trying to acquire permits, as if
     * permits had been made available by a call to {@link #release()}.
     *
     * @param permits the number of permits to acquire
     * @throws InterruptedException if the current thread is interrupted
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public void acquire(int permits) throws InterruptedException {
        if (permits < 0) throw new IllegalArgumentException();
        sync.acquireSharedInterruptibly(permits);
    }

    /**
     * 从这个信号量获得给定数量的许可，阻塞直到所有的都可用。
     *
     * <p>Acquires the given number of permits, if they are available,
     * and returns immediately, reducing the number of available permits
     * by the given amount.
     *
     * <p>If insufficient permits are available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * some other thread invokes one of the {@link #release() release}
     * methods for this semaphore, the current thread is next to be assigned
     * permits and the number of available permits satisfies this request.
     *
     * <p>If the current thread is {@linkplain Thread#interrupt interrupted}
     * while waiting for permits then it will continue to wait and its
     * position in the queue is not affected.  When the thread does return
     * from this method its interrupt status will be set.
     *
     * @param permits the number of permits to acquire
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public void acquireUninterruptibly(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        sync.acquireShared(permits);
    }

    /**
     * 仅当所有许可证在调用时可用时，才从该信号量获取给定数量的许可证。
     *
     * <p>Acquires the given number of permits, if they are available, and
     * returns immediately, with the value {@code true},
     * reducing the number of available permits by the given amount.
     *
     * <p>If insufficient permits are available then this method will return
     * immediately with the value {@code false} and the number of available
     * permits is unchanged.
     *
     * <p>Even when this semaphore has been set to use a fair ordering
     * policy, a call to {@code tryAcquire} <em>will</em>
     * immediately acquire a permit if one is available, whether or
     * not other threads are currently waiting.  This
     * &quot;barging&quot; behavior can be useful in certain
     * circumstances, even though it breaks fairness. If you want to
     * honor the fairness setting, then use {@link #tryAcquire(int,
     * long, TimeUnit) tryAcquire(permits, 0, TimeUnit.SECONDS) }
     * which is almost equivalent (it also detects interruption).
     *
     * @param permits the number of permits to acquire
     * @return {@code true} if the permits were acquired and
     *         {@code false} otherwise
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public boolean tryAcquire(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        return sync.nonfairTryAcquireShared(permits) >= 0;
    }

    /**
     * 如果在给定的等待时间内所有许可都可用且当前线程没有被中断，则从该信号量获取给定数量的许可。
     *
     * <p>Acquires the given number of permits, if they are available and
     * returns immediately, with the value {@code true},
     * reducing the number of available permits by the given amount.
     *
     * <p>If insufficient permits are available then
     * the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of three things happens:
     * <ul>
     * <li>Some other thread invokes one of the {@link #release() release}
     * methods for this semaphore, the current thread is next to be assigned
     * permits and the number of available permits satisfies this request; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>The specified waiting time elapses.
     * </ul>
     *
     * <p>If the permits are acquired then the value {@code true} is returned.
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * to acquire the permits,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * Any permits that were to be assigned to this thread, are instead
     * assigned to other threads trying to acquire permits, as if
     * the permits had been made available by a call to {@link #release()}.
     *
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.  If the time is less than or equal to zero, the method
     * will not wait at all.  Any permits that were to be assigned to this
     * thread, are instead assigned to other threads trying to acquire
     * permits, as if the permits had been made available by a call to
     * {@link #release()}.
     *
     * @param permits the number of permits to acquire
     * @param timeout the maximum time to wait for the permits
     * @param unit the time unit of the {@code timeout} argument
     * @return {@code true} if all permits were acquired and {@code false}
     *         if the waiting time elapsed before all permits were acquired
     * @throws InterruptedException if the current thread is interrupted
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit)
        throws InterruptedException {
        if (permits < 0) throw new IllegalArgumentException();
        return sync.tryAcquireSharedNanos(permits, unit.toNanos(timeout));
    }

    /**
     * 释放给定数量的许可证，将它们返回到信号量。
     *
     * <p>Releases the given number of permits, increasing the number of
     * available permits by that amount.
     * If any threads are trying to acquire permits, then one
     * is selected and given the permits that were just released.
     * If the number of available permits satisfies that thread's request
     * then that thread is (re)enabled for thread scheduling purposes;
     * otherwise the thread will wait until sufficient permits are available.
     * If there are still permits available
     * after this thread's request has been satisfied, then those permits
     * are assigned in turn to other threads trying to acquire permits.
     *
     * <p>There is no requirement that a thread that releases a permit must
     * have acquired that permit by calling {@link Semaphore#acquire acquire}.
     * Correct usage of a semaphore is established by programming convention
     * in the application.
     *
     * @param permits the number of permits to release
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public void release(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        sync.releaseShared(permits);
    }

    /**
     * 返回此信号量中可用的许可证的当前数量。
     *
     * <p>This method is typically used for debugging and testing purposes.
     *
     * @return the number of permits available in this semaphore
     */
    public int availablePermits() {
        return sync.getPermits();
    }

    /**
     * 获取并返回所有立即可用的许可证。
     *
     * @return the number of permits acquired
     */
    public int drainPermits() {
        return sync.drainPermits();
    }

    /**
     * 通过指定的减少来减少可用许可证的数量。
     * 此方法在使用信号量跟踪不可用资源的子类中非常有用。
     * 这种方法与acquire的不同之处在于它不会阻止许可证的获取。
     *
     * @param reduction the number of permits to remove
     * @throws IllegalArgumentException if {@code reduction} is negative
     */
    protected void reducePermits(int reduction) {
        if (reduction < 0) throw new IllegalArgumentException();
        sync.reducePermits(reduction);
    }

    /**
     * 如果这个信号量的公平性设置为真，则返回真。
     *
     * @return {@code true} if this semaphore has fairness set true
     */
    public boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * 查询是否有线程正在等待获取。请注意，因为取消可能随时发生，
     * 一个真正的返回并不保证任何其他线程将获得。该方法主要用于监控系统状态。
     *
     * @return {@code true} if there may be other threads waiting to
     *         acquire the lock
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * 返回等待获取的线程数量的估计值。这个值只是一个估计值，
     * 因为当这个方法遍历内部数据结构时，线程的数量可能会动态变化。
     * 此方法设计用于监视系统状态，而不是用于同步控制。
     *
     * @return the estimated number of threads waiting for this lock
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * 返回一个包含可能正在等待获取的线程的集合。因为在构造这个结果时，
     * 实际的线程集可能会动态变化，所以返回的集合只是一个最佳效果的估计。
     * 返回集合的元素没有特定的顺序。这种方法的目的是为了方便构建提供更广泛的监视设施的子类。
     *
     * @return the collection of threads
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * 返回标识这个信号量的字符串及其状态。括号中的状态包括字符串“permissions =”和许可证数量。
     *
     * @return a string identifying this semaphore, as well as its state
     */
    public String toString() {
        return super.toString() + "[Permits = " + sync.getPermits() + "]";
    }
}
