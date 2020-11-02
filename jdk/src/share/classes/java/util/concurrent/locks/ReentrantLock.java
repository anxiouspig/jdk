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
import java.util.concurrent.TimeUnit;
import java.util.Collection;

/**
 * 可重入互斥锁，具有与使用同步方法和语句访问的隐式监视器锁相同的基本行为和语义，但具有扩展功能。
 *
 * <p>ReentrantLock由最后一次成功锁定的线程拥有，但尚未解锁它。当锁不属于另一个线程时，
 * 调用锁的线程将返回，并成功获取锁。如果当前线程已经拥有锁，则该方法将立即返回。
 * 可以使用方法isHeldByCurrentThread()和getHoldCount()进行检查。
 *
 * <p>该类的构造函数接受一个可选的公平性参数。当设置为真时，在争用状态下，锁定倾向于授予对最长等待线程的访问权。
 * 否则，此锁不保证任何特定的访问顺序。使用多个线程访问的公平锁的程序可能会显示较低的总体吞吐量
 * (即更慢;通常比那些使用默认设置的要慢得多)，但是在获得锁和保证不会饿死方面的时间差异更小。
 * 但是请注意，锁的公平性并不保证线程调度的公平性。因此，使用公平锁的多个线程中的一个可能会连续多次获得它，
 * 而其他活动线程没有进展，也没有当前持有锁。还要注意，不定时的tryLock()方法不支持公平性设置。
 * 如果锁可用，即使其他线程正在等待，它也会成功。
 *
 * <p>建议的做法是，总是立即跟随一个调用来锁定一个try块，最典型的是在一个前后结构，如:
 *
 *  <pre> {@code
 * class X {
 *   private final ReentrantLock lock = new ReentrantLock();
 *   // ...
 *
 *   public void m() {
 *     lock.lock();  // block until condition holds
 *     try {
 *       // ... method body
 *     } finally {
 *       lock.unlock()
 *     }
 *   }
 * }}</pre>
 *
 * <p>除了实现锁接口之外，该类还定义了许多用于检查锁状态的公共方法和受保护方法。其中一些方法仅对检测和监视有用。
 *
 * <p>这个类的序列化与内置锁的行为方式相同:反序列化的锁处于解锁状态，而与它在序列化时的状态无关。
 *
 * <p>此锁最多支持同一线程的2147483647个递归锁。试图超过此限制将导致锁定方法抛出错误。
 *
 * @since 1.5
 * @author Doug Lea
 */
public class ReentrantLock implements Lock, java.io.Serializable {
    private static final long serialVersionUID = 7373984872572414699L;
    /** 提供所有实现功能的同步 */
    private final Sync sync;

    /**
     * 这个锁的同步控制基础。子类化为公平和非公平的版本下面。使用AQS状态表示锁上的持有数。
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -5179523762034025860L;

        /**
         * Performs {@link Lock#lock}. 子类化的主要原因是允许非公平版本的快速路径。
         */
        abstract void lock();

        /**
         * Performs non-fair tryLock.  tryAcquire 在子类实现。
         * but both need nonfair try for trylock method.
         */
        final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            // 拿到当前线程
            int c = getState();
            // 拿到锁的状态，初始化为0
            if (c == 0) {
                // 若为0，可以拥有锁
                if (compareAndSetState(0, acquires)) { // 锁状态设为acquires
                    setExclusiveOwnerThread(current); // 设置排它锁
                    return true; // 返回成功
                }
            }
            else if (current == getExclusiveOwnerThread()) { // 当前线程是不是排它锁的线程
                int nextc = c + acquires; // 新值
                if (nextc < 0) // overflow
                    throw new Error("Maximum lock count exceeded");
                setState(nextc); // 设置状态值
                return true; // 重入
            }
            return false; // 得到锁失败
        }

        // 释放锁
        protected final boolean tryRelease(int releases) {
            int c = getState() - releases; // 新值
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException(); // 当前线程不为排它锁内线程的话抛出异常
            boolean free = false;
            if (c == 0) { // 状态=0，则设置排它锁内为null
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c); // 设置状态
            return free; // 返回结果
        }

        // 是否持有排它锁
        protected final boolean isHeldExclusively() {
            // 我们一般在拥有锁之前读取状态,
            // 如果当前线程拥有锁，我们不需要去检查
            return getExclusiveOwnerThread() == Thread.currentThread();
        }
        // 条件锁
        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        // 依赖于外部类的方法

        // 当前排它锁的线程
        final Thread getOwner() {
            return getState() == 0 ? null : getExclusiveOwnerThread();
        }
        // 得到持有锁数量
        final int getHoldCount() {
            return isHeldExclusively() ? getState() : 0;
        }
        // 是否锁定状态
        final boolean isLocked() {
            return getState() != 0;
        }

        /**
         * Reconstitutes the instance from a stream (that is, deserializes it).
         */
        private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            setState(0); // reset to unlocked state
        }
    }

    /**
     * 非公平锁的同步对象
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = 7316153563782823691L;

        /**
         * 执行锁. Try immediate barge, backing up to normal
         * acquire on failure.
         */
        final void lock() { // 将state0设为1，失败的话去获取锁
            if (compareAndSetState(0, 1))
                setExclusiveOwnerThread(Thread.currentThread());
            else
                acquire(1);
        }

        protected final boolean tryAcquire(int acquires) { // 试图获取锁
            return nonfairTryAcquire(acquires);
        }
    }

    /**
     * 公平锁的同步对象
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = -3000897897090466540L;

        final void lock() { // 试图获取锁
            acquire(1);
        }

        /**
         * Fair version of tryAcquire.  Don't grant access unless
         * recursive call or no waiters or is first.
         */
        protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread(); // 拿到当前线程
            int c = getState(); // 拿到当前锁状态
            if (c == 0) { // 为0尝试获取锁
                if (!hasQueuedPredecessors() && // 是否有前任
                    compareAndSetState(0, acquires)) { // 当前线程锁上
                    setExclusiveOwnerThread(current); // 设置当前线程为排他锁线程
                    return true; // 返回获得所成功
                }
            }
            else if (current == getExclusiveOwnerThread()) { // 当前先生是否是排它锁线程
                int nextc = c + acquires;
                if (nextc < 0)
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true; // 获得锁成功
            }
            return false; // 否则获得锁失败
        }
    }

    /**
     * Creates an instance of {@code ReentrantLock}.
     * This is equivalent to using {@code ReentrantLock(false)}.
     */
    public ReentrantLock() {
        sync = new NonfairSync();
    }

    /**
     * Creates an instance of {@code ReentrantLock} with the
     * given fairness policy.
     *
     * @param fair {@code true} if this lock should use a fair ordering policy
     */
    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }

    /**
     * Acquires the lock.
     *
     * <p>如果锁不被其他线程持有，则获取锁并立即返回，将锁持有计数设置为1。
     *
     * <p>如果当前线程已经持有锁，那么持有计数将增加1，方法立即返回。
     *
     * <p>如果锁被另一个线程持有，那么当前线程将出于线程调度的目的而禁用，并处于休眠状态，
     * 直到锁被获取，此时锁持有计数被设置为1。
     */
    public void lock() {
        sync.lock();
    }

    /**
     * 除非当前线程中断，否则获取锁。
     *
     * <p>如果锁不被其他线程持有，则获取锁并立即返回，将锁持有计数设置为1。
     *
     * <p>如果当前线程已经持有该锁，那么持有计数将增加1，该方法立即返回。
     *
     * <p>如果锁是由另一个线程持有，那么当前线程就会出于线程调度的目的而被禁用，
     * 并处于休眠状态，直到发生以下两种情况之一:
     *
     * <ul>
     *
     * <li>锁被当前线程获取;
     *
     * <li>其他一些线程中断当前线程。
     *
     * </ul>
     *
     * <p>如果锁被当前线程获取，那么锁持有计数被设置为1。
     *
     * <p>如果当前线程:
     *
     * <ul>
     *
     * <li>在进入此方法时设置其中断状态;
     *
     * <li>在获取锁时中断，然后抛出InterruptedException，并清除当前线程的中断状态。
     *
     * </ul>
     *
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>在这个实现中，由于这个方法是一个显式的中断点，所以优先响应中断而不是正常的或可重入的锁获取。
     *
     * @throws InterruptedException if the current thread is interrupted
     */
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    /**
     * 仅当锁在调用时不被其他线程持有时，才获取锁。
     *
     * <p>如果锁未被其他线程持有，则获取锁，并立即返回值true，将锁持有计数设置为1。
     * 即使这个锁被设置为使用公平的排序策略，如果锁可用，调用tryLock()也会立即获得锁，
     * 不管其他线程是否正在等待锁。这种“冲撞”行为在某些情况下是有用的，即使它破坏了公平。
     * 如果您想为这个锁执行公平设置，那么使用tryLock(0, TimeUnit.SECONDS)，这几乎是等价的(它还可以检测到中断)。
     *
     * <p>如果当前线程已经持有该锁，那么持有计数将增加1，方法返回true。
     *
     * <p>如果锁被另一个线程持有，那么这个方法将立即返回值false。
     *
     * @return {@code true} if the lock was free and was acquired by the
     *         current thread, or the lock was already held by the current
     *         thread; and {@code false} otherwise
     */
    public boolean tryLock() {
        return sync.nonfairTryAcquire(1);
    }

    /**
     * 如果在给定的等待时间内没有被其他线程持有，并且当前线程没有被中断，则获取锁。
     *
     * <p>如果锁未被其他线程持有，则获取锁，并立即返回值true，将锁持有计数设置为1。
     * 如果这个锁被设置为使用公平的排序策略，那么如果任何其他线程正在等待这个锁，那么将不会获得一个可用的锁。
     * 这与tryLock()方法相反。如果你想要一个定时的tryLock，允许对一个公平的锁，
     * 然后结合在一起的时间和不定时的形式:
     *
     *  <pre> {@code
     * if (lock.tryLock() ||
     *     lock.tryLock(timeout, unit)) {
     *   ...
     * }}</pre>
     *
     * <p>如果当前线程已经持有该锁，那么持有计数将增加1，方法返回true。
     *
     * <p>如果锁是由另一个线程持有，那么当前线程就会出于线程调度的目的而被禁用，
     * 并处于休眠状态，直到发生以下三种情况之一:
     *
     * <ul>
     *
     * <li>锁被当前线程获取;
     *
     * <li>其他一些线程中断当前线程;
     *
     * <li>指定的等待时间已经过了
     *
     * </ul>
     *
     * <p>如果获取了锁，则返回true值，并将锁持有计数设置为1。
     *
     * <p>如果当前线程:
     *
     * <ul>
     *
     * <li>在进入此方法时已设置其中断状态;
     *
     * <li>在获取锁时中断，然后抛出InterruptedException，并清除当前线程的中断状态。
     *
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>如果指定的等待时间过期，则返回false值。如果时间小于或等于0，则该方法根本不会等待。
     *
     * <p>在这个实现中，由于这个方法是一个显式的中断点，所以优先响应中断，而不是正常的或可重入的获取锁，
     * 或者报告等待时间的流逝。
     *
     * @param timeout the time to wait for the lock
     * @param unit the time unit of the timeout argument
     * @return {@code true} 如果锁是空闲的并且被当前线程获取，或者锁已经被当前线程持有，则为true;
     * 如果在获取锁之前的等待时间已经过去，则为false
     * @throws InterruptedException if the current thread is interrupted
     * @throws NullPointerException if the time unit is null
     */
    public boolean tryLock(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }

    /**
     * 试图释放此锁。
     *
     * <p>如果当前线程是这个锁的持有者，那么持有计数将递减。如果持有计数现在为零，则释放锁。
     * 如果当前线程不是这个锁的持有者，那么抛出IllegalMonitorStateException。
     *
     * @throws IllegalMonitorStateException if the current thread does not
     *         hold this lock
     */
    public void unlock() {
        sync.release(1);
    }

    /**
     * 返回用于此锁实例的条件实例。
     *
     * <p>当与内置的监视器锁一起使用时，返回的条件实例支持与对象监视器方法(wait、notify和notifyAll)相同的用法。
     *
     * <ul>
     *
     * <li>如果在调用任何条件等待或发送信号方法时未持有此锁，则抛出IllegalMonitorStateException。
     *
     * <li>当条件等待方法被调用时，锁被释放，在它们返回之前，锁被重新获得，锁持有计数恢复到方法被调用时的值。
     *
     * <li>如果线程在等待期间中断，那么等待将终止，抛出InterruptedException，并清除线程的中断状态。
     *
     * <li> 等待线程按FIFO顺序发出信号。
     *
     * <li>从等待方法返回的线程的锁重获顺序与最初获取锁的线程相同(在默认情况下未指定)，但对于公平锁，
     * 优先使用那些等待时间最长的线程。
     *
     * </ul>
     *
     * @return the Condition object
     */
    public Condition newCondition() {
        return sync.newCondition();
    }

    /**
     * 查询当前线程持有此锁的次数。
     *
     * <p>一个线程对每个锁操作都持有一个锁，而每个锁操作都与一个解锁操作不匹配。
     *
     * <p>hold count信息通常仅用于测试和调试目的。例如，如果一个特定的代码段不应该与已经持有的锁一起输入，
     * 那么我们可以断言这个事实:
     *
     *  <pre> {@code
     * class X {
     *   ReentrantLock lock = new ReentrantLock();
     *   // ...
     *   public void m() {
     *     assert lock.getHoldCount() == 0;
     *     lock.lock();
     *     try {
     *       // ... method body
     *     } finally {
     *       lock.unlock();
     *     }
     *   }
     * }}</pre>
     *
     * @return the number of holds on this lock by the current thread,
     *         or zero if this lock is not held by the current thread
     */
    public int getHoldCount() {
        return sync.getHoldCount();
    }

    /**
     * 查询当前线程是否持有此锁。
     *
     * <p>与用于内置监视器锁的Thread.holdsLock(Object)方法类似，此方法通常用于调试和测试。
     * 例如，只有在锁被持有时才应该调用的方法可以断言:
     *
     *  <pre> {@code
     * class X {
     *   ReentrantLock lock = new ReentrantLock();
     *   // ...
     *
     *   public void m() {
     *       assert lock.isHeldByCurrentThread();
     *       // ... method body
     *   }
     * }}</pre>
     *
     * <p>它也可以用来确保可重入锁以不可重入的方式使用，例如:
     *
     *  <pre> {@code
     * class X {
     *   ReentrantLock lock = new ReentrantLock();
     *   // ...
     *
     *   public void m() {
     *       assert !lock.isHeldByCurrentThread();
     *       lock.lock();
     *       try {
     *           // ... method body
     *       } finally {
     *           lock.unlock();
     *       }
     *   }
     * }}</pre>
     *
     * @return {@code true} if current thread holds this lock and
     *         {@code false} otherwise
     */
    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /**
     * 查询此锁是否由任何线程持有。此方法设计用于监视系统状态，而不是用于同步控制。
     *
     * @return {@code true} if any thread holds this lock and
     *         {@code false} otherwise
     */
    public boolean isLocked() {
        return sync.isLocked();
    }

    /**
     * 如果此锁的公平性设置为真，则返回真。
     *
     * @return {@code true} if this lock has fairness set true
     */
    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * 返回当前拥有此锁的线程，如果不拥有则返回null。当非所有者的线程调用此方法时，
     * 返回值反映当前锁状态的最佳状态近似值。例如，所有者可能暂时为空，
     * 即使有线程试图获取锁，但尚未这样做。此方法的目的是为了方便构造提供更广泛的锁监视设施的子类。
     *
     * @return the owner, or {@code null} if not owned
     */
    protected Thread getOwner() {
        return sync.getOwner();
    }

    /**
     * 查询是否有线程正在等待获取此锁。注意，因为取消可能在任何时候发生，
     * 一个真正的返回并不保证任何其他线程将获得这个锁。该方法主要用于监控系统状态。
     *
     * @return {@code true} if there may be other threads waiting to
     *         acquire the lock
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * 查询给定线程是否正在等待获取此锁。注意，因为取消可能随时发生，所以真正的返回并不保证这个线程将获得这个锁。
     * 该方法主要用于监控系统状态。
     *
     * @param thread the thread
     * @return {@code true} if the given thread is queued waiting for this lock
     * @throws NullPointerException if the thread is null
     */
    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }

    /**
     * 返回等待获取此锁的线程数量的估计值。这个值只是一个估计值，因为当这个方法遍历内部数据结构时，
     * 线程的数量可能会动态变化。此方法设计用于监视系统状态，而不是用于同步控制。
     *
     * @return the estimated number of threads waiting for this lock
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * 返回一个包含可能正在等待获取此锁的线程的集合。因为在构造这个结果时，实际的线程集可能会动态变化，
     * 所以返回的集合只是一个最佳效果的估计。返回集合的元素没有特定的顺序。
     * 这种方法的目的是为了方便构建提供更广泛的监视设施的子类。
     *
     * @return the collection of threads
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * 查询是否有线程正在等待与此锁关联的给定条件。注意，因为超时和中断可能随时发生，
     * 所以真正的返回并不保证将来的信号将唤醒任何线程。该方法主要用于监控系统状态。
     *
     * @param condition the condition
     * @return 如果有任何正在等待的线程，则为真
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    public boolean hasWaiters(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * 返回与此锁关联的给定条件下等待的线程数的估计值。
     * 请注意，由于超时和中断可能随时发生，因此估计值仅作为实际等待者数量的上限。
     * 此方法设计用于监视系统状态，而不是用于同步控制。
     *
     * @param condition the condition
     * @return the estimated number of waiting threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    public int getWaitQueueLength(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * 返回一个集合，其中包含可能正在等待与此锁关联的给定条件的线程。因为在构造这个结果时，
     * 实际的线程集可能会动态变化，所以返回的集合只是一个最佳效果的估计。
     * 返回集合的元素没有特定的顺序。这种方法的目的是为了方便构建提供更广泛的状态监视设施的子类。
     *
     * @param condition the condition
     * @return the collection of threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * 返回标识此锁的字符串及其锁状态。括号中的状态包括字符串“解锁”或字符串“Locked by”，后跟所属线程的名称。
     *
     * @return a string identifying this lock, as well as its lock state
     */
    public String toString() {
        Thread o = sync.getOwner();
        return super.toString() + ((o == null) ?
                                   "[Unlocked]" :
                                   "[Locked by thread " + o.getName() + "]");
    }
}
